package com.chen.midb3;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableRow.LayoutParams;

public class DeviceReport extends Activity  implements Runnable{
	private static final int MENU_REFRESH_UI = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_QUIT = 2;
	private static final String DB_NAME = "midb";
	private SQLiteDatabase db;
	private String OMC = "", OMCURL = "", systag_str= "", ne_name = "";
	private Integer systag = 0;
	private TableLayout tl;
	private String[] Header = { "Rank","Type","Alarm ", "Rating", "Incident ID", "Time" };
	private String[] SEVMAP = {"Indeterminate", "Critical", "Major", "Minor", "Warning", "Clear"};
	private String[] INCMAP = { "AV", "UT", "AA" };
	private String[] INCMAP_LONG = { "Availability", "Utilization", "Actionable Alarm" };
	private int HDRCNT = 6, EntryCount = 0;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	private Float FontSize = 9.0f;
	private String SMSText = null;
	private ProgressDialog pd;
	private String LastUpdated = "";
	
	View.OnClickListener clickListener = new View.OnClickListener() {
		public void onClick(View v) {
			TextView tv =  (TextView) v;
			String IncID = tv.getText().toString();
//			Log.i("Chenthil","Clicked a button: "+v.getId()+" "+IncID);
			ShowDetails(v.getId(), IncID);
		}
	};
	View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			int id = v.getId() / 100;
			String IncID = null;
			TextView tv = (TextView) v;
//			Log.d("Chenthil", "Long Clicked on: "+id+" "+tv.getText()+" "+tv.getHint());
			IncID = tv.getHint().toString();
			ShowDetails(id, IncID);
			return true;
		}
	};
	
	public static String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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
			FontSize = 10.0f;
		}
		String Ver = savedInstanceState != null ? savedInstanceState.getString("Version"):null;
		if(Ver == null)
		{
			Bundle extras = getIntent().getExtras();
			Ver = extras != null ? extras.getString("Version"):"0.0";
			OMC = extras != null ? extras.getString("OMC"):"br_sp2sys";
			systag_str = extras != null ? extras.getString("systag"):"0";
			systag = Integer.parseInt(systag_str);
			ne_name = extras != null ? extras.getString("ne_name"):" ";
		}
		if(OMC.equals("br_sp2sys"))
		{
//			OMCURL = "https://10.232.176.204:8443/webmmi/idb?requestType=5&key="+systag;
			OMCURL = "https://203.8.58.185:8443/webmmi/idb?requestType=5&key="+systag;
		}else if(OMC.equals("br_sp3sys"))
		{
			OMCURL = "https://203.8.58.29:8443/webmmi/idb?requestType=5&key="+systag;
		}
//		Log.i("Chenthil", "Version: "+Ver+" OMC: "+OMC);
		
		FetchData();
	}
	public void run() {
		GetDR rp = new GetDR();
//		Log.d("Chenthil", "Calling ParseResults with: "+OMCURL);
		EntryCount = rp.ParseResults(getApplicationContext(), OMCURL);
//		Log.d("Chenthil", "Got "+EntryCount+" entries from server");
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

		TopN_Title.setText("Device Report - "+ne_name);
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
		menu.add(0, MENU_QUIT, 0, "Close").setAlphabeticShortcut('Q').setIcon(R.drawable.midb_close);
		return true;
	}

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
			Cursor c = db.rawQuery("select * from device_report where rank = ? and key = ?", params);
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
				incident_type = INCMAP_LONG[c.getInt(12)];
				addtl_text = c.getString(15);
			}
