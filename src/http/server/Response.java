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

    public Response(int statusCode, HashMap<String, String> header, byte[] body, String httpVersion) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        this.httpVersion = httpVersion;
        this.reasonPhrase = reasonPhrase;
    }
    public Response(int statusCode, HashMap<String, String> header, String httpVersion) {
        this.statusCode = statusCode;
        this.header = header;
        this.httpVersion = httpVersion;
        this.reasonPhrase = reasonPhrase;
    }

    private String getHttpVersion() {
        return httpVersion;
    }

    private String getReasonPhrase() {
        switch (statusCode) {
            case 200:
                return "OK";
            case 304:
                return "Not Modified";
            case 400:
                return "Bad Request";
            case 402:
                return "Payment Required";
            case 404:
                return "Not Found";
            case 500:
                return "Server Error";
            default:
                return "";
        }
    }

    public HashMap<String, String> getHeader() {
        return this.header;
    }

    private int getStatusCode() {
        return statusCode;
    }

    public String getStatusLine() {
        return getHttpVersion() + " " + getStatusCode() + " " + getReasonPhrase();
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

