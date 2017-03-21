package http.server;

import http.Method;
import http.server.exceptions.BadRequestException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;

public class Request {
	private Method method;
	private String file;
	private String httpVersion;
	
	private HashMap<String, String> headers;
	
	private byte[] message;
	
	Request(Method method, String file, String httpVersion, HashMap<String, String> headers) throws BadRequestException {
		this(method, file, httpVersion, headers, null);
	}
	
	Request(Method method, String file, String httpVersion, HashMap<String, String> headers, byte[] message) throws BadRequestException {
		if ((method == Method.POST || method == Method.PUT) && message == null) {
			throw new BadRequestException();
		}
		
		if (httpVersion.equals("HTTP/1.1")) {
			// check if host header is present
			if ((! headers.containsKey("Host"))) {
				throw new BadRequestException();
			}
		}
		
		this.method = method;
		this.file = file;
		this.httpVersion = httpVersion;
		this.headers = headers;
		this.message = message;
	}

	Method getMethod() {
		return method;
	}

	String getFile() {
		return file;
	}

	String getHttpVersion() {
		return httpVersion;
	}

	HashMap<String, String> getHeaders() {
		return headers;
	}

	void saveMessage() {
		assert message != null : "SERVERTHREAD - Message attempted to store was null";

		String path = "output/" + new Date().getTime() + "/";
		File file = new File(path + "request_message.txt");
		// Create new file
		try {
			file.getParentFile().mkdir();
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Write request message to file
		try {
			Files.write(file.toPath(), message);
			System.out.println("Request message written to: " + file.getPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		if (getMethod() == Method.GET || getMethod() == Method.HEAD) {
			return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders();
		} else {
			return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders()
			+ "\n\n" + new String(this.message);
		}
	}
}
