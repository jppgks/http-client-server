package http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;

public final class IO {

	/**
	 * Reads one line of the BufferedInputStream and returns it as a String
	 * 
	 * @param in
	 * @return String with the content of the line read
	 * @throws SocketException
	 * @throws SocketTimeoutException
	 */
	public static String readLine(BufferedInputStream in) throws SocketException, SocketTimeoutException {
		StringBuilder sb = new StringBuilder();
		while (sb.lastIndexOf("\r\n") == -1) {
			try {
				int ch = in.read();
				sb.append((char) ch);
			} catch (SocketTimeoutException | SocketException e) {
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String line = sb.toString();
		// remove line-end characters at the end
		line = line.substring(0, line.length() - 2);

		return line;
	}

	/**
	 * Reads the number of bytes specified from the BufferedInputStream and
	 * returns them as a byte array
	 * 
	 * @param in
	 * @param number
	 * @return
	 * @throws SocketTimeoutException
	 */
	public static byte[] readBytes(BufferedInputStream in, int number) throws SocketTimeoutException {
		byte[] data = new byte[number];
		int bytesRead = 0;
		int newRead;
		try {
			while (bytesRead < number) {
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
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * Reads the headers of an HTTP-message from an BufferedInputStream and
	 * returns it as a HashMap<String, String>
	 * 
	 * @param in
	 * @return
	 * @throws SocketTimeoutException
	 * @throws SocketException
	 */
	public static HashMap<String, String> readHeaders(BufferedInputStream in)
			throws SocketTimeoutException, SocketException {
		HashMap<String, String> headers = new HashMap<>();

		String header = null;
		String value = null;
		String line;
		boolean stop = false;
		while (!stop) {
			line = readLine(in);
			if (line.startsWith(" ") || line.startsWith("\t")) {
				// lines beginning with spaces or tabs belong to the previous
				// header line
				line = line.trim();
				value.concat(line);
			} else {
				// put last header + value in map
				if (header != null) {
					headers.put(header, value);
				}

				if (line.isEmpty()) {
					stop = true;
				} else {
					// read new header
					header = line.substring(0, line.indexOf(":"));
					value = line.substring(line.indexOf(":") + 1).trim();
				}
			}
		}
		return headers;
	}

	/**
	 * Reads the body of a HTTP-message from a BufferedInputStream and returns
	 * it as byte array. The HashMap with headers is used to determine the size
	 * of the body.
	 * 
	 * @param in
	 * @param headers
	 * @return
	 * @throws SocketTimeoutException
	 */
	public static byte[] readMessage(BufferedInputStream in, HashMap<String, String> headers)
			throws SocketTimeoutException {
		boolean chunkedTE = headers.containsKey("Transfer-Encoding")
				&& "chunked".equals(headers.get("Transfer-Encoding"));
		int size;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			if (chunkedTE) {
				// Read chunked message
				boolean stop = false;
				while (!stop) {
					// read line with chunk size
					String line = readLine(in);
					if (line.isEmpty()) {
						line = readLine(in);
					}
					if (line.contains(";")) {
						size = Integer.parseInt(line.substring(0, line.indexOf(";")), 16);
					} else {
						size = Integer.parseInt(line, 16);
					}

					if (size == 0) {
						stop = true;
					}
					// read chunk
					stream.write(readBytes(in, size));
				}
			} else {
				if (headers.containsKey("Content-Length")) {
					size = Integer.parseInt(headers.get("Content-Length"));
					// read number of bytes specified by Content-Length
					stream.write(readBytes(in, size));
				} else {
					// read to the end (until the connection is closed)
					// read all bytes one by one
					byte[] bt;
					while ((bt = readBytes(in, 1)) != null) {
						stream.write(bt);
					}
				}
			}
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stream.toByteArray();
	}
}
