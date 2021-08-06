package ru.r2cloud.exporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;

import io.prometheus.client.CollectorRegistry;

public class ModemCollectorTest {

	private static final Logger LOG = LoggerFactory.getLogger(ModemCollectorTest.class);

	private HttpServer server;
	private Thread collector;

	@Test
	public void testFailure() throws Exception {
		assertEquals("", readMetrics("http://127.0.0.1:9841"));
	}

	@Test
	public void testSuccess() throws Exception {
		server.createContext("/xml_action.cgi", new MockResponse(200));
		String expected;
		try (InputStream is = ModemCollectorTest.class.getClassLoader().getResourceAsStream("expected.metrics.txt")) {
			expected = MockResponse.convertToString(is);
		}
		assertEquals(expected.trim(), readMetrics("http://127.0.0.1:9841").trim());
	}

	private static String readMetrics(String basePath) {
		HttpClient httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(Long.valueOf(10000))).build();
		Builder result = HttpRequest.newBuilder().uri(URI.create(basePath + "/metrics"));
		HttpRequest request = result.GET().build();
		int retry = 0;
		int maxRetries = 3;
		HttpResponse<String> response = null;
		while (retry < maxRetries) {
			try {
				response = httpclient.send(request, BodyHandlers.ofString());
				break;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("interrupted");
				return null;
			} catch (IOException e) {
				retry++;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					fail("interrupted");
					return null;
				}
			}
		}
		if (response == null) {
			fail("unable to read");
			return null;
		}
		if (response.statusCode() != 200) {
			fail("invalid status code: " + response.statusCode());
		}
		return response.body();
	}

	@Before
	public void start() throws Exception {
		String host = "localhost";
		int port = 8000;
		server = HttpServer.create(new InetSocketAddress(host, port), 0);
		server.start();
		collector = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Main.main(new String[] { "./src/test/resources/config.properties" });
				} catch (Exception e) {
					LOG.error("unable to start", e);
				}
			}
		}, "collector");
		collector.start();
	}

	@After
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
		if (collector != null) {
			collector.interrupt();
			try {
				collector.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		CollectorRegistry.defaultRegistry.clear();
	}

}
