package ticket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dev.HttpDownloadUtility;
import dev.UsersWriter;
import util.ConnectProperties;
import util.ErrorLogWriter;

public class GetTicketList {

	static int TIMED_OUT = 10000;
	static String PROXY_IP = "172.21.8.65";
	static int PROXY_PORT = 8080;
	private final static String FAILED_ORG_NULL = "Failed to Sync to CRM due cannot get Organization/Account info";
//	private final static String FAILED_ORG_GUID_NULL = "Failed to Sync to CRM due cannot get Organization/Account GUID or Organization/Account GUID is null";
	private final static String FAILED_CREATE_NOTE_ERROR = "Failed to Sync to CRM Note Error";
	private final static String FAILED_CREATE_TICKET_ERROR = "Failed to Sync to CRM Ticket Error";

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZWxkaWVuLmhhc21hbnRvQHRyZWVzc29sdXRpb25zLmNvbTpXZWxjb21lMQ==";
	private final static String ZENDESK_DOMAIN = "https://idsmed1475491095.zendesk.com";

	// FIXME PROD CREDENTiAL
	// private final static String TOKEN =
	// "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";
	// private final static String ZENDESK_DOMAIN =
	// "https://idsmed.zendesk.com";

//	private final static String URL_TICKET_ZENDESK = ZENDESK_DOMAIN + "/api/v2/search.json?query=type:ticket%20645";
	private final static String URL_TICKET_ZENDESK = ZENDESK_DOMAIN +"/api/v2/search.json?query=type:ticket%20updated%3E10minutes&sort_by=created_at&sort_order=asc";
	// private final static String URL_TICKET_ZENDESK_TESTING = ZENDESK_DOMAIN +
	// "/api/v2/search.json?query=160";

	private final static String URL_CREATE_TICKET_CRM = "https://idsmedmobileapps.lfuat.net/zendesk-api/Zendesk.asmx/createCase";
	private final static String URL_UPDATE_TICKET_CRM = "https://idsmedmobileapps.lfuat.net/zendesk-api/Zendesk.asmx/updateCase";
	private final static String URL_CREATE_NOTES_CRM = "https://idsmedmobileapps.lfuat.net/zendesk-api/Zendesk.asmx/CreateNote";

	//FIXME UAT
	private final static String SYNC_TO_CRM = "52066448";
	private final static String CASE_TYPE = "46314427";
	private final static String INSTALL_BASE_ID = "46314467";
	private final static String CASE_CATEGORY = "43566688";

	// private final static String SYNC_TO_CRM = "76932108";
	// private final static String CASE_TYPE = "47282847";
	// private final static String INSTALL_BASE_ID = "47283467";
	// private final static String CASE_CATEGORY = "43566688";

	private final static String CASE_SERVICE = "service";

	private final static String SYNCED = "synced";
	private final static String PENDING = "pending";
	private final static String FAILED = "failed";

	static JSONArray jsonErr = new JSONArray();

	private static String AUTH_TOKEN = "Ehjp5St4nGGDsK7CZu16M2ztcz3LG00uDAT8F63epFw=";

	static Date oneHourBack = null;
	static boolean isChild = true;
	static boolean isProd = new ConnectProperties().isTesting;
	static boolean willSync = false;
	static boolean isServiceCase = false;

