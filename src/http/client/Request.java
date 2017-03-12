package http.client;

import http.Method;
import http.Response;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;

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
    private URL url;

    /**
     * Construct request object from given url.
     *
     * @param method HTTP method
     * @param url    URL (including protocol, host and file)
     */
    private Request(Method method, String url) {
        this.method = method;
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Construct request object with empty body.
     *
     * @param method HTTP method
     * @param host   Host
     * @param port   Port on host to connect at
     */
    public Request(Method method, String host, int port) {
        this(method, host, port, "");
    }

    /**
     * Full-fledged constructor, initializing all fields of the request object.
     *
     * @param method HTTP method
     * @param host   Host
     * @param port   Port on host to connect at
     * @param body   Content to write to host
     */
    public Request(Method method, String host, int port, String body) {
        this.method = method;
        try {
            this.url = new URL("http", host, port, "/#q=chunked+transfer+encoding");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.body = body;
    }

    /**
     * Stores contents of the given InputStream in a string.
     *
     * @param br InputStream to convert
     */
    private static String stringFromBufferedReader(BufferedReader br, StringType type) {
        StringBuilder sb = new StringBuilder();
        String readLine;
        try {
            while (((readLine = br.readLine()) != null)) {
                if (readLine.isEmpty() && type == StringType.HEADER) {
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

    /**
     * Execute properly, based on the method of this request.
     *
     * @return Response object with resulting status code and contents of the executed request.
     */
    Response execute() throws IOException {
        // Port is -1 if not initialized, use default port in that case
        Socket clientSocket = new Socket(url.getHost(), url.getPort() < 0 ? url.getDefaultPort() : url.getPort());

        // Time out on read when server is unresponsive for the given amount of time.
        clientSocket.setSoTimeout(1000);

        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        // BufferedInputStream streamInFromServer = new BufferedInputStream(clientSocket.getInputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Write initial line and header
        outToServer.writeBytes(getInitialLineAndHeader(clientSocket.getPort()));

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

    private String getInitialLineAndHeader(int port) {
        String initialLine = getMethod() + " " + url.getFile() + " HTTP/1.1" + "\r\n";
        // Get port from socket, because if it isn't initialized in the URL object, that will return -1
        String requestHeader = "Host: " + url.getHost() + ":" + port + "\r\n\r\n";
        return initialLine + requestHeader;
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

        HashMap<String, String> headerDict = readHeaders(inFromServer);
        
        boolean chunkedTE = headerDict.containsKey("Transfer-Encoding") && "chunked".equals(headerDict.get("Transfer-Encoding"));
        String body = readMessage(inFromServer, chunkedTE);

        return new Response(statusCode, headerDict, body);
    }
    
    private HashMap<String,String> readHeaders(BufferedReader in) {
		HashMap<String,String> headers = new HashMap<>();
		
		String header = null;
		String value = null;
		String line;
		try {
			while (((line = in.readLine()) != null)) {
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
		} catch (SocketTimeoutException ignored) {
		} catch (IOException e) {
			System.err.println("Something went wrong while reading the headers.");
			e.printStackTrace();
		}
		
    	return headers;
    }
    
    private String readMessage(BufferedReader in, boolean chunkedTE) {
    	StringBuilder sb = new StringBuilder();
        String line;
        int limit = 0;
        try {
            while (((line = in.readLine()) != null)) {
            	if (chunkedTE) {
            		if (this.getBytesRead() == limit) {
            			// line with new limit
            			System.err.println(line);
            			if (line.contains(";")) {
            				limit = Integer.parseInt(line.substring(0, line.indexOf(";")), 16);
            			} else {
            				limit = Integer.parseInt(line, 16);
            			}
            			
            			if (limit == 0) {
            				break;
            			}
            		} else {
            			// just another regular line
            			sb.append(line);
            			addBytesRead(line.length());
            			if (getBytesRead() < limit) {
            				sb.append("\n");
            				addBytesRead(1);
            			}
            		}
            	} else {
            		sb.append(line);
                    sb.append("\n");
            	}
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
        	System.err.println("Something went wrong while reading the message.");
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void resetBytesRead() {
        setBytesRead(0);
    }

    private void addBytesRead(int number) {
        setBytesRead(getBytesRead() + number);
    }

    private int getBytesRead() {
        return bytesRead;
    }

    private void setBytesRead(int charactersRead) {
        this.bytesRead = charactersRead;
    }
    
    private int bytesRead;
}
