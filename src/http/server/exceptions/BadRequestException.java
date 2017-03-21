package http.server.exceptions;

public class BadRequestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1925690000113783651L;

	public String getHtmlBody() {
		return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>400 - Bad Request</title></head><body><h1>400 - Bad Request</h1><p>The HTTP request sent was not valid.</p></body></html>";
	}

	public int getStatusCode() {
		return 400;
	}
}
