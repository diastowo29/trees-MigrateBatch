package user;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetUserList {
	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "aGVybWF3YW5AYmx1ZWJpcmRncm91cC5jb206SmFrYXJ0YTIwMTcj";
	private final static String URL_USER_ZENDESK = "https://bluebirdgroupid.zendesk.com/api/v2/users.json";
	// private final static String TOKEN =
	// "e18bc42a0783f79f3ce9cb8a2791a2c7527cc554d83a4bf16d238ba90436df7c";\

	static JSONArray userArray = new JSONArray();
	static String nextPage = "";

	public static void main(String[] args) throws JSONException {
		boolean isNextPage = true;
		int count = 0;
		while (isNextPage) {
			count++;
			String URL = "";
			if (nextPage.isEmpty()) {
				URL = URL_USER_ZENDESK;
			} else {
				URL = nextPage;
			}
			JSONObject users = doGetUsers(URL, "GET");
			nextPage = users.getString("next_page");

			if (nextPage.isEmpty() || nextPage == null) {
				isNextPage = false;
			} else {
				isNextPage = true;
			}
			doWriteLog(userArray, count);
			userArray = new JSONArray();
		}
	}

	private static void doWriteLog(JSONArray userLists, int count) throws JSONException {
		try {
			File fileTicket = new File("C:/Users/Aumiz/Documents/IDSMED/TestUsersData__" + count + ".txt");
			if (!fileTicket.exists()) {
				fileTicket.createNewFile();
			}

			FileWriter fw = new FileWriter(fileTicket.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			StringBuffer header = new StringBuffer("id ~ name");
			bw.write(header.toString());
			bw.newLine();
			for (int i = 0; i < userLists.length(); i++) {
				bw.write(userLists.getJSONObject(i).getString("id") + " ~ "
						+ userLists.getJSONObject(i).getString("name"));
				bw.newLine();
			}

			bw.close();
			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static JSONObject doGetUsers(String urlUserZendesk, String string) throws JSONException {

		JSONObject userList = readJsonFromUrl(urlUserZendesk, "GET");
		for (int i = 0; i < userList.getJSONArray("users").length(); i++) {
			System.out.println(userList.getJSONArray("users").getJSONObject(i).get("name"));
			userArray.put(new JSONObject().put("id", userList.getJSONArray("users").getJSONObject(i).get("id"))
					.put("name", userList.getJSONArray("users").getJSONObject(i).get("name")));
		}

		return userList;
	}

	public static JSONObject readJsonFromUrl(String url, String method) {
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

				// OutputStream os = con.getOutputStream();
				// OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
				//
				// osw.write(ticket.toString());
				// osw.flush();

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
