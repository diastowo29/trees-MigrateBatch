package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class ErrorLogWriter {
	@SuppressWarnings("deprecation")
	public static void errorCon(String date) {
		try {
			File file = new File("C:/users/Diastowo/" + date + "_ErrorLog.txt");

			Date currentDate = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(currentDate);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(cal.getTime().toLocaleString() +" - Error due LOST CONNECTION/NO ACTIVE CONNECTION");
			bw.close();

			System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
