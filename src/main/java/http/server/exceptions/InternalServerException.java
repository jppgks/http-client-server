package http.server.exceptions;

public class InternalServerException extends ServerException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4111127047161094389L;

	public String getHtmlBody() {
		return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>500 - Internal Server Error</title></head><body><h1>500 - Internal Server Error</h1><p>The server experienced some issues.</p></body></html>";
	}
	
	public int getStatusCode() {
		return 500;
	}
}
