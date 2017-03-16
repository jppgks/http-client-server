package http.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores relevant response attributes.
 */
public class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private byte[] body;
    private String name;
    private String host;
    
    public Response(int statusCode, HashMap<String, String> header, byte[] body, String host, String name) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        setName(name);
        this.host = host;
    } 
    
    public Response(int statusCode, HashMap<String, String> header, String host, String name) {
    	this.statusCode = statusCode;
        this.header = header;
        setName(name);
        this.host = host;
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

    /**
     * 
     * Retrieves other objects on the page and creates a Request for them
     */
    public ArrayList<Request> handle() {
    	if (body != null) {
	    	if (getHeader().get("Content-Type").contains("text/html")) {
	    		// Only retrieve other objects embedded in an HTML file
	    		//String pattern = "<\\w+ [^<>]* src=\"(.*)\" [^<>]*>";
	    		String pattern = "<.*? src=\"(.*?)\".*?>";
	    		Pattern r = Pattern.compile(pattern);
	    		Matcher m = r.matcher(new String(getBody()));
	    		
	    		while (m.find()) {
	    			System.out.println(m.group(1));
	    		}
	    		
	    		return null;
	    	} else {
	    		return null;
	    	}
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
