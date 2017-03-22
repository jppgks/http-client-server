package http.client;

import http.Method;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores relevant response attributes.
 */
class Response {

    private int statusCode;
    private HashMap<String, String> header;
    private byte[] body;
    private String name;
    private String host;
    private int port;

    Response(int statusCode, HashMap<String, String> header, byte[] body, String host, int port, String name) throws IOException {
        this.statusCode = statusCode;
        this.header = header;
        this.body = body;
        setName(name);
        this.host = host;
        this.port = port;
    }

    Response(int statusCode, HashMap<String, String> header, String host, int port, String name) {
        this.statusCode = statusCode;
        this.header = header;
        setName(name);
        this.host = host;
        this.port = port;
    }

    private HashMap<String, String> getHeader() {
        return this.header;
    }

    int getStatusCode() {
        return statusCode;
    }

    byte[] getBody() {
        return body;
    }

    private String getName() {
        return this.name;
    }

    private void setName(String name) {
    	if (name.endsWith("/")) {
    		name += "index." + getExtension();
    	}
        this.name = name;
    }

    private String getHost() {
        return this.host;
    }

    private int getPort() {
        return this.port;
    }

    /**
     * Retrieves other objects on the page and creates a Request for them
     */
    HashSet<Request> handle() {
        HashSet<Request> requests = new HashSet<>();
        if (body != null && getHeader().get("Content-Type").contains("text/html")) {
            // Only retrieve other objects embedded in an HTML file
            //String pattern = "<\\w+ [^<>]* src=\"(.*)\" [^<>]*>";
            String pattern = "<.*? src=\"(.*?)\".*?>";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(new String(getBody()));

            while (m.find()) {
                // create new request for each resource
                String path = m.group(1);
                System.out.println(path);
                if (isRelativePath(path)) {
                    // request on the same host
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    requests.add(new Request(Method.GET, getHost(), getPort(), path));
                } else {
                    // remove protocol (if present)
                    if (path.startsWith("http://")) {
                        path = path.substring("http://".length());
                    } else if (path.startsWith("https://")) {
                        path = path.substring("https://".length());
                    } else if (path.startsWith("//")) {
                        path = path.substring("//".length());
                    }

                    String host;
                    String file;
                    if (path.contains("/")) {
                        host = path.substring(0, path.indexOf("/"));
                        file = path.substring(path.indexOf("/"));
                    } else {
                        host = path;
                        file = "/";
                    }

                    requests.add(new Request(Method.GET, host, 80, file));
                }
            }
        }
        return requests;
    }

    void save(String path) {
        if (body != null) {
            File file = new File(path + getName());
            // Show message if file already exists
            if (file.exists()) {
            	System.err.println("Could not write to " + path + ". File already exists.");
            	return;
            }
            // Create new file
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Write response body to file
            try {
                Files.write(file.toPath(), getBody());
                System.out.println("File written to: " + file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean isRelativePath(String path) {
        return !(path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//"));
    }


    /**
     * @return Returns the file extension for a file of a given MIME-type
     * The MIME-type is looked up in the headers.
     */
    private String getExtension() {
        String contentType = getHeader().get("Content-Type");
        if (contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(";"));
        }
        switch (contentType) {
            case "text/plain":
                return "txt";
            case "application/javascript":
                return "js";
            case "image/x-icon":
                return "ico";
            case "text/javascript":
                return "txt";
            default:
                return contentType.substring(contentType.indexOf("/") + 1);
        }
    }

    /**
     * Prints the status code and header to standard output
     */
    void print() {
        System.out.println("Status code: " + this.statusCode);
        System.out.println();
        this.header.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("\r\n" + new String(getBody()));
    }

    String getRedirectLocation() {
        // Get value of Location header
        return this.header.get("Location");
    }
}
