package util;

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

import util.ConnectProperties.api;
import util.ConnectProperties.methods;

public class ZendeskAPICall {
	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";
	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String USERS_API = "https://idsmed1475491095.zendesk.com/api/v2/users.json";
//	private final static String TICKET_API = "https://idsmed1475491095.zendesk.com/api/v2/tickets.json";
//	private final static String ORG_API = "https://idsmed1475491095.zendesk.com/api/v2/organizations.json";

	public static JSONObject makeRequest(api apis, JSONObject jsonObject) {
		String url = null;
		methods method = null;
		HttpURLConnection con = null;
		JSONObject json = null;
		int retry = 1;
		for(int i = 0; i<retry; i++){
			if (apis == api.userCreate) {
				url = USERS_API;
				method = methods.POST;
			} else {
				url = USERS_API;
				method = methods.GET;
			}

			try {
				URL obj = new URL(url);
				con = (HttpURLConnection) obj.openConnection();
				// optional default is GET
				con.setRequestMethod(method.toString());
				con.setDoOutput(true);

				// add request header
				con.setRequestProperty("User-Agent", USER_AGENT);
				con.addRequestProperty("Authorization", "Basic " + TOKEN);
				con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
				if (method.equals(methods.POST)) {
					OutputStream os = con.getOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
					System.out.println(jsonObject.toString());
					osw.write(jsonObject.toString());
					osw.flush();
				}
				if (con.getResponseCode() == 200 || con.getResponseCode() == 201) {
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					json = new JSONObject(jsonText);
					json.put("responseCode", con.getResponseCode());
				} else {
					BufferedReader rd = new BufferedReader(
							new InputStreamReader(con.getErrorStream(), Charset.forName("UTF-8")));
					String jsonText = readUser(rd);
					json = new JSONObject(jsonText);
					json.put("responseCode", con.getResponseCode());
				}
			} catch (Exception e) {
				e.printStackTrace();
				if(retry<=5){
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
