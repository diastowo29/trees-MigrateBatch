package dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.eventusermodel.EventWorkbookBuilder.SheetRecordCollectingListener;
import org.apache.poi.hssf.eventusermodel.FormatTrackingHSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.eventusermodel.MissingRecordAwareHSSFListener;
import org.apache.poi.hssf.eventusermodel.dummyrecord.LastCellOfRowDummyRecord;
import org.apache.poi.hssf.eventusermodel.dummyrecord.MissingCellDummyRecord;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BlankRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.MulBlankRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.RowRecord;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A XLS -> CSV processor, that uses the MissingRecordAware EventModel code to
 * ensure it outputs all columns and rows.
 * 
 * @author Nick Burch
 */
public class UploadXLSDB implements HSSFListener {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String TOKEN = "ZmFyYWRpbGF1dGFtaUBpZHNtZWQuY29tOlczbGNvbWUxMjM";
	// private final static String TOKEN =
	// "ZWxkaWVuLmhhc21hbnRvQHRyZWVzc29sdXRpb25zLmNvbTpXM2xjb21lMTIz";
	// TREESDEMO1
	private final static String DOMAIN = "idsmed";
	private static ArrayList<String> logList = new ArrayList<String>();
	// private int lastRow;
	private int thisMaxColumn;
	private int minColumns;
	private POIFSFileSystem fs;
	private int lastRowNumber;
	private int lastColumnNumber;
	private List<String> rowlist = new ArrayList<String>();
	private static String urlAccount;
	private static String METHOD;
	static JSONArray jsonErr = new JSONArray();

	/** Should we output the formula, or the value it has? */
	private boolean outputFormulaValues = true;

	/** For parsing Formulas */
	private SheetRecordCollectingListener workbookBuildingListener;
	private HSSFWorkbook stubWorkbook;

	// Records we pick up as we process
	private SSTRecord sstRecord;
	private FormatTrackingHSSFListener formatListener;

	/** So we known which sheet we're on */
	private int sheetIndex = -1;
	private BoundSheetRecord[] orderedBSRs;
	private List<BoundSheetRecord> boundSheetRecords = new ArrayList<BoundSheetRecord>();

	// For handling formulas with string results
	private int nextRow;
	private int nextColumn;
	private int rowlistRow = 0;
	private int rowlistCount = 0;
	// private int rowCount = 0;
	private boolean outputNextStringRecord;
	private RowRecord rowrec;
	private static String datas;

	StringBuffer INSERT = new StringBuffer();
	StringBuffer CREATE = new StringBuffer();

	private JSONObject newUsersObj = new JSONObject();
	private JSONObject newUsersAnyObj = new JSONObject();
	private JSONObject newUsersArray = new JSONObject();

	private JSONObject newOrgObj = new JSONObject();
	private JSONObject newOrgObjFix = new JSONObject();

	static String filepath;

	private JSONObject newOrgMemObj = new JSONObject();
	private JSONObject newOrgMemObjFix = new JSONObject();

	private static JSONArray updateMany = new JSONArray();

	private StringRecord srec;
	// private JSONArray userArr = new JSONArray();

	/**
	 * Creates a new XLS -> CSV converter
	 * 
	 * @param fs
	 *            The POIFSFileSystem to process
	 * @param output
	 *            The PrintStream to output the CSV to
	 * @param minColumns
	 *            The minimum number of columns to output, or -1 for no minimum
	 * @param tablename2
	 */
	public UploadXLSDB(POIFSFileSystem fs) {
		this.fs = fs;
	}

