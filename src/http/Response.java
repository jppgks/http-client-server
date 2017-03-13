package http;

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
     * Prints the status code and body to standard output and generates local html file from body.
     * TODO: Retrieve and store other objects on the page
     */
    public void handle() {
        // Print to standard output
        print();

        // Save to local HTML file
        //saveToLocalHTMLFile();
    }
    
    public void save(String path) {
    	String extension = getExtension();
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
            System.out.println();
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
    	switch (contentType) {
    	case "text/plain": return "txt";
    	case "application/javascript": return "js";
    	case "image/x-icon": return "ico";
    	default: return contentType.substring(contentType.indexOf("/") + 1);
    	}
    }

    private void saveToLocalHTMLFile() {
        int randInt = new Random().nextInt();
        File file = new File("output/response" + Integer.toString(randInt) + ".html");
        // Create output file and directory (if non-existent)
        try {
            file.getParentFile().mkdir();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Write response body to file
        try {
            Files.write(file.toPath(), getBody());
            System.out.println();
            System.out.println("HTML written to: " + file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void print() {
        System.out.println("Status code: " + this.statusCode);
        System.out.println();
        this.header.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println();
        // System.out.print(new String(getBody()));
    }

    public String getRedirectLocation() {
        // Get value of Location header
        return this.header.get("Location");
    }
}
