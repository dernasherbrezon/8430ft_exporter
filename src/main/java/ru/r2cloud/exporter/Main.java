package ru.r2cloud.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.exporter.HTTPServer;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			LOG.error("configuration file is missing");
			System.exit(1);
			return;
		}

		File f = new File(args[0]);
		if (!f.exists()) {
			LOG.error("cannot find configuration file: {}", f.getAbsolutePath());
			System.exit(1);
			return;
		}

		Properties props = new Properties();
		try (InputStream is = new FileInputStream(f)) {
			props.load(is);
		}
		ModemCollector collector = new ModemCollector(props);
		collector.register();
		LOG.info("collector started");
		new HTTPServer(props.getProperty("server.hostname"), Integer.valueOf(props.getProperty("server.port")), false);

	}

}