	/**
	 * Creates a new XLS -> CSV converter
	 * 
	 * @param uploadfile
	 *            The file to process
	 * @param minColumns
	 *            The minimum number of columns to output, or -1 for no minimum
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public UploadXLSDB(String uploadfile) throws IOException, FileNotFoundException {
		this(new POIFSFileSystem(new FileInputStream(uploadfile)));

		// Workbook wb = new HSSFWorkbook(new POIFSFileSystem(new
		// FileInputStream(uploadfile)));
		// Sheet sheet = wb.getSheetAt(0);
		// lastRow = sheet.getLastRowNum();
	}

	/**
	 * Initiates the processing of the XLS file to CSV
	 */
	public void process() throws IOException {
		MissingRecordAwareHSSFListener listener = new MissingRecordAwareHSSFListener(this);
		formatListener = new FormatTrackingHSSFListener(listener);

		HSSFEventFactory factory = new HSSFEventFactory();
		HSSFRequest request = new HSSFRequest();

		if (outputFormulaValues) {
			request.addListenerForAllRecords(formatListener);
		} else {
			workbookBuildingListener = new SheetRecordCollectingListener(formatListener);
			request.addListenerForAllRecords(workbookBuildingListener);
		}

		factory.processWorkbookEvents(request, fs);
	}

