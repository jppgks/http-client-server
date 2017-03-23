package http.client;

import static http.IO.readHeaders;
import static http.IO.readLine;
import static http.IO.readMessage;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import http.Method;

class Connection {
	private final String host;
	private final int port;
	private Socket clientSocket;
	private DataOutputStream outToServer;
	private BufferedInputStream inFromServer;
	private boolean closed = false;
	private int nbRedirects = 0;

	Connection(String host, int port) {
		this.host = host;
		this.port = port;

		initialize();
	}

	void initialize() {
		try {
			clientSocket = new Socket(getHost(), getPort());
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	Response execute(Request request) throws IOException {
		// Check if hosts and port of request matches these of the connection
		if (!(this.getHost().equals(request.getHost()) && this.getPort() == request.getPort())) {
			this.close();
			Connection connection = new Connection(request.getHost(), request.getPort());
			return connection.execute(request);
		}

		// Write initial line and header
		outToServer.writeBytes(request.getInitialLineAndHeader());

		// Write body (if PUT or POST)
		if (request.getMethod() == Method.PUT || request.getMethod() == Method.POST) {
			outToServer.writeBytes(request.getBody());
		}

		// Generate response
		int statusCode = Integer.parseInt(readLine(inFromServer).split(" ")[1]);
		// Process status code 100
		if (statusCode == 100) {
			// Read empty newline
			readLine(inFromServer);
			// Print status
			System.out.println("CONNECTION - Response with status code 100 received. Continuing...");
			System.out.println();
			// Read new statuscode
			statusCode = Integer.parseInt(readLine(inFromServer).split(" ")[1]);
		}
		// Continue with response
		HashMap<String, String> headers = readHeaders(inFromServer);
		Response response;
		if (request.getMethod() != Method.HEAD) {
			byte[] body = readMessage(inFromServer, headers);
			if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
				// read (optional) footers
				HashMap<String, String> footers = readHeaders(inFromServer);
				headers.putAll(footers);
			}
			response = new Response(statusCode, headers, body, request.getHost(), request.getPort(), request.getFile());
		} else {
			response = new Response(statusCode, headers, request.getHost(), request.getPort(), request.getFile());
		}

		// Redirect if needed
		if (String.valueOf(response.getStatusCode()).charAt(0) == '3'
				&& response.getHeaders().containsKey("Location")) {
			if (nbRedirects > 10) {
				// break endless loops of redirects
				return response;
			} else {
				nbRedirects++;
				System.out.println("CONNECTION - Page moved, redirecting to new location.\n");
				// Generate new request
				Request newRequest = new Request(request.getMethod(), response.getRedirectLocation());

				// only execute new request when host and file are not the same
				// as the current request
				if (newRequest.getHost().equals(request.getHost()) && newRequest.getFile().equals(request.getFile())) {
					return response;
				}

				// Execute new request and make sure to return that response
				response = this.execute(newRequest);
			}
		} else {
			// reset nbRedirects count
			nbRedirects = 0;
		}

		if (headers.containsKey("Connection") && headers.get("Connection").equals("close")) {
			this.closed = true;
			close();
		}

		return response;
	}

	void close() {
		try {
			outToServer.close();
			inFromServer.close();
			clientSocket.close();
			System.out.println("CONNECTION - Connection to " + getHost() + " at port " + getPort() + " closed.\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String getHost() {
		return this.host;
	}

	int getPort() {
		return this.port;
	}

	boolean isClosed() {
		return this.closed;
	}
}