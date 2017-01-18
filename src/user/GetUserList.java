package user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetUserList {
	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";
	private final static String URL_USER_ZENDESK = "https://idsmed.zendesk.com/api/v2/users.json";
	// private final static String TOKEN =
	// "e18bc42a0783f79f3ce9cb8a2791a2c7527cc554d83a4bf16d238ba90436df7c";\
	static JSONArray jsonErr = new JSONArray();

	public static void main(String[] args) {
		readJsonFromUrl(URL_USER_ZENDESK, "GET");
	}

	public static void readJsonFromUrl(String url, String method) {
		HttpURLConnection con = null;
		JSONObject json = null;
		try {
			URL obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			// optional default is GET
			con.setRequestMethod(method);
			con.setDoOutput(true);

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.addRequestProperty("Authorization", "Basic " + TOKEN);
			con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");

			if (con.getResponseCode() == 200) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
				String jsonText = readUser(rd);
				json = new JSONObject(jsonText);
				System.out.println(json);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			con.disconnect();
		}
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
