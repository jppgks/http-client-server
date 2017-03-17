package http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(8080);
		while (true) {
			Socket clientSocket = serverSocket.accept();
			Thread thread = new Thread(new ServerThread(clientSocket));
			thread.start();
		}
	}

}
