package server;

import server.exceptions.BadRequestException;
import util.Method;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;

import static util.StringOperations.jsonEscape;

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
        try {
			this.file = java.net.URLDecoder.decode(file, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new BadRequestException();
		}
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

        String path = "uploads/" + new Date().getTime() + "/";
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
            System.out.println("server.Request message written to: " + file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
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
