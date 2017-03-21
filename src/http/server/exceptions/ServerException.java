package http.server.exceptions;

public abstract class ServerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8779074122387479047L;
	
	public abstract String getHtmlBody();
	public abstract int getStatusCode();

}
