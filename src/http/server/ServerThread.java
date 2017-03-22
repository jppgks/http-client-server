package http.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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
			System.out.println("SERVERTHREAD - Connection closed");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Request readRequest() throws BadRequestException, SocketTimeoutException, SocketException {
		Request request = null;

		String firstLine = readLine();
		if (Arrays.stream(Method.values()).noneMatch(e -> e.getName().equals(firstLine.split(" ")[0]))) {
			// HTTP Method not supported
			throw new BadRequestException();
		}
		Method method = Method.valueOf(firstLine.split(" ")[0]);
		String file = firstLine.split(" ")[1];
		String httpVersion = firstLine.split(" ")[2];

		HashMap<String, String> headers = readHeaders();

		if (method == Method.POST || method == Method.PUT) {
			// Read message
			byte[] body = readMessage(headers);
			// Read (optional) footers
			if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
				HashMap<String, String> footers = readHeaders();
				headers.putAll(footers);
			}
			request = new Request(method, file, httpVersion, headers, body);
		} else {
			request = new Request(method, file, httpVersion, headers);
		}
		return request;
	}

	private Response handle(Request request) throws ServerException {
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
				path = Paths.get(HttpServer.path + request.getFile() + "index.html");
			} else {
				path = Paths.get(HttpServer.path + request.getFile());
			}

			if (Files.exists(path) && Files.isRegularFile(path)) {
				try {
					headers.put("Content-Type", Files.probeContentType(path));
					// Check if page is modified since time given in header
					// (if given)
					String since = request.getHeaders().getOrDefault("If-Modified-Since", null);
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
	private boolean fileIsModified(Path path, String since) throws BadRequestException {
		DateFormat df = new SimpleDateFormat();
		Date sinceDate;
		try {
			sinceDate = df.parse(since);
		} catch (ParseException e) {
			throw new BadRequestException();
		}
		Date lastModified = new Date(path.toFile().lastModified());
		return lastModified.after(sinceDate);
	}

	private void send(Response response) throws IOException {
		outToClient.writeBytes(response.getStatusLine() + "\r\n");
		for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
			outToClient.writeBytes(entry.getKey() + ": " + entry.getValue() + "\r\n");
		}
		outToClient.writeBytes("\r\n");

		if (response.getBody() != null) {
			outToClient.write(response.getBody());
		}
	}

	private HashMap<String, String> readHeaders() throws SocketTimeoutException, SocketException {
		HashMap<String, String> headers = new HashMap<>();

		String header = null;
		String value = null;
		String line;
		boolean stop = false;
		while (!stop) {
			line = readLine();
			if (line.startsWith(" ") || line.startsWith("\t")) {
				// lines beginning with spaces or tabs belong to the previous
				// header line
				line = line.trim();
				value.concat(line);
			} else {
				// put last header + value in map
				if (header != null) {
					headers.put(header, value);
				}

				if (line.isEmpty()) {
					stop = true;
				} else {
					// read new header
					header = line.substring(0, line.indexOf(":"));
					value = line.substring(line.indexOf(":") + 1).trim();
				}
			}
		}
		return headers;
	}

	private byte[] readMessage(HashMap<String, String> headers) {
		boolean chunkedTE = headers.containsKey("Transfer-Encoding")
				&& "chunked".equals(headers.get("Transfer-Encoding"));
		int size;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			if (chunkedTE) {
				// Read chunked message
				boolean stop = false;
				while (!stop) {
					// read line with chunk size
					String line = readLine();
					if (line.isEmpty()) {
						line = readLine();
					}
					if (line.contains(";")) {
						size = Integer.parseInt(line.substring(0, line.indexOf(";")), 16);
					} else {
						size = Integer.parseInt(line, 16);
					}

					if (size == 0) {
						stop = true;
					}
					// read chunk
					stream.write(readBytes(size));
				}
			} else {
				if (headers.containsKey("Content-Length")) {
					size = Integer.parseInt(headers.get("Content-Length"));
					// read number of bytes specified by Content-Length
					stream.write(readBytes(size));
				} else {
					// read to the end (until the connection is closed)
					// read all bytes one by one
					byte[] bt;
					while ((bt = readBytes(1)) != null) {
						stream.write(bt);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return stream.toByteArray();
	}

	private String readLine() throws SocketException, SocketTimeoutException {
		StringBuilder sb = new StringBuilder();
		while (sb.lastIndexOf("\r\n") == -1) {
			try {
				int ch = inFromClient.read();
				sb.append((char) ch);
			} catch (SocketTimeoutException | SocketException e) {
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String line = sb.toString();
		// remove line-end characters at the end
		line = line.substring(0, line.length() - 2);

		return line;
	}

	private byte[] readBytes(int number) throws SocketTimeoutException {
		byte[] data = new byte[number];
		int bytesRead = 0;
		int newRead;
		try {
			while (bytesRead < number) {
				if (inFromClient.available() > 0) {

				}
				newRead = inFromClient.read(data, bytesRead, number - bytesRead);
				if (newRead == -1) {
					if (bytesRead == 0) {
						return null;
					} else {
						return Arrays.copyOf(data, bytesRead);
					}
				} else {
					bytesRead += newRead;
				}
			}
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}
}