	public static void main(String[] args) {
//		HashMap<String, String> errorLogHash = new HashMap<String, String>();
//		ArrayList<HashMap<String, String>> errorLogsArray = new ArrayList<>();
		JSONArray errorJsonArray = new JSONArray();
		// FIXME TRY ENCODING
		/*
		 * String encodedUrl; try { String credent =
		 * "eldien.hasmanto@treessolutions.com:Welcome1"; encodedUrl =
		 * URLEncoder.encode(credent, "UTF-8"); System.out.println(encodedUrl);
		 * byte[] baseAuth = Base64.encodeBase64(credent.getBytes());
		 * System.out.println(new String(baseAuth)); } catch
		 * (UnsupportedEncodingException e1) { // TODO Auto-generated catch
		 * block e1.printStackTrace(); }
		 */

		String new_customer = "";

		Date currentDate = new Date();
		String pattern = "dd-MM-yyyy";
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		Calendar cal = Calendar.getInstance();
		cal.setTime(currentDate);
		if (isProd) {
			// - 8 to adjust UTC
			// - 12 to adjust Windows Scheduler
			cal.add(Calendar.HOUR, -8);
			cal.add(Calendar.MINUTE, -10);
		} else {
			// - 7 to adjust UTC
			cal.add(Calendar.HOUR, -7);
			cal.add(Calendar.MINUTE, -70);
		}
		oneHourBack = cal.getTime();

		// String parentId = null;
		String stateCode = null;
		String caseStatus = null;
		Boolean isSynced = false;
		String ownerID = null;
		boolean justComment = false;

		try {
			JSONObject jsonTicket = CallUrl.readJsonFromUrl(URL_TICKET_ZENDESK, "GET");
			JSONArray ticketList = jsonTicket.getJSONArray("results");
			if (ticketList.length() > 0) {
				for (int i = 0; i < ticketList.length(); i++) {
					justComment = false;
					String ticketID = ticketList.getJSONObject(i).get("id").toString();
					JSONArray custom_fields = ticketList.getJSONObject(i).getJSONArray("custom_fields");
					JSONArray tags = ticketList.getJSONObject(i).getJSONArray("tags");
					isChild = false;
					JSONObject ticket = new JSONObject();
					System.out.println("ticket number: " + ticketID);
					for (int h = 0; h < custom_fields.length(); h++) {
						if (custom_fields.getJSONObject(h).get("id").toString().equals(SYNC_TO_CRM)) {
							if (custom_fields.getJSONObject(h).get("value").toString().equals(SYNCED)) {
								justComment = true;
								isSynced = true;
							} else if (custom_fields.getJSONObject(h).get("value").toString().equals(PENDING)) {
								isSynced = true;
							} else {
								isSynced = false;
							}
						}
					}
					StringBuffer URL_USERS_ZENDESK = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/users/");
					URL_USERS_ZENDESK.append(ticketList.getJSONObject(i).get("requester_id").toString())
							.append(".json");
					JSONObject zendUser = CallUrl.readJsonFromUrl(URL_USERS_ZENDESK.toString(), "GET");
//					String contactGUID = zendUser.getJSONObject("user").getJSONObject("user_fields").get("contactguid")
//							.toString();
//					if (zendUser.getJSONObject("user").get("organization_id").toString().equals("null")) {
//						System.out.println("ORG is NULL");
//						updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
//					} else {
						
//						System.out.println("ACCOUNT GUID: " + getOrg.getJSONObject("organization")
//								.getJSONObject("organization_fields").get("accountguid").toString());
//						if (getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid")
//								.toString().equalsIgnoreCase("null")) {
//							System.out.println("ORG GUID is NULL");
//							updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
//						} else {

							for (int cf = 0; cf < custom_fields.length(); cf++) {
								if (custom_fields.getJSONObject(cf).get("id").toString().equals(CASE_CATEGORY)) {
									if (custom_fields.getJSONObject(cf).get("value").toString().equals(CASE_SERVICE)) {
										System.out.println("IT IS SERVICE CASE..");
										isServiceCase = true;
									} else {
										System.out.println("IT ISN'T SERVICE CASE..");
										isServiceCase = false;
									}
								}
							}

							if (tags.length() == 0) {
								System.out.println("THIS TICKET HAS NO TAG");
								isChild = false;

								if (isServiceCase) {
									willSync = true;
								} else {
									willSync = false;
								}

							} else {
								for (int t = 0; t < tags.length(); t++) {
									if (tags.get(t).toString().contains("project_parent")) {
										System.out.println("PARENT CASE FOUND");
										willSync = true;
										isChild = false;
									} else if (tags.get(t).toString().contains("project_child")) {
										System.out.println("CHILD CASE FOUND");
										willSync = false;
										isChild = true;
									}
								}
								if (!willSync && !isChild) {
									if (isServiceCase) {
										willSync = true;
									} else {
										willSync = false;
									}
								}
							}
							System.out.println("THIS TICKET " + ticketID + " will be sync : " + willSync);
							// FIXME LANJUT BROOHH..!
							if (willSync) {
								if (isSynced && justComment) {
									// if (!isChild) {
									System.out.println("SYNCED already");
									createComment(ticketList.getJSONObject(i), false);
									// }
								} else if (isSynced) {
									System.out.println("PENDING sync");
									// FIXME UPDATE CASE
									System.out.println("already synced, begin UPDATE");
									ticket.put("AuthenticationToken", AUTH_TOKEN);
									ticket.put("TicketNo", ticketID);
									switch (ticketList.getJSONObject(i).get("status").toString()) {
									case "open":
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									case "new":
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									case "hold":
										caseStatus = "On Hold";
										stateCode = "Active";
										break;
									case "pending":
										caseStatus = "Waiting for Details";
										stateCode = "Active";
										break;
									case "solved":
										caseStatus = "Problem Resolved";
										stateCode = "Inactive";
										break;
									default:
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									}
									ticket.put("Status", caseStatus);
									ticket.put("StateCode", stateCode);
									System.out.println(new JSONObject().put("ticket", ticket));
									if (zendUser.getJSONObject("user").get("organization_id").toString().equals("null")) {
										updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
										errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error", "ORG is NULL"));
//										errorLogHash.put("ticketID", ticketID);
//										errorLogHash.put("error", "ORG is NULL");
//										errorLogsArray.add(errorLogHash);
//										errorLogHash.clear();
									} else {
										StringBuffer URL_ZENDESK_USER_ORG = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/organizations/")
												.append(zendUser.getJSONObject("user").get("organization_id")).append(".json");
										JSONObject getOrg = CallUrl.readJsonFromUrl(URL_ZENDESK_USER_ORG.toString(), "GET");
										
										if (getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString().equalsIgnoreCase("null")) {
											System.out.println("ORG GUID is NULL");
											updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
											errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error", "ORG GUID is NULL"));
//											errorLogHash.put("ticketID", ticketID);
//											errorLogHash.put("error", "ORG GUID is NULL");
//											errorLogsArray.add(errorLogHash);
//											errorLogHash.clear();
										} else {
											ticket.put("CustomerID", getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString());
											JSONObject updateCase = writeJsonFromUrl(URL_UPDATE_TICKET_CRM, "POST",
													new JSONObject().put("ticket", ticket));
											System.out.println("NEW TICKET:");
											System.out.println(new JSONObject().put("ticket", ticket));
											System.out.println(updateCase);
											if (updateCase.has("Error")
													|| updateCase.get("responseCode").toString().equalsIgnoreCase("500")) {
												System.out.println("UPDATE FAILED");
												updateTicketFailed(FAILED_CREATE_TICKET_ERROR, ticketID, SYNC_TO_CRM, PENDING);
												errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error", updateCase.getString("Error")));
//												errorLogHash.put("ticketID", ticketID);
//												errorLogHash.put("error", updateCase.getString("Error"));
//												errorLogsArray.add(errorLogHash);
//												errorLogHash.clear();
											} else {
												updateTicketSuccess(ticketID, SYNC_TO_CRM, SYNCED);
												createComment(ticketList.getJSONObject(i), false);
											}
										}
									}
									
								} else {
									System.out.println("FAILED or CREATE new");
									// FIXME CREATE NEW CASE
									// FIXME GET OWNER ID
									if (ticketList.getJSONObject(i).get("assignee_id").toString().equals("null")) {
										ownerID = "GAMMA\\FaradilaUtami";
									} else {
										URL_USERS_ZENDESK = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/users/")
												.append(ticketList.getJSONObject(i).get("assignee_id")).append(".json");
										JSONObject zendAssUser = CallUrl.readJsonFromUrl(URL_USERS_ZENDESK.toString(),
												"GET");
										if (zendAssUser.getJSONObject("user").getJSONObject("user_fields")
												.get("msadusername").toString().equalsIgnoreCase("null")
												|| zendAssUser.getJSONObject("user").getJSONObject("user_fields")
														.get("msadusername").toString().equalsIgnoreCase("null")) {
											ownerID = "GAMMA\\FaradilaUtami";
										} else {
											ownerID = zendAssUser.getJSONObject("user").getJSONObject("user_fields")
													.get("msadusername").toString();
											// System.out.println(ownerID);
										}
									}
									String caseType = null;
									for (int z = 0; z < custom_fields.length(); z++) {
										if (custom_fields.getJSONObject(z).get("id").toString().equals(CASE_TYPE)) {
											switch (custom_fields.getJSONObject(z).get("value").toString()) {
											case "request_installation_commissioning":
												caseType = "Request - Installation & Commissioning";
												break;
											case "customer_service":
												caseType = "Customer Service";
												break;
											case "request_preventive_maintenance":
												caseType = "Request - Preventive Maintenance";
												break;
											case "training":
												caseType = "Training";
												break;
											case "promotion":
												caseType = "Promotion";
												break;
											case "question":
												caseType = "Question";
												break;
											case "problem":
												caseType = "Problem";
												break;
											case "training_edu":
												caseType = "Training-Edu";
												break;
											case "routine_visit":
												caseType = "Routine Visit";
												break;
											default:
												caseType = "Customer Service";
												break;
											}
										} else if (custom_fields.getJSONObject(z).get("id").toString()
												.equals(INSTALL_BASE_ID)) {
											if (!custom_fields.getJSONObject(z).get("value").toString()
													.equalsIgnoreCase("null")) {
												ticket.put("InstalledBaseID",
														custom_fields.getJSONObject(z).get("value").toString());
											}
										}
									}
									ticket.put("AuthenticationToken", AUTH_TOKEN);
									ticket.put("TicketNo", ticketID);
									ticket.put("Subject", ticketList.getJSONObject(i).get("subject"));
									ticket.put("CustomerID", new_customer.replace("{", "").replace("}", ""));
									switch (ticketList.getJSONObject(i).get("status").toString()) {
									case "open":
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									case "new":
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									case "hold":
										caseStatus = "On Hold";
										stateCode = "Active";
										break;
									case "Pending":
										caseStatus = "Waiting for Details";
										stateCode = "Active";
										break;
									case "solved":
										caseStatus = "Problem Resolved";
										stateCode = "Inactive";
										break;
									default:
										caseStatus = "In Progress";
										stateCode = "Active";
										break;
									}
									ticket.put("Type", caseType);
									ticket.put("Status", caseStatus);
									ticket.put("StateCode", stateCode);
									ticket.put("OwnerID", ownerID.replace("\\\\", "\\"));
									// System.out.println(ownerID);
									System.out.println(new JSONObject().put("ticket", ticket));
//									if (new_customer.equalsIgnoreCase("null")) {
//										System.out.println("CANNOT GET GUID/GUID IS NULL");
//										updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
//									} else {
									if (zendUser.getJSONObject("user").get("organization_id").toString().equals("null")) {
										updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
										errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error", "ORG is NULL"));
//										errorLogHash.put("ticketID", ticketID);
//										errorLogHash.put("error", "ORG is NULL");
//										errorLogsArray.add(errorLogHash);
//										errorLogHash.clear();
										
									} else {
										StringBuffer URL_ZENDESK_USER_ORG = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/organizations/")
												.append(zendUser.getJSONObject("user").get("organization_id")).append(".json");
										JSONObject getOrg = CallUrl.readJsonFromUrl(URL_ZENDESK_USER_ORG.toString(), "GET");
//										getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString();
										if (getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString().equalsIgnoreCase("null")) {
											System.out.println("ORG GUID is NULL");
											updateTicketFailed(FAILED_ORG_NULL, ticketID, SYNC_TO_CRM, FAILED);
											errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error", "ORG GUID is NULL"));
//											errorLogHash.put("ticketID", ticketID);
//											errorLogHash.put("error", "ORG GUID is NULL");
//											errorLogsArray.add(errorLogHash);
//											errorLogHash.clear();
										} else {
											ticket.put("CustomerID", getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString());
											JSONObject createCase = writeJsonFromUrl(URL_CREATE_TICKET_CRM, "POST",
													new JSONObject().put("ticket", ticket));
											System.out.println("NEW TICKET:");
											System.out.println(new JSONObject().put("ticket", ticket));
											System.out.println(createCase);
											if (createCase.has("Error")) {
												System.out.println();
												updateTicketFailed(FAILED_CREATE_TICKET_ERROR, ticketID, SYNC_TO_CRM, FAILED);
												errorJsonArray.put(new JSONObject().put("ticketID", ticketID).put("error",createCase.getString("Error")));
//												errorLogHash.put("ticketID", ticketID);
//												errorLogHash.put("error", createCase.getString("Error"));
//												errorLogsArray.add(errorLogHash);
//												errorLogHash.clear();
											} else {
												updateTicketSuccess(ticketID, SYNC_TO_CRM, SYNCED);
												System.out.println("CREATE NOTES");
												createComment(ticketList.getJSONObject(i), true);
											}
										}
									}
								}
							} else {
								System.out.println("THIS TICKET WILL NOT BE SYNCED");
							}
//						}
//					}

					// FIXME OLD FLOW
					/*
					 * if (tags.length() == 0){ System.out.println("Ticket: " +
					 * ticketID + " is a CHILD ticket.."); isChild = true; }
					 * 
					 * for (int y = 0; y < tags.length(); y++) {
					 * if(tags.get(y).toString().contains("project_parent")) {
					 * System.out.println("Ticket: " + ticketID +
					 * " is a PARENT ticket.."); isChild = false; } }
					 * System.out.println("Ticket is CHILD Ticket: " + isChild);
					 * System.out.println("CONTACT GUID: "+ contactGUID); if
					 * (zendUser.getJSONObject("user").get("organization_id").
					 * toString().equals("null")) { new_customer = contactGUID;
					 * } else { StringBuffer URL_ZENDESK_USER_ORG = new
					 * StringBuffer(ZENDESK_DOMAIN + "/api/v2/organizations/")
					 * .append(zendUser.getJSONObject("user").get(
					 * "organization_id")).append(".json"); JSONObject getOrg =
					 * CallUrl.readJsonFromUrl(URL_ZENDESK_USER_ORG.toString(),
					 * "GET"); System.out.println("ACCOUNT GUID: " +
					 * getOrg.getJSONObject("organization").getJSONObject(
					 * "organization_fields") .get("accountguid").toString());
					 * if (getOrg.getJSONObject("organization").getJSONObject(
					 * "organization_fields")
					 * .get("accountguid").toString().equalsIgnoreCase("null")){
					 * new_customer = contactGUID; } else { new_customer =
					 * "ACCOUNT GUID: " +
					 * getOrg.getJSONObject("organization").getJSONObject(
					 * "organization_fields") .get("accountguid").toString(); }
					 * }
					 */

					// if
					// (getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid")
					// .toString().equalsIgnoreCase("null")) {
					// updateTicketFailed(FAILED_ORG_GUID_NULL, ticketID,
					// SYNC_TO_CRM, FAILED);
					// } else {

					// FIXME OLD FLOW PT.2
					/*
					 * if (isSynced && justComment) { if (!isChild) {
					 * System.out.println("SYNCED already");
					 * createComment(ticketList.getJSONObject(i), false); } }
					 * else if (isSynced) { System.out.println("PENDING sync");
					 * // FIXME UPDATE CASE
					 * System.out.println("already synced, begin UPDATE");
					 * ticket.put("AuthenticationToken", AUTH_TOKEN);
					 * ticket.put("TicketNo", ticketID); switch
					 * (ticketList.getJSONObject(i).get("status").toString()) {
					 * case "open": caseStatus = "In Progress"; stateCode =
					 * "Active"; break; case "new": caseStatus = "In Progress";
					 * stateCode = "Active"; break; case "hold": caseStatus =
					 * "On Hold"; stateCode = "Active"; break; case "pending":
					 * caseStatus = "Waiting for Details"; stateCode = "Active";
					 * break; case "solved": caseStatus = "Problem Resolved";
					 * stateCode = "Inactive"; break; default: caseStatus =
					 * "In Progress"; stateCode = "Active"; break; }
					 * ticket.put("Status", caseStatus); ticket.put("StateCode",
					 * stateCode); System.out.println(new
					 * JSONObject().put("ticket", ticket)); JSONObject
					 * updateCase = writeJsonFromUrl(URL_UPDATE_TICKET_CRM,
					 * "POST", new JSONObject().put("ticket", ticket));
					 * System.out.println(updateCase); if (!isChild) { if
					 * (updateCase.has("Error")) {
					 * updateTicketFailed(FAILED_CREATE_TICKET_ERROR, ticketID,
					 * SYNC_TO_CRM, PENDING); } else {
					 * updateTicketSuccess(ticketID, SYNC_TO_CRM, SYNCED);
					 * createComment(ticketList.getJSONObject(i), false); } } }
					 * else { if(!isChild){
					 * System.out.println("FAILED or CREATE new"); // FIXME
					 * CREATE NEW CASE // FIXME GET OWNER ID if
					 * (ticketList.getJSONObject(i).get("assignee_id").toString(
					 * ).equals("null")) { ownerID = "GAMMA\\FaradilaUtami"; }
					 * else { URL_USERS_ZENDESK = new
					 * StringBuffer(ZENDESK_DOMAIN + "/api/v2/users/")
					 * .append(ticketList.getJSONObject(i).get("assignee_id")).
					 * append(".json"); JSONObject zendAssUser =
					 * CallUrl.readJsonFromUrl(URL_USERS_ZENDESK.toString(),
					 * "GET"); if
					 * (zendAssUser.getJSONObject("user").getJSONObject(
					 * "user_fields")
					 * .get("msad_username").toString().equalsIgnoreCase("null")
					 * || zendAssUser.getJSONObject("user").getJSONObject(
					 * "user_fields")
					 * .get("msad_username").toString().equalsIgnoreCase("null")
					 * ) { ownerID = "GAMMA\\FaradilaUtami"; } else { ownerID =
					 * zendAssUser.getJSONObject("user").getJSONObject(
					 * "user_fields") .get("msad_username").toString(); //
					 * System.out.println(ownerID); } } //
					 * System.out.println("not synced, begin // CREATE"); String
					 * caseType = null; for (int z = 0; z <
					 * custom_fields.length(); z++) { if
					 * (custom_fields.getJSONObject(z).get("id").toString().
					 * equals(CASE_TYPE)) { switch
					 * (custom_fields.getJSONObject(z).get("value").toString())
					 * { case "request_installation_commissioning": caseType =
					 * "Request - Installation & Commissioning"; break; case
					 * "customer_service": caseType = "Customer Service"; break;
					 * case "request_preventive_maintenance": caseType =
					 * "Request - Preventive Maintenance"; break; case
					 * "training": caseType = "Training"; break; case
					 * "promotion": caseType = "Promotion"; break; case
					 * "question": caseType = "Question"; break; case "problem":
					 * caseType = "Problem"; break; case "training_edu":
					 * caseType = "Training-Edu"; break; case "routine_visit":
					 * caseType = "Routine Visit"; break; default: caseType =
					 * "Customer Service"; break; } } else if
					 * (custom_fields.getJSONObject(z).get("id").toString()
					 * .equals(INSTALL_BASE_ID)) { if
					 * (!custom_fields.getJSONObject(z).get("value").toString()
					 * .equalsIgnoreCase("null")) {
					 * ticket.put("InstalledBaseID",
					 * custom_fields.getJSONObject(z).get("value").toString());
					 * } } } // FIXME IS A CHILD TICKET? // for (int y = 0; y <
					 * tags.length(); y++) { // if //
					 * (tags.get(y).toString().matches(".*\\d+.*")) // {
					 * // @SuppressWarnings("resource") // Scanner in = new //
					 * Scanner(tags.get(y).toString()).useDelimiter("[^0-9]+");
					 * // int integer = in.nextInt(); // parentId =
					 * String.valueOf(integer); // } // if //
					 * (tags.get(y).toString().contains("project_child")) // {
					 * // isChild = true; //
					 * System.out.println("is a child ticket"); //
					 * ticket.put("ParentTicketNo", parentId); // } // }
					 * ticket.put("AuthenticationToken", AUTH_TOKEN);
					 * ticket.put("TicketNo", ticketID); ticket.put("Subject",
					 * ticketList.getJSONObject(i).get("subject"));
					 * ticket.put("CustomerID", new_customer.replace("{",
					 * "").replace("}", "")); switch
					 * (ticketList.getJSONObject(i).get("status").toString()) {
					 * case "open": caseStatus = "In Progress"; stateCode =
					 * "Active"; break; case "new": caseStatus = "In Progress";
					 * stateCode = "Active"; break; case "hold": caseStatus =
					 * "On Hold"; stateCode = "Active"; break; case "Pending":
					 * caseStatus = "Waiting for Details"; stateCode = "Active";
					 * break; case "solved": caseStatus = "Problem Resolved";
					 * stateCode = "Inactive"; break; default: caseStatus =
					 * "In Progress"; stateCode = "Active"; break; }
					 * ticket.put("Type", caseType); ticket.put("Status",
					 * caseStatus); ticket.put("StateCode", stateCode);
					 * ticket.put("OwnerID", ownerID.replace("\\\\", "\\")); //
					 * System.out.println(ownerID); System.out.println(new
					 * JSONObject().put("ticket", ticket)); if
					 * (new_customer.equalsIgnoreCase("null")){
					 * System.out.println("CANNOT GET GUID/GUID IS NULL");
					 * updateTicketFailed(FAILED_ORG_GUID_NULL, ticketID,
					 * SYNC_TO_CRM, FAILED); } else { JSONObject createCase =
					 * writeJsonFromUrl(URL_CREATE_TICKET_CRM, "POST", new
					 * JSONObject().put("ticket", ticket));
					 * System.out.println(createCase); if
					 * (createCase.has("Error")) {
					 * updateTicketFailed(FAILED_CREATE_TICKET_ERROR, ticketID,
					 * SYNC_TO_CRM, FAILED); } else { if(!isChild){
					 * updateTicketSuccess(ticketID, SYNC_TO_CRM, SYNCED);
					 * System.out.println("CREATE NOTES");
					 * createComment(ticketList.getJSONObject(i), true); } } }
					 * 
					 * } }
					 */
					// }
					// }
				}
				UsersWriter.writeLogs(errorJsonArray);
				System.out.println(errorJsonArray);
				System.out.println("Writting error logs");
				errorJsonArray = new JSONArray();
			}
			System.out.println("BATCH RUNNING SUCCESSFUL");
			System.out.println("Done running batch..");
		} catch (NullPointerException e) {
			e.printStackTrace();
			ErrorLogWriter.errorCon(format.format(new Date()));
			System.out.println("NULL");
		} catch (JSONException x) {
			x.printStackTrace();
			System.out.println("JSONS");
		}
	}
	
