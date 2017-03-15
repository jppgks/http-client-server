package http.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import http.Method;

public class HttpClient {
    public static void main(String args[]) {
        // Parse arguments [HTTPCommand, URI, Port] into request
        Request request = generateRequestFromArgs(args);
        Connection connection = new Connection(request.getHost(), request.getPort());

        try {
            // Execute request
            Response response = connection.execute(request);
            // Display response
            response.save("output/" + new Date().getTime() + "/");
            response.print();
            connection.execute(request).print();
            connection.close();
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
        Method method = Method.valueOf(args[0]);
        String address = args[1];
        
        // remove protocol (if present)
        if (address.startsWith("http://")) {
        	address = address.substring("http://".length());
        } else if (address.startsWith("https://")) {
        	address = address.substring("https://".length());
        }
        
        String host;
        String file;
        if (address.contains("/")) {
        	host = address.substring(0, address.indexOf("/"));
        	file = address.substring(address.indexOf("/"));
        } else {
        	host = address;
        	file = "/";
        }
        int port = Integer.parseInt(args[2]);
        String body = "";
        
        if (method == Method.POST || method == Method.PUT) {
        	// read from interactive command prompt
        	body = System.console().readLine();
        }
        
        return new Request(method, host, port, file, body);
    }
}