//			Log.d("Chenthil", "Retrieved: "+key+" <"+addtl_text+">");
		}
		catch(Exception e) {
			Log.e("Chenthil", "DR: ShowDetails() - "+e.toString());
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
		SMSText = "MIDB "+ne_name+"\nRank: "+rank.toString()+"  Rate: "+Rate.toString()+"\n"+incident_type+"\n"+severity+"\n"+alarm_name+"\n"+alarm_text+"\n"+alm_time+"\nEOM";
		layout.setVerticalScrollBarEnabled(true);
		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		alertDialog = builder.create();
		alertDialog.setCancelable(true);
		alertDialog.setCanceledOnTouchOutside(true);
		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Send as SMS", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				SmsManager m = SmsManager.getDefault();
				String destinationNumber ="5556";  
//				Log.i("Chenthil", "Sending SMS to: "+destinationNumber+" Length: "+SMSText.length());
//				Log.i("Chenthil", "Sending SMS: "+SMSText);
				m.sendTextMessage(destinationNumber, null, SMSText, null, null);
				Toast.makeText(getApplicationContext(), "Sent SMS to "+destinationNumber+"\n"+SMSText, Toast.LENGTH_LONG).show();
				dialog.cancel();
			}
		});
		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		alertDialog.show();
	}
	public void FetchData()
	{
		pd = ProgressDialog.show(this, null, "Retriving Alarms for "+ne_name, true, false);
		Thread thread = new Thread(this);
		thread.start();
//		Log.d("Chenthil", "Thread execution finished");
	}
	public void PopulateUI()
	{
//		FetchData();
//		Log.d("Chenthil", "Entered PopulateUI");
		
		tl = new TableLayout(this);
		tl.setPadding(5, 5, 5, 5);
		tl.setBackgroundColor(Color.argb(255, 128, 128, 128));
		tl.setStretchAllColumns(true);
		tl.setColumnShrinkable(2, true);
		tl.setColumnShrinkable(4, true);
		tl.setColumnShrinkable(5, true);
//		tl.setColumnShrinkable(6, true);
		tl.setHorizontalScrollBarEnabled(true);

		TableRow tr = new TableRow(this);
		for(int i=0;i<HDRCNT; i++)
		{
			TextView hdr_text = new TextView(this);
			hdr_text.setText(Header[i]);
			hdr_text.setTextColor(Color.BLACK);
			hdr_text.setBackgroundColor(Color.argb(255, 192, 192, 192));
			hdr_text.setSingleLine();
			if(i == 0) hdr_text.setGravity(Gravity.CENTER_HORIZONTAL);
			/*if(i == 2) hdr_text.setMinEms(4);
			if(i == 3 || i== 5) hdr_text.setMinEms(4);*/
			TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			tlo.setMargins(1, 2, 1, 1);
			hdr_text.setLayoutParams(tlo);
			hdr_text.setPadding(0, 0, 3, 0);
			hdr_text.setTextSize(FontSize);
			hdr_text.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
			tr.addView(hdr_text);
		}
		tl.addView(tr, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if(EntryCount <= 0)
		{	
//			Log.d("Chenthil", "RowData is null");
			return;
		}
		else
		{
			String [] params = new String[1];
			params[0] = systag.toString();
			Integer rank = 0, Count = 0, BGColor = 0;
			Float rate = 0.0f;
			String IncType = "", IncID = "", alm_time = "", alarm_name = "";
			try {
				db = openOrCreateDatabase(DB_NAME, 0, null);
				Cursor c = db.rawQuery("select  * from device_report where systag = ? order by time desc", params);
				while(c.moveToNext())
				{
					Count++;
					IncID = c.getString(0);
					alarm_name = c.getString(3);
//					ne_name = c.getString(4);
					alm_time = c.getString(9);
					rate = c.getFloat(10);
					rank = c.getInt(11);
					IncType = INCMAP[c.getInt(12)];
//					Log.d("Chenthil", "Adding: "+ne_name+" "+alarm_name+" "+rank);
					if(Count%2 == 0)
					{
						BGColor = Color.argb(255, 227, 227, 227);
					}
					else
					{
						BGColor = Color.WHITE;
					}
					addEntry(IncType, alarm_name, rate, rank, IncID, alm_time, BGColor);
				}
			}
			catch(Exception e) {
				Log.e("Chenthil", "PopulateUI(): Query DR - "+e.toString());
			}
			db.close();
		}
	}
	
	public void addEntry(String IncType, String alarm_name, Float rate, Integer rank, String IncID, String alm_time,int BGColor)
	{
		TableRow tr_data = null;
		TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		tlo.setMargins(1, 1, 1, 1);

		tr_data = new TableRow(this);

		TextView Rank_text = new TextView(this);
		Rank_text.setText(rank.toString());
		Rank_text.setTextColor(Color.BLACK);
		Rank_text.setBackgroundColor(BGColor);
		Rank_text.setEllipsize(TextUtils.TruncateAt.END);
		Rank_text.setPadding(0, 0, 3, 0);
		Rank_text.setTextSize(FontSize);
		Rank_text.setLayoutParams(tlo);
//		Rank_text.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
		Rank_text.setGravity(Gravity.CENTER_HORIZONTAL);
		// RED
		if(rate >= 500.0f) { Rank_text.setBackgroundColor(Color.argb(255, 255, 0, 0)); }
		// ORANGE
		else if(rate >= 100.0f && rate < 500.0f) { Rank_text.setBackgroundColor(Color.argb(255, 255, 153, 0)); }
		// YELLOW
		else if(rate > 0.0f && rate < 100.0f) { Rank_text.setBackgroundColor(Color.argb(255, 255, 255, 0)); }
		// BLUE
		else if(rate == 0.0f) { Rank_text.setBackgroundColor(Color.argb(255, 0, 255, 255)); }
		// GREEN
		else if(rate < 0.0f) { Rank_text.setBackgroundColor(Color.argb(255, 0, 255, 0)); }
		
		tr_data.addView(Rank_text);

		TextView IncType_text = new TextView(this);
		IncType_text.setText(IncType);
		IncType_text.setTextColor(Color.BLACK);
		IncType_text.setBackgroundColor(BGColor);
		IncType_text.setEllipsize(TextUtils.TruncateAt.END);
		IncType_text.setPadding(0, 0, 3, 0);
		IncType_text.setTextSize(FontSize);
		IncType_text.setLayoutParams(tlo);
		tr_data.addView(IncType_text);

		TextView name_text = new TextView(this);
		name_text.setText(alarm_name);
		name_text.setTextColor(Color.BLACK);
		name_text.setBackgroundColor(BGColor);
		name_text.setEllipsize(TextUtils.TruncateAt.END);
		name_text.setPadding(0, 0, 3, 0);
		name_text.setTextSize(FontSize);
		name_text.setLayoutParams(tlo);
		name_text.setHint(IncID);
		int myid = rank * 100;
		name_text.setId(myid);
		name_text.setOnLongClickListener(longClickListener);
		tr_data.addView(name_text);

		TextView rate_text = new TextView(this);
		rate_text.setText(rate.toString());
		rate_text.setTextColor(Color.BLACK);
		rate_text.setBackgroundColor(BGColor);
		rate_text.setEllipsize(TextUtils.TruncateAt.END);
		rate_text.setPadding(0, 0, 3, 0);
		rate_text.setTextSize(FontSize);
		rate_text.setLayoutParams(tlo);
		tr_data.addView(rate_text);

		Button inc_button = new Button(this, null, android.R.attr.buttonStyleSmall);
		inc_button.setText(IncID);
		inc_button.setTextColor(Color.BLACK);
		inc_button.setHint(IncID);
		inc_button.setEllipsize(TextUtils.TruncateAt.END);
		inc_button.setTextSize(9.0f);
		inc_button.setId(rank);
		inc_button.setOnClickListener(clickListener);
		inc_button.setLayoutParams(tlo);
		tr_data.addView(inc_button);
		
		TextView time_text = new TextView(this);
		time_text.setText(alm_time);
		time_text.setTextColor(Color.BLACK);
		time_text.setBackgroundColor(BGColor);
//		time_text.setEllipsize(TextUtils.TruncateAt.END);
//		time_text.setSingleLine(false);
		time_text.setTextSize(FontSize);
		TableRow.LayoutParams tlo2 = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		tlo2.setMargins(1, 1, 3, 1);
		time_text.setLayoutParams(tlo2);
		tr_data.addView(time_text);
		
		tl.addView(tr_data);
	}

}//End of class
