package http.client;

import http.Method;

import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RequestTest {
    private String URI;
    private Method method;
    private int port;
    private Request request;

    @Before
    public void setUp() throws Exception {
        this.method = Method.GET;
        this.URI = "www.kuleuven.be";
        this.port = 80;
        this.request = new Request(this.method, this.URI, this.port);
    }

    @org.junit.Test
    public void executeSocketsTest() throws Exception {
    	Connection connection = new Connection(request.getHost(), request.getPort());
        Response response = connection.execute(request);
        int statusCode = response.getStatusCode();
        assertEquals(200, statusCode);
        // Expected to have a non-empty response body
        assertFalse(response.getBody().length == 0);
    }

}