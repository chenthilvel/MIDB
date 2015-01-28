package com.chen.midb3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class MIDB3 extends Activity  {
	public static final String APP_VERSION="v0.3";
	public static final String APP_TITLE="Mobile iDashBoard "+APP_VERSION;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_QUIT = 2;
	private static final String DB_NAME = "midb";
	private SQLiteDatabase db;
	private Spinner s;
	private String TBL_CREATE_DBVERSION = "create table dbversion(version integer);";
	private String TBL_CREATE_TOPN = "create table topn(key text , systag integer not null, alarm_type integer, " +
	"alarm_name text, ne_name text, state integer, severity integer, alarm_id integer, alarm_text text, time text, " +
	"rate integer, rank integer, incident_type integer, first_occurrence text, last_occurrence text, " +
	"additional_text text, nodeid integer);";
	private String TBL_CREATE_HMX = "create table hmx(systag integer not null, ne_name text, rate integer, nodeid integer);";
	private String TBL_CREATE_KPI = "create table kpi(kpi_id integer not null,systag integer not null, Timestamp text, " +
			"Cell1 integer, Cell2 integer, Cell3 integer, Total integer);";
	private String TBL_CREATE_DR = "create table device_report(key text , systag integer not null, alarm_type integer, " +
	"alarm_name text, ne_name text, state integer, severity integer, alarm_id integer, alarm_text text, time text, " +
	"rate integer, rank integer, incident_type integer, first_occurrence text, last_occurrence text, " +
	"additional_text text, nodeid integer);";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.setTitle(com.chen.midb3.MIDB3.APP_TITLE);
		s = (Spinner) findViewById(R.id.spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.omcs, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		Button b_topn = (Button) findViewById(R.id.topn);
		b_topn.setOnClickListener(new OnClickListener() {
			public void onClick(View   v) 
			{         
				String str = ((String) s.getSelectedItem()).trim();
//				Log.d("Chenthil", "Selected OMC: <"+str+">");
				Intent intent_TopN = new Intent(MIDB3.this, TopN.class);
				CreateDatabase(getApplicationContext());
				intent_TopN.putExtra("Version", "1.25");
				intent_TopN.putExtra("OMC", str);
				startActivity(intent_TopN);
			}                                           
		});
		Button b_hmx = (Button) findViewById(R.id.hmx);
		b_hmx.setOnClickListener(new OnClickListener() {
			public void onClick(View   v) 
			{         
				String str = ((String) s.getSelectedItem()).trim();
//				Log.d("Chenthil", "Selected OMC: <"+str+">");
				Intent intent_HMX = new Intent(MIDB3.this, HMX.class);
				CreateDatabase(getApplicationContext());
				intent_HMX.putExtra("Version", "1.25");
				intent_HMX.putExtra("OMC", str);
				startActivity(intent_HMX);
			}                                           
		});
		Button b_kpi = (Button) findViewById(R.id.kpi);
		b_kpi.setOnClickListener(new OnClickListener() {
			public void onClick(View   v) 
			{         
				String str = ((String) s.getSelectedItem()).trim();
//				Log.d("Chenthil", "Selected OMC: <"+str+">");
				Intent intent_HMX = new Intent(MIDB3.this, KPIChart.class);
				CreateDatabase(getApplicationContext());
				intent_HMX.putExtra("Version", "1.25");
				intent_HMX.putExtra("OMC", str);
				startActivity(intent_HMX);
			}                                           
		});
	}
	public void CreateDatabase(Context ctx)
	{
		try {
			db = ctx.openOrCreateDatabase(DB_NAME, 0, null);
//			Log.d("Chenthil", "DB Version: "+db.getVersion());
			String [] params = {"dbversion"};
			Cursor c = db.rawQuery("select tbl_name from sqlite_master where name = ?", params);
			int createDB = c.getCount();
//			Log.d("Chenthil", "Row Count: "+createDB);
			c.close();
			if(createDB == 0) 
			{
				db.execSQL(TBL_CREATE_DBVERSION);
				db.execSQL(TBL_CREATE_TOPN);
				db.execSQL(TBL_CREATE_HMX);
				db.execSQL(TBL_CREATE_KPI);
				db.execSQL(TBL_CREATE_DR);
//				Log.d("Chenthil","Created Tables");
			}
		} catch (Exception e) {
			Log.d("Chenthil","Caught Exception while creating DB: "+e.toString());
		}
	}

	/* Creates the menu items */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ABOUT, 0, "About").setAlphabeticShortcut('A').setIcon(R.drawable.midb_info);
		menu.add(0, MENU_QUIT, 0, "Quit").setAlphabeticShortcut('Q').setIcon(R.drawable.midb_close);
		return true;
	}

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
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
}//End of Class
