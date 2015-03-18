package com.wisense.cadal;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

class SensorAdapter extends ArrayAdapter
{
    
	private ArrayList<SensorEntry> objects;

	public SensorAdapter(Context context, int textViewResourceId, ArrayList<SensorEntry> objects){//, boolean nuovo) {
    	super(context, textViewResourceId, objects);
        this.objects = objects;
        //this.nuovo=nuovo;
    }
    
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		
		View v = convertView;
        //ViewHolder view;

        if (v == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.sensor, null);
			//if (nuovo) v.setBackgroundResource(R.drawable.listitem_sfondo2nuovo);
		}
        
        SensorEntry i = objects.get(position);
        
        if (i != null) {
        	TextView typeTV=(TextView) v.findViewById(R.id.sensorType);
        	TextView vendorTV=(TextView) v.findViewById(R.id.sensorVendor);
        	TextView nameTV=(TextView) v.findViewById(R.id.sensorName);
        	TextView minDelayTV=(TextView) v.findViewById(R.id.sensorMinDelay);
        	TextView maxRangeTV=(TextView) v.findViewById(R.id.sensorMaxRange);
        	TextView resolutionTV=(TextView) v.findViewById(R.id.sensorResolution);
        	TextView powerTV=(TextView) v.findViewById(R.id.sensorPower);
        	
        	typeTV.setText(i.getType());
        	vendorTV.setText(i.getVendor());
        	nameTV.setText(i.getName());
        	minDelayTV.setText(Integer.toString(i.getMinDelay()));
        	maxRangeTV.setText(Float.toString(i.getMaxRange()));
        	resolutionTV.setText(Float.toString(i.getResolution()));
        	powerTV.setText(Float.toString(i.getPower()));
        }
		
		return v;
	}
}
