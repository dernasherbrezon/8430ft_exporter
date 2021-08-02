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

	private static final String TX_BYTE = "tx_byte";
	private static final String RX_BYTE = "rx_byte";
	private static final String ERRORS = "errors";
	private static final String QUOTA = "quota";
	private static final String TOTAL_USED_UNLIMIT = "total_used_unlimit";
	private static final String TOTAL_USED_PERIOD = "total_used_period";
	private static final String TOTAL_USED_MONTH = "total_used_month";
	private static final String CUR_BAND = "cur_band";
	private static final String QS_COMPLETE = "qs_complete";
	private static final String CUR_LOGIN_STATUS = "cur_login_status";
	private static final String CHECK_UPGRADE_TIP = "check_upgrade_tip";
	private static final String NEW_SMS_NUM = "new_sms_num";
	private static final String SYS_MODE = "sys_mode";
	private static final String BATTERY_CONNECT = "Battery_connect";
	private static final String BATTERY_VOLTAGE = "Battery_voltage";
	private static final String BATTERY_CHARGE = "Battery_charge";
	private static final String BATTERY_CHARGING = "Battery_charging";
	private static final String ROAMING = "roaming";
	private static final String AUTO_APN = "auto_apn";
	private static final String PIN_STATUS = "pin_status";
	private static final String SIM_STATUS = "sim_status";

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
			result.add(new GaugeMetricFamily(SIM_STATUS, SIM_STATUS, Double.valueOf(status.getString(SIM_STATUS, "0"))));
			result.add(new GaugeMetricFamily(PIN_STATUS, PIN_STATUS, Double.valueOf(status.getString(PIN_STATUS, "0"))));
			result.add(new GaugeMetricFamily(AUTO_APN, AUTO_APN, Double.valueOf(status.getString(AUTO_APN, "0"))));
			result.add(new GaugeMetricFamily(ROAMING, ROAMING, Double.valueOf(status.getString(ROAMING, "0"))));

			result.add(new GaugeMetricFamily(BATTERY_CHARGING, BATTERY_CHARGING, Double.valueOf(status.getString(BATTERY_CHARGING, "0"))));
			result.add(new GaugeMetricFamily(BATTERY_CHARGE, BATTERY_CHARGE, Double.valueOf(status.getString(BATTERY_CHARGE, "0"))));
			result.add(new GaugeMetricFamily(BATTERY_VOLTAGE, BATTERY_VOLTAGE, Double.valueOf(status.getString(BATTERY_VOLTAGE, "0"))));
			result.add(new GaugeMetricFamily(BATTERY_CONNECT, BATTERY_CONNECT, Double.valueOf(status.getString(BATTERY_CONNECT, "0"))));
			result.add(new GaugeMetricFamily(SYS_MODE, SYS_MODE, Double.valueOf(status.getString(SYS_MODE, "0"))));

			result.add(new GaugeMetricFamily("nr_connected_dev", "Number of connected devices", Double.valueOf(status.getString("nr_connected_dev", "0"))));
			result.add(new GaugeMetricFamily(NEW_SMS_NUM, NEW_SMS_NUM, Double.valueOf(status.getString(NEW_SMS_NUM, "0"))));

			result.add(new GaugeMetricFamily("sms_unread", "Number of unread SMS", Double.valueOf(status.getString("sms_unread_long_num", "0"))));
			result.add(new GaugeMetricFamily("rssi", "received signal strength indicator", Double.valueOf(status.getString("rssi", "0"))));

			result.add(new GaugeMetricFamily(CHECK_UPGRADE_TIP, CHECK_UPGRADE_TIP, Double.valueOf(status.getString(CHECK_UPGRADE_TIP, "0"))));
			result.add(new GaugeMetricFamily(CUR_LOGIN_STATUS, CUR_LOGIN_STATUS, Double.valueOf(status.getString(CUR_LOGIN_STATUS, "0"))));
			result.add(new GaugeMetricFamily(QS_COMPLETE, QS_COMPLETE, Double.valueOf(status.getString(QS_COMPLETE, "0"))));
			result.add(new GaugeMetricFamily(CUR_BAND, CUR_BAND, Double.valueOf(status.getString(CUR_BAND, "0"))));

			long uptime = TimeUnit.DAYS.toSeconds(Long.valueOf(status.getString("run_days", "0"))) + TimeUnit.HOURS.toSeconds(Long.valueOf(status.getString("run_hours", "0"))) + TimeUnit.MINUTES.toSeconds(Long.valueOf(status.getString("run_minutes", "0")))
					+ Long.valueOf(status.getString("run_seconds", "0"));

			result.add(new CounterMetricFamily("uptime_seconds", "uptime in seconds", uptime));
		}

		JsonObject statistics = downloadJson("json_statistics");
		if (statistics != null) {
			result.add(new CounterMetricFamily(TOTAL_USED_MONTH, TOTAL_USED_MONTH, Double.valueOf(statistics.getString(TOTAL_USED_MONTH, "0"))));
			result.add(new CounterMetricFamily(TOTAL_USED_PERIOD, TOTAL_USED_PERIOD, Double.valueOf(statistics.getString(TOTAL_USED_PERIOD, "0"))));
			result.add(new CounterMetricFamily(TOTAL_USED_UNLIMIT, TOTAL_USED_UNLIMIT, Double.valueOf(statistics.getString(TOTAL_USED_UNLIMIT, "0"))));
			result.add(new GaugeMetricFamily(QUOTA, QUOTA, Double.valueOf(statistics.getString(QUOTA, "0"))));
			result.add(new CounterMetricFamily(ERRORS, ERRORS, Double.valueOf(statistics.getString(ERRORS, "0"))));

			result.add(new CounterMetricFamily("rx", "rx", Double.valueOf(statistics.getString("rx", "0"))));
			result.add(new CounterMetricFamily("tx", "tx", Double.valueOf(statistics.getString("tx", "0"))));
			result.add(new CounterMetricFamily(RX_BYTE, RX_BYTE, Double.valueOf(statistics.getString(RX_BYTE, "0"))));
			result.add(new CounterMetricFamily(TX_BYTE, TX_BYTE, Double.valueOf(statistics.getString(TX_BYTE, "0"))));

			// the following metrics can be derived from rx_byte/tx_byte
			// tx_byte_all
			// rx_byte_all
			// curUpSpeed
			// curDnSpeed
			// maxUpSpeed
			// maxDnSpeed
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
