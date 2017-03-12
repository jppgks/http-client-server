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
    private String body;

    public Response(int statusCode, HashMap<String, String> header, String body) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    /**
     * Prints the status code and body to standard output and generates local html file from body.
     * TODO: Retrieve and store other objects on the page
     */
    public void handle() {
        // Print to standard output
        print();

        // Save to local HTML file
        saveToLocalHTMLFile();
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
            Files.write(file.toPath(), body.getBytes());
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
        System.out.print(this.body);
    }

    public String getRedirectLocation() {
        // Get value of Location header
        return this.header.get("Location");
    }
}
