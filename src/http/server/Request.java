package http.server;

import http.Method;
import http.server.exceptions.BadRequestException;
import jdk.nashorn.internal.ir.debug.JSONWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;

public class Request {
    private final Method method;
    private final String file;
    private final String httpVersion;

    private final HashMap<String, String> headers;

    private final byte[] message;

    Request(Method method, String file, String httpVersion, HashMap<String, String> headers) throws BadRequestException {
        this(method, file, httpVersion, headers, null);
    }

    Request(Method method, String file, String httpVersion, HashMap<String, String> headers, byte[] message) throws BadRequestException {
        if ((method == Method.POST || method == Method.PUT) && message == null) {
            throw new BadRequestException();
        }

        if (httpVersion.equals("HTTP/1.1")) {
            // check if host header is present
            if ((!headers.containsKey("Host"))) {
                throw new BadRequestException();
            }
        }

        this.method = method;
        this.file = file;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.message = message;
    }

    Method getMethod() {
        return method;
    }

    String getFile() {
        return file;
    }

    String getHttpVersion() {
        return httpVersion;
    }

    HashMap<String, String> getHeaders() {
        return headers;
    }

    String saveMessage() {
        assert message != null : "SERVERTHREAD - Message attempted to store was null";

        String path = "server/" + new Date().getTime() + "/";
        File file = new File(path + this.getMethod().getName() + ".json");
        // Create new file
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Write request message to file
        String json = "{\r\n"
        		+ "  " + "\"method\": \"" + jsonEscape(this.getMethod().getName()) + "\"," + "\r\n"
        		+ "  " + "\"version\": \"" + jsonEscape(this.getHttpVersion()) + "\"," + "\r\n"
        		+ "  " + "\"file\": \"" + jsonEscape(this.getFile()) + "\"," + "\r\n"
        		+ "  " + "\"message\": \"" + jsonEscape(new String(message)) + "\"" + "\r\n"
        		+ "}";
        try {
            Files.write(file.toPath(), json.getBytes());
            System.out.println("Request message written to: " + file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }
    
    // TODO: place in other file, along with other methods that can be reused
    public String jsonEscape(String in) {
    	return in.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override
    public String toString() {
        if (getMethod() == Method.GET || getMethod() == Method.HEAD) {
            return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders();
        } else {
            return this.getMethod() + " " + this.getFile() + " " + this.getHttpVersion() + "\n" + this.getHeaders()
                    + "\n\n" + new String(this.message);
        }
    }
}
