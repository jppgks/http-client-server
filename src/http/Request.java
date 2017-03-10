package http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

/**
 * Houses HTTP method to use, URI + port and body to post (if any).
 * Also establishes an HTTP connection with host.
 */
public class Request {
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
    public Request(Method method, String URI, int port) {
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
    public Request(Method method, String URI, int port, String body) {
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
    public Response execute() throws IOException {
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