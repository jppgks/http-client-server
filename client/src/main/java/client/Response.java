package client;

import util.Method;
import util.StringOperations;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores relevant response attributes.
 */
class Response {

	private int statusCode;
	private HashMap<String, String> headers;
	private byte[] body;
	private String name;
	private String host;
	private int port;

	Response(int statusCode, HashMap<String, String> headers, byte[] body, String host, int port, String name)
			throws IOException {
		this.statusCode = statusCode;
		this.headers = headers;
		this.body = body;
		setName(name);
		this.host = host;
		this.port = port;
	}

	Response(int statusCode, HashMap<String, String> headers, String host, int port, String name) {
		this.statusCode = statusCode;
		this.headers = headers;
		setName(name);
		this.host = host;
		this.port = port;
	}

	HashMap<String, String> getHeaders() {
		return this.headers;
	}

	int getStatusCode() {
		return statusCode;
	}

	byte[] getBody() {
		return body;
	}

	String getName() {
		return this.name;
	}

	/**
	 * Delete characters that are not allowed in the filename and adds an
	 * extension if the file has no extension
	 * 
	 * @param name
	 */
	private void setName(String name) {
		if (name.contains("?")) {
			// take part before ?
			name = name.substring(0, name.indexOf("?"));
		}

		if (name.contains("#")) {
			// take part before #
			name = name.substring(0, name.indexOf("#"));
		}

		if (name.endsWith("/")) {
			name += "index." + getExtension();
		}

		if (!name.contains(".")) {
			// name has no extension
			name += "." + getExtension();
		}
		try {
			this.name = java.net.URLDecoder.decode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Retrieves other objects on the page and creates a client.Request for them
	 */
	HashSet<Request> handle() {
		HashSet<Request> requests = new HashSet<>();
		if (body != null && getHeaders().containsKey("Content-Type") && getHeaders().get("Content-Type").contains("text/html")) {
			// Only retrieve other objects embedded in an HTML file
			// retrieve objects of the pattern <... src="<location>" ...>
			String pattern = "<.*? src=\"(.*?)\".*?>";
			Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
			Matcher m = r.matcher(new String(getBody()));
			requests.addAll(findMatches(m));

			// retrieve CSS and other resources in link tags
			pattern = "<link .*?href=\"(.*?)\".*?";
			r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
			m = r.matcher(new String(getBody()));
			requests.addAll(findMatches(m));
		}
		return requests;
	}

	/**
	 * Find matches and return the Requests for these resources
	 * 
	 * @param m
	 *            Matcher that will be used to find matches
	 * @return HashSet with Requests for which matches were found
	 */
	private HashSet<Request> findMatches(Matcher m) {
		HashSet<Request> requests = new HashSet<>();
		while (m.find()) {
			// create new request for each resource
			String path = m.group(1);
			System.out.println(path);
			if (StringOperations.isRelativePath(path)) {
				// request on the same host
				if (!path.startsWith("/")) {
					// subdirectory of the current directory
					String newPath = getName().substring(0, getName().lastIndexOf("/"));
					path = newPath + "/" + path;
				}
				requests.add(new Request(Method.GET, host, port, path));
			} else {
				// remove protocol (if present)
				if (path.startsWith("http://")) {
					path = path.substring("http://".length());
				} else if (path.startsWith("https://")) {
					path = path.substring("https://".length());
				} else if (path.startsWith("//")) {
					path = path.substring("//".length());
				}

				String host;
				String file;
				if (path.contains("/")) {
					host = path.substring(0, path.indexOf("/"));
					file = path.substring(path.indexOf("/"));
				} else {
					host = path;
					file = "/";
				}

				requests.add(new Request(Method.GET, host, 80, file));
			}
		}
		return requests;
	}

	/**
	 * Save the client.Response on disk at the given path. Non-existent files and
	 * directories will be created
	 * 
	 * @param path
	 *            Path to the place where the file will be saved
	 */
	void save(String path) {
		try {
			path = java.net.URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (body != null && body.length > 0) {
			File file = new File(path + getName());
			// Show message if file already exists
			if (file.exists()) {
				System.err.println("Could not write to " + path + getName() + ". File already exists.");
				return;
			}
			// Create new file
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// Write response body to file
			try {
				Files.write(file.toPath(), getBody());
				System.out.println("File written to: " + file.getPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return Returns the file extension for a file of a given MIME-type. The
	 *         MIME-type is looked up in the headers.
	 */
	private String getExtension() {
		if (getHeaders().containsKey("Content-Type")) {
			String contentType = getHeaders().get("Content-Type");
			if (contentType.contains(";")) {
				contentType = contentType.substring(0, contentType.indexOf(";"));
			}
			switch (contentType) {
			case "text/plain":
				return "txt";
			case "application/javascript":
				return "js";
			case "image/x-icon":
				return "ico";
			case "text/javascript":
				return "txt";
			default:
				return contentType.substring(contentType.indexOf("/") + 1);
			}
		} else {
			return "";
		}
	}

	/**
	 * Prints the status code and header to standard output
	 */
	void print() {
		System.out.println("Status code: " + this.statusCode);
		System.out.println();
		this.headers.forEach((key, value) -> System.out.println(key + ": " + value));
		if (getBody() != null) {
			System.out.println("\r\n" + new String(getBody()));			
		}
	}

	String getRedirectLocation() {
		// Get value of Location header
		return this.headers.get("Location");
	}
}
