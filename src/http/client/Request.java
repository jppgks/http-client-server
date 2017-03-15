package http.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import http.Method;
import http.Response;

enum StringType {
    HEADER, BODY
}

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

    /**
     * Execute properly, based on the method of this request.
     *
     * @return Response object with resulting status code and contents of the executed request.
     */
    Response execute() throws IOException {
        // Port is -1 if not initialized, use default port in that case
        Socket clientSocket = new Socket(getHost(), getPort());

        // Time out on read when server is unresponsive for the given amount of time.
        // clientSocket.setSoTimeout(1000);

        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedInputStream inFromServer = new BufferedInputStream(clientSocket.getInputStream());

        // Write initial line and header
        outToServer.writeBytes(getInitialLineAndHeader());

        // Write body
        if (this.method == Method.PUT || this.method == Method.POST) {
            // post body
            outToServer.writeBytes(getBody());
        }

        Response response = generateResponse(inFromServer);

        // Redirect if needed
        if (String.valueOf(response.getStatusCode()).charAt(0) == '3') {
            // Generate new request
            Request newRequest = new Request(this.getMethod(), response.getRedirectLocation());
            // Execute new request and make sure to return that response
            response = newRequest.execute();
        }

        outToServer.close();
        inFromServer.close();
        clientSocket.close();

        return response;
    }
    
    private String readLine(BufferedInputStream in) {
    	String line = new String();
    	while (! line.contains("\r\n")) {
    		try {
				int ch = in.read();
				line += (char) ch;
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	// remove line-end characters at the end
    	line = line.substring(0, line.length() - 2);
    	
		return line;
    }
    
    private byte[] readBytes(BufferedInputStream in, int number) {
		byte[] data = new byte[number];
		int bytesRead = 0;
		try {
			while (bytesRead < number) {
				bytesRead += in.read(data, bytesRead, number - bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return data;
    }

    String getInitialLineAndHeader() {
        String initialLine = getMethod() + " " + getFile() + " HTTP/1.1" + "\r\n";
        String requestHeader = "Host: " + getHost() + "\r\n\r\n";
        System.out.println(initialLine + requestHeader);
        return initialLine + requestHeader;
    }

    /**
     * Returns a response object with status code and body read from the BufferedReader.
     *
     * @param inFromServer Server response BufferedReader
     * @return Response object with status code and body read from the BufferedReader
     * @throws IOException If an I/O error occurs.
     */
    private Response generateResponse(BufferedInputStream inFromServer) throws IOException {
        int statusCode = Integer.parseInt(readLine(inFromServer).split(" ")[1]);

        HashMap<String, String> headerDict = readHeaders(inFromServer);
        
        byte[] body = readMessage(inFromServer, headerDict);

        return new Response(statusCode, headerDict, body, getFile());
    }
    
    private HashMap<String,String> readHeaders(BufferedInputStream in) {
		HashMap<String,String> headers = new HashMap<>();
		
		String header = null;
		String value = null;
		String line;
		while (((line = readLine(in)) != null)) {
		    if (line.startsWith(" ") || line.startsWith("\t")) {
		    	// lines beginning with spaces or tabs belong to the previous header line
		    	line = line.trim();
		    	value.concat(line);
		    } else {
		    	// put last header + value in map
		    	if (header != null) {
		    		headers.put(header, value);
		    	}
		    	
		    	if (line.isEmpty()) {
			        break;
			    } else {
			    	// read new header
			    	header = line.substring(0, line.indexOf(":"));
			    	value = line.substring(line.indexOf(":") + 1).trim();
			    }
		    }
		}
    	return headers;
    }
    
    private byte[] readMessage(BufferedInputStream in, HashMap<String,String> headers) {
    	boolean chunkedTE = headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"));
    	int size;
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
    	try {
	    	if (chunkedTE) {
	    		// Read chunked message
	    		while (true) {
		    		// read line with chunk size
		    		String line = readLine(in);
		    		if (line.isEmpty()) {
		    			line = readLine(in);
		    		}
		    		if (line.contains(";")) {
						size = Integer.parseInt(line.substring(0, line.indexOf(";")), 16);
					} else {
						size = Integer.parseInt(line, 16);
					}
		    		
		    		if (size == 0) {
		    			break;
		    		}
		    		// read chunk
					stream.write(readBytes(in, size));
	    		}
	    	} else {
	    		if (headers.containsKey("Content-Length")) {
	    			size = Integer.parseInt(headers.get("Content-Length"));
	    			// read number of bytes specified by Content-Length
					stream.write(readBytes(in, size));
	    		} else {
	    			// read to the end (until the connection is closed)
	    			// read all bytes one by one
	    			byte[] bt = new byte[1];
	    			while ((bt = readBytes(in, 1)) != null) {
						stream.write(bt);
	    			}
	    		}
	    	}
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	return stream.toByteArray();
    }
}
