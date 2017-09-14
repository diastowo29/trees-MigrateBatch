package ticket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONObject;

public class ExportTicket {

	static final String URL = "";
	static final String USER_AGENT = "";
	static final String TOKEN = "";

	public static void main(String[] args) {
		
	}

	public static JSONObject writeJsonFromUrl(String url, String method, JSONObject ticket) {
		HttpURLConnection con = null;
		JSONObject json = new JSONObject();
		int retry = 1;
		for (int i = 0; i < retry; i++) {
			try {
				URL obj = new URL(url);
				con = (HttpURLConnection) obj.openConnection();

				// optional default is GET
				System.out.println("calling the api: " + url);
				con.setRequestMethod(method);
				con.setDoOutput(true);

				// add request header
				con.setRequestProperty("User-Agent", USER_AGENT);
				con.addRequestProperty("Authorization", "Basic " + TOKEN);
				con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
				// con.setConnectTimeout(10000);
				// con.setReadTimeout(10000);
				// System.out.println(ticket);

				OutputStream os = con.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

				osw.write(ticket.toString());
				osw.flush();
				System.out.println(con.getResponseCode() + " " + con.getResponseMessage());

				if (con.getResponseCode() == 200) {
					System.out.println("reading response..");
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					json = new JSONObject(jsonText);
				} else {
					System.out.println("error, reading error response");
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getErrorStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					if (con.getResponseCode() != 500) {
						json = new JSONObject(jsonText);
					}
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
