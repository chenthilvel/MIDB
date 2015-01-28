package com.chen.midb3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

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
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GetTopN extends DefaultHandler{
	private String curValue = "";
	private String tempResult = "";
	private static final String DB_NAME = "midb";
	private String[] INCMAP = { "AV", "UT", "AA" };
	private String ne_name = "", alarm_name="", alarm_text="", alm_time= "", IncType="", IncID="", addtl_text="";
	private String first_occurrence = "", last_occurrence = "";
	private Integer systag = 0, alarm_type = 0, state = 0, sev = 0, alarm_id = 0, rank = 0, incident_type = 0, nodeid = 0;
	private Float Rate = 0.0f;
	private String [] [] RowData = new String[20][9];
	private SQLiteDatabase db;

	public String[][] ParseResults(final Context mContext, String OMCURL)
	{
		String HTTPS = OMCURL;
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			db.execSQL("delete from topn;");
//			Log.d("Chenthil", "TOP N Rows deleted");
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		try { 
			System.setProperty("http.keepAlive", "false");
			String KA = System.getProperty("http.keepAlive");
			Log.d("Chenthil", "KeepAlive is: "+KA);
			SSLContext sc = SSLContext.getInstance("TLS"); 
			sc.init(null, new TrustManager[] { new MyTrustManager() }, new SecureRandom()); 
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
			HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier()); 
			HttpsURLConnection con = (HttpsURLConnection) new URL(HTTPS).openConnection(); 
			con.setDoOutput(true); 
			con.setDoInput(true);
//			con.setChunkedStreamingMode(2048);
//			Log.i("Chenthil","Calling connect");
			con.connect(); 

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
//			Log.i("Chenthil", "Calling setContentHandler");
			xr.setContentHandler(this);
			int conlen = con.getContentLength();
//			Map<String, List<String>>  headermap = con.getHeaderFields();
//			headermap.size();
			int respcode = con.getResponseCode();
			String EncType = con.getContentEncoding();
			Log.d("Chenthil", con.getResponseMessage()+" "+con.getContentType()+" ");
			
			Log.d("Chenthil", "HTTP Resp Code: "+respcode+" Encoding: "+EncType+" Len: "+conlen);
			InputStream istream = con.getInputStream();
			InputSource isrc = null;
			if(istream != null)
			{
//				Log.i("Chenthil", "Calling Parser "+istream.available());
				isrc = new InputSource(istream);
				xr.parse(isrc);
			}
			else
			{
				Log.i("Chenthil","InputStream is null");
			}
			con.disconnect();
		} catch (IOException e) {
			Log.e("Chenthil IOException", e.toString());
		} catch (SAXException e) {
			Log.e("Chenthil", e.toString()); e.printStackTrace();
		} catch (ParserConfigurationException e) {
			Log.e("Chenthil", "ParserConfigurationException: "+e.toString());
		} catch (Exception e) {
			Log.e("Chenthil", "Genl"+e.toString());
		}
		db.close();
		if(tempResult == "")
		{
			return null;
		}
		else
		{
			return RowData;
		}

	}
	public void startElement(String uri, String name, String qName,
			Attributes atts) {
	}

	public void endElement(String uri, String name, String qName)
	throws SAXException {
		String key = name.trim();

		if(key.equals("NE_NAME"))
		{
			ne_name = curValue.trim();
		}
		else if(key.equals("SYSTAG"))
		{
			systag = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("ALARM_TYPE"))
		{
			alarm_type = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("STATE"))
		{
			state = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("ALARM_ID"))
		{
			alarm_id = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("NODEID"))
		{
			nodeid = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("FIRST_OCCURRENCE"))
		{
			first_occurrence = curValue.trim();
		}
		else if(key.equals("LAST_OCCURRENCE"))
		{
			last_occurrence = curValue.trim();
		}
		else if(key.equals("ALARM_NAME"))
		{
			alarm_name = curValue.trim();
		}
		else if(key.equals("ADDITIONAL_TEXT"))
		{
			addtl_text = curValue.trim();
		}
		else if(key.equals("ALARM_TEXT"))
		{
			alarm_text = curValue.trim();
		}
		else if(key.equals("SEVERITY"))
		{
			sev = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("KEY"))
		{
			IncID = curValue.trim();
		}
		else if(key.equals("INCIDENT_TYPE"))
		{
			incident_type = Integer.parseInt(curValue.trim());
			IncType = INCMAP[incident_type];
		}
		else if(key.equals("RANK"))
		{
			rank = Integer.parseInt(curValue);
		}
		else if(key.equals("TIME"))
		{
			alm_time = curValue.trim();
		}
		else if(key.equals("RATE"))
		{
			Rate = Float.parseFloat(curValue.trim());
		}
		else if(key.equals("POTENTIAL_FLAG_TEXT"))
		{
//			tempResult = tempResult+"Rank: "+rank+"\nType: "+IncType+"\nName: "+ne_name+"\nRate: "+Rate+"\nAlarm Name: "+alarm_name+"\nAlarm Text: "+alarm_text+"\nTime: "+alm_time+"\n\n";
			tempResult = tempResult+" "+rank+"\n";
			RowData[rank][0] = Rate.toString();
			RowData[rank][1] = rank.toString();
			RowData[rank][2] = IncType;
			RowData[rank][3] = ne_name;
			RowData[rank][4] = Rate.toString();
			RowData[rank][5] = IncID;
			RowData[rank][6] = alm_time;
			RowData[rank][7] = alarm_name;
			RowData[rank][8] = alarm_text;
			try 
			{
				ContentValues values = new ContentValues();
				values.put("key", IncID); 
				values.put("systag", systag);
				values.put("alarm_type", alarm_type);
				values.put("alarm_name", alarm_name);
				values.put("ne_name", ne_name); 
				values.put("state", state);
				values.put("severity", sev);
				values.put("alarm_id", alarm_id);
				values.put("alarm_text", alarm_text); 
				values.put("time", alm_time);
				values.put("rate", Rate);
				values.put("rank", rank);
				values.put("incident_type", incident_type); 
				values.put("first_occurrence", first_occurrence);
				values.put("last_occurrence", last_occurrence);
				values.put("additional_text", addtl_text);
				values.put("nodeid", nodeid);

				db.insert("topn", "", values);
				ne_name = ""; alarm_name=""; alarm_text=""; alm_time= ""; IncType=""; IncID=""; addtl_text=""; first_occurrence = ""; last_occurrence = "";
				systag = 0; alarm_type = 0; state = 0; sev = 0; alarm_id = 0; rank = 0; incident_type = 0; nodeid = 0;
				Rate = 0.0f;
			}
			catch(Exception e) {
				Log.e("Chenthil", "DB Insert: "+e.toString());
			}
		}
	}

	public void characters(char ch[], int start, int length) {
		String chars = (new String(ch).substring(start, start + length));
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
