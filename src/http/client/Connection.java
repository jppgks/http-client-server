package http.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import http.Method;
import http.Response;

public class Connection {
	public Connection(String host, int port) {
		this.host = host;
		this.port = port;
		
		initialize();
	}
	
	private void initialize() {
		try {
			clientSocket = new Socket(getHost(), getPort());
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Response execute(Request request) throws IOException {
		// Check if hosts and port of request matches these of the connection
		if (!(this.getHost().equals(request.getHost()) && this.getPort() == request.getPort())) {
			throw new IllegalArgumentException();
		}
		
		// Write initial line and header
		outToServer.writeBytes(request.getInitialLineAndHeader());
		
		// Write body (if PUT or POST)
		if (request.getMethod() == Method.PUT || request.getMethod() == Method.POST) {
            outToServer.writeBytes(request.getBody());
        }
		
		// Generate response
		int statusCode = Integer.parseInt(readLine(inFromServer).split(" ")[1]);
		HashMap<String, String> headers = readHeaders(inFromServer);
		byte[] body = readMessage(inFromServer, headers);
		
		Response response = new Response(statusCode, headers, body, request.getFile());
		
		// Redirect if needed
		if (String.valueOf(response.getStatusCode()).charAt(0) == '3') {
            // Generate new request
            Request newRequest = new Request(request.getMethod(), response.getRedirectLocation());
            // Execute new request and make sure to return that response
            response = this.execute(newRequest);
        }
		
		return response;
	}
	
	public void close() {
		try {
			outToServer.close();
			inFromServer.close();
			clientSocket.close();
			System.out.println("Connection to " + getHost() + " at port " + getPort() + " closed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public String getHost() {
		return this.host;
	}
	
	private final String host;
	
	public int getPort() {
		return this.port;
	}
	
	private final int port;
	
	private Socket clientSocket;
	
	private DataOutputStream outToServer;
	
	private BufferedInputStream inFromServer;
}