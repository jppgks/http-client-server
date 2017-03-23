package http.server;

import http.Method;
import http.server.exceptions.FileNotFoundException;
import http.server.exceptions.ServerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HttpServer.class)
public class ServerThreadTest {

    private Request request;
    private ServerThread serverThread;
    private String file = "/1/index.html";

    @Before
    public void setUp() throws Exception {
        // Setup server thread
        Socket s = new Socket();
        Socket socket = spy(s);
        PipedOutputStream outputStream = new PipedOutputStream(); // output stream that doesn't take constructor args
        doReturn(outputStream).when(socket).getOutputStream();
        PipedInputStream inputStream = new PipedInputStream(outputStream); // same, but for the input stream
        doReturn(inputStream).when(socket).getInputStream();
        ServerThread st = new ServerThread(socket);
        serverThread = spy(st);

        // Setup request
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Host", "testhost");
        headers.put("If-Modified-Since", "Fri, 31 Dec 1999 23:59:59 GMT");
        request = new Request(Method.GET, file, "HTTP/1.1", headers);

        // Mock HttpServer getPath call
        PowerMockito.mockStatic(HttpServer.class);
        when(HttpServer.getPath()).thenReturn("files");
    }

    /**
     * Helper that creates an index.html file in the 1/ directory under the server root
     *
     * @throws IOException  (Obviously)
     */
    private void createIndexHtmlFile() throws IOException {
        // Create index.html in files directory
        Paths.get(HttpServer.getPath()).toFile().mkdirs();
        File file = Paths.get(HttpServer.getPath() + "/1/index.html").toFile();
        Files.write(file.toPath(), "<html></html>".getBytes());
    }

    @Test(expected = FileNotFoundException.class)
    public void test404() throws ServerException {
        // File shouldn't exist in order to throw 404
        try {
            Files.delete(Paths.get("files" + file));
        } catch (IOException ignored) {}
        // Fire request
        serverThread.handle(request);
    }

    @Test
    public void testIfModifiedSince_wasModified() throws Exception {
        /* GIVEN */
        createIndexHtmlFile();
        // Stub fileIsModified call
        doReturn(true).when(serverThread).fileIsModified(any(Path.class), anyString());

        /* WHEN */
        Response response = serverThread.handle(request);

        /* THEN */
        Assert.assertEquals(200, response.getStatusCode());
    }

    @Test
    public void testIfModifiedSince_wasNotModified() throws Exception {
        /* GIVEN */
        // Create index.html in files directory
        createIndexHtmlFile();
        // Stub fileIsModified call
        doReturn(false).when(serverThread).fileIsModified(any(Path.class), anyString());

        /* WHEN */
        Response response = serverThread.handle(request);

        /* THEN */
        Assert.assertEquals(304, response.getStatusCode());
    }

}
