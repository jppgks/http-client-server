package http.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Random;

/**
 * Stores relevant response attributes.
 */
public class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private byte[] body;
    private String name;

    public Response(int statusCode, HashMap<String, String> header, byte[] body) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
    }
    
    public Response(int statusCode, HashMap<String, String> header, byte[] body, String name) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        setName(name);
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

    /**
     * 
     * Retrieves and stores other objects on the page
     */
    public void handle() {
       
    }
    
    public void save(String path) {
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
