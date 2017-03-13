package http.client;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import http.Method;
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
        Method method = Method.valueOf(args[0]);
        String address = args[1];
        
        // remove protocol (if present)
        if (address.startsWith("http://")) {
        	address.replaceFirst("http://", "");
        } else if (address.startsWith("https://")) {
        	address.replaceFirst("https://", "");
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
        URL url = null;
		try {
			url = new URL("http", host, port, file);
		} catch (MalformedURLException e1) {
			System.err.println("URL not formed properly.");
			e1.printStackTrace();
		}
        String body = "";
        
        if (method == Method.POST || method == Method.PUT) {
        	// read from interactive command prompt
        	body = System.console().readLine();
        }
        
        return new Request(method, url, body);
    }
}