	public static boolean orgGuidIsNull(StringBuffer org_url) throws JSONException {
		JSONObject getOrg = CallUrl.readJsonFromUrl(org_url.toString(), "GET");
		getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString();
		
		return getOrg.getJSONObject("organization").getJSONObject("organization_fields").get("accountguid").toString()
				.equalsIgnoreCase("null");
	}

	public static void createComment(JSONObject ticketList, boolean newTicket) throws JSONException {
		// Date date1 = null;
		Date date2 = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		JSONObject notes = new JSONObject();
		JSONObject comments = getTicketComments(ticketList.get("id").toString());
		JSONArray commentz = comments.getJSONArray("comments");

		for (int a = 0; a < commentz.length(); a++) {
			notes = new JSONObject();
			try {
				date2 = sdf.parse(commentz.getJSONObject(a).get("created_at").toString());
			} catch (ParseException e) {
				e.printStackTrace();
			}
			if (newTicket) {
				System.out.println("new comment on new ticket");
				notes.put("AuthenticationToken", AUTH_TOKEN);
				notes.put("TicketNo", ticketList.get("id").toString());
				notes.put("Subject", ticketList.get("subject"));
				notes.put("Body", commentz.getJSONObject(a).get("body"));
				System.out.println(notes);
				// FIXME FIND ATTACHMENTS
				if (commentz.getJSONObject(a).getJSONArray("attachments").length() > 0) {
					for (int b = 0; b < commentz.getJSONObject(a).getJSONArray("attachments").length(); b++) {
						String fileURL = commentz.getJSONObject(a).getJSONArray("attachments").getJSONObject(b)
								.get("content_url").toString();
						try {
							String base64 = HttpDownloadUtility.downloadFile(fileURL);
							notes.put("FileName", commentz.getJSONObject(a).getJSONArray("attachments").getJSONObject(b)
									.get("file_name"));
							notes.put("FileBody", base64);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				// System.out.println(new JSONObject().put("note", notes));
				JSONObject createNotes = writeJsonFromUrl(URL_CREATE_NOTES_CRM, "POST",
						new JSONObject().put("note", notes));
				// System.out.println(createNotes);
				if (createNotes.has("Error")) {
					updateTicketFailed(FAILED_CREATE_NOTE_ERROR, ticketList.get("id").toString(), SYNC_TO_CRM, PENDING);
				} else {
					System.out.println("DONE");
					updateTicketSuccess(ticketList.get("id").toString(), SYNC_TO_CRM, SYNCED);
				}
			} else {
				if (date2.compareTo(oneHourBack) > 0) {
					System.out.println("new comment");
					notes.put("AuthenticationToken", AUTH_TOKEN);
					notes.put("TicketNo", ticketList.get("id").toString());
					notes.put("Subject", ticketList.get("subject"));
					notes.put("Body", commentz.getJSONObject(a).get("body"));
					System.out.println(notes);
					// FIXME FIND ATTACHMENTS
					if (commentz.getJSONObject(a).getJSONArray("attachments").length() > 0) {
						for (int b = 0; b < commentz.getJSONObject(a).getJSONArray("attachments").length(); b++) {
							String fileURL = commentz.getJSONObject(a).getJSONArray("attachments").getJSONObject(b)
									.get("content_url").toString();
							try {
								String base64 = HttpDownloadUtility.downloadFile(fileURL);
								notes.put("FileName", commentz.getJSONObject(a).getJSONArray("attachments")
										.getJSONObject(b).get("file_name"));
								notes.put("FileBody", base64);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}
					// System.out.println(new JSONObject().put("note", notes));
					JSONObject createNotes = writeJsonFromUrl(URL_CREATE_NOTES_CRM, "POST",
							new JSONObject().put("note", notes));
					// System.out.println(createNotes);
					if (createNotes.has("Error")) {
						updateTicketFailed(FAILED_CREATE_NOTE_ERROR, ticketList.get("id").toString(), SYNC_TO_CRM,
								PENDING);
					} else {
						System.out.println("DONE create NOTES");
						updateTicketSuccess(ticketList.get("id").toString(), SYNC_TO_CRM, SYNCED);
					}
				} else {
					System.out.println("No New Notes Created");
				}
			}
		}
	}

	private static JSONObject getTicketComments(String ticketId) {
		StringBuffer URL_ZENDESK_TICKET_COMMENT = new StringBuffer(
				ZENDESK_DOMAIN + "/api/v2/tickets/" + ticketId + "/comments.json");
		JSONObject comment = CallUrl.readJsonFromUrl(URL_ZENDESK_TICKET_COMMENT.toString(), "GET");
		return comment;
	}

	public static JSONObject updateTicketFailed(String reason, String ticketId, String key, String flags)
			throws JSONException {
//		String realReason = "";
		StringBuffer URL_UPDATE_TICKET_ZENDESK = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/tickets/");
		URL_UPDATE_TICKET_ZENDESK.append(ticketId).append(".json");
		JSONObject updateZendTicketBody = new JSONObject();
		updateZendTicketBody.put("status", "open");
		updateZendTicketBody.put("custom_fields", new JSONObject().put("id", key).put("value", flags));
		if (!reason.equalsIgnoreCase(FAILED_ORG_NULL)) {
			updateZendTicketBody.put("comment", new JSONObject().put("body", reason).put("public", "false"));
		}
		JSONObject updateZendTicket = writeJsonFromUrl(URL_UPDATE_TICKET_ZENDESK.toString(), "PUT",
				new JSONObject().put("ticket", updateZendTicketBody));
		// System.out.println(updateZendTicket);
		return updateZendTicket;
	}

	public static JSONObject updateTicketSuccess(String ticketId, String key, String flags) throws JSONException {
		StringBuffer URL_UPDATE_TICKET_ZENDESK = new StringBuffer(ZENDESK_DOMAIN + "/api/v2/tickets/");
		URL_UPDATE_TICKET_ZENDESK.append(ticketId).append(".json");
		JSONObject updateZendTicketBody = new JSONObject();
		updateZendTicketBody.put("custom_fields", new JSONObject().put("id", key).put("value", flags));
		JSONObject updateZendTicket = writeJsonFromUrl(URL_UPDATE_TICKET_ZENDESK.toString(), "PUT",
				new JSONObject().put("ticket", updateZendTicketBody));
		// System.out.println(updateZendTicket.getJSONObject("ticket").get("id"));
		return updateZendTicket;
	}

	public static JSONObject writeJsonFromUrl(String url, String method, JSONObject ticket) {
		HttpURLConnection con = null;
		JSONObject json = new JSONObject();
		int retry = 1;
		for (int i = 0; i < retry; i++) {
			try {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_IP, PROXY_PORT));
				URL obj = new URL(url);
				if (isProd) {
					con = (HttpURLConnection) obj.openConnection(proxy);
				} else {
					con = (HttpURLConnection) obj.openConnection();
				}

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