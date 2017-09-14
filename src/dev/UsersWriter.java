package dev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

public class UsersWriter {
	public static void main(String[] args, JSONArray usersArr, int count, boolean isOrg, String newData)
			throws JSONException {
		try {
			File file = new File("C:/Users/Aumiz/Documents/IDSMED/IDSMEDProd_" + newData + "_" + count + ".txt");
			if (!file.exists()) {
				file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < usersArr.length(); i++) {
				for (int j = 0; j < usersArr.getJSONArray(i).length(); j++) {
					bw.newLine();
					// boolean isOrg = true;
					if (isOrg) {
						bw.write(usersArr.getJSONArray(i).getJSONObject(j).getString("id") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("name") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getJSONObject("organization_fields")
										.getString("accountguid"));
					} else {
						bw.write(usersArr.getJSONArray(i).getJSONObject(j).getString("id") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("name") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("email") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("phone") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("organization_id") + "| "
								+ usersArr.getJSONArray(i).getJSONObject(j).getJSONObject("user_fields")
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
	
	public static void writeLogs(JSONArray errorJsonArray) throws JSONException {
		Date currentDate = new Date();
		SimpleDateFormat df = new SimpleDateFormat("MM_dd_YYYY_HH_mm_ss Z");
		// System.out.println(errorJsonArray);
		// System.out.println(df.format(currentDate));
		try {
			File file = new File("D:/Java Project/Logs_" + df.format(currentDate) + ".txt");
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("TicketsID | Error Reason");
			for (int i = 0; i < errorJsonArray.length(); i++) {
				bw.newLine();
				bw.write(errorJsonArray.getJSONObject(i).getString("ticketID") + " | "
						+ errorJsonArray.getJSONObject(i).getString("error"));
			}
			bw.close();
			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}