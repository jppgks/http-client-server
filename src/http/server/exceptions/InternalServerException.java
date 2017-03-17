package http.server.exceptions;

public class InternalServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4111127047161094389L;

	
	public int getStatusCode() {
		return 500;
	}
}
