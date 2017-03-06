package com.kuleuven;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

public class HttpClient {
    public static void main(String args[]) {
        // Parse arguments [HTTPCommand, URI, Port] into request
        Request request = generateRequestFromArgs(args);
        
        try {
			request.executeSockets();
		} catch (IOException e) {
			e.printStackTrace();
		}

//        // Execute request and handle response
//        handleResponse(request.execute());
    }

    /**
     * Creates a Request object from command line arguments
     * @param args  [HTTPCommand, URI, Port]
     * @return      Request with given HTTP method, URI and port number
     */
    private static Request generateRequestFromArgs(String[] args) {
        assert args.length == 3;
        // Parse command line arguments (HTTPCommand, URI, Port)
        return new Request(args[0], args[1], args[2]);
    }

    /**
     * Takes a Response object and prints the error code and content to standard output.
     *
     * TODO: Store content to local .html file + retrieve and store other objects on the page
     *
     * @param response Object with methods {@code getStatusCode()}, which returns an integer, and
     *                 {@code getContent()}, which returns an InputStream.
     */
    private static void handleResponse(Response response) {
        System.out.println("Status code: " + response.getStatusCode());
        System.out.println();

        printInputStream(response.getContent());
    }

    /**
     * Prints contents of the given InputStream to standard output.
     *
     * @param in    InputStream to print
     */
    private static void printInputStream(InputStream in) {
        // https://javastorage.wordpress.com/2011/02/21/how-to-read-an-inputstream/
        String readLine;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            while (((readLine = br.readLine()) != null)) {
                System.out.println(readLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/**
 * Houses HTTP method to use, content to post and an HTTP connection.
 */
class Request {
    Request(String method, String URI, String port) {
        this(method, URI, port, "");
    }

    Request(String method, String URI, String port, String content) {
        this.method = method;
        this.URI = URI;
        this.port = Integer.parseInt(port);
        
        try {
            // source: http://stackoverflow.com/questions/2793150/using-java-net-urlconnection-to-fire-and-handle-http-requests
            URL url = new URL("http", URI, Integer.parseInt(port), "");
            // Open connection with URL
            this.connection = (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.content = content;
    }

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
     * HTTP connection to URL
     */
    private HttpURLConnection connection;

    /**
     * Execute properly, based on the method of this request.
     *
     * @return  Response object with resulting status code and contents of the executed request.
     */
    Response execute() {
        // source: http://stackoverflow.com/questions/2793150/using-java-net-urlconnection-to-fire-and-handle-http-requests

        // Write output if method is PUT or POST
        if (this.method.equals("PUT") || this.method.equals("POST")) {
            this.connection.setDoOutput(true);
            try {
                // Post content
                OutputStream os = this.connection.getOutputStream();
                PrintStream printStream = new PrintStream(os);
                printStream.print(this.content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Handle response
        Response response = new Response();
        try {
            response.setContent(connection.getInputStream());  // Implicitly fires request
            response.setStatusCode(connection.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
    
    Response executeSockets() throws UnknownHostException, IOException {
    	Socket clientSocket = new Socket(getURI(), getPort());
    	
    	DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
    	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    	outToServer.writeBytes(getMethod() + " /kuleuven/ HTTP/1.0" + "\n" + "\n");
    	
    	if (getMethod() == "PUT" || getMethod() == "POST") {
    		// post content
    		
    		outToServer.writeBytes(getContent());
    	}
    	
    	String line;
    	while ((line = inFromServer.readLine()) != null) {
    		System.out.println(line);
    	}
    	
    	outToServer.close();
    	inFromServer.close();
    	clientSocket.close();
    	
    	Response response = new Response();
    	// response.setContent(clientSocket.getInputStream());
    	// set status code, initial line contains status code (see resource in assignment)
    	
		return response;
    }
}

/**
 * Stores relevant response attributes.
 */
class Response {

    private int statusCode;
    private InputStream content;

    int getStatusCode() {
        return statusCode;
    }

    InputStream getContent() {
        return content;
    }

    void setContent(InputStream content) {
        this.content = content;
    }

    void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
