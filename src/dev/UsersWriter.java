package dev;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;

public class UsersWriter {
	public static void main(String[] args, JSONArray usersArr, int count) throws JSONException {
		try {
			File file = new File("C:/users/Diastowo/" + count + "_USER.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < usersArr.length(); i++) {
				for (int j = 0; j < usersArr.getJSONArray(i).length(); j++) {
					bw.newLine();
					boolean isOrg = true;
					if(isOrg ){
						bw.write(usersArr.getJSONArray(i).getJSONObject(j).getString("id") + ", "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("name") + ", "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("external_id") + ", "
								+ usersArr.getJSONArray(i).getJSONObject(j).getJSONObject("organization_fields")
										.getString("accountguid"));
					} else {
						bw.write(usersArr.getJSONArray(i).getJSONObject(j).getString("id") + ", "
								+ usersArr.getJSONArray(i).getJSONObject(j).getString("email") + ", "
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
}
