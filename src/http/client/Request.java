package http.client;

import http.Method;

/**
 * Houses HTTP method to use, url + port and body to post (if any).
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
    private String body;
    private String host;
    private int port;
    private String file;


    public Request(Method method, String host, int port) {
    	this(method, host, port, "/");
    }
    
    
    /**
     * Construct request object with empty body.
     *
     * @param method HTTP method
     * @param host   Host
     * @param port   Port on host to connect at
     * @param file	 Path to the requested file
     */
    public Request(Method method, String host, int port, String file) {
        this(method, host, port, file, "");
    }

    /**
     * Full-fledged constructor, initializing all fields of the request object.
     *
     * @param method HTTP method
     * @param host   Host
     * @param port   Port on host to connect at
     * @param file	 Path to the requested file
     * @param body   Content to write to host
     */
    public Request(Method method, String host, int port, String file, String body) {
        this.method = method;
        this.host = host;
        this.port = port;
        this.file = file;
        this.body = body;
    }
    
    public Request(Method method, String address) {
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
        
        this.method = method;
        this.host = host;
        this.port = 80;
        this.file = file;
        this.body = "";
    }

    Method getMethod() {
        return method;
    }

    String getBody() {
        return body;
    }
    
    String getHost() {
    	return this.host;
    }
    
    int getPort() {
    	return this.port;
    }
    
    String getFile() {
    	return this.file;
    }

    String getInitialLineAndHeader() {
        String initialLine = getMethod() + " " + getFile() + " HTTP/1.1" + "\r\n";
        String requestHeader = "Host: " + getHost() + "\r\n\r\n";
        System.out.println(initialLine + requestHeader);
        return initialLine + requestHeader;
    }
}
