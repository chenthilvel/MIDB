package com.chen.midb3;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class KPIChart extends Activity implements Runnable {
	private String OMCURL = "https://203.8.58.29:8443/webmmi/idb?requestType=4&key=36&kpi_id=1&key2=93321";
	public Integer systag = 93321, kpi_id = 1;
	private ProgressDialog pd;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(com.chen.midb3.MIDB3.APP_TITLE);
		CreateChart();
	}
	
	private void CreateChart()
	{
		FetchData();
	}
	public void FetchData()
	{
		pd = ProgressDialog.show(this, null, "Fetching Interconnect Call Statistics for RJ927RJ ", true, false);
		Thread thread = new Thread(this);
		thread.start();
//		Log.d("Chenthil", "Thread execution finished");
	}
	public void run() {
		@SuppressWarnings("unused")
		Integer results = 0;
		GetKPIValues kpi = new GetKPIValues();
		results = kpi.ParseResults(getApplicationContext(), OMCURL, systag, kpi_id);
//		Log.d("Chenthil", "Got data for "+results+" intervals");
		
		handler.sendEmptyMessage(0);
	}
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			pd.dismiss();
//			Log.d("Chenthil", "Dismissed dialog.. Calling RefreshUI");
			Intent intent = new DisplayChart().execute(getApplicationContext(), systag, kpi_id);
			startActivity(intent);
//			this.finish();
			com.chen.midb3.KPIChart.this.finish();
		}
	};
} //End of Class
