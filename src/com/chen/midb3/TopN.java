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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableRow.LayoutParams;

public class TopN extends Activity implements SensorEventListener, Runnable {
	private static final int MENU_REFRESH_UI = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_QUIT = 2;
	private static final String DB_NAME = "midb";
	private SQLiteDatabase db;
	private String [] [] RowData = null;
	private String OMC = "", OMCURL = "";
	private TableLayout tl;
	private String[] Header = { "Rank","Type","NE  Name", "Rating ", "Incident ID", "Time" };
	private String[] SEVMAP = {"Indeterminate", "Critical", "Major", "Minor", "Warning", "Clear"};
	private String[] INCMAP = { "Availability", "Utilization", "Actionable Alarm" };
	private int HDRCNT = 6;
	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	private Float FontSize = 9.0f;
	private String SMSText = null;
	private ProgressDialog pd;
	private String LastUpdated = "";
	
	View.OnClickListener clickListener = new View.OnClickListener() {
		public void onClick(View v) {
//			Log.i("Chenthil","Clicked a button: "+v.getId()+" "+RowData[v.getId()][5]);
			String IncID =  null;
			if(RowData[v.getId()][5] != null)
			{
				IncID = RowData[v.getId()][5];
			}
			ShowDeviceReport(v.getId(), IncID);
//			ShowDetails(v.getId(), IncID);
		}
	};
	View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
		public boolean onLongClick(View v) {
			int id = v.getId() / 100;
			String IncID = null;
//			Log.i("Chenthil","Clicked a button: "+id+" "+v.getId()+" ");
			TextView tv = (TextView) v;
//			Log.d("Chenthil", "Long Clicked on: "+tv.getText()+" "+tv.getHint());
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
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.setTitle(com.chen.midb3.MIDB3.APP_TITLE);
		this.setTheme(android.R.style.Theme_Light);
		// 0 - Undefined 1 - Portrait 2 - Landscape 3 - Square
		
		
		String Ver = savedInstanceState != null ? savedInstanceState.getString("Version"):null;
		if(Ver == null)
		{
			Bundle extras = getIntent().getExtras();
			Ver = extras != null ? extras.getString("Version"):"0.0";
			OMC = extras != null ? extras.getString("OMC"):"br_sp2sys";
		}
		if(OMC.equals("br_sp2sys"))
		{
			OMCURL = "https://203.8.58.185:8443/webmmi/idb?requestType=1";
//			OMCURL = "https://10.232.176.204:8443/webmmi/idb?requestType=1";
//			OMCURL = "http://10.232.170.99:8020/TopN.xml";
		}
		else if(OMC.equals("br_sp3sys"))
		{
			OMCURL = "https://203.8.58.29:8443/webmmi/idb?requestType=1";
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

/*	@Override
    protected void onResume() {
		Log.d("Chenthil","TopN: onResume called");
		super.onResume();
    }*/

	public void FetchData()
	{
		pd = ProgressDialog.show(this, null, "Fetching Top N Incidents from "+OMC, true, false);
		Thread thread = new Thread(this);
		thread.start();
//		Log.d("Chenthil", "Thread execution finished");
	}
	public void run() {
		GetTopN rp = new GetTopN();
//		Log.d("Chenthil", "Calling ParseResults with: "+OMCURL);
		RowData = rp.ParseResults(getApplicationContext(), OMCURL);
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

		TopN_Title.setText("Top N Incidents - "+OMC);
		TopN_Title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
		TopN_Title.setTextColor(Color.BLUE);
		TopN_Title.setBackgroundColor(Color.WHITE);
//		TopN_Title.setBackgroundResource(android.R.drawable.bottom_bar);
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
		menu.add(0, MENU_QUIT, 0, "Close TopN").setAlphabeticShortcut('Q').setIcon(R.drawable.midb_close);
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
	public void ShowDeviceReport(int Rank, String IncID)
	{
		String[] params = new String[2]; 
		params[0] = Integer.toString(Rank);
		params[1] = IncID;
		Integer systag = 0;
		String ne_name = "";
		try {
			db = openOrCreateDatabase(DB_NAME, 0, null);
			Cursor c = db.rawQuery("select systag, ne_name from topn where rank = ? and key = ?", params);
			while(c.moveToNext())
			{
				systag = c.getInt(0);
				ne_name = c.getString(1);
			}
//			Log.d("Chenthil", "Retrieved: "+systag.toString()+"for "+IncID);
		}
		catch(Exception e) {
			Log.e("Chenthil", "DB Open: "+e.toString());
		}
		db.close();
		Intent intent_DR = new Intent(TopN.this, DeviceReport.class);
		intent_DR.putExtra("Version", "1.25");
		intent_DR.putExtra("OMC", OMC);
		intent_DR.putExtra("systag", systag.toString());
		intent_DR.putExtra("ne_name", ne_name);
		startActivity(intent_DR);
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
	public void PopulateUI()
	{
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
//			hdr_text.setBackgroundResource(android.R.drawable.bottom_bar);
			hdr_text.setSingleLine();
			if(i == 0) hdr_text.setGravity(Gravity.CENTER_HORIZONTAL);
			if(i == 2) hdr_text.setMinEms(6);
			if(i == 3) hdr_text.setMinEms(4);
			TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			tlo.setMargins(1, 2, 1, 1);
			hdr_text.setLayoutParams(tlo);
			hdr_text.setPadding(0, 0, 5, 0);
			hdr_text.setTextSize(FontSize);
			hdr_text.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
			tr.addView(hdr_text);
		}
		tl.addView(tr, new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if(RowData == null)
		{	
//			Log.d("Chenthil", "RowData is null");
			return;
		}
		else
		{
			TableRow tr_data = null;
			Float crate = 0.0f;
			for(Integer i=1;i<=10; i++)
			{
				tr_data = new TableRow(this);
				crate = -1.0f;
				int BGColor=0; 
				if(i%2 == 0)
				{
					BGColor = Color.argb(255, 227, 227, 227);
				}
				else
				{
					BGColor = Color.WHITE;
				}
				for(Integer j=0;j<=HDRCNT;j++)
				{
					if(j == 0 && RowData[i][j] != null) crate = Float.parseFloat(RowData[i][j]);
					if(j == 5) // For Buttons
					{
						Button inc_button = new Button(this, null, android.R.attr.buttonStyleSmall);
						inc_button.setText(RowData[i][j]);
						inc_button.setTextColor(Color.BLACK);
						inc_button.setHint(RowData[i][j]);
						inc_button.setEllipsize(TextUtils.TruncateAt.END);
						inc_button.setTextSize(9.0f);
						inc_button.setId(i);
						inc_button.setOnClickListener(clickListener);
						TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
						tlo.setMargins(1, 1, 1, 1);
						inc_button.setLayoutParams(tlo);
						tr_data.addView(inc_button);
					}
					else if(j != 0)
					{
						TextView hdr_text = new TextView(this);
						hdr_text.setText(RowData[i][j]);
						hdr_text.setTextColor(Color.BLACK);
						hdr_text.setBackgroundColor(BGColor);
						hdr_text.setEllipsize(TextUtils.TruncateAt.END);
						hdr_text.setTextSize(FontSize);
						if(j != 6) hdr_text.setPadding(0, 0, 5, 0);

						if(j == 3) 
						{ 
							hdr_text.setHint(RowData[i][5]);
							int myid = Integer.parseInt(RowData[i][1]) * 100;
							hdr_text.setId(myid);
							hdr_text.setOnLongClickListener(longClickListener);
							hdr_text.setGravity(Gravity.CENTER_HORIZONTAL);
//							hdr_text.setTextSize(FontSize+1);
						}
						
						TableRow.LayoutParams tlo = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
						tlo.setMargins(1, 1, 1, 1);
						hdr_text.setLayoutParams(tlo);
						if(j == 1)
						{
							hdr_text.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD), Typeface.BOLD);
							hdr_text.setGravity(Gravity.CENTER_HORIZONTAL);
							// RED
							if(crate >= 500.0f) { hdr_text.setBackgroundColor(Color.argb(255, 255, 0, 0)); }
							// ORANGE
							else if(crate >= 100.0f && crate < 500.0f) { hdr_text.setBackgroundColor(Color.argb(255, 255, 153, 0)); }
							// YELLOW
							else if(crate > 0.0f && crate < 100.0f) { hdr_text.setBackgroundColor(Color.argb(255, 255, 255, 0)); }
							// BLUE
							else if(crate == 0.0f) { hdr_text.setBackgroundColor(Color.argb(255, 0, 255, 255)); }
							// GREEN
							else if(crate < 0.0f) { hdr_text.setBackgroundColor(Color.argb(255, 0, 255, 0)); }
						}
						tr_data.addView(hdr_text);
					}
				}
				tl.addView(tr_data);
			}
		}
	}
	public void onSensorChanged(SensorEvent event)
	{
//		Log.d("Chenthil", "onSensorChanged fired for: "+event.sensor.getName()+" Type: "+event.sensor.getType());
	}
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		Log.d("Chenthil","onAccuracyChanged: " + sensor.getName() + ", accuracy: " + accuracy);
	}
}//End of class
