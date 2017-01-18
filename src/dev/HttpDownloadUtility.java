package dev;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

public class HttpDownloadUtility {
	/**
	 * Downloads a file from a URL
	 * 
	 * @param fileURL
	 *            HTTP URL of the file to be downloaded
	 * @param saveDir
	 *            path of the directory to save the file
	 * @return
	 * @throws IOException
	 */
	public static String downloadFile(String fileURL) throws IOException {
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
		httpConn.addRequestProperty("Authorization", "Basic ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM");
		httpConn.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
		int responseCode = httpConn.getResponseCode();
		byte[] bytes = null;

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {
			String fileName = "";
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			if (disposition != null) {
				// extracts file name from header field
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				// extracts file name from URL
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
			}

			System.out.println("Content-Type = " + contentType);
			System.out.println("Content-Disposition = " + disposition);
			System.out.println("Content-Length = " + contentLength);
			System.out.println("fileName = " + fileName);

			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();
			bytes = IOUtils.toByteArray(inputStream);
//			System.out.println(Base64.encodeBase64String(bytes));

			// FileInputStream fin = (FileInputStream) inputStream;
			// byte[] imageBytes = new byte[(int)];
			// fin.read(imageBytes, 0, imageBytes.length);
			// fin.close();
			// return Base64.encodeToString(imageBytes, Base64);

			// opens an output stream to save into file
			// FileOutputStream outputStream = new
			// FileOutputStream(saveFilePath);

			// int bytesRead = -1;
			// byte[] buffer = new byte[BUFFER_SIZE];
			// while ((bytesRead = inputStream.read(buffer)) != -1) {
			// outputStream.write(buffer, 0, bytesRead);
			// }
			//
			// outputStream.close();
			inputStream.close();
			// System.out.println(Base64.encode);
		} else {
			System.out.println("No file to download. Server replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
		return Base64.encodeBase64String(bytes);
	}
}
