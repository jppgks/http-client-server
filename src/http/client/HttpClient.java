package http.client;

import java.io.*;
import java.util.Arrays;

import http.Method;
import http.Request;
import http.Response;

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
