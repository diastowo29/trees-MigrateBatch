package dev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
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
	private final static String search_url = "https://idsmed.zendesk.com/api/v2/users/search.json?query=contactguid%3A";
	private final static String search_org_url = "https://idsmed.zendesk.com/api/v2/organizations/search.json?external_id=";
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
		// } /*else {
		// System.out.println("test");
		// }*/
		// Handle new row
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
		// String test = "[{'organization':{'id':'13622088508','name':'PT. MUARA
		// SAKTI'}},{'organization':{'id':'14037325288','name':'PT. DELVI
		// PRIMATAMA-PT. DELVI
		// PRIMATAMA'}},{'organization':{'id':'13621151408','name':'BEND. RSUD.
		// DR.WAHIDIN SUDIRO HUSODO KODYA MOJOKERTO-BEND.
		// RSUD.DR.WAHIDI'}},{'organization':{'id':'13601468868','name':'CASH
		// SURABAYA'}},{'organization':{'id':'13961830887','name':'PT. BALI
		// MEDIKA-RS. KASIH IBU
		// KEDON'}},{'organization':{'id':'13621368568','name':'(NON AKTIF)
		// BAPAK PUTRA-PUTRA,
		// BAPAK'}},{'organization':{'id':'13621207928','name':'RS. HORAS
		// INSANI'}},{'organization':{'id':'14030126268','name':'PT. HASNA
		// MEDIKA-PT. HASNA
		// MEDIKA'}},{'organization':{'id':'13601784068','name':'RS. PERMATA
		// BUNDA'}},{'organization':{'id':'13621769628','name':'BEND.BENDAHARA
		// RSUD KAB. BINTAN-RSUD KAB
		// BINTAN'}},{'organization':{'id':'13621209008','name':'YAY. BAKTI
		// TIMAH-YAY. BAKTI
		// TIMAH'}},{'organization':{'id':'13601730308','name':'PT. SABANNA
		// KUMITA'}},{'organization':{'id':'13600743628','name':'RSUD
		// Purworejo-'}},{'organization':{'id':'14037325448','name':'PT.
		// GUNANUSA UTAMA FABRICATORS-PT. GUNANUSA UTAMA
		// F'}},{'organization':{'id':'13601662668','name':'FOUZAL
		// ASWAD'}},{'organization':{'id':'13961832987','name':'RS. BINA
		// KASIH'}},{'organization':{'id':'13601729468','name':'PT. GRAHA
		// ALIYYAH-PT. GRAHA
		// ALIYYA'}},{'organization':{'id':'13622079988','name':'PT. ANUGRAH
		// PRAJA MANDIRI-ANUGRAH PRAJA
		// MANDIR'}},{'organization':{'id':'13621369908','name':'PT. ZYMMA
		// PERKASA-PT. ZYMMA
		// PERKASA'}},{'organization':{'id':'14037325628','name':'(NON AKTIF)
		// PT. GRATIA JAYA MULYA-PT. GRATIA JAYA
		// MULY'}},{'organization':{'id':'13622079268','name':'PT. BENTARA
		// KARTIKA BAKTI-RS SATYA
		// NEGARA'}},{'organization':{'id':'13601729888','name':'Rs. Ahmad
		// Yani-'}},{'organization':{'id':'13601783108','name':'PT.PEKANBARU
		// MEDIKAL SENTER-RS
		// PMC'}},{'organization':{'id':'13600701268','name':'RS Bergerak
		// Sumbawa Tengah-'}},{'organization':{'id':'13601838048','name':'(NON
		// AKTIF) DR.NOVRIANTI D. A, SpPD-NOVRIANTI D. A,
		// DR,'}},{'organization':{'id':'13622087008','name':'PT. BINA DINAMIKA
		// RAGA-IMPRESSIONS BODY
		// CAR'}},{'organization':{'id':'13621369588','name':'PT. MUKA
		// BERSERISERI-KLINIK SKIN
		// PLUS'}},{'organization':{'id':'13600772928','name':'RSU KUDUS-RSU
		// KUDUS'}},{'organization':{'id':'14030126448','name':'PT. MEDIKANA
		// PRATAMAJAYA-PT. MEDIKANA
		// PRATAMA'}},{'organization':{'id':'13600489508','name':'RS
		// Santosa-'}},{'organization':{'id':'13967015967','name':'PT. OSHINDO
		// MEDIKA PRATAMA-KLINIK
		// MEDILAB-BATAM'}},{'organization':{'id':'13621133688','name':'DENNY
		// SIMATUPANG'}},{'organization':{'id':'13600489188','name':'RS
		// sudosoro-'}},{'organization':{'id':'13622086888','name':'PT.
		// MEDIKALOKA DAAN MOGOT-MEDIKALOKA
		// DAANMOGOT'}},{'organization':{'id':'13621388368','name':'CUT MELIZA
		// ZAINUMI'}},{'organization':{'id':'14040740908','name':'PT. HARAPAN
		// KELUARGA BAHAGIA-PERMATA LIPPO
		// CIKARA'}},{'organization':{'id':'13621389688','name':'RSJ Daerah Dr.
		// Amino
		// Gondohutomo-'}},{'organization':{'id':'14040747268','name':'(NONAKTIF)
		// RS. DR.MOEDJITO DWIDJOSISWOJO-RS. DR.MOEDJITO
		// DWID'}},{'organization':{'id':'13961833927','name':'RSUD
		// ABEPURA'}},{'organization':{'id':'13961834247','name':'RSUD.
		// CIBINONG'}},{'organization':{'id':'13601842848','name':'(NON AKTIF)
		// RSUD. KAB. DATI II SIDOARJO-DATI II
		// SIDOARJO'}},{'organization':{'id':'13621134588','name':'IBU
		// TIUR'}},{'organization':{'id':'13600772908','name':'RSUD SOEGIRI
		// LAMONGAN-'}},{'organization':{'id':'13601729868','name':'PT. SENTRAL
		// MEDIKA CEMERLANG-PT. SENTRAL MEDIKA
		// C'}},{'organization':{'id':'13621134348','name':'ARIF FADILLAH,
		// DR'}},{'organization':{'id':'13621395408','name':'PT. CITRA RAYA
		// MEDIKA-RS CIPUTRA
		// HOSPITAL'}},{'organization':{'id':'14040737448','name':'(NON AKTIF)
		// PT. HARAPAN INTERNASIONAL-PT. HARAPAN INTERNAS'}}]";
		File folder = new File("C:\\Users\\Diastowo\\Documents\\IDSMed\\CSV\\");
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			long start = System.currentTimeMillis();
			if (files[i].getName().contains("UserZendesk")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "UserZendesk");
			} else if (files[i].getName().contains("OrganizationZendesk")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "OrganizationZendesk");
				// } else if (files[i].getName().contains("MIGRATE")) {
				// System.out.println(files[i].getName());
				// ExecuteUpload(files[i].toString(), "Contact_relationship");
			} else if (files[i].getName().contains("MIGRATEV3")) {
				System.out.println(files[i].getName());
				ExecuteUpload(files[i].toString(), "organizationEdited");
			} else {
				System.out.println("TEST");
			}
			long elapsedTime = System.currentTimeMillis() - start;
			System.out.println(elapsedTime);

			System.out.println(logList);
			// LogWriter.main(null, logList);
			logWriter(datas, jsonErr);
		}
	}

	private static void ExecuteUpload(String uploadedFile, String dataType) throws FileNotFoundException, IOException {
		System.out.println("===UploadedFile: " + uploadedFile);
		UploadXLSDB xlsprocess = new UploadXLSDB(uploadedFile);
		System.out.println(dataType);
		datas = dataType;
		xlsprocess.process();
	}

	private void extractRow(List<String> exRowList, int rowNumber, int rowlistCounts) throws JSONException {
		JSONObject userObj = new JSONObject();
		JSONObject xlsJson = new JSONObject();
		boolean userIsNull = false;
		try {
			for (int i = 0; i < exRowList.size(); i++) {
				if (datas.equalsIgnoreCase("Contact_relationship")) {
					if (i == 0) {
						try {
							JSONObject jsons = getUser(search_url + exRowList.get(i), rowNumber, "users", exRowList);
							if (jsons.getJSONArray("users").isNull(0)) {
								userIsNull = true;
								// System.out.println("USER is NULL: " +
								// exRowList.get(i));
								System.out.println(exRowList);
								logList.add(exRowList.toString());
							} else {
								String userZendeskId = jsons.getJSONArray("users").getJSONObject(0).get("id")
										.toString();
								System.out.println("userZendeskID: " + userZendeskId);
								xlsJson.put("user_id", userZendeskId);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						// xlsJson.put("user_id", exRowList.get(i));
					} else {
						if (userIsNull) {
							System.out.println("user is null, organization is null");
						} else {
							System.out.println("user not null, looking for organization");
							try {
								JSONObject jsons = getUser(search_org_url + exRowList.get(i), rowNumber, "organization",
										exRowList);
								if (jsons.getJSONArray("organizations").isNull(0)) {
									// System.out.println("ORG is NULL: " +
									// exRowList.get(i));
									System.out.println(exRowList);
									logList.add(exRowList.toString());
								} else {
									String orgZendeskId = jsons.getJSONArray("organizations").getJSONObject(0).get("id")
											.toString();
									System.out.println("orgZendeskID: " + orgZendeskId);
									if (orgZendeskId == null) {
										orgZendeskId = "";
									}
									xlsJson.put("organization_id", orgZendeskId);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						// xlsJson.put("organization_id", exRowList.get(i));
					}
				} else if (datas.equalsIgnoreCase("organizationEdited")) {
					switch (i) {
					case 0:
						userObj.put("id", exRowList.get(i));
						break;
					case 1:
						userObj.put("name", exRowList.get(i));
						break;
					default:
						break;
					}
				}
			}
			// System.out.println(userObj);
			// rowCount++;
			// System.out.println(exRowList);

			/* FIXME USING CREATE MANY */
			// userArr.put(xlsJson);
			// // if (rowlistCount == 9) {
			// // System.out.println("rowlistCount is 10");
			// // System.out.println(logList);
			// // }
			// if (rowlistCount == 70) {
			// userObj.put("organization", userObj);
			// try {
			// MigrateInit.main(null, userObj, datas, rowNumber);
			// } catch (Exception e) {
			// System.out.println("MIGRATE INIT ERROR: " + userObj);
			// e.printStackTrace();
			// }
			// userArr = new JSONArray();
			// } else if (rowCount == rowrec.getRowNumber() + 1) {
			// userObj.put("organization", userObj);
			// try {
			// MigrateInit.main(null, userObj, datas, rowNumber);
			// } catch (Exception e) {
			// System.out.println("MIGRATE INIT ERROR: " + userObj);
			// e.printStackTrace();
			// }
			// userArr = new JSONArray();
			// }

			/* FIXME UPDATE ORGANIZATION */
			if (!userObj.get("name").toString().equalsIgnoreCase("kosong")
					&& !userObj.get("id").toString().equalsIgnoreCase("zendesk_id")) {
				System.out.println("id: " + userObj.getString("id") + new JSONObject().put("organization", userObj));
				System.out.println(jsonErr);
				try {
					doUpdate(null, new JSONObject().put("organization", userObj), datas,
							userObj.getString("id").toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (JSONException jsonx) {
			jsonx.printStackTrace();
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

	public static void doUpdate(String[] args, JSONObject userObj, String dataType, String Row)
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
		} else if (dataType.equalsIgnoreCase("organizationEdited")) {
			System.out.println(dataType);
			urlAccount = "https://idsmed.zendesk.com/api/v2/organizations/" + Row + ".json";
			METHOD = "PUT";
		}
		// System.out.println(userObj);
		// System.out.println(userObj.getJSONArray("organization_memberships").length());
		readJsonFromUrl(urlAccount, userObj, Row, METHOD);
		// logWriter(dataType, jsonErr);
		// System.out.println(jsonErr);
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
			con.setConnectTimeout(20000);
			con.setReadTimeout(20000);

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

	public static void logWriter(String Datatype, JSONArray jsonArray) throws JSONException {
		try {
			File file = new File("C:/users/Diastowo/" + Datatype + "Cool.txt");

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			for (int i = 0; i < jsonArray.length(); i++) {
				// System.out.println(jsonArray.getJSONObject(i).getJSONObject("organization").get("id"));
				// for (int j = 0; j < jsonObject.getJSONArray(i).length(); j++)
				// {
				// boolean isOrg = true;
				// if(isOrg ){
				bw.write(jsonArray.getJSONObject(i).getJSONObject("organization").get("id") + ", "
						+ jsonArray.getJSONObject(i).getJSONObject("organization").get("name"));
				// } else {
				// bw.write(jsonObject.getJSONArray(i).getJSONObject(j).getString("id")
				// + ", "
				// +
				// jsonObject.getJSONArray(i).getJSONObject(j).getString("email")
				// + ", "
				// +
				// jsonObject.getJSONArray(i).getJSONObject(j).getJSONObject("user_fields")
				// .getString("contactguid"));
				// }
				bw.newLine();
				// }

			}
			bw.close();
			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}