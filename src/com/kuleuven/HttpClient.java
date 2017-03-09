package com.kuleuven;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class HttpClient {
    public static void main(String args[]) {
        // Parse arguments [HTTPCommand, URI, Port] into request
        Request request = generateRequestFromArgs(args);

        try {
            // Execute request
            Response response = request.execute();
            // Display response
            response.handle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a Request object from command line arguments
     *
     * @param args [HTTPCommand, URI, Port]
     * @return Request with given HTTP method, URI and port number
     */
    private static Request generateRequestFromArgs(String[] args) {
        assert args.length == 3;
        // Parse command line arguments (HTTPCommand, URI, Port)
        return new Request(args[0], args[1], Integer.parseInt(args[2]));
    }
}

/**
 * Houses HTTP method to use, URI + port and content to post (if any).
 * Also establishes an HTTP connection with host.
 */
class Request {
    /**
     * @value HEAD, GET, PUT or POST
     */
    private final String method;
    /**
     * Content of PUT or POST request
     */
    private final String content;
    private final String URI;
    private final int port;

    /**
     * Construct request object with default port 80.
     *
     * @param method HTTP method
     * @param URI    Host URI
     */
    Request(String method, String URI) {
        this(method, URI, 80);
    }

    /**
     * Construct request object with empty content.
     *
     * @param method HTTP method
     * @param URI    Host URI
     * @param port   Port on host to connect at
     */
    Request(String method, String URI, int port) {
        this(method, URI, port, "");
    }

    /**
     * Full-fledged constructor, initializing all fields of the request object.
     *
     * @param method  HTTP method
     * @param URI     Host URI
     * @param port    Port on host to connect at
     * @param content Content to write to host
     */
    private Request(String method, String URI, int port, String content) {
        this.method = method;
        this.URI = URI;
        this.port = port;
        this.content = content;
    }

    /**
     * Stores contents of the given InputStream in a string.
     *
     * @param br InputStream to convert
     */
    private static String stringFromBufferedReader(BufferedReader br) {
        StringBuilder sb = new StringBuilder();
        String readLine;
        try {
            while (((readLine = br.readLine()) != null)) {
                sb.append(readLine);
                sb.append("\n");
            }
        } catch (SocketTimeoutException socketTimeoutException) {
            // Silently continue
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String getMethod() {
        return method;
    }

    private String getContent() {
        return content;
    }

    private String getURI() {
        return URI;
    }

    private int getPort() {
        return port;
    }

    /**
     * Execute properly, based on the method of this request.
     *
     * @return Response object with resulting status code and contents of the executed request.
     */
    Response execute() throws IOException {
        Socket clientSocket = new Socket(getURI(), getPort());

        // Time out on read when server is unresponsive for the given amount of time.
        clientSocket.setSoTimeout(1000);

        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        String initialLine = getMethod() + " / HTTP/1.1" + "\r\n";
        String requestHeader = "Host: " + getURI() + ":" + getPort() + "\r\n\r\n";
        outToServer.writeBytes(initialLine + requestHeader);

        if (getMethod().equals("PUT") || getMethod().equals("POST")) {
            // post content
            outToServer.writeBytes(getContent());
        }

        Response response = generateResponse(inFromServer);

        outToServer.close();
        inFromServer.close();
        clientSocket.close();

        return response;
    }

    /**
     * Returns a response object with status code and content read from the BufferedReader.
     *
     * @param inFromServer Server response BufferedReader
     * @return Response object with status code and content read from the BufferedReader
     * @throws IOException If an I/O error occurs.
     */
    private Response generateResponse(BufferedReader inFromServer) throws IOException {
        int statusCode = Integer.parseInt(inFromServer.readLine().split(" ")[1]);
        String content = stringFromBufferedReader(inFromServer);

        return new Response(statusCode, content);
    }
}

/**
 * Stores relevant response attributes.
 */
class Response {

    private int statusCode;
    private String content;

    Response(int statusCode, String content) throws IOException {
        this.statusCode = statusCode;
        this.content = content;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getContent() {
        return content;
    }

    /**
     * Prints the status code and content to standard output.
     * <p>
     * TODO: Store content to local .html file + retrieve and store other objects on the page
     */
    void handle() {
        System.out.println("Status code: " + this.statusCode);
        System.out.println();
        System.out.print(this.content);
    }
}