	/**
	 * Main HSSFListener method, processes events, and outputs the CSV as the
	 * file is processed.
	 */
	public void processRecord(Record record) {
		int thisRow = -1;
		int thisColumn = -1;
		String thisStr = null;

		// if (sheetIndex <= 0) {
		// System.out.println(sheetIndex);
		switch (record.getSid()) {
		case BoundSheetRecord.sid:
			boundSheetRecords.add((BoundSheetRecord) record);
			break;
		case BOFRecord.sid:
			BOFRecord br = (BOFRecord) record;
			if (br.getType() == BOFRecord.TYPE_WORKSHEET) {
				// Create sub workbook if required
				if (workbookBuildingListener != null && stubWorkbook == null) {
					stubWorkbook = workbookBuildingListener.getStubHSSFWorkbook();
				}
				sheetIndex++;
				if (orderedBSRs == null) {
					orderedBSRs = BoundSheetRecord.orderByBofPosition(boundSheetRecords);
				}
				// output.println();
				// output.println(orderedBSRs[sheetIndex].getSheetname() + " ["
				// + (sheetIndex + 1) + "]:");
			}
			break;

		case SSTRecord.sid:
			sstRecord = (SSTRecord) record;
			break;
		case RowRecord.sid:
			rowrec = (RowRecord) record;
			if (thisMaxColumn == 0) {
				thisMaxColumn = rowrec.getLastCol() - 1;
			}
			break;
		case BlankRecord.sid:
			BlankRecord brec = (BlankRecord) record;
			thisRow = brec.getRow();
			thisColumn = brec.getColumn();
			// if (brec.getColumn() < thisMaxColumn + 1) {
			thisStr = "";
			rowlist.add(thisStr);
			// }
			break;
		case BoolErrRecord.sid:
			BoolErrRecord berec = (BoolErrRecord) record;

			thisRow = berec.getRow();
			thisColumn = berec.getColumn();
			// if (berec.getColumn() < thisMaxColumn + 1) {
			thisStr = "";
			rowlist.add(thisStr);
			// }
			break;

		case FormulaRecord.sid:
			FormulaRecord frec = (FormulaRecord) record;

			thisRow = frec.getRow();
			thisColumn = frec.getColumn();

			// if (outputFormulaValues) {
			// if (Double.isNaN(frec.getValue())) {
			// // Formula result is a string
			// // This is stored in the next record
			// // System.out.println("IM HERE !!");
			// // rowlist.add(srec.getString());
			// outputNextStringRecord = true;
			// nextRow = frec.getRow();
			// nextColumn = frec.getColumn();
			// } else {
			//// if (frec.getColumn() < thisMaxColumn + 1) {
			// if (frec.getCachedResultType() == 1) {
			// nextRow = frec.getRow();
			// nextColumn = frec.getColumn();
			// // System.out.println(nextRow +" "+nextColumn);
			// outputNextStringRecord = true;
			// // System.out.println("resulttyepe :
			// // "+frec.getCachedResultType());
			// } else {
			// thisStr = formatListener.formatNumberDateCell(frec);
			// // System.out.println(formatListener.formatNumberDateCell(frec)+"
			// // "+frec.getRow()+" "+frec.getColumn());
			// rowlist.add(thisStr);
			// }
			//// }
			// }
			// } else {
			//// if (frec.getColumn() < thisMaxColumn + 1) {
			// thisStr = '"' + HSSFFormulaParser.toFormulaString(stubWorkbook,
			// frec.getParsedExpression())
			// + '"';
			// rowlist.add(thisStr);
			//// }
			// }
			break;
		case StringRecord.sid:
			// System.out.println("testString");
			if (outputNextStringRecord) {
				// String for formula
				srec = (StringRecord) record;
				System.out.println("StringRecord: " + srec.getString());
				thisStr = srec.getString();
				rowlist.add(thisStr);
				thisRow = nextRow;
				thisColumn = nextColumn;
				outputNextStringRecord = false;
			}
			break;
		case LabelRecord.sid:
			// System.out.println("testLabel");
			LabelRecord lrec = (LabelRecord) record;

			thisRow = lrec.getRow();
			thisColumn = lrec.getColumn();
			// if (lrec.getColumn() < thisMaxColumn + 1) {
			thisStr = lrec.getValue();
			rowlist.add(thisStr);
			// }
			break;
		case LabelSSTRecord.sid:
			// System.out.println("testLabelSST");
			LabelSSTRecord lsrec = (LabelSSTRecord) record;
			thisStr = sstRecord.getString(lsrec.getSSTIndex()).toString();
			rowlist.add(thisStr.trim());
			// rowlist.add(lsrec)
			// System.out.println(lsrec.);
			thisRow = lsrec.getRow();
			thisColumn = lsrec.getColumn();
			break;
		case NoteRecord.sid:
			// System.out.println("testNote");
			NoteRecord nrec = (NoteRecord) record;
			thisRow = nrec.getRow();
			thisColumn = nrec.getColumn();
			// if (nrec.getColumn() < thisMaxColumn + 1) {
			thisStr = '"' + "(TODO)" + '"';
			rowlist.add(thisStr);
			// }
			break;
		case NumberRecord.sid:
			NumberRecord numrec = (NumberRecord) record;
			thisStr = String.valueOf((long) numrec.getValue());
			thisRow = numrec.getRow();
			thisColumn = numrec.getColumn();
			rowlist.add(thisStr);

			// Format
			// if (tableName.equals("src_new_disbursement")) {
			// if (numrec.getColumn() != 31) {
			// if (numrec.getColumn() < thisMaxColumn + 1) {
			// if (formatListener.getFormatString(numrec).equals("0.0%")
			// || formatListener.getFormatString(numrec).equals("0.00%")) {
			// thisStr = df.format(numrec.getValue());
			// } else {
			// thisStr = formatListener.formatNumberDateCell(numrec);
			// }
			// rowlist.add(thisStr);
			// }
			// }
			// } else {
			// if (numrec.getColumn() < thisMaxColumn + 1) {
			// if (formatListener.getFormatString(numrec).equals("0.0%")
			// || formatListener.getFormatString(numrec).equals("0.00%")) {
			// thisStr = df.format(numrec.getValue());
			// } else {
			// thisStr = formatListener.formatNumberDateCell(numrec);
			// }
			// rowlist.add(thisStr);
			// }
			// }
			break;
		case RKRecord.sid:
			RKRecord rkrec = (RKRecord) record;

			thisRow = rkrec.getRow();
			thisColumn = rkrec.getColumn();
			// if (rkrec.getColumn() < thisMaxColumn + 1) {
			thisStr = '"' + "(TODO)" + '"';
			rowlist.add(thisStr);
			// }
			break;
		case MulBlankRecord.sid:
			MulBlankRecord mulblankrec = (MulBlankRecord) record;
			if (mulblankrec.getNumColumns() > 0) {
				for (int i = mulblankrec.getFirstColumn(); i < mulblankrec.getLastColumn(); i++) {
					// if (i < thisMaxColumn + 1) {
					thisStr = "";
					rowlist.add(thisStr);
					// }
				}
			}
			break;
		default:
			break;
		}
		if (thisRow != -1 && thisRow != lastRowNumber) {
			lastColumnNumber = -1;
		}

		// Handle missing column
		if (record instanceof MissingCellDummyRecord) {
			MissingCellDummyRecord mc = (MissingCellDummyRecord) record;
			thisRow = mc.getRow();
			thisColumn = mc.getColumn();
			// if (thisColumn < thisMaxColumn) {
			thisStr = "";
			rowlist.add(thisStr);
			// }
		}

		// If we got something to print out, do so
		// if(thisStr != null) {
		// if(thisColumn > 0) {
		// output.print(',');
		// }
		// output.print(thisStr);
		// }

		// Update column and row count
		if (thisRow > -1)
			lastRowNumber = thisRow;
		if (thisColumn > -1)
			lastColumnNumber = thisColumn;

		// Handle end of row
		if (record instanceof LastCellOfRowDummyRecord) {
			// Print out any missing commas if needed
			if (minColumns > 0) {
				// Columns are 0 based
				if (lastColumnNumber == -1) {
					lastColumnNumber = 0;
				}
				for (int i = lastColumnNumber; i < (minColumns); i++) {
					// output.print(',');
				}
			}

			// We're onto a new row
			lastColumnNumber = -1;

			// End the row
			if (sheetIndex <= 0) {
				// output.println();
				// if(rowlistRow<7){
				/*
				 * in the end if the rowlist size is still not full add some
				 * null value for the rest of blank column (BlankRecord or
				 * MulBlankRecord) can't read it as blank or multiblank value
				 * too
				 */
				// System.out.println(rowlist + "size : " + rowlist.size() + "
				// row : " + rowlistRow);
				try {
					extractRow(rowlist, rowlistRow, rowlistCount);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (rowlistCount == 70) {
					rowlistCount = 0;
				}
				rowlist.clear();
				// }
				rowlistRow++;
				rowlistCount++;
			}
		}
	}

	public static void main(String[] args) throws IOException, JSONException {
		File folder = new File("C:\\Users\\Diastowo\\Documents\\Trees\\Trees-IDSMed\\CSV\\");
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().contains("UserZendesk")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "UserZendesk");
			} else if (files[i].getName().contains("OrganizationZendesk")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "OrganizationZendesk");
			} else if (files[i].getName().contains("ContactRelationship")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "Contact_relationship");
			} else if (files[i].getName().contains("updateGroup")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "updateGroup");
				processArray(updateMany, updateMany.length());
			} else {
				System.out.println("TEST");
			}
			// System.out.println(logList);
			// System.out.println(jsonErr);
		}
	}

	private static void processArray(JSONArray updateData, int length) throws JSONException {
		// TODO Auto-generated method stub
		JSONArray updateArray = new JSONArray();
		try {
			for (int i = 0; i < length; i++) {
				updateArray.put(updateData.get(i));
				if (i % 100 == 0) {
					// System.out.println(updateArray.length());
					System.out.println(new JSONObject().put("organizations", updateArray));
					try {
						doUpdate(null, new JSONObject().put("organizations", updateArray), "UpdateManyOrganization",
								String.valueOf(i), 0);
					} catch (Exception e) {
						e.printStackTrace();
					}
					updateArray = new JSONArray();
				} else if (i == length - 1) {
					// System.out.println(updateArray.length());
					System.out.println(new JSONObject().put("organizations", updateArray));
					try {
						doUpdate(null, new JSONObject().put("organizations", updateArray), "UpdateManyOrganization",
								String.valueOf(i), 0);
					} catch (Exception e) {
						e.printStackTrace();
					}
					updateArray = new JSONArray();
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private static void ExecuteUpload(String uploadedFile, String dataType) throws FileNotFoundException, IOException {
		System.out.println("===UploadedFile: " + uploadedFile);
		filepath = uploadedFile;
		UploadXLSDB xlsprocess = new UploadXLSDB(uploadedFile);
		// System.out.println(dataType);
		datas = dataType;
		xlsprocess.process();
	}

	private void extractRow(List<String> exRowList, int rowNumber, int rowlistCounts) throws JSONException {
		if (datas.equalsIgnoreCase("UserZendesk")) {
			if (rowNumber == 0) {
				newUsersObj = new JSONObject();
			} else {
				for (int i = 0; i < exRowList.size(); i++) {
					switch (i) {
					case 0:
						newUsersObj.put("id", Integer.valueOf(exRowList.get(i).replace("null", "")));
						break;
					case 1:
						newUsersObj.put("name", exRowList.get(i));
						break;
					case 2:
						newUsersObj.put("email", exRowList.get(i).replace("null", ""));
						break;
					case 3:
						newUsersObj.put("phone", exRowList.get(i).replace("null", ""));
						break;
					case 4:
						newUsersAnyObj.put("contactguid", exRowList.get(i).replace("null", ""));
						newUsersObj.put("user_fields", newUsersAnyObj);
						newUsersAnyObj = new JSONObject();
						newUsersArray.put("user", newUsersObj);
						break;
					default:
						break;
					}
				}
			}
			System.out.println("its done");
			System.out.println(newUsersArray);
			try {
				if (rowNumber != 0) {
					doUpdate(null, newUsersArray, datas, exRowList.get(0),
							rowNumber);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (datas.equalsIgnoreCase("OrganizationZendesk")) {
			// newOrgObjFix = new JSONObject();
			if (rowNumber == 0) {
				newOrgObj = new JSONObject();
			} else {
				for (int i = 0; i < exRowList.size(); i++) {
					switch (i) {
					case 0:
						newOrgObj.put("id", exRowList.get(i));
						break;
					case 1:
						newOrgObj.put("name", exRowList.get(i));
						break;
					case 2:
						newOrgObj.put("organization_fields", new JSONObject().put("accountguid", exRowList.get(i)));
						newOrgObjFix.put("organization", newOrgObj);
						break;
					default:
						break;
					}
				}
			}
			System.out.println(newOrgObjFix);
			try {
				if (rowNumber != 0) {
					doUpdate(null, newOrgObjFix, datas, newOrgObjFix.getJSONObject("organization").get("id").toString(),
							rowNumber);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (datas.equalsIgnoreCase("UpdateGroup")) {
			newOrgObj = new JSONObject();
			if (rowNumber == 0) {
				newOrgObj = new JSONObject();
			} else {
				for (int i = 0; i < exRowList.size(); i++) {
					switch (i) {
					case 0:
						newOrgObj.put("id", Long.valueOf(exRowList.get(i)));
						break;
					case 1:
						newOrgObj.put("name", exRowList.get(i));
						break;
					case 2:
						newOrgObj.put("organization_fields", new JSONObject().put("accountguid", exRowList.get(i)));
						break;
					default:
						break;

					}
				}
			}
			updateMany.put(newOrgObj);
		} else if (datas.equalsIgnoreCase("Contact_relationship")) {
			if (rowNumber == 0) {
				newOrgMemObj = new JSONObject();
			} else {
				for (int i = 0; i < exRowList.size(); i++) {
					switch (i) {
					case 0:
						newOrgMemObj.put("user_id", exRowList.get(i));
						break;
					case 1:
						newOrgMemObj.put("organization_id", exRowList.get(i));
						newOrgMemObjFix.put("organization_membership", newOrgMemObj);
						break;
					default:
						break;
					}
				}
			}
			System.out.println(newOrgMemObjFix);
			try {
				if (rowNumber != 0) {
					doUpdate(null, newOrgMemObjFix, datas,
							newOrgMemObjFix.getJSONObject("organization_membership").get("user_id").toString(),
							rowNumber);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Permisi.. Numpang lewat.. :)");
		}
	}

	private static String readUser(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject getUser(String url, int rowNumber, String dataType, List<String> newExRowList)
			throws IOException, ConnectException, JSONException {
		HttpURLConnection con = null;
		JSONObject json = null;
		try {
			System.out.println("getting " + dataType + " data from row :" + (rowNumber + 1));
			URL obj = new URL(url);
			con = (HttpURLConnection) obj.openConnection();
			// optional default is GET
			con.setRequestMethod("GET");

			con.setRequestProperty("User-Agent", USER_AGENT);
			con.addRequestProperty("Authorization", "Basic " + TOKEN);
			con.addRequestProperty("Content-Type", "application/json; charset=UTF-8");
			con.setConnectTimeout(10000);
			con.setReadTimeout(10000);

			if (con.getResponseCode() == 200) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
				String jsonText = readUser(rd);
				json = new JSONObject(jsonText);
			} else {
				logList.add(newExRowList.toString());
				System.out.println(newExRowList);
			}
		} catch (Exception e) {
			System.out.println("error getting " + dataType + " data on row: " + (rowNumber + 1));
			logList.add(newExRowList.toString());
			e.printStackTrace();
		} finally {
			con.disconnect();
		}
		return json;
	}

	public static void doUpdate(String[] args, JSONObject userObj, String dataType, String id, int row)
			throws IOException, JSONException, InvalidFormatException {
		if (dataType.equalsIgnoreCase("UserZendesk")) {
			System.out.println("UserZendesk");
			urlAccount = "https://" + DOMAIN + ".zendesk.com/api/v2/users.json";
			METHOD = "POST";
		} else if (dataType.equalsIgnoreCase("OrganizationZendesk")) {
			System.out.println("OrganizationZendesk");
			urlAccount = "https://" + DOMAIN + ".zendesk.com/api/v2/organizations/create_or_update.json";
			METHOD = "POST";
		} else if (dataType.equalsIgnoreCase("Contact_relationship")) {
			System.out.println("Contact_relationship");
			urlAccount = "https://" + DOMAIN + ".zendesk.com/api/v2/organization_memberships.json";
			METHOD = "POST";
		} else if (dataType.equalsIgnoreCase("organizationEdited")) {
			System.out.println(dataType);
			urlAccount = "https://" + DOMAIN + ".zendesk.com/api/v2/organizations/" + id + ".json";
			METHOD = "PUT";
		} else if (dataType.equalsIgnoreCase("UpdateManyOrganization")) {
			System.out.println(dataType);
			urlAccount = "https://" + DOMAIN + ".zendesk.com/api/v2/organizations/update_many.json";
			METHOD = "PUT";
		}
		readJsonFromUrl(urlAccount, userObj, id, METHOD, row);
	}

	public static void readJsonFromUrl(String url, JSONObject user, String id, String method, int row)
			throws JSONException, InvalidFormatException {
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
			con.setConnectTimeout(20000);
			con.setReadTimeout(20000);

			OutputStream os = con.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");

			osw.write(user.toString());
			osw.flush();
			System.out.println(con.getResponseCode());
			int responseCode = con.getResponseCode();
			if (responseCode == 201 || responseCode == 200) {
				System.out.println("Success creating data from row :" + id);
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
				String jsonText = readUser(rd);
				JSONObject json = new JSONObject(jsonText);
				FileInputStream fsIP = new FileInputStream(new File(filepath));
				Workbook wb = WorkbookFactory.create(fsIP);
				Sheet worksheet = wb.getSheetAt(0);
				Cell cell = worksheet.getRow(row).createCell(10);
				Cell cell2 = worksheet.getRow(row).createCell(11);
				cell.setCellValue("Success");
				cell2.setCellValue(json.getJSONObject("user").getString("id"));
				fsIP.close();
				FileOutputStream output_file = new FileOutputStream(new File(filepath));
				wb.write(output_file);
				output_file.close();

			} else {
				System.out.println("Error creating data from row :" + id);
				jsonErr.put(user);
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(con.getErrorStream(), Charset.forName("UTF-8")));
				String jsonText = readUser(rd);
				JSONObject json = new JSONObject(jsonText);
				FileInputStream fsIP = new FileInputStream(new File(filepath));
				Workbook wb = WorkbookFactory.create(fsIP);
				Sheet worksheet = wb.getSheetAt(0);
				Cell cell = worksheet.getRow(row).createCell(10);
				cell.setCellValue("Error " + con.getResponseCode() + ": " + json);
				fsIP.close();
				FileOutputStream output_file = new FileOutputStream(new File(filepath));
				wb.write(output_file);
				output_file.close();
			}
			System.out.println(jsonErr);
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
}