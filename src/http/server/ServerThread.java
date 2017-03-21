package http.server;

import http.Method;
import http.server.exceptions.BadRequestException;
import http.server.exceptions.FileNotFoundException;
import http.server.exceptions.InternalServerException;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
					request = readRequest();
				} catch (SocketTimeoutException e) {
					closed = true;
				} catch (BadRequestException e) {
					String body = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>400 - Bad Request</title></head><body><h1>400 - Bad Request</h1><p>The HTTP request sent was not valid.</p></body></html>";
					HashMap<String, String> headers = new HashMap<>();
					headers.put("Content-Type", "text/html");
					send(new Response(400, headers, body.getBytes(), request.getHttpVersion()));
				} catch (SocketException e) {
					closed = true;
					break;
				}
				if (request == null) {
					// timeout: break the while loop
					closed = true;
				} else {
					// handle request
					System.out.println(request.toString());
					try {
						Response response = handle(request);
						send(response);
					} catch (FileNotFoundException e) {
						String body = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>404 - Not Found</title></head><body><h1>404 - Not Found</h1><p>The page you requested cannot be found on this server.</p></body></html>";
						HashMap<String, String> headers = new HashMap<>();
						headers.put("Content-Type", "text/html");
						send(new Response(404, headers, body.getBytes(), request.getHttpVersion()));
					} catch (InternalServerException e) {
						String body = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>500 - Internal Server Error</title></head><body><h1>500 - Internal Server Error</h1><p>The server experienced some issues.</p></body></html>";
						HashMap<String, String> headers = new HashMap<>();
						headers.put("Content-Type", "text/html");
						send(new Response(500, headers, body.getBytes(), request.getHttpVersion()));
					}
					if ((request.getHeaders().containsKey("Connection")
							&& request.getHeaders().get("Connection").equals("Close"))
							|| request.getHttpVersion().equals("HTTP/1.0")) {
						// set closed to true to break the while loop
						closed = true;
					}
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

	private Response handle(Request request) throws FileNotFoundException, InternalServerException {
		String httpVersion = request.getHttpVersion();
		byte[] message;
		Response response = null;
		HashMap<String, String> headers = new HashMap<>();

		if (request.getHeaders().containsKey("Connection") && request.getHeaders().get("Connection").equals("close")) {
			headers.put("Connection", "close");
		}

		if (request.getMethod() == Method.GET || request.getMethod() == Method.HEAD) {
			// TODO: check if page is modified since last time (304)
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
					if (request.getMethod() == Method.HEAD) {
						response = new Response(200, headers, httpVersion);
					} else {
						message = Files.readAllBytes(path);
						response = new Response(200, headers, message, httpVersion);
					}
				} catch (IOException e) {
					throw new InternalServerException();
				}
			} else {
				throw new FileNotFoundException();
			}
		} else {
			// Save message on PUT or POST
			request.saveMessage();
			// Construct response
            headers.put("Content-Type", "text/plain");
            String successMsg = "Succesfully posted request body.";
            response = new Response(200, headers, successMsg.getBytes(), httpVersion);
		}
		// success
		return response;
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
		String line = new String();
		while (!line.contains("\r\n")) {
			try {
				int ch = inFromClient.read();
				line += (char) ch;
			} catch (SocketTimeoutException | SocketException e) {
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
