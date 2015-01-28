package com.chen.midb3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GetKPIValues extends DefaultHandler{
	private String curValue = "";
//	private String tempResult = "";
	private static final String DB_NAME = "midb";
	private String tstmp = "", ne_name = "";
	private Integer systag = 0, kpi_id = 0, EntryCount = 0;
	private Float Cell1 = 0.0f, Cell2 = 0.0f, Cell3=0.0f, Total=0.0f;
	private SQLiteDatabase db;
	private XMLReader xr;
	private HttpsURLConnection con;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT_DIFF = "HH:mm:ss";
	SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	SimpleDateFormat sdf2 = new SimpleDateFormat(DATE_FORMAT_DIFF);
//	private String TIME_DIFF = "00:30:00";
	String key = "";
	List<Date[]> dates = new ArrayList<Date[]>();
	List<double[]> values = new ArrayList<double[]>();
	double yMin;
	double yMax; 
	Date[] xMin = new Date[1];
	Date[] xMax = new Date[1];
	
	public Date[] ConvertDate(String strdate)
	{
		Date[] mydate = new Date[1];
		try {
			 mydate[0] = sdf.parse(strdate);
			}
			catch(Exception e)
			{
				Log.d("Chenthil", "ConvertDate: "+strdate+" "+e.toString());
			}
			return mydate;
	}
	public Date[] getxMin()
	{
		return xMin;
	}
	public Date[] getxMax()
	{
		return xMax;
	}
	public double getyMin()
	{
		return yMin;
	}
	public double getyMax()
	{
		return yMax+40;
	}
	public String getNEName()
	{
		return ne_name;
	}
	public Integer FetchData(final Context mContext,Integer systag, Integer kpi_id)
	{
/*		Date DATE_DIFF = null;
		try {
			DATE_DIFF = sdf2.parse(TIME_DIFF);
		} catch (ParseException e1) {
			//  Auto-generated catch block
			e1.printStackTrace();
		}
*/		
		String[] params = new String[2]; 
		params[0] = systag.toString(); params[1] = kpi_id.toString();
		String tstmp = "";
		double kpi_value = 0.0;
		Date[]  mydate = null;
		double[] mykpis = null;
		Integer Count = 0;
		
		try {
			String[] name_params = new String[1];
			name_params[0] = systag.toString();
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select ne_name from hmx where systag = ?", name_params );
			while(c.moveToNext())
			{
				ne_name = c.getString(0);
			}
//			Log.d("Chenthil", "Retrieved Name: "+ne_name);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select Timestamp, Total from kpi where systag = ? and kpi_id = ?", params);
//			Log.d("Chenthil", "Row Cnt: "+c.getCount());
			mydate = new Date[c.getCount()];
			mykpis = new double[c.getCount()];
			while(c.moveToNext())
			{
				tstmp = c.getString(0);
				kpi_value = c.getDouble(1);
				mykpis[Count] = kpi_value;
//				Log.d("Chenthil", "Retrieved "+Count.toString()+": "+tstmp+" <"+kpi_value+">");
				try {
					 mydate[Count] = sdf.parse(tstmp);
					}
					catch(Exception e)
					{
						Log.d("Chenthil", e.toString());
					}
					Count++;
					
			}
			dates.add(mydate);
			values.add(mykpis);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		// Y-Min
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select min(Total) from kpi where systag = ? and kpi_id = ?", params);
			while(c.moveToNext())
			{
				kpi_value = c.getDouble(0);
				yMin = kpi_value;
			}
//			Log.d("Chenthil", "Retrieved Y-Min: "+kpi_value);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		// Y-Max
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select max(Total) from kpi where systag = ? and kpi_id = ?", params);
			while(c.moveToNext())
			{
				kpi_value = c.getDouble(0);
				yMax = kpi_value;
			}
//			Log.d("Chenthil", "Retrieved Y-Max: "+kpi_value);		
			}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		// X-Min
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select min(timestamp) from kpi where systag = ? and kpi_id = ?", params);
			while(c.moveToNext())
			{
				tstmp = c.getString(0);
				xMin = ConvertDate(tstmp);
//				xMin[0].setTime(xMin[0].getTime()-(30*60*1000));
			}
//			Log.d("Chenthil", "Retrieved X-Min: "+tstmp);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		// X-Max
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select max(timestamp) from kpi where systag = ? and kpi_id = ?", params);
			while(c.moveToNext())
			{
				tstmp = c.getString(0);
				xMax = ConvertDate(tstmp);
				xMax[0].setTime(xMax[0].getTime()+(30*60*1000));
//				xMax[0]= ();
			}
//			Log.d("Chenthil", "Retrieved X-Max: "+tstmp);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		db.close();
		return Count;
	}
	public List<Date[]> getDates()
	{
		return dates;
	}
	public List<double[]> getValues()
	{
		return values;
	}
	public Integer ParseResults(final Context mContext, String OMCURL, Integer systag, Integer kpi_id)
	{
		String HTTPS = OMCURL;
		this.systag = systag;
		this.kpi_id = kpi_id;
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			db.execSQL("delete from kpi;");
//			Log.d("Chenthil", "KPI Rows deleted");
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		try { 
			SSLContext sc = SSLContext.getInstance("TLS"); 
			sc.init(null, new TrustManager[] { new MyTrustManager() }, new SecureRandom()); 
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
			HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier()); 
			con = (HttpsURLConnection) new URL(HTTPS).openConnection(); 
			con.setDoOutput(true); 
			con.setDoInput(true); 
//			Log.i("Chenthil","Calling connect");
			con.connect(); 

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			 xr = sp.getXMLReader();
//			Log.i("Chenthil", "Calling setContentHandler");
			xr.setContentHandler(this);
			InputStream istream = con.getInputStream();
			if(istream != null)
			{
//				Log.i("Chenthil", "Calling Parser");
				xr.parse(new InputSource(istream) );
			}
			else
			{
				Log.i("Chenthil","InputStream is null");
			}
			con.disconnect();
		} catch (IOException e) {
			Log.e("Chenthil IOException", e.toString());
		} catch (SAXException e) {
			Log.e("Chenthil", e.toString()); 
		} catch (ParserConfigurationException e) {
			Log.e("Chenthil", "ParserConfigurationException: "+e.toString());
		} catch (Exception e) {
			Log.e("Chenthil", "Genl"+e.toString());
		}
		
		db.close();
		return EntryCount;
	}
	public void startElement(String uri, String name, String qName,
			Attributes atts) {
		key = name.trim();
	}

	public void endElement(String uri, String name, String qName)
	throws SAXException {
		 key = name.trim();
/*		Sample XML		
  	<KPIDATA>
		<Timestamp>2010-01-01 21:00:00</Timestamp>
		<CELL1>135.0</CELL1>
		<CELL2>312.0</CELL2>
		<CELL3>168.0</CELL3>
		<TOTAL>615.0</TOTAL>
	</KPIDATA>
*/
		if(key.equals("Timestamp"))
		{
//			Log.e("Chenthil", "Timestamp: "+curValue.toString());
			tstmp = curValue.trim();
		}
		else if(key.equals("CELL1"))
		{
			Cell1 = Float.parseFloat(curValue.trim());
		}
		else if(key.equals("CELL2"))
		{
			Cell2 = Float.parseFloat(curValue.trim());
		}
		else if(key.equals("CELL3"))
		{
			Cell3 = Float.parseFloat(curValue.trim());
		}
		else if(key.equals("TOTAL") && tstmp.length() == 19)
		{
			Total = Float.parseFloat(curValue.trim());
			EntryCount++;
//			tempResult = tempResult+" "+Total+"\n";
			try 
			{
				ContentValues values = new ContentValues();
				values.put("kpi_id", kpi_id);
				values.put("systag", systag); 
				values.put("Timestamp", tstmp);
				values.put("Cell1", Cell1); values.put("Cell2", Cell2); values.put("Cell3", Cell3);
				values.put("Total", Total);
//				Log.i("Chenthil", "Inserting: "+tstmp+" "+Total.toString());
				db.insert("kpi", "", values);
				
			}
			catch(SQLException e) {
				Log.e("Chenthil", "DB Insert: "+e.toString());
			}
		}
	}

	public void characters(char ch[], int start, int length) {
		String chars = (new String(ch).substring(start, start + length));
//		Log.v("Chenthil", "Created: "+key+" "+start+" Len:"+length+" "+chars);
		curValue = chars;
	}

	/** 
	 * MyHostnameVerifier 
	 */ 
	private class MyHostnameVerifier implements HostnameVerifier { 

		public boolean verify(String hostname, SSLSession session) { 
			return true; 
		} 
	} 

	/** 
	 * MyTrustManager 
	 */ 
	private class MyTrustManager implements X509TrustManager {

		public void checkClientTrusted(X509Certificate[] chain, String authType) { 
		} 

		public void checkServerTrusted(X509Certificate[] chain, String authType) { 
		} 

		public X509Certificate[] getAcceptedIssuers() { 
			return null; 
		} 
	} 

}//End of class
