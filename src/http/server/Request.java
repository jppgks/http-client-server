package http.server;

import http.Method;
import http.server.exceptions.BadRequestException;

import java.util.HashMap;

public class Request {
	private Method method;
	private String file;
	private String httpVersion;
	
	private HashMap<String, String> headers;
	
	private byte[] message;
	
	public Request(Method method, String file, String httpVersion, HashMap<String, String> headers) throws BadRequestException {
		this(method, file, httpVersion, headers, null);
	}
	
	public Request(Method method, String file, String httpVersion, HashMap<String, String> headers, byte[] message) throws BadRequestException {
		if (method == Method.POST || method == Method.PUT) {
			throw new BadRequestException();
		}
		
		this.method = method;
		this.file = file;
		this.httpVersion = httpVersion;
		this.headers = headers;
		this.message = message;
	}

	public Method getMethod() {
		return method;
	}

	public String getFile() {
		return file;
	}

	public String getHttpVersion() {
		return httpVersion;
	}

	public HashMap<String, String> getHeaders() {
		return headers;
	}

	public byte[] getMessage() {
		return message;
	}

	@Override
	public String toString() {
		if (getMethod() == Method.GET || getMethod() == Method.HEAD) {
			return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders();
		} else {
			return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders()
			+ "\n\n" + new String(this.getMessage());
		}
	}
}
