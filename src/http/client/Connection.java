package http.client;

import http.Method;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

class Connection {
    private final String host;
    private final int port;
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedInputStream inFromServer;
    private boolean closed = false;

    Connection(String host, int port) {
        this.host = host;
        this.port = port;

        initialize();
    }

    void initialize() {
        try {
            clientSocket = new Socket(getHost(), getPort());
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Response execute(Request request) throws IOException {
        // Check if hosts and port of request matches these of the connection
        if (!(this.getHost().equals(request.getHost()) && this.getPort() == request.getPort())) {
            this.close();
            Connection connection = new Connection(request.getHost(), request.getPort());
            return connection.execute(request);
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
        Response response;
        if (request.getMethod() != Method.HEAD) {
            byte[] body = readMessage(inFromServer, headers);
            if (headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"))) {
                // read (optional) footers
                HashMap<String, String> footers = readHeaders(inFromServer);
                headers.putAll(footers);
            }
            response = new Response(statusCode, headers, body, request.getHost(), request.getPort(), request.getFile());
        } else {
            response = new Response(statusCode, headers, request.getHost(), request.getPort(), request.getFile());
        }

        // Redirect if needed
        if (String.valueOf(response.getStatusCode()).charAt(0) == '3') {
            System.out.println("CONNECTION - Page moved, redirecting to new location.\n");
            // Generate new request
            Request newRequest = new Request(request.getMethod(), response.getRedirectLocation());
            // Execute new request and make sure to return that response
            response = this.execute(newRequest);
        }
        
        if (headers.containsKey("Connection") && headers.get("Connection").equals("close")) {
        	this.closed = true;
        	close();
        }

        return response;
    }

    void close() {
        try {
            outToServer.close();
            inFromServer.close();
            clientSocket.close();
            System.out.println("CONNECTION - Connection to " + getHost() + " at port " + getPort() + " closed.\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readLine(BufferedInputStream in) {
        StringBuilder sb = new StringBuilder();
        while (sb.lastIndexOf("\r\n") == -1) {
            try {
                int ch = in.read();
                sb.append((char) ch);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String line = sb.toString();
        // remove line-end characters at the end
        line = line.substring(0, line.length() - 2);

        return line;
    }

    private byte[] readBytes(BufferedInputStream in, int number) {
        byte[] data = new byte[number];
        int bytesRead = 0;
        int newRead;
        try {
            while (bytesRead < number) {
                if (in.available() > 0) {

                }
                newRead = in.read(data, bytesRead, number - bytesRead);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private HashMap<String, String> readHeaders(BufferedInputStream in) {
        HashMap<String, String> headers = new HashMap<>();

        String header = null;
        String value = null;
        String line;
        boolean stop = false;
        while (!stop) {
            line = readLine(in);
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

    private byte[] readMessage(BufferedInputStream in, HashMap<String, String> headers) {
        boolean chunkedTE = headers.containsKey("Transfer-Encoding") && "chunked".equals(headers.get("Transfer-Encoding"));
        int size;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            if (chunkedTE) {
                // Read chunked message
                boolean stop = false;
                while (!stop) {
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
                        stop = true;
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
                    byte[] bt;
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

    String getHost() {
        return this.host;
    }

    int getPort() {
        return this.port;
    }
    
    boolean isClosed() {
    	return this.closed;
    }
}