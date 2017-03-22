package http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

import com.sun.xml.internal.ws.util.StringUtils;

public class HttpServer {

	static String path;

	public static void main(String[] args) throws IOException {
		intializePath();
		ServerSocket serverSocket = new ServerSocket(8080);
		while (true) {
			Socket clientSocket = serverSocket.accept();
			Thread thread = new Thread(new ServerThread(clientSocket));
			thread.start();
		}
	}

	/**
	 * Sets the directory to serve files from. The directory name with highest
	 * numerical value will be set as the root directory of the server.
	 * 
	 * @throws IOException
	 */
	private static void intializePath() throws IOException {
		path = "files";
		Path p = Paths.get(path);
		if (!(Files.exists(p) && Files.isDirectory(p))) {
			throw new AssertionError();
		}
		Stream<Path> files = Files.list(p);
		OptionalLong folder = files
				.filter(f -> Files.isDirectory(f))
				.map(f -> f.getFileName())
				.map(f -> f.toString())
				.filter(f -> f.matches("[0-9]+"))
				.mapToLong(f -> Long.parseLong(f))
				.max();

		files.close();

		if (folder.isPresent()) {
			path += "/" + String.valueOf(folder.getAsLong());
		}
		System.out.println("Root directory: " + path);
	}
}
