package com.wisense.cadal;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

public class GetSensors implements SensorEventListener{
	
	protected Context context;
	
	static String TAG="FALL_DETECTION";
	
	private SensorManager sensorManager;
	
	String[] sensorsS={"Accelerometro","Campo Magnetico","Giroscopio","Orientazione","Vett. Rot. Geom.","Gravità","Acc. Lineare","Vett. Rotazione","Movimenti","Contapassi","Rilev. Passo","Pressione","Temp. Ambiente","Umid. Relativa"};
	boolean[] sensorsPresent={false,false,false,false,false,false,false,false,false,false,false,false,false,false};
	int[] sensorTypes={Sensor.TYPE_ACCELEROMETER,Sensor.TYPE_MAGNETIC_FIELD,Sensor.TYPE_GYROSCOPE,Sensor.TYPE_ORIENTATION,Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,Sensor.TYPE_GRAVITY,Sensor.TYPE_LINEAR_ACCELERATION,Sensor.TYPE_ROTATION_VECTOR,Sensor.TYPE_SIGNIFICANT_MOTION,Sensor.TYPE_STEP_COUNTER,Sensor.TYPE_STEP_DETECTOR, Sensor.TYPE_PRESSURE,Sensor.TYPE_AMBIENT_TEMPERATURE,Sensor.TYPE_RELATIVE_HUMIDITY};
	String[] sensorsTypes=new String[sensorTypes.length];
	String[] sensorsNames=new String[sensorTypes.length];
	String[] sensorsVendors=new String[sensorTypes.length];
	int[] sensorsMinDelayes=new int[sensorTypes.length];
	float[] sensorsMaxRanges=new float[sensorTypes.length];
	float[] sensorsPowers=new float[sensorTypes.length];
	float[] sensorsResolutions=new float[sensorTypes.length];
	ArrayList<Sensor>sensors=new ArrayList<Sensor>();
	
	ArrayList<SensorEntry> sensorsList=new ArrayList<SensorEntry>();

	
	public GetSensors(Context context){
        this.context = context.getApplicationContext();
    }




//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		// TODO Auto-generated method stub
//		super.onCreate(savedInstanceState);
//		;
////		for (int i=0; i<sensorTypes.length;i++){
////			if(sensorManager.registerListener(this, sensorManager.getDefaultSensor(
////					sensorTypes[i]), SensorManager.SENSOR_DELAY_FASTEST)){
////				sensorsPresent[i]=true;
////			}
////		}
//	}
//
//	
//
//
//
//
//	@Override
//	protected void onPause() {
//		// TODO Auto-generated method stub
//		super.onPause();
////		sensorManager.unregisterListener(this);
//		finish();
//	}





	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	

	
public ArrayList getSensorList(){
	sensorManager=(SensorManager) context.getSystemService(context.SENSOR_SERVICE);
	
	Log.d(TAG,"GetSensors, getSensorList, sensorManager: "+sensorManager.toString());
		
		for (int i=0; i<sensorTypes.length;i++){
			
			Log.d(TAG,"GetSensors, getSensorList, i= "+Integer.toString(i)+", Sensore: "+sensorsS[i]);
			
			SensorEntry sensor=new SensorEntry();
			
			sensor.setType(sensorsS[i]);
			
			try {
				sensors.add( sensorManager.getDefaultSensor(sensorTypes[i]));
				//				if (sensors[i]!=null) {
				sensorsNames[i] = sensors.get(i).getName();
				sensorsVendors[i] = sensors.get(i).getVendor();
				sensorsMinDelayes[i] = sensors.get(i).getMinDelay();
				sensorsMaxRanges[i] = sensors.get(i).getMaximumRange();
				sensorsPowers[i] = sensors.get(i).getPower();
				sensorsResolutions[i] = sensors.get(i).getResolution();
				Log.d(TAG,"GetSensors, getSensorList, Sensore: "+sensorsS[i]+", Name: "+sensorsNames[i]+", Vendor: "+sensorsVendors[i]+
						", Min Delay: "+Integer.toString(sensorsMinDelayes[i])+", Max Range: "+Float.toString(sensorsMaxRanges[i])+
						", Power: "+Float.toString(sensorsPowers[i])+", Resolution: "+Float.toString(sensorsResolutions[i]));
			} catch (Exception e) {
				
				
//				} else{
					sensorsNames[i] = "Assente";
					sensorsVendors[i] = "";
					sensorsMinDelayes[i] = 0;
					sensorsMaxRanges[i] = 0;
					sensorsPowers[i] = 0;
					sensorsResolutions[i] = 0;
//				}
			}
					
				
				sensor.setVendor(sensorsVendors[i]);
				sensor.setName(sensorsNames[i]);
				sensor.setMinDelay(sensorsMinDelayes[i]);
				sensor.setMaxRange(sensorsMaxRanges[i]);
				sensor.setResolution(sensorsResolutions[i]);
				sensor.setPower(sensorsPowers[i]);
				
				sensorsList.add(sensor);
			
		}
		
		return sensorsList;
		
	}
	
	

}
