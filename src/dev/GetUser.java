package dev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GetUser {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZWxkaWVuLmhhc21hbnRvQHRyZWVzc29sdXRpb25zLmNvbTpXZWxjb21lMQ=="; //UAT
//	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM"; //LIVE
	
//	private final static String TOKEN = "ZWxkaWVuLmhhc21hbnRvQHRyZWVzc29sdXRpb25zLmNvbTpXM2xjb21lMTIz"; //TREESDEMO1
	private final static String users_url = "https://idsmed1475491095.zendesk.com/api/v2/users.json";
	private final static String org_url = "https://idsmed1475491095.zendesk.com/api/v2/organizations.json";
	
	static boolean isOrg = true;

	public static void main(String[] args) {
//		boolean isOrg = true;
		
		String nextPage = null;
		String typeData;
		System.out.println("Getting org list..");
		JSONObject jsonUser = new JSONObject();
		JSONObject jsonNextUser = new JSONObject();
		JSONArray usersArr = new JSONArray();
		
		try {
			int count = 1;
			if(isOrg){
				typeData = "ORG";
				jsonUser = getUser(org_url);
				usersArr.put(jsonUser.getJSONArray("organizations"));
			} else {
				typeData = "USERS";
				jsonUser = getUser(users_url);
				usersArr.put(jsonUser.getJSONArray("users"));
			}
			nextPage = (String) jsonUser.get("next_page");
			System.out.println(usersArr.length());
			while(nextPage!=null){
				count++;
				System.out.println(nextPage);
				System.out.println("Getting another records..");
				jsonNextUser = getUser(nextPage);
				if(isOrg){
					usersArr.put(jsonNextUser.getJSONArray("organizations"));
				} else {
					usersArr.put(jsonNextUser.getJSONArray("users"));
				}
				System.out.println(usersArr.length());
				if(!jsonNextUser.get("next_page").toString().equals("null")){
					nextPage = (String) jsonNextUser.get("next_page");
				} else {
					nextPage = null;
				}
				UsersWriter.main(null, usersArr, count, isOrg, typeData);
			}
			
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			System.out.println("ConnectException!");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("IOException!");
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println("JSONException!");
			e.printStackTrace();
		}
	}

	public static JSONObject getUser(String url) throws IOException, ConnectException, JSONException {
		HttpURLConnection con = null;
		JSONObject json = null;
		try {
			URL obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			// optional default is GET
			con.setRequestMethod("GET");

			con.setRequestProperty("User-Agent", USER_AGENT);
			con.addRequestProperty("Authorization", "Basic " + TOKEN);
			con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");

			if (con.getResponseCode() == 200) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
				String jsonText = readUser(rd);
				json = new JSONObject(jsonText);
			} else {

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			con.disconnect();
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