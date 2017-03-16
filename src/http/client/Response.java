package http.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import http.Method;

/**
 * Stores relevant response attributes.
 */
public class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private byte[] body;
    private String name;
    private String host;
    private int port;
    
    public Response(int statusCode, HashMap<String, String> header, byte[] body, String host, int port, String name) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        setName(name);
        this.host = host;
        this.port = port;
    } 
    
    public Response(int statusCode, HashMap<String, String> header, String host, int port, String name) {
    	this.statusCode = statusCode;
        this.header = header;
        setName(name);
        this.host = host;
        this.port = port;
    }
    
    private HashMap<String, String> getHeader() {
    	return this.header;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public byte[] getBody() {
        return body;
    }
    
    private String getName() {
    	return this.name;
    }
    
    private void setName(String name) {
    	this.name = name.replaceAll("[^A-Za-z0-9]", "");
    	if (this.name.isEmpty()) {
    		this.name = "index";
    	}
    }
    
    private String getHost() {
    	return this.host;
    }
    
    private int getPort() {
    	return this.port;
    }

    /**
     * 
     * Retrieves other objects on the page and creates a Request for them
     */
    public HashSet<Request> handle() {
    	if (body != null && getHeader().get("Content-Type").contains("text/html")) {
    		// Only retrieve other objects embedded in an HTML file
    		HashSet<Request> requests = new HashSet<>();
    		//String pattern = "<\\w+ [^<>]* src=\"(.*)\" [^<>]*>";
    		String pattern = "<.*? src=\"(.*?)\".*?>";
    		Pattern r = Pattern.compile(pattern);
    		Matcher m = r.matcher(new String(getBody()));
    		
    		while (m.find()) {
    			// create new request for each resource
    			String path = m.group(1);
    			System.out.println(path);
    			if (isRelativePath(path)) {
    				// request on the same host
    				if (! path.startsWith("/")) {
    					path = "/" + path;
    				}
    				requests.add(new Request(Method.GET, getHost(), getPort(), path));
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
    	} else {
    		return null;
    	}
    }
    
    public void save(String path) {
    	if (body != null) {
	    	File file = new File(path + getName() + "." + getExtension());
	    	while (file.exists()) {
	    		file = new File(path + getName() + Integer.toString(new Random().nextInt()) + "." + getExtension());
	    	}
	    	
	    	try {
	            file.getParentFile().mkdir();
	            file.createNewFile();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        // Write response body to file
	        try {
	            Files.write(file.toPath(), getBody());
	            System.out.println("HTML written to: " + file.getPath());
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
    	}
    }
    
    
    private boolean isRelativePath(String path) {
    	if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//")) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    
    /**
     * @return	Returns the file extension for a file of a given MIME-type
     * 			The MIME-type is looked up in the headers.
     */
    private String getExtension() {
    	String contentType = getHeader().get("Content-Type");
    	if (contentType.contains(";")) {
    		contentType = contentType.substring(0, contentType.indexOf(";"));
    	}
    	switch (contentType) {
    	case "text/plain": return "txt";
    	case "application/javascript": return "js";
    	case "image/x-icon": return "ico";
    	case "text/javascript": return "txt";
    	default: return contentType.substring(contentType.indexOf("/") + 1);
    	}
    }

    /**
     * Prints the status code and header to standard output
     */
    public void print() {
        System.out.println("Status code: " + this.statusCode);
        System.out.println();
        this.header.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    public String getRedirectLocation() {
        // Get value of Location header
        return this.header.get("Location");
    }
}
