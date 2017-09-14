package dev;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import util.ConnectProperties.api;
import util.LdapUtil;
import util.ZendeskAPICall;

public class TestLdapAuthentication {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		JSONObject response = null;
		String username = "administrator";
		String password = "admin";
		String host = "192.168.90.140:389";
		HashMap<String, String> hMap = new HashMap<String, String>();

		@SuppressWarnings("rawtypes")
		ArrayList data = LdapUtil.getEmailAndFullName(host, username, password);
		System.out.println(data);
		for (int i = 0; i < data.size(); i++) {
			hMap = (HashMap<String, String>) data.get(i);
			System.out.println(hMap);
			if (hMap.get("zendeskflag").equalsIgnoreCase("n")) {
				if (!hMap.get("mail").equalsIgnoreCase("null")) {
					System.out.println("CREATE NEW AGENT");
					try {
						response = ZendeskAPICall.makeRequest(api.userCreate,
								new JSONObject().put("user", new JSONObject().put("name", hMap.get("name").toString())
										.put("email", hMap.get("mail").toString()).put("role", "agent"))
						/* FIXME add msadusername */
						/*
						 * .put("user_fields", new
						 * JSONObject().put("msadusername",
						 * hMap.get("msadusername").toString()))
						 */);
						System.out.println(response);
						if (response.get("responseCode").toString().equals("200")
								|| response.get("responseCode").toString().equals("201")) {
							LdapUtil.modifiedAtr(host, username, password, hMap.get("name"));
						} else {
							System.out.println("CREATE FAILED : " + response.getString("error"));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
			/* FIXME for testing only */
			// LdapUtil.modifiedAtr(host, username, password, hMap.get("name"));
		}
	}
}
