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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GetHMX extends DefaultHandler{
	private String curValue = "";
	private String tempResult = "";
	private static final String DB_NAME = "midb";
	private String ne_name = "";
	private Integer systag = 0, nodeid = 0, EntryCount = 0;
	private Float Rate = 0.0f;
	private SQLiteDatabase db;
	private XMLReader xr;
	private HttpsURLConnection con;
	public Integer ParseResults(final Context mContext, String OMCURL)
	{
		String HTTPS = OMCURL;
		try {
			db = mContext.openOrCreateDatabase(DB_NAME, 0, null);
			db.execSQL("delete from hmx;");
//			Log.d("Chenthil", "HMX Rows deleted");
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
	}

	public void endElement(String uri, String name, String qName)
	throws SAXException {
		String key = name.trim();
/*		Sample XML		
  		<NODE>
		<NE_NAME>EBTS_42</NE_NAME>
		<NE_TYPE>41</NE_TYPE>
		<NE_SYSTAG>6139</NE_SYSTAG>
		<AGGREGATED_RATING>135.0</AGGREGATED_RATING>
		<NE_TYPE_TEXT>EBTS</NE_TYPE_TEXT>
		</NODE>
*/
		if(key.equals("NE_NAME"))
		{
			ne_name = curValue.trim();
		}
		else if(key.equals("NE_SYSTAG"))
		{
			systag = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("NODEID"))
		{
			nodeid = Integer.parseInt(curValue.trim());
		}
		else if(key.equals("AGGREGATED_RATING"))
		{
			Rate = Float.parseFloat(curValue.trim());
		}
		else if(key.equals("NE_TYPE_TEXT"))
		{
			tempResult = tempResult+" "+Rate+"\n";
			try 
			{
				ContentValues values = new ContentValues();
				values.put("systag", systag);
				values.put("ne_name", ne_name); 
				values.put("rate", Rate);
				values.put("nodeid", nodeid);

				db.insert("hmx", "", values);
				EntryCount++;
//				if(EntryCount%25 == 0) Log.d("Chenthil", "Retrieved "+EntryCount+" HMX Entries");
				if(EntryCount >= 20)
				{
					throw new SAXException("Parsed alteast 20 entries");
				}
			}
			catch(SQLException e) {
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
