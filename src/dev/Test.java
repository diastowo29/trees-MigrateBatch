package dev;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Test {
	public static void main(String[] args) {
		// int test = 101;
		// System.out.println(test%100 == 0);
		Date currentDate = new Date();
		SimpleDateFormat df = new SimpleDateFormat("MM_dd_YYYY_HH_mm_ss Z");
		System.out.println(df.format(currentDate));
	}
}