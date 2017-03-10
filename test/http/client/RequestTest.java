package http.client;

import org.junit.Before;

import http.Method;
import http.Response;
import http.client.Request;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

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
        Response response = request.execute();
        int statusCode = response.getStatusCode();
        // Expected redirect to /kuleuven/
        assertThat(Integer.toString(statusCode), startsWith("3"));
        // Expected to have a non-empty response body
        assertFalse(response.getBody().isEmpty());
    }

}
