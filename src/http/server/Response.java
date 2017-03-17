package http.server;

import java.io.IOException;
import java.util.HashMap;

/**
 * Stores relevant response attributes.
 */
public class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private byte[] body;
    private String httpVersion;
    private String reasonPhrase;

    public Response(int statusCode, HashMap<String, String> header, byte[] body, String httpVersion, String reasonPhrase) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        this.httpVersion = httpVersion;
        this.reasonPhrase = reasonPhrase;
    }
    public Response(int statusCode, HashMap<String, String> header, String httpVersion, String reasonPhrase) {
        this.statusCode = statusCode;
        this.header = header;
        this.httpVersion = httpVersion;
        this.reasonPhrase = reasonPhrase;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
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

    /**
     * Prints the status code and header to standard output
     */
    public void print() {
        System.out.println("Status code: " + this.statusCode);
        System.out.println();
        this.header.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}

