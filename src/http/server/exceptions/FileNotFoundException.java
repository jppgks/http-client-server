package http.server.exceptions;

public class FileNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8073140546677422575L;

	public String getHtmlBody() {
		return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>404 - Not Found</title></head><body><h1>404 - Not Found</h1><p>The page you requested cannot be found on this server.</p></body></html>";
	}

	public int getStatusCode() {
		return 404;
	}
}
