package com.wisense.cadal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.sax.StartElementListener;
import android.support.v4.widget.DrawerLayout;
import android.webkit.WebView.FindListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	


	static String TAG="FALL_DETECTION";
	static final String ACTION_FOREGROUND="com.wisense.cadal.FallDetectionService.FOREGROUND";
	
	
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    
    /**
     * Views elements
     */
    static ImageButton onOffButton;
    static TextView onOffTV;
    
    /**
     * Flags
     */
    private static boolean detectionRunning=false;
    
    /**
     * Classes
     */
    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
        /**
         * Views initializations
         */
       
        
    }
    
    

    @Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(TAG,"MainActivity, onResume");
		if(isServiceRunning(FallDetectionService.class)){
			onOffButton.setImageResource(R.drawable.on);
			onOffTV.setText(R.string.detection_active);
    		detectionRunning=true;
		} else {
			onOffButton.setImageResource(R.drawable.off);
			onOffTV.setText(R.string.detection_not_active);
    		detectionRunning=false;
		}

        SVM svm=new SVM(this);
        try {
            svm.train(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    


	@Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
            
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	View rootViewDef = null;
        	if(getArguments().getInt(ARG_SECTION_NUMBER)==1){
        		Log.d(TAG,"Main Activity, onCreateView, ARG_SECTION_NUMBER: 1");
        		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        		onOffButton= (ImageButton) rootView.findViewById(R.id.onoff_button_detection);
        	    onOffTV=(TextView) rootView.findViewById(R.id.status_detection);
        		return rootView;
        	} else if (getArguments().getInt(ARG_SECTION_NUMBER)==2){
                Log.d(TAG,"Main Activity, onCreateView, ARG_SECTION_NUMBER: 2");
                View rootView = inflater.inflate(R.layout.fragmen_falls_list, container, false);
                TextView nrFallsTV=(TextView)rootView.findViewById(R.id.totfalls);
                TextView nrCancFallsTV=(TextView)rootView.findViewById(R.id.cancfalls);
                ListView fallsLV=(ListView) rootView.findViewById(R.id.fallsList);
                ArrayList<FallEntry> falls=new ArrayList<FallEntry>();
                SQLite sql=new SQLite(getActivity());
                falls=sql.getAllFall();
                nrFallsTV.setText(sql.getFallsNumber());
                nrCancFallsTV.setText(sql.getCancFallsNumber());
                fallsLV.setAdapter(new FallAdapter(inflater.getContext(), R.layout.fall,falls));
                return rootView;

            } else if (getArguments().getInt(ARG_SECTION_NUMBER)==3){
        		Log.d(TAG,"Main Activity, onCreateView, ARG_SECTION_NUMBER: 3");
        		View rootView = inflater.inflate(R.layout.fragment_sensor_list, container, false);
        		ListView sensorLV=(ListView) rootView.findViewById(R.id.sensorList);
        		ArrayList<SensorEntry> sensors =new ArrayList<SensorEntry>();
        		GetSensors getSensors=new GetSensors(getActivity().getApplicationContext());
        		sensors=getSensors.getSensorList();
        		sensorLV.setAdapter(new SensorAdapter  (inflater.getContext(), R.layout.sensor, sensors));
//        		SensorAdapter<String> adapter = new ArrayAdapter<String>(  
//        			     inflater.getContext(), android.R.layout.simple_list_item_1,  
//        			     numbers_text);  
//        		sensorLV.setAdapter(adapter);  
        		return rootView;
        	} else return rootViewDef;
        	
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }


    
//    private static OnClickListener onOffClickListener = new OnClickListener() {
//
//		@Override
//		public void onClick(View v) {
//			if(!detectionRunning){
//        		onOffTV.setText(R.string.detection_active);
//        		onOffButton.setImageResource(R.drawable.on);
//        		detectionRunning=true;
//        		startFallDetectionService(this);
//        	} else {
//        		onOffTV.setText(R.string.detection_not_active);
//        		onOffButton.setImageResource(R.drawable.off);
//        		detectionRunning=false;
//        	}		
//		}
//};

    public void onOff(View v){
    	Log.d(TAG, "MainActivity, onOff");
    	if(!detectionRunning){
    		onOffTV.setText(R.string.detection_active);
    		onOffButton.setImageResource(R.drawable.on);
    		detectionRunning=true;
    		startFallDetectionService();
    		Toast.makeText(getApplicationContext(), "Fall Detection Active", 
   Toast.LENGTH_LONG).show();
    		//stopThread.start();
    	} else {
    		onOffTV.setText(R.string.detection_not_active);
    		onOffButton.setImageResource(R.drawable.off);
    		detectionRunning=false;
    		stopFallDetectionService();
    	}	
    }
    
    Thread stopThread = new Thread(){
        @Override
       public void run() {
            try {
               Thread.sleep(3500); // As I am using LENGTH_LONG in Toast
               MainActivity.this.finish();
           } catch (Exception e) {
               e.printStackTrace();
           }
        }  
      };

	private void startFallDetectionService() {
		Log.d(TAG, "MainActivity, startFallDetectionService");
		Intent fallDetIntent=new Intent(FallDetectionService.ACTION_FOREGROUND);
		fallDetIntent.setClass(MainActivity.this, FallDetectionService.class);
		startService(fallDetIntent);
	}
	
	private void stopFallDetectionService(){
    	Log.d(TAG,"MainActivity, stopFallDetectionService");
    	Intent fallDetIntent=new Intent();
    	fallDetIntent.setClass(MainActivity.this, FallDetectionService.class);
		stopService(fallDetIntent);
    }
	
    private boolean isServiceRunning(Class<?> serviceClass) {
        Log.i(TAG,"MainActivity, isServiceRunning");
    	ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
	
    
}
