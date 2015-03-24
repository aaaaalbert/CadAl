package com.wisense.cadal;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by lucapernini on 17/03/15.
 * Adapter for Fall Elements for database and statistics listview in mainactivity
 */
public class FallAdapter extends ArrayAdapter {

    private static final String TAG="FALL DETECTION";


    private ArrayList<FallEntry> objects;

    public FallAdapter(Context context, int textViewResourceId, ArrayList<FallEntry> objects){//, boolean nuovo) {
        super(context, textViewResourceId, objects);
        this.objects = objects;
        Log.d(TAG,"objects: "+objects.size());


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Log.d(TAG, "getView: position " + position);
        // TODO Auto-generated method stub

        View v = convertView;


        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.fall, null);

        }

        FallEntry i = objects.get(position);
        Log.d(TAG,"date 0: "+i.getDate());

        if (i != null) {
            TextView dateTv=(TextView) v.findViewById(R.id.Date);
            TextView confirmedTV=(TextView) v.findViewById(R.id.confirmation);
            TextView trainTV=(TextView) v.findViewById(R.id.train);
            TextView notifiedTV=(TextView) v.findViewById(R.id.notified);



            dateTv.setText(i.getDate());
            if(i.getConfirmed()==1){
                confirmedTV.setText("CONFIRMED");
                confirmedTV.setTextColor(Color.GREEN);
                if (i.getTrain()==1){
                    trainTV.setText("USED FOR TRAINING");
                    trainTV.setTextColor(Color.GREEN);
                } else {
                    trainTV.setText("NOT USED FOR TRAINING");
                    trainTV.setTextColor(Color.RED);
                }
                if (i.getNotified()==1){
                    notifiedTV.setText("NOTIFIED TO RELATIVE");
                    notifiedTV.setTextColor(Color.GREEN);
                } else {
                    notifiedTV.setText("NOT NOTIFIED TO RELATIVE");
                    notifiedTV.setTextColor(Color.RED);
                }
            } else {
                confirmedTV.setText("NOT CONFIRMED");
                confirmedTV.setTextColor(Color.RED);
                if (i.getTrain()==1){
                    trainTV.setText("USED FOR TRAINING");
                    trainTV.setTextColor(Color.GREEN);
                } else {
                    trainTV.setText("NOT USED FOR TRAINING");
                    trainTV.setTextColor(Color.RED);
                }
            }
        }

        return v;
    }
}
