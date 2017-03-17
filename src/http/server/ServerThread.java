package http.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import http.Method;
import http.server.exceptions.BadRequestException;
import http.server.exceptions.FileNotFoundException;
import http.server.exceptions.InternalServerException;

public class ServerThread implements Runnable {

	private Socket socket;
	private DataOutputStream outToClient;
	private BufferedInputStream inFromClient;
	private boolean closed;
	
	public ServerThread(Socket socket) throws IOException {
		this.socket = socket;
		
		outToClient = new DataOutputStream(socket.getOutputStream());
		inFromClient = new BufferedInputStream(socket.getInputStream());
	}
	
	@Override
	public void run() {
		try {
			socket.setSoTimeout(10000);
			Request request = null;
			while (! closed) {
				try {
					request = readRequest();
				} catch (SocketTimeoutException e) {
					closed = true;
				} catch (BadRequestException e) {
					// send bad request error 400
				}
				if (request == null) {
					// timeout: break the while loop
					closed = true;
				} else {
					// handle request
					System.out.println(request.toString());
					if ((request.getHeaders().containsKey("Connection") && request.getHeaders().get("Connection").equals("Close")) || request.getHttpVersion() == "HTTP/1.0") {
						// set closed to true to break the while loop
						closed = true;
					}
				}
			}
			// close connection
			socket.close();
			System.out.println("Connection closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private Request readRequest() throws BadRequestException, SocketTimeoutException {
		Request request = null;
		
		String firstLine = readLine();
		if (Arrays.stream(Method.values()).noneMatch(e -> e.getName().equals(firstLine.split(" ")[0]))) {
			// HTTP Method not supported
			throw new BadRequestException();
		}
		Method method = Method.valueOf(firstLine.split(" ")[0]);
		String file = firstLine.split(" ")[1];
		String httpVersion = firstLine.split(" ")[2];
		
		HashMap<String, String> headers = readHeaders();
		
		byte[] body;		
		if (method == Method.POST || method == Method.PUT) {
			// read message
			body = readMessage(headers);
			if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
				// read (optional) footers
				HashMap<String, String> footers = readHeaders();
				headers.putAll(footers);
				
				request = new Request(method, file, httpVersion, headers, body);
			}
		} else {
			request = new Request(method, file, httpVersion, headers);
		}
		return request;
	}
	
	
	private Response handle(Request request) throws FileNotFoundException, InternalServerException {
		String httpVersion = request.getHttpVersion();
		byte[] message;
		Response response;
		int messageLength;
		
		if (request.getMethod() == Method.GET || request.getMethod() == Method.HEAD) {
			// read file
			Path path = Paths.get(HttpServer.path + request.getFile());
			if (Files.exists(path) && Files.isRegularFile(path)) {
				try {
					messageLength = (int) Files.size(path);
					if (request.getMethod() == Method.GET) {
						message = Files.readAllBytes(path);
					}
				} catch (IOException e) {
					throw new InternalServerException();
				}
			} else {
				throw new FileNotFoundException();
			}
		} else {
			// save message
		}
		// success
		//response = new Response(200, header, body, httpVersion);
		return null;
	}
	
	private HashMap<String,String> readHeaders() throws SocketTimeoutException {
		HashMap<String,String> headers = new HashMap<>();
		
		String header = null;
		String value = null;
		String line;
		boolean stop = false;
		while (! stop) {
			line = readLine();
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
			        stop = true;
			    } else {
			    	// read new header
			    	header = line.substring(0, line.indexOf(":"));
			    	value = line.substring(line.indexOf(":") + 1).trim();
			    }
		    }
		}
    	return headers;
    }
	
	private byte[] readMessage(HashMap<String,String> headers) {
    	boolean chunkedTE = headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"));
    	int size;
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
    	try {
	    	if (chunkedTE) {
	    		// Read chunked message
	    		boolean stop = false;
	    		while (! stop) {
		    		// read line with chunk size
		    		String line = readLine();
		    		if (line.isEmpty()) {
		    			line = readLine();
		    		}
		    		if (line.contains(";")) {
						size = Integer.parseInt(line.substring(0, line.indexOf(";")), 16);
					} else {
						size = Integer.parseInt(line, 16);
					}
		    		
		    		if (size == 0) {
		    			stop = true;
		    		}
		    		// read chunk
					stream.write(readBytes(size));
	    		}
	    	} else {
	    		if (headers.containsKey("Content-Length")) {
	    			size = Integer.parseInt(headers.get("Content-Length"));
	    			// read number of bytes specified by Content-Length
					stream.write(readBytes(size));
	    		} else {
	    			// read to the end (until the connection is closed)
	    			// read all bytes one by one
	    			byte[] bt;
	    			while ((bt = readBytes(1)) != null) {
	    				stream.write(bt);
	    			}
	    		}
	    	}
	    		
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	return stream.toByteArray();
    }
	
	private String readLine() throws SocketTimeoutException {
    	String line = new String();
    	while (! line.contains("\r\n")) {
    		try {
				int ch = inFromClient.read();
				line += (char) ch;
    		} catch (SocketTimeoutException e) {
    			throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	// remove line-end characters at the end
    	line = line.substring(0, line.length() - 2);
    	
		return line;
    }
    
    private byte[] readBytes(int number) throws SocketTimeoutException {
		byte[] data = new byte[number];
		int bytesRead = 0;
		int newRead;
		try {
			while (bytesRead < number) {
				if (inFromClient.available() > 0) {
					
				}
				newRead = inFromClient.read(data, bytesRead, number - bytesRead);
				if (newRead == -1) {
					if (bytesRead == 0) {
						return null;
					} else {
						return Arrays.copyOf(data, bytesRead);
					}
				} else {
					bytesRead += newRead;
				}
			}
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return data;
    }
}
