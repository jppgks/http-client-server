package http.server.exceptions;

public class FileNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8073140546677422575L;

	public int getStatusCode() {
		return 404;
	}
}
