package com.kuleuven;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

enum Method {
    GET("GET"), PUT("PUT"), POST("POST"), HEAD("HEAD");

    private String name;

    Method(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }
}

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
        // Check support for requested method
        assert Arrays.stream(Method.values()).anyMatch(e -> e.getName().equals(args[0])) : "Given HTTP method not supported";
        // Parse command line arguments (HTTPCommand, URI, Port)
        return new Request(Method.valueOf(args[0]), args[1], Integer.parseInt(args[2]));
    }
}

/**
 * Houses HTTP method to use, URI + port and body to post (if any).
 * Also establishes an HTTP connection with host.
 */
class Request {
    /**
     * @value HEAD, GET, PUT or POST
     */
    private final Method method;
    /**
     * Content of PUT or POST request
     */
    private final String body;
    private final String URI;
    private final int port;

    /**
     * Construct request object with default port 80.
     *
     * @param method HTTP method
     * @param URI    Host URI
     */
    Request(Method method, String URI) {
        this(method, URI, 80);
    }

    /**
     * Construct request object with empty body.
     *
     * @param method HTTP method
     * @param URI    Host URI
     * @param port   Port on host to connect at
     */
    Request(Method method, String URI, int port) {
        this(method, URI, port, "");
    }

    /**
     * Full-fledged constructor, initializing all fields of the request object.
     *
     * @param method HTTP method
     * @param URI    Host URI
     * @param port   Port on host to connect at
     * @param body   Content to write to host
     */
    private Request(Method method, String URI, int port, String body) {
        this.method = method;
        this.URI = URI;
        this.port = port;
        this.body = body;
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
                if (readLine.isEmpty()) {
                    break;
                }
                sb.append(readLine);
                sb.append("\n");
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Method getMethod() {
        return method;
    }

    private String getBody() {
        return body;
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

        if (this.method == Method.PUT || this.method == Method.POST) {
            // post body
            outToServer.writeBytes(getBody());
        }

        Response response = generateResponse(inFromServer);

        outToServer.close();
        inFromServer.close();
        clientSocket.close();

        return response;
    }

    /**
     * Returns a response object with status code and body read from the BufferedReader.
     *
     * @param inFromServer Server response BufferedReader
     * @return Response object with status code and body read from the BufferedReader
     * @throws IOException If an I/O error occurs.
     */
    private Response generateResponse(BufferedReader inFromServer) throws IOException {
        int statusCode = Integer.parseInt(inFromServer.readLine().split(" ")[1]);

        String header = stringFromBufferedReader(inFromServer);
        HashMap<String, String> headerDict = new HashMap<>();
        for (String headerLine : header.split("\n")) {
            int sepIndex = headerLine.indexOf(":");
            headerDict.put(headerLine.substring(0, sepIndex), headerLine.substring(sepIndex + 1).trim());
        }

        String body = stringFromBufferedReader(inFromServer);

        return new Response(statusCode, headerDict, body);
    }
}

/**
 * Stores relevant response attributes.
 */
class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private Document body;

    Response(int statusCode, HashMap<String, String> header, String body) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = Jsoup.parse(body);
    }

    int getStatusCode() {
        return statusCode;
    }

    Document getBody() {
        return body;
    }

    /**
     * Prints the status code and body to standard output and generates local html file from body.
     * <p>
     * TODO: Retrieve and store other objects on the page
     */
    void handle() {
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
            Files.write(file.toPath(), body.outerHtml().getBytes());
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
}
