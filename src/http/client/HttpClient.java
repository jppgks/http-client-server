package http.client;

import http.Method;

import java.io.IOException;
import java.util.*;

public class HttpClient {
    public static void main(String args[]) {
        // Parse arguments [HTTPCommand, URI, Port] into request
        Request request = generateRequestFromArgs(args);
        Connection connection = new Connection(request.getHost(), request.getPort());

        try {
            // Execute request
            Response response = connection.execute(request);
            // Display response
            String path = "output/" + new Date().getTime() + "/";
            response.save(path);
            response.print();

            HashSet<Request> requests = response.handle();
            // order requests by host
            HashMap<String, ArrayList<Request>> requestsByHost = new HashMap<>();
            for (Request r : requests) {
                if (requestsByHost.containsKey(r.getHost() + ":" + r.getPort())) {
                    requestsByHost.get(r.getHost() + ":" + r.getPort()).add(r);
                } else {
                    ArrayList<Request> reqs = new ArrayList<>();
                    reqs.add(r);
                    requestsByHost.put(r.getHost() + ":" + r.getPort(), reqs);
                }
            }

            // execute requests for current host
            if (requestsByHost.containsKey(connection.getHost() + ":" + connection.getPort())) {
                ArrayList<Request> requestsForConnection = requestsByHost.get(connection.getHost() + ":" + connection.getPort());
                for (Request r : requestsForConnection) {
                	if (connection.isClosed()) {
                		connection.initialize();
                	}
                    connection.execute(r).save(path);
                }
                connection.close();
                requestsByHost.remove(connection.getHost() + ":" + connection.getPort());
            }

            // execute requests by host
            for (Map.Entry<String, ArrayList<Request>> entry : requestsByHost.entrySet()) {
                ArrayList<Request> requestsForConnection = entry.getValue();
                connection = new Connection(requestsForConnection.get(0).getHost(), requestsForConnection.get(0).getPort());
                for (Request r : requestsForConnection) {
                	if (connection.isClosed()) {
                		connection.initialize();
                	}
                    connection.execute(r).save(path);
                }
                connection.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a Request object from command line arguments
     *
     * @param args [HTTPCommand, URI, Port]
     * @return Request with given HTTP method, URI and port number
     */
    private static Request generateRequestFromArgs(String[] args) {
        assert args.length == 3;
        // Check support for requested method
        assert Arrays.stream(Method.values()).anyMatch(e -> e.getName().equals(args[0])) : "Given HTTP method not supported";
        // Parse command line arguments (HTTPCommand, URI, Port)
        Method method = Method.valueOf(args[0]);
        String address = args[1];

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
        int port = Integer.parseInt(args[2]);
        String body = "";

        if (method == Method.POST || method == Method.PUT) {
            // read from interactive command prompt
            System.out.print("Enter the body of your request: ");
            Scanner scan = new Scanner(System.in);
            body = scan.next();
            scan.close();
        }

        return new Request(method, host, port, file, body);
    }
}
