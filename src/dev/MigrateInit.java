package dev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MigrateInit {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";
	// private final static String TOKEN =
	// "e18bc42a0783f79f3ce9cb8a2791a2c7527cc554d83a4bf16d238ba90436df7c";
	private static String urlAccount;
	private static String METHOD;
	static JSONArray jsonErr = new JSONArray();

	public static void main(String[] args, JSONObject userObj, String dataType, String Row)
			throws IOException, JSONException {
		if (dataType.equalsIgnoreCase("UserZendesk")) {
			System.out.println("UserZendesk");
			urlAccount = "https://idsmed.zendesk.com/api/v2/users.json";
			METHOD = "POST";
		} else if (dataType.equalsIgnoreCase("OrganizationZendesk")) {
			System.out.println("OrganizationZendesk");
			urlAccount = "https://idsmed.zendesk.com/api/v2/organization.json";
			METHOD = "POST";
		} else if (dataType.equalsIgnoreCase("Contact_relationship")) {
			System.out.println("Contact_relationship");
			urlAccount = "https://idsmed.zendesk.com/api/v2/organization_memberships/create_many.json";
			METHOD = "POST";
		} else if(dataType.equalsIgnoreCase("organizationEdited")) {
			System.out.println(dataType);
			urlAccount = "https://idsmed.zendesk.com/api/v2/organizations/" + Row + ".json";
			METHOD = "PUT";
		}
//		System.out.println(userObj);
//		System.out.println(userObj.getJSONArray("organization_memberships").length());
		readJsonFromUrl(urlAccount, userObj, Row, METHOD);
		logWriter(dataType, jsonErr);
		System.out.println(jsonErr);
	}

	// private static String readAll(Reader rd) throws IOException {
	// StringBuilder sb = new StringBuilder();
	// int cp;
	// while ((cp = rd.read()) != -1) {
	// sb.append((char) cp);
	// }
	// return sb.toString();
	// }

	public static void readJsonFromUrl(String url, JSONObject user, String Row, String method) {
		try {
			System.out.println(url);
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			// optional default is GET
			con.setRequestMethod(method);
			con.setDoOutput(true);

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.addRequestProperty("Authorization", "Basic " + TOKEN);
			con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");

			OutputStream os = con.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

			osw.write(user.toString());
			osw.flush();
			System.out.println(con.getResponseCode());

			int responseCode = con.getResponseCode();
			if (responseCode == 201 || responseCode == 200) {
				System.out.println("Success creating data from row :" + Row);
			} else {
				System.out.println("Error creating data from row :" + Row);
				jsonErr.put(user);
			}
		} catch (ConnectException cex) {
			jsonErr.put(user);
			System.out.println("Connection Timeout !!!");
			cex.printStackTrace();
		} catch (IOException ioex) {
			jsonErr.put(user);
			System.out.println("IOException !!!");
			ioex.printStackTrace();
		}
	}
	
	public static void logWriter(String Datatype, JSONArray jsonErrArr) throws JSONException{
			try {
				File file = new File("C:/users/Diastowo/" + Datatype + ".txt");

				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				for (int i = 0; i < jsonErrArr.length(); i++) {
					for (int j = 0; j < jsonErrArr.getJSONArray(i).length(); j++) {
						bw.newLine();
						boolean isOrg = true;
						if(isOrg ){
							bw.write(jsonErrArr.getJSONArray(i).getJSONObject(j).getString("id") + ", "
									+ jsonErrArr.getJSONArray(i).getJSONObject(j).getString("name"));
						} else {
							bw.write(jsonErrArr.getJSONArray(i).getJSONObject(j).getString("id") + ", "
									+ jsonErrArr.getJSONArray(i).getJSONObject(j).getString("email") + ", "
									+ jsonErrArr.getJSONArray(i).getJSONObject(j).getJSONObject("user_fields")
											.getString("contactguid"));
						}
					}
				}
				bw.close();
				System.out.println("Done");
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
}