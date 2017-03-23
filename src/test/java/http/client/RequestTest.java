package http.client;

import http.Method;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RequestTest {
    private String URI;
    private Method method;
    private int port;
    private Request request;
    private Connection connection;

    @Before
    public void setUp() throws Exception {
        method = Method.GET;
        URI = "www.kuleuven.be";
        port = 80;
        request = new Request(method, URI, port);
        connection = new Connection(request.getHost(), request.getPort());
    }

    @Test
    public void executeSocketsTest() throws Exception {
        Response response = connection.execute(request);
        int statusCode = response.getStatusCode();
        assertEquals(200, statusCode);
        // Expected to have a non-empty response body
        assertFalse(response.getBody().length == 0);
    }

    @Test
    public void testSave() throws IOException {
        // Given
        String URI = "whitehouse.gov";
        Request request = new Request(method, URI, port);
        Connection connection = new Connection(request.getHost(), request.getPort());
        // When
        Response response = connection.execute(request);
        response.save("files/2");
        // Then
        if (response.getBody().length > 0) {
            assertTrue(Files.exists(Paths.get("files/2/" + response.getName())));
        }
    }

}
