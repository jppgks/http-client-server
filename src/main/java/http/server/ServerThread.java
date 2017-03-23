package http.server;

import static http.IO.readHeaders;
import static http.IO.readLine;
import static http.IO.readMessage;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import http.Method;
import http.server.exceptions.BadRequestException;
import http.server.exceptions.FileNotFoundException;
import http.server.exceptions.InternalServerException;
import http.server.exceptions.ServerException;

public class ServerThread implements Runnable {

	private Socket socket;
	private DataOutputStream outToClient;
	private BufferedInputStream inFromClient;
	private boolean closed;

	ServerThread(Socket socket) throws IOException {
		System.out.println("SERVERTHREAD - Connected");
		this.socket = socket;

		outToClient = new DataOutputStream(socket.getOutputStream());
		inFromClient = new BufferedInputStream(socket.getInputStream());
	}

	@Override
	public void run() {
		try {
			socket.setSoTimeout(10000);
			Request request = null;
			while (!closed) {
				try {
					// Read request
					request = readRequest();
				} catch (SocketTimeoutException | SocketException e) {
					// Close connection when timed out
					closed = true;
					break;
				} catch (ServerException e) {
					// catch ServerException and send error page
					String body = e.getHtmlBody();
					HashMap<String, String> headers = new HashMap<>();
					headers.put("Content-Type", "text/html");
					send(new Response(e.getStatusCode(), headers, body.getBytes(), request.getHttpVersion()));
				}

				if (request == null) {
					// timeout: break the while loop
					closed = true;
					break;
				}

				// Print request to standard output
				System.out.println(request.toString());
				// Handle request and send response back to client
				try {
					Response response = handle(request);
					send(response);
				} catch (ServerException e) {
					HashMap<String, String> headers = new HashMap<>();
					headers.put("Content-Type", "text/html");
					send(new Response(e.getStatusCode(), headers, e.getHtmlBody().getBytes(),
							request.getHttpVersion()));
				}
				// Check if this is the last request from the client
				if ((request.getHeaders().containsKey("Connection")
						&& request.getHeaders().get("Connection").equals("Close"))
						|| request.getHttpVersion().equals("HTTP/1.0")) {
					// set closed to true to break the while loop
					closed = true;
				}
			}

			// close connection
			socket.close();
			outToClient.close();
			inFromClient.close();
			System.out.println("SERVERTHREAD - Connection closed");
		} catch (IOException | NullPointerException e) {
			try {
				socket.close();
				outToClient.close();
				inFromClient.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

	}

	private Request readRequest() throws ServerException, SocketTimeoutException, SocketException {
		Request request;

		String firstLine = readLine(inFromClient);
		if (Arrays.stream(Method.values()).noneMatch(e -> e.getName().equals(firstLine.split(" ")[0]))) {
			// HTTP Method not supported
			throw new BadRequestException();
		}
		Method method = Method.valueOf(firstLine.split(" ")[0]);
		String file = firstLine.split(" ")[1];
		String httpVersion = firstLine.split(" ")[2];

		if (httpVersion.equals("HTTP/1.1")) {
			try {
				// Send "100 Continue" response
				send(new Response());
			} catch (IOException e) {
				throw new InternalServerException();
			}
		}

		HashMap<String, String> headers = readHeaders(inFromClient);

		if (method == Method.POST || method == Method.PUT) {
			// Read message
			byte[] body = readMessage(inFromClient, headers);
			// Read (optional) footers
			if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
				HashMap<String, String> footers = readHeaders(inFromClient);
				headers.putAll(footers);
			}
			request = new Request(method, file, httpVersion, headers, body);
		} else {
			request = new Request(method, file, httpVersion, headers);
		}
		return request;
	}

	Response handle(Request request) throws ServerException {
		String httpVersion = request.getHttpVersion();
		byte[] message;
		Response response;
		HashMap<String, String> headers = new HashMap<>();

		if (request.getHeaders().containsKey("Connection") && request.getHeaders().get("Connection").equals("close")) {
			headers.put("Connection", "close");
		}

		if (request.getMethod() == Method.GET || request.getMethod() == Method.HEAD) {
			// read file
			Path path;
			if (request.getFile().endsWith("/")) {
				path = Paths.get(HttpServer.getPath() + request.getFile() + "index.html");
			} else {
				path = Paths.get(HttpServer.getPath() + request.getFile());
			}

			if (Files.exists(path) && Files.isRegularFile(path)) {
				try {
					headers.put("Content-Type", Files.probeContentType(path));
					// Check if page is modified since time given in header
					// (if given)
					String since = request.getHeaders().get("If-Modified-Since");
					if (since == null || fileIsModified(path, since)) {
						// Page was modified since time given in header, or
						// no If-Modified-Since in header
						if (request.getMethod() == Method.HEAD) {
							headers.put("Content-Length", Long.toString(Files.size(path)));
							response = new Response(200, headers, httpVersion);
						} else {
							message = Files.readAllBytes(path);
							response = new Response(200, headers, message, httpVersion);
						}
					} else {
						// File wasn't modified
						headers.put("Content-Length", Long.toString(Files.size(path)));
						response = new Response(304, headers, httpVersion);
					}
				} catch (IOException e) {
					throw new InternalServerException();
				}
			} else {
				throw new FileNotFoundException();
			}
		} else {
			// Save message on PUT or POST
			String json = request.saveMessage();
			// Construct response
			headers.put("Content-Type", "application/json");
			response = new Response(200, headers, json.getBytes(), httpVersion);
		}
		// success
		return response;
	}

	/**
	 * Checks if the given file has been modified since the given date.
	 *
	 * @param path
	 *            The file to check modification date for.
	 * @param since
	 *            The time to compare against.
	 * @return {@code true} if modified since, {@code false} otherwise.
	 * @throws BadRequestException
	 */
	boolean fileIsModified(Path path, String since) throws BadRequestException {
		System.out.println("Since " + since);
		// 3 possible formats
		// rfc1123
		DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		Date sinceDate;
		try {
			sinceDate = df.parse(since);
		} catch (ParseException e) {
			try {
				// rfc850
				df = new SimpleDateFormat("EEEEEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH);
				sinceDate = df.parse(since);
			} catch (ParseException e1) {
				try {
					// asctime with one day character
					df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
					sinceDate = df.parse(since);
				} catch (ParseException e2) {
					throw new BadRequestException();
				}
			}
		}
		Date lastModified = new Date(path.toFile().lastModified());
		return lastModified.after(sinceDate);
	}

	private void send(Response response) throws IOException {
		// Write status line
		outToClient.writeBytes(response.getStatusLine() + "\r\n");
		// Write headers
		if (response.getHeaders() != null) {
			for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
				outToClient.writeBytes(entry.getKey() + ": " + entry.getValue() + "\r\n");
			}
		}
		// Write newline
		outToClient.writeBytes("\r\n");
		// Write body
		if (response.getBody() != null) {
			outToClient.write(response.getBody());
		}
	}
}
