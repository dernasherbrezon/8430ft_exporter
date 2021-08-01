package ru.r2cloud.exporter;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;

public class ModemCollector extends Collector {

	private static final Logger LOG = LoggerFactory.getLogger(ModemCollector.class);

	private final Properties props;
	private final HttpClient httpclient;

	public ModemCollector(Properties props) {
		this.props = props;
		Authenticator auth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(props.getProperty("modem.login"), props.getProperty("modem.password").toCharArray());
			}
		};
		httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(Long.valueOf(props.getProperty("modem.timeoutMillis")))).authenticator(auth).build();
	}

	@Override
	public List<MetricFamilySamples> collect() {
		List<MetricFamilySamples> result = new ArrayList<>();

		JsonObject status = downloadJson("json_status");
		if (status != null) {
			result.add(new GaugeMetricFamily("sim_status", "sim_status", Double.valueOf(status.getString("sim_status", "0"))));
			result.add(new GaugeMetricFamily("pin_status", "pin_status", Double.valueOf(status.getString("pin_status", "0"))));
			result.add(new GaugeMetricFamily("auto_apn", "auto_apn", Double.valueOf(status.getString("auto_apn", "0"))));
			result.add(new GaugeMetricFamily("roaming", "roaming", Double.valueOf(status.getString("roaming", "0"))));

			result.add(new GaugeMetricFamily("Battery_charging", "Battery_charging", Double.valueOf(status.getString("Battery_charging", "0"))));
			result.add(new GaugeMetricFamily("Battery_charge", "Battery_charge", Double.valueOf(status.getString("Battery_charge", "0"))));
			result.add(new GaugeMetricFamily("Battery_voltage", "Battery_voltage", Double.valueOf(status.getString("Battery_voltage", "0"))));
			result.add(new GaugeMetricFamily("Battery_connect", "Battery_connect", Double.valueOf(status.getString("Battery_connect", "0"))));
			result.add(new GaugeMetricFamily("sys_mode", "sys_mode", Double.valueOf(status.getString("sys_mode", "0"))));

			result.add(new GaugeMetricFamily("nr_connected_dev", "Number of connected devices", Double.valueOf(status.getString("nr_connected_dev", "0"))));
			result.add(new GaugeMetricFamily("new_sms_num", "new_sms_num", Double.valueOf(status.getString("new_sms_num", "0"))));

			result.add(new GaugeMetricFamily("sms_unread", "Number of unread SMS", Double.valueOf(status.getString("sms_unread_long_num", "0"))));
			result.add(new GaugeMetricFamily("rssi", "received signal strength indicator", Double.valueOf(status.getString("rssi", "0"))));

			result.add(new GaugeMetricFamily("check_upgrade_tip", "check_upgrade_tip", Double.valueOf(status.getString("check_upgrade_tip", "0"))));
			result.add(new GaugeMetricFamily("cur_login_status", "cur_login_status", Double.valueOf(status.getString("cur_login_status", "0"))));
			result.add(new GaugeMetricFamily("qs_complete", "qs_complete", Double.valueOf(status.getString("qs_complete", "0"))));
			result.add(new GaugeMetricFamily("cur_band", "cur_band", Double.valueOf(status.getString("cur_band", "0"))));

			long uptime = TimeUnit.DAYS.toSeconds(Long.valueOf(status.getString("run_days", "0"))) + TimeUnit.HOURS.toSeconds(Long.valueOf(status.getString("run_hours", "0"))) + TimeUnit.MINUTES.toSeconds(Long.valueOf(status.getString("run_minutes", "0")))
					+ Long.valueOf(status.getString("run_seconds", "0"));

			result.add(new CounterMetricFamily("uptime_seconds", "uptime in seconds", uptime));
		}

		JsonObject statistics = downloadJson("json_statistics");
		if (statistics != null) {
			result.add(new CounterMetricFamily("total_used_month", "total_used_month", Double.valueOf(statistics.getString("total_used_month", "0"))));
			result.add(new CounterMetricFamily("total_used_period", "total_used_period", Double.valueOf(statistics.getString("total_used_period", "0"))));
			result.add(new CounterMetricFamily("total_used_unlimit", "total_used_unlimit", Double.valueOf(statistics.getString("total_used_unlimit", "0"))));
			result.add(new GaugeMetricFamily("quota", "quota", Double.valueOf(statistics.getString("quota", "0"))));
			result.add(new CounterMetricFamily("errors", "errors", Double.valueOf(statistics.getString("errors", "0"))));

			result.add(new CounterMetricFamily("rx", "rx", Double.valueOf(statistics.getString("rx", "0"))));
			result.add(new CounterMetricFamily("tx", "tx", Double.valueOf(statistics.getString("tx", "0"))));
			result.add(new CounterMetricFamily("rx_byte", "rx_byte", Double.valueOf(statistics.getString("rx_byte", "0"))));
			result.add(new CounterMetricFamily("tx_byte", "tx_byte", Double.valueOf(statistics.getString("tx_byte", "0"))));
			result.add(new CounterMetricFamily("tx_byte_all", "tx_byte_all", Double.valueOf(statistics.getString("tx_byte_all", "0"))));
			result.add(new CounterMetricFamily("rx_byte_all", "rx_byte_all", Double.valueOf(statistics.getString("rx_byte_all", "0"))));

			result.add(new GaugeMetricFamily("curUpSpeed", "curUpSpeed", Double.valueOf(statistics.getString("curUpSpeed", "0"))));
			result.add(new GaugeMetricFamily("curDnSpeed", "curDnSpeed", Double.valueOf(statistics.getString("curDnSpeed", "0"))));
			result.add(new GaugeMetricFamily("maxUpSpeed", "maxUpSpeed", Double.valueOf(statistics.getString("maxUpSpeed", "0"))));
			result.add(new GaugeMetricFamily("maxDnSpeed", "maxDnSpeed", Double.valueOf(statistics.getString("maxDnSpeed", "0"))));
		}
		return result;
	}

	private JsonObject downloadJson(String type) {
		Builder result = HttpRequest.newBuilder().uri(URI.create(props.getProperty("modem.host") + ":" + props.getProperty("modem.port") + "/xml_action.cgi?method=get&module=duster&file=" + type + System.currentTimeMillis()));
		HttpRequest request = result.GET().build();
		HttpResponse<String> response;
		try {
			response = httpclient.send(request, BodyHandlers.ofString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (IOException e) {
			LOG.error("unable to get: {}", type, e);
			return null;
		}
		if (response.statusCode() != 200) {
			LOG.error("invalid response code for: {} code: {}", type, response.statusCode());
			return null;
		}
		return Json.parse(response.body()).asObject();
	}

}
