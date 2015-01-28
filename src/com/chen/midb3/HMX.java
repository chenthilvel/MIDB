package com.chen.midb3;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;

public class HMX extends Activity implements Runnable {
	private static final int MENU_REFRESH_UI = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_QUIT = 2;
	private static final String DB_NAME = "midb";
	private SQLiteDatabase db;
	private String OMC = "", OMCURL = "";
	private TableLayout tl;
	private String[] Header = { "NE Name","Aggregated Rating","Node ID" };
	private String[] SEVMAP = {"Indeterminate", "Critical", "Major", "Minor", "Warning", "Clear"};
	private String[] INCMAP = { "Availability", "Utilization", "Actionable Alarm" };
	private int HDRCNT = 3;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	private Float FontSize = 9.0f;
	private Integer HMXCount = 0;
	private ProgressDialog pd;
	private String LastUpdated = "";
	
	View.OnClickListener clickListener = new View.OnClickListener() {
		public void onClick(View v) {
			Integer systag = v.getId();
//			Log.i("Chenthil","Clicked a button: "+v.getId()+" ");
			Button b = (Button) v;
			String ne_name = b.getHint().toString();
			ShowDeviceReport(systag, ne_name);
		}
	};
	View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
//			Log.i("Chenthil","Clicked a button: "+v.getId()+" ");
//			TextView tv = (TextView) v;
//			Log.d("Chenthil", "Long Clicked on: "+tv.getText());
//			ShowDetails(v.getId(), IncID);
			return true;
		}
	};
	public void ShowDeviceReport(Integer systag, String ne_name)
	{
		Intent intent_DR = new Intent(HMX.this, DeviceReport.class);
		intent_DR.putExtra("Version", "1.25");
		intent_DR.putExtra("OMC", OMC);
		intent_DR.putExtra("systag", systag.toString());
		intent_DR.putExtra("ne_name", ne_name);
		startActivity(intent_DR);
	}
	public static String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(com.chen.midb3.MIDB3.APP_TITLE);
		// 0 - Undefined 1 - Portrait 2 - Landscape 3 - Square
		int CurrentOrientation = this.getResources().getConfiguration().orientation;
//		Log.d("Chenthil", "Current Orientation: "+CurrentOrientation);
		if(CurrentOrientation == 2)
		{
			FontSize = 12.0f;
		}
		else if(CurrentOrientation == 1)
		{
			FontSize = 11.0f;
		}
		String Ver = savedInstanceState != null ? savedInstanceState.getString("Version"):null;
		if(Ver == null)
		{
			Bundle extras = getIntent().getExtras();
			Ver = extras != null ? extras.getString("Version"):"0.0";
			OMC = extras != null ? extras.getString("OMC"):"br_sp2sys";
		}
		if(OMC.equals("br_sp2sys"))
		{
			OMCURL = "https://203.8.58.185:8443/webmmi/idb?requestType=3&key=2";
//			OMCURL = "https://10.232.176.204:8443/webmmi/idb?requestType=3&key=2";
		}else if(OMC.equals("br_sp3sys"))
		{
			OMCURL = "https://203.8.58.29:8443/webmmi/idb?requestType=3&key=2";
		}
//		Log.i("Chenthil", "Version: "+Ver+" OMC: "+OMC);
		FetchData();
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig);
	  Log.d("Chenthil", "Config changed; calling RefreshUI");
	  RefreshUI();
	}
	public void RefreshUI()
	{
		ScrollView sv = new ScrollView(this);
		TextView tstmp = new TextView(this);
		TextView TopN_Title = new TextView(this);
		int CurrentOrientation = this.getResources().getConfiguration().orientation;
//		Log.d("Chenthil", "Current Orientation: "+CurrentOrientation);
		if(CurrentOrientation == 2)
		{
			FontSize = 12.0f;
		}
		else if(CurrentOrientation == 1)
		{
			FontSize = 10.0f;
		}
		PopulateUI();

		String TimeStamp = "    Last Updated at: "+LastUpdated;
		tstmp.setText(TimeStamp);
		tstmp.setTextSize(11.0f);
		tstmp.setTextColor(Color.BLACK);
		tstmp.setBackgroundColor(Color.WHITE);
		TableLayout.LayoutParams tlo = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
		tlo.setMargins(1, 2, 1, 1);
		tstmp.setLayoutParams(tlo);
		tl.addView(tstmp, 0);

		TopN_Title.setText("Health Matrix - "+OMC);
		TopN_Title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
		TopN_Title.setTextColor(Color.BLUE);
		TopN_Title.setBackgroundColor(Color.WHITE);
		TopN_Title.setPadding(5, 5, 5, 5);
		TopN_Title.setLayoutParams(tlo);
		TopN_Title.setGravity(Gravity.CENTER_HORIZONTAL);
		tl.addView(TopN_Title, 1);

		sv.addView(tl);
		sv.setHorizontalScrollBarEnabled(true);
//		Integer childcnt = (Integer)tl.getChildCount();
//		Log.i("Chenthil", "Child Count: "+childcnt.toString());
		if( tl.getChildCount() < 4)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Unable to Fetch Data!\nPlease try again afer few seconds")
			.setTitle(com.chen.midb3.MIDB3.APP_TITLE)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		setContentView(sv);
//		Log.v("Chenthil", "Done with setcontentview");
	}
	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH_UI, 0, "Refresh").setAlphabeticShortcut('R').setIcon(R.drawable.midb_reload);
		menu.add(0, MENU_ABOUT, 0, "About").setAlphabeticShortcut('A').setIcon(R.drawable.midb_info);
		menu.add(0, MENU_QUIT, 0, "Close HMX").setAlphabeticShortcut('Q').setIcon(R.drawable.midb_close);
		return true;
	}
	public void FetchData()
	{
		pd = ProgressDialog.show(this, null, "Retriving Health Matrix from "+OMC, true, false);
		Thread thread = new Thread(this);
		thread.start();
//		Log.d("Chenthil", "Thread execution finished");
	}
	public void run() {
		GetHMX rp = new GetHMX();
//		Log.d("Chenthil", "Calling GetHMX.ParseResults with: "+OMCURL);
		
		HMXCount = rp.ParseResults(getApplicationContext(), OMCURL);
		
//		Log.d("Chenthil", "Got "+HMXCount+" HMX entries");
		handler.sendEmptyMessage(0);
	}
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			pd.dismiss();
//			Log.d("Chenthil", "Dismissed dialog.. Calling RefreshUI");
			LastUpdated =  getTimeStamp();
			RefreshUI();
		}
	};
	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH_UI:
//			RefreshUI();
			FetchData();
			return true;
		case MENU_ABOUT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("View iDEN OMC Dashboards on Mobile\n" +
			" ")
			.setTitle(com.chen.midb3.MIDB3.APP_TITLE)
			.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		case MENU_QUIT:
			this.finish();
			return true;
		}
		return false;
	}
	public void ShowDetails(int Rank, String IncID)
	{
//		Log.d("Chenthil", "ShowDetails called with: "+IncID);
		String[] params = new String[2]; 
		params[0] = Integer.toString(Rank);
		params[1] = IncID;
		String ne_name = "", alarm_name="", alarm_text="", alm_time= "", addtl_text="", severity="", key = "";
		String incident_type = "";
		Integer rank = 0;
		Float Rate = 0.0f;
		try {
			db = openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select * from topn where rank = ? and key = ?", params);
			while(c.moveToNext())
			{
				key = c.getString(0);
				alarm_name = c.getString(3);
				ne_name = c.getString(4);
				severity = SEVMAP[c.getInt(6)];
				alarm_text = c.getString(8);
				alm_time = c.getString(9);
				Rate = c.getFloat(10);
				rank = c.getInt(11);
				incident_type = INCMAP[c.getInt(12)];
				addtl_text = c.getString(15);
//				if(addtl_text == null || addtl_text == "") 
//					addtl_text = "this resembles the structure of an HTML table";
			}
//			Log.d("Chenthil", "Retrieved: "+key+" <"+addtl_text+">");
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		db.close();

		AlertDialog.Builder builder;
		AlertDialog alertDialog;
		TextView text ;
		Context mContext = getApplicationContext();
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.custom_dialog, (ViewGroup) findViewById(R.id.layout_root));
		text = (TextView) layout.findViewById(R.id.nename);
		text.setText(ne_name);
		text = (TextView) layout.findViewById(R.id.incid);
		text.setText(key);
		text = (TextView) layout.findViewById(R.id.rate);
		text.setText(Rate.toString());
		text = (TextView) layout.findViewById(R.id.rank);
		text.setText(rank.toString());		
		text = (TextView) layout.findViewById(R.id.type);
		text.setText(incident_type);
		text = (TextView) layout.findViewById(R.id.sev);
		text.setText(severity);
		text = (TextView) layout.findViewById(R.id.almname);
		text.setText(alarm_name);
		text = (TextView) layout.findViewById(R.id.almtext);
		text.setText(alarm_text);
		text = (TextView) layout.findViewById(R.id.time);
		text.setText(alm_time);
		text = (TextView) layout.findViewById(R.id.addtl);
		text.setText(addtl_text);		
		layout.setVerticalScrollBarEnabled(true);
		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		alertDialog = builder.create();
		alertDialog.setCancelable(true);
		alertDialog.setCanceledOnTouchOutside(true);
		alertDialog.show();
	}
	public void PopulateUI()
	{
		String ne_name ="";
		Float Rate = 0.0f;
		Integer nodeid = 0, CurCnt = 0, systag = 0;
		TableRow tr_data = null;

		tl = new TableLayout(this);
		tl.setPadding(5, 5, 5, 5);
		tl.setBackgroundColor(Color.argb(255, 128, 128, 128));
		tl.setStretchAllColumns(true);
		tl.setColumnShrinkable(2, true);
		tl.setColumnShrinkable(4, true);
		tl.setColumnShrinkable(5, true);
		tl.setColumnShrinkable(6, true);
		tl.setHorizontalScrollBarEnabled(true);

		TableRow tr = new TableRow(this);
		for(int i=0;i<HDRCNT; i++)
		{
			TextView hdr_text = new TextView(this);
			hdr_text.setText(Header[i]);
			hdr_text.setTextColor(Color.BLACK);
			hdr_text.setBackgroundColor(Color.argb(255, 192, 192, 192));
			if(i != 1) hdr_text.setSingleLine();
//			if(i == 0) hdr_text.setGravity(Gravity.CENTER_HORIZONTAL);
			if(i == 0) hdr_text.setMinEms(6);
//			if(i == 3) hdr_text.setMinEms(4);
			TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			tlo.setMargins(1, 2, 1, 1);
			hdr_text.setLayoutParams(tlo);
			hdr_text.setPadding(0, 0, 5, 0);
			hdr_text.setTextSize(FontSize);
			hdr_text.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
			tr.addView(hdr_text);
		}
		tl.addView(tr, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if(HMXCount <= 0)
		{	
//			Log.d("Chenthil", "Insufficient HMX entries: "+HMXCount.toString() );
			return;
		}
		else
		{
			String[] params = new String[1]; 
			params[0] = "20";
			Integer BGColor;
			TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			tlo.setMargins(1, 1, 1, 1);
			try {
				db = openOrCreateDatabase(DB_NAME, 0, null);
				Cursor c = db.rawQuery("select  * from hmx order by rate desc limit ?",params);
				while(c.moveToNext())
				{
					systag = c.getInt(0);
					ne_name = c.getString(1);
					Rate = c.getFloat(2);
					nodeid = c.getInt(3);
					CurCnt++;
					tr_data = new TableRow(this);
					if(CurCnt%2 == 0)
					{
						BGColor = Color.argb(255, 227, 227, 227);
					}
					else
					{
						BGColor = Color.WHITE;
					}
					TextView tv_nename = new TextView(this);
					tv_nename.setText(ne_name);
					tv_nename.setTextColor(Color.BLACK);
					tv_nename.setBackgroundColor(BGColor);
					tv_nename.setEllipsize(TextUtils.TruncateAt.END);
					tv_nename.setPadding(0, 0, 5, 0);
					tv_nename.setHint(ne_name);
					tv_nename.setTextSize(FontSize);
					tv_nename.setLayoutParams(tlo);
					tv_nename.setGravity(Gravity.CENTER_HORIZONTAL);
					tv_nename.setOnLongClickListener(longClickListener);
					tr_data.addView(tv_nename);
					
					TextView tv_rate = new TextView(this);
					tv_rate.setText(Rate.toString());
					tv_rate.setTextColor(Color.BLACK);
					tv_rate.setBackgroundColor(BGColor);
					tv_rate.setEllipsize(TextUtils.TruncateAt.END);
					tv_rate.setPadding(0, 0, 5, 0);
					tv_rate.setHint(Rate.toString());
					tv_rate.setTextSize(FontSize);
					tv_rate.setLayoutParams(tlo);
					tv_rate.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
					tv_rate.setGravity(Gravity.CENTER_HORIZONTAL);
					// RED
					if(Rate >= 500.0f) { tv_rate.setBackgroundColor(Color.argb(255, 255, 0, 0)); }
					// ORANGE
					else if(Rate >= 100.0f && Rate < 500.0f) { tv_rate.setBackgroundColor(Color.argb(255, 255, 153, 0)); }
					// YELLOW
					else if(Rate > 0.0f && Rate < 100.0f) { tv_rate.setBackgroundColor(Color.argb(255, 255, 255, 0)); }
					// BLUE
					else if(Rate == 0.0f) { tv_rate.setBackgroundColor(Color.argb(255, 0, 255, 255)); }
					// GREEN
					else if(Rate < 0.0f) { tv_rate.setBackgroundColor(Color.argb(255, 0, 255, 0)); }
					tr_data.addView(tv_rate);
					
					Button inc_button = new Button(this, null, android.R.attr.buttonStyleSmall);
					inc_button.setText(nodeid.toString());
					inc_button.setTextColor(Color.BLACK);
					inc_button.setHint(ne_name);
					inc_button.setEllipsize(TextUtils.TruncateAt.END);
					inc_button.setTextSize(10.0f);
					inc_button.setId(systag);
					inc_button.setOnClickListener(clickListener);
					inc_button.setLayoutParams(tlo);
					tr_data.addView(inc_button);
					
					tl.addView(tr_data);
				}
			}
			catch(Exception e) {
				Log.e("Chenthil", "DB Open: "+e.toString());
			}
			db.close();
		}
	}
}//End of class
