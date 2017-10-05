package ticket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;

import util.ConnectProperties;

public class CallUrl {
	
	static int TIMED_OUT = 10000;
	static String PROXY_IP = "172.21.8.65";
	static int PROXY_PORT = 8080;

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";

	static JSONArray jsonErr = new JSONArray();
	static boolean conPro = new ConnectProperties().isTesting;

	public static JSONObject readJsonFromUrl(String url, String method) {
		HttpURLConnection con = null;
		JSONObject json = null;
		int retry = 1;
		for (int i = 0; i < retry; i++) {
			try {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_IP, PROXY_PORT));
				URL obj = new URL(url);
				if(conPro){
					con = (HttpURLConnection) obj.openConnection(proxy);
				} else {
					con = (HttpURLConnection) obj.openConnection();
				}
				// optional default is GET
				System.out.println("calling the APIS: " + url);
				con.setRequestMethod(method);
				con.setDoOutput(true);

				// add request header
				con.setRequestProperty("User-Agent", USER_AGENT);
				con.addRequestProperty("Authorization", "Basic " + TOKEN);
				con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
				con.setConnectTimeout(TIMED_OUT);
				con.setReadTimeout(TIMED_OUT);
				System.out.println("Response Code: " + con.getResponseCode() + " " + con.getResponseMessage());

				if (con.getResponseCode() == 200) {
					System.out.println("reading response..");
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					if (jsonText.length() > 0) {
						json = new JSONObject(jsonText);
					}
				} else {
					System.out.println("error, reading error response");
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getErrorStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					json = new JSONObject(jsonText);
				}
				json.put("responseCode", con.getResponseCode());
			} catch (Exception e) {
				e.printStackTrace();
				if (retry < 5) {
					retry++;
				}
			} finally {
				con.disconnect();
			}
		}
		return json;
	}

	private static String readUser(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
