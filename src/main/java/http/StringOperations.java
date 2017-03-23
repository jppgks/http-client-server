package http;

public final class StringOperations {

	/**
	 * Checks for a given path whether it is relative or not.
	 * 
	 * @param path
	 * @return boolean that indicates whether the given path is relative or not
	 */
	public static boolean isRelativePath(String path) {
		return !(path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//"));
	}
	
	
	/**
	 * Escapes a given String for use in JSON
	 * @param in
	 * @return
	 */
	public static String jsonEscape(String in) {
    	return in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
	
}
