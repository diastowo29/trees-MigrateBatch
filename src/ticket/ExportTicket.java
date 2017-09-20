package ticket;

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
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExportTicket {
//	static final String TOKEN = "ZWxkaWVuLmhhc21hbnRvQHRyZWVzc29sdXRpb25zLmNvbTpXM2xjb21lMTIz";
//	static final String ZENDESK_DOMAIN = "https://treesdemo1.zendesk.com";

	static final String TOKEN = "aGVybWF3YW5AYmx1ZWJpcmRncm91cC5jb206SmFrYXJ0YTIwMTcj";
	static final String ZENDESK_DOMAIN = "https://bluebirdgroupid.zendesk.com";
	
	static final String ZENDESK_TICKET_URL = "/api/v2/tickets.json";
	static final String ZENDESK_TICKETCOMMENTS_URL = "/api/v2/tickets/";
	static final String ZENDESK_TICKETFIELDS_URL = "/api/v2/ticket_fields/";
	static final String URL = ZENDESK_DOMAIN + ZENDESK_TICKET_URL;
	static final String USER_AGENT = "Mozilla/5.0";
	static final String METHOD_GET = "GET";
	static boolean nextPage = true;

	public static void main(String[] args) throws JSONException {
		String nextPageUrl = "https://bluebirdgroupid.zendesk.com/api/v2/tickets.json?page=960";
		JSONObject ticketList = null;
		JSONObject ticketFields = null;
		ArrayList<String> ticketFieldsName = new ArrayList<String>();
		int count = 959;
		while (nextPage) {
			count++;
			if (nextPageUrl.isEmpty()) {
				ticketList = hitAPI(URL, METHOD_GET);
				for (int i = 0; i < ticketList.getJSONArray("tickets").getJSONObject(0).getJSONArray("custom_fields").length(); i++) {
					ticketFields = hitAPI(
							ZENDESK_DOMAIN + ZENDESK_TICKETFIELDS_URL
									+ ticketList.getJSONArray("tickets").getJSONObject(0).getJSONArray("custom_fields").getJSONObject(i).getString("id"),
							METHOD_GET);
					ticketFieldsName.add(ticketFields.getJSONObject("ticket_field").getString("title"));
				}
			} else {
				ticketList = hitAPI(nextPageUrl, METHOD_GET);
			}
			JSONArray ticketData = processJson(ticketList);
			doWriteLog(ticketData, count, ticketFieldsName);
			if (ticketList.get("next_page") == null) {
				nextPage = false;
			} else {
				nextPageUrl = ticketList.get("next_page").toString();
				nextPage = true;
			}
		}
		/*count++;
		if (nextPageUrl.isEmpty()) {
			ticketList = hitAPI(URL, METHOD_GET);
			for (int i = 0; i < ticketList.getJSONArray("tickets").getJSONObject(0).getJSONArray("custom_fields").length(); i++) {
				ticketFields = hitAPI(
						ZENDESK_DOMAIN + ZENDESK_TICKETFIELDS_URL
								+ ticketList.getJSONArray("tickets").getJSONObject(0).getJSONArray("custom_fields").getJSONObject(i).getString("id"),
						METHOD_GET);
				ticketFieldsName.add(ticketFields.getJSONObject("ticket_field").getString("title"));
			}
		} else {
			ticketList = hitAPI(nextPageUrl, METHOD_GET);
		}
		JSONArray ticketData = processJson(ticketList);
		doWriteLog(ticketData, count, ticketFieldsName);
		if (ticketList.get("next_page") == null) {
			nextPage = false;
		} else {
			nextPageUrl = ticketList.get("next_page").toString();
			nextPage = true;
		}*/
	}

	private static JSONArray processJson(JSONObject ticketList) throws JSONException {
		JSONArray ticketData = ticketList.getJSONArray("tickets");
		for (int i = 0; i < ticketList.getJSONArray("tickets").length(); i++) {
			System.out.println(ticketList.getJSONArray("tickets").getJSONObject(i).get("id"));
			JSONObject ticketComment = hitAPI(ZENDESK_DOMAIN + ZENDESK_TICKETCOMMENTS_URL
					+ ticketList.getJSONArray("tickets").getJSONObject(i).get("id") + "/comments.json", METHOD_GET);
			ticketData.getJSONObject(i).put("comments", ticketComment.getJSONArray("comments"));
//			for (int c = 0; c < ticketComment.getJSONArray("comments").length(); c++) {
//				System.out.println(
//						"comments: " + ticketComment.getJSONArray("comments").getJSONObject(c).getString("body"));
//			}
//			System.out.println("ticketData");
//			System.out.println(ticketData.getJSONObject(i).getJSONArray("comments"));
//			if (i == 0) {
//				doWriteLog(ticketData, 0);				
//			}
			
		}
		return ticketData;
	}

	public static JSONObject hitAPI(String url, String method) {
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
	
	public static void doWriteLog (JSONArray ticketData, int count, ArrayList<String> ticketFieldsName) throws JSONException {
		try {
			File fileTicket = new File("C:/Users/Aumiz/Documents/IDSMED/TicketsData__" + count + ".txt");
			if (!fileTicket.exists()) {
				fileTicket.createNewFile();
			}

			FileWriter fw = new FileWriter(fileTicket.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			StringBuffer header = new StringBuffer("id ~ subject ~ requester_id ~ submitter_id ~ assignee_id ~ organization_id ~ group_id ~ tags ~ brand_id ~ comments ~ author ~ created_date");
			for (int i=0; i<ticketFieldsName.size(); i++){
				header.append("~" + ticketFieldsName.get(i));
			}
			bw.write(header.toString());
			bw.newLine();
			for (int i = 0; i < ticketData.length(); i++) {
				System.out.println(ticketData.getJSONObject(i).getString("id"));
				bw.write(ticketData.getJSONObject(i).getString("id") + " ~ "
						+ ticketData.getJSONObject(i).getString("subject").replaceAll("[\\t\\n\\r]+", " ") + " ~ "
						+ ticketData.getJSONObject(i).getString("requester_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("submitter_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("assignee_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("organization_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("group_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("tags") + " ~ "
//						+ ticketData.getJSONObject(i).getString("ticket_form_id") + " ~ "
						+ ticketData.getJSONObject(i).getString("brand_id") + " ~ "
						+ /*ticketData.getJSONObject(i).getJSONArray("comments").getJSONObject(0).getString("plain_body") +*/ "null ~ "
						+ /*ticketData.getJSONObject(i).getJSONArray("comments").getJSONObject(0).getString("author_id")*/ "null ~ "
						+ ticketData.getJSONObject(i).getString("created_at"));
				for (int f=0; f<ticketData.getJSONObject(i).getJSONArray("custom_fields").length(); f++){
					bw.write(" ~ " + ticketData.getJSONObject(i).getJSONArray("custom_fields").getJSONObject(f).getString("value"));
				}
				bw.newLine();
				if (ticketData.getJSONObject(i).has("comments")) {
					for (int c=0; c<ticketData.getJSONObject(i).getJSONArray("comments").length(); c++) {
						bw.write(ticketData.getJSONObject(i).getString("id") + " ~ "
								+ ticketData.getJSONObject(i).getString("subject").replaceAll("[\\t\\n\\r]+", " ") + " ~ "
								+ ticketData.getJSONObject(i).getString("requester_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("submitter_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("assignee_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("organization_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("group_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("tags") + " ~ "
//								+ ticketData.getJSONObject(i).getString("ticket_form_id") + " ~ "
								+ ticketData.getJSONObject(i).getString("brand_id") + " ~ "
								+ ticketData.getJSONObject(i).getJSONArray("comments").getJSONObject(c).getString("body").replaceAll("[\\t\\n\\r]+", " ").replaceAll("~", " ") + " ~ "
								+ ticketData.getJSONObject(i).getJSONArray("comments").getJSONObject(c).getString("author_id") + " ~ "
								+ ticketData.getJSONObject(i).getJSONArray("comments").getJSONObject(c).getString("created_at"));
						bw.newLine();
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
