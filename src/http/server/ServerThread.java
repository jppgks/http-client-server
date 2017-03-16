package http.server;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class ServerThread implements Runnable {

	Socket socket;
	DataOutputStream outToClient;
	BufferedInputStream inFromClient;
	
	public ServerThread(Socket socket) throws IOException {
		this.socket = socket;
		
		outToClient = new DataOutputStream(socket.getOutputStream());
		inFromClient = new BufferedInputStream(socket.getInputStream());
	}
	
	@Override
	public void run() {
		
	}

	
	private String readLine(BufferedInputStream in) {
    	String line = new String();
    	while (! line.contains("\r\n")) {
    		try {
				int ch = in.read();
				line += (char) ch;
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	// remove line-end characters at the end
    	line = line.substring(0, line.length() - 2);
    	
		return line;
    }
    
    private byte[] readBytes(BufferedInputStream in, int number) {
		byte[] data = new byte[number];
		int bytesRead = 0;
		int newRead;
		try {
			while (bytesRead < number) {
				if (in.available() > 0) {
					
				}
				newRead = in.read(data, bytesRead, number - bytesRead);
				if (newRead == -1) {
					if (bytesRead == 0) {
						return null;
					} else {
						return Arrays.copyOf(data, bytesRead);
					}
				} else {
					bytesRead += newRead;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return data;
    }
}
