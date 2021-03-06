package server;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

/**
 * Stores relevant response attributes.
 */
class Response {

    public int getStatusCode() {
        return statusCode;
    }

    private int statusCode;
    private HashMap<String, String> headers;
    private byte[] body;
    private String httpVersion;

    Response(int statusCode, HashMap<String, String> header, byte[] body, String httpVersion) {
        this.statusCode = statusCode;
        this.headers = header;
        this.body = body;
        this.httpVersion = httpVersion;
        addDefaultHeaders();
    }

    Response(int statusCode, HashMap<String, String> header, String httpVersion) {
        this.statusCode = statusCode;
        this.headers = header;
        this.httpVersion = httpVersion;
        addDefaultHeaders();
    }

    /**
     * Constructor for generating a "100 Continue" response.
     * Requires no headers and can only be sent when the HTTP version is 1.1.
     */
    Response() {
        this.statusCode = 100;
        this.httpVersion = "HTTP/1.1";
    }

    private String getReasonPhrase() {
        switch (statusCode) {
            case 100:
                return "Continue";
            case 200:
                return "OK";
            case 304:
                return "Not Modified";
            case 400:
                return "Bad server.Request";
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

    HashMap<String, String> getHeaders() {
        return this.headers;
    }

    String getStatusLine() {
        return httpVersion + " " + statusCode + " " + getReasonPhrase();
    }

    byte[] getBody() {
        return body;
    }

    /**
     * Automatically adds headers to the response: date, content-length, server
     */
    private void addDefaultHeaders() {
        headers.put("Server", "SCJG");
        headers.putIfAbsent("Date", java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT"))));

        if (body != null) {
            headers.put("Content-Length", Integer.toString(body.length));
        }
    }
}

