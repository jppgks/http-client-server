package http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalLong;
import java.util.stream.Stream;

public class HttpServer {

	public static String getPath() {
		return path;
	}

	static String path;

	public static void main(String[] args) throws IOException {
		initializePath();
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
	private static void initializePath() throws IOException {
		path = "files";
		Path p = Paths.get(path);
		// Create files directory if nonexistent
		p.toFile().mkdirs();
		// Find folder with files most recently received by client
		Stream<Path> files = Files.list(p);
		OptionalLong folder = files.filter(f -> Files.isDirectory(f)).map(f -> f.getFileName()).map(f -> f.toString())
				.filter(f -> f.matches("[0-9]+")).mapToLong(f -> Long.parseLong(f)).max();

		files.close();
		// If such folder exists,
		if (folder.isPresent()) {
			// serve files from that directory now
			path += "/" + String.valueOf(folder.getAsLong());
		}
		System.out.println("Root directory: " + path);
	}
}
