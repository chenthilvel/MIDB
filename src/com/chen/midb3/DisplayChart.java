package com.chen.midb3;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public  class DisplayChart extends AbstractChart {

	public String[][] CHART_TITLE = { {""}, {"Interconnect Calls"} };

	public String getName() {
		return "Sales growth";
	}
	public String getDesc() {
		return "The sales growth across several years (time chart)";
	}

	public Intent execute(Context context, Integer systag, Integer kpi_id) 
	{
		String[] titles = new String[1];
		
		GetKPIValues kpi = new GetKPIValues();
		kpi.FetchData(context, systag, kpi_id);
//		titles[0] = CHART_TITLE[kpi_id][0]+" - "+kpi.getNEName();
		titles[0] = CHART_TITLE[kpi_id][0]+" - RJ927RJ";
		List<Date[]> dates = new ArrayList<Date[]>();
		List<double[]> values = new ArrayList<double[]>();
		dates = kpi.getDates();
		values = kpi.getValues();
		Date[] min = kpi.getxMin();
		Date[] max = kpi.getxMax();
//		double yMin = kpi.getyMin();
		double yMax = kpi.getyMax();
//		Date[] tmp = dates.get(0);
//		Log.d("Chenthil", "D: "+dates.size()+"L: "+tmp.length+" "+tmp[0].toString()+" "+tmp[0].getTime());
//		Log.d("Chenthil", "min: "+min[0].toString()+" max: "+max[0].toString());
		/*Date[] dateValues = new Date[] { new Date(95, 0, 1), new Date(95, 3, 1), new Date(95, 6, 1),
				new Date(95, 9, 1), new Date(96, 0, 1), new Date(96, 3, 1), new Date(96, 6, 1),
				new Date(96, 9, 1), new Date(97, 0, 1), new Date(97, 3, 1), new Date(97, 6, 1),
				new Date(97, 9, 1), new Date(98, 0, 1), new Date(98, 3, 1), new Date(98, 6, 1),
				new Date(98, 9, 1), new Date(99, 0, 1), new Date(99, 3, 1), new Date(99, 6, 1),
				new Date(99, 9, 1), new Date(100, 0, 1), new Date(100, 3, 1), new Date(100, 6, 1),
				new Date(100, 9, 1), new Date(100, 11, 1) };
		dates.add(dateValues);

		values.add(new double[] { 4.9, 5.3, 3.2, 4.5, 6.5, 4.7, 5.8, 4.3, 4, 2.3, 0.5, 2.9, 3.2,
				5.5, 4.6, 9.4, 4.3, 1.2, 0, 0.4, 4.5, 3.4, 4.5, 4.3, 4 });*/
		int[] colors = new int[] { Color.GREEN };
		PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE };
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		setChartSettings(renderer, titles[0], "Date", "Calls", 
				min[0].getTime(), max[0].getTime(), 
				0, yMax,  Color.GRAY, Color.LTGRAY);
		renderer.setYLabels(10);
		renderer.setXLabels(8);
		renderer.setShowLegend(false);
		
		return ChartFactory.getTimeChartIntent(context, buildDateDataset(titles, dates, values),
				renderer, "dd/MM HH:mm");
	}
}
