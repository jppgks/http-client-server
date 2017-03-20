package http.server;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

/**
 * Stores relevant response attributes.
 */
public class Response {

    private int statusCode;
    private HashMap<String, String> headers;
    private byte[] body;
    private String httpVersion;

    public Response(int statusCode, HashMap<String, String> header, byte[] body, String httpVersion) throws IOException {
        this.statusCode = statusCode;
        this.headers = header;
        this.body = body;
        this.httpVersion = httpVersion;
        addDefaultHeaders();
    }
    public Response(int statusCode, HashMap<String, String> header, String httpVersion) {
        this.statusCode = statusCode;
        this.headers = header;
        this.httpVersion = httpVersion;
        addDefaultHeaders();
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

    public HashMap<String, String> getHeaders() {
        return this.headers;
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
        this.headers.forEach((key, value) -> System.out.println(key + ": " + value));
    }
    
    
    /**
     * Automatically adds headers to the response: date, content-length, server
     */
    private void addDefaultHeaders() {
    	headers.put("Server", "SCJG");
    	headers.put("Date", java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT"))));
    	
    	if (getBody() != null) {
    		headers.put("Content-Length", Integer.toString(getBody().length));
    	}
    }
}

