package ru.r2cloud.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MockResponse implements HttpHandler {

	private final int statusCode;

	private String userAgent;

	public MockResponse(int statusCode) {
		this.statusCode = statusCode;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
		String query = exchange.getRequestURI().getQuery();
		String fileToRead;
		if (query.contains("file=json_status")) {
			fileToRead = "json_status.json";
		} else if (query.contains("file=json_statistics")) {
			fileToRead = "json_statistics.json";
		} else {
			throw new IOException("unknown file");
		}
		String message = null;
		try (InputStream is = MockResponse.class.getClassLoader().getResourceAsStream(fileToRead)) {
			message = convertToString(is);
		}
		byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, bytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(bytes);
		os.close();
	}

	static String convertToString(InputStream is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			is.transferTo(baos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(baos.toByteArray());
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getUserAgent() {
		return userAgent;
	}
}
