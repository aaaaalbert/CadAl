package com.wisense.cadal;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wisense.cadal.Algorithms.MahonyAHRSUpdateResponse;

import java.io.IOException;
import java.util.Arrays;

public class FallDetectionService extends Service implements SensorEventListener {

    /**
     * Debug String
     */
    String TAG = "FALL_DETECTION";

    /**
     * Useful for Foreground feature of service
     */
    static final String ACTION_FOREGROUND = "com.wisense.cadal.FallDetectionService.FOREGROUND";

    int samplingTime = 25;

    /**
     * Sensors
     */
    private SensorManager sensorManager;
    //	private SensorManager sensorManagerACC;
//	private SensorManager sensorManagerMAG;
//	private SensorManager sensorManagerGIR;
//	private SensorManager sensorManagerBAR;
//	private SensorManager sensorManagerTEMP;
//	boolean accelerometerPresent=false;
//	boolean magneticFieldPresent=false;
//	boolean gyroscopePresent=false;
//	boolean pressurePresent=false;
//	boolean ambientTemperaturePresent=false;
    String[] sensorsS = {"Accelerometro", "Campo Magnetico"}; //OTHERS NOT USED: ,"Giroscopio","Orientazione","Vett. Rot. Geom.","Gravit�","Acc. Lineare","Vett. Rotazione","Movimenti","Contapassi","Rilev. Passo","Pressione","Temp. Ambiente","Umid. Relativa"};
    boolean[] sensorsPresent = {false, false}; //OTHERS NOT USED: ,false,false,false,false,false,false,false,false,false,false,false,false};
    int[] sensorTypes = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD}; //OTHERS NOT USED: ,Sensor.TYPE_GYROSCOPE,Sensor.TYPE_ORIENTATION,Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,Sensor.TYPE_GRAVITY,Sensor.TYPE_LINEAR_ACCELERATION,Sensor.TYPE_ROTATION_VECTOR,Sensor.TYPE_SIGNIFICANT_MOTION,Sensor.TYPE_STEP_COUNTER,Sensor.TYPE_STEP_DETECTOR, Sensor.TYPE_PRESSURE,Sensor.TYPE_AMBIENT_TEMPERATURE,Sensor.TYPE_RELATIVE_HUMIDITY};
    String[] sensorsTypes = new String[sensorTypes.length];
    String[] sensorsNames = new String[sensorTypes.length];
    String[] sensorsVendors = new String[sensorTypes.length];
    int[] sensorsMinDelayes = new int[sensorTypes.length];
    float[] sensorsMaxRanges = new float[sensorTypes.length];
    float[] sensorsPowers = new float[sensorTypes.length];
    float[] sensorsResolutions = new float[sensorTypes.length];
    Sensor[] sensors = new Sensor[sensorTypes.length];

    /**
     * Raw Data
     */
    float[] acc = new float[3];
    float[] gyr = new float[3];
    float[] mag = new float[3];
    //	float[] ori=new float[3]; NOT USED
//	float[] quat=new float[4]; NOT USED
//	float temp,pres; NOT USED
    float[] sensData = new float[15];
    float[][] accData = new float[3][2];
    float[][] magData = new float[3][2];
    //	float[] linAccData=new float[3]; NOT USED
    int alternateAcc = 0;
    int alternateMag = 0;
    //	int alternateLinAcc=0; NOT USED
//	float[] RotM= new float[16]; NOT USED
    float[] prevRotM = new float[16];
    //	float[] I=new float[16]; NOT USED
//	float[] angleChange=new float[3]; NOT USED
//	float[] orientation=new float[3]; NOT USED
//	float inclination; NOT USED
//	float[] Q=new float[4]; NOT USED
//	double filtZAcc=0; NOT USED
    int preWindow=160;
    int postWindow=160;
    float[] last2SecondsZAcceleration = new float[(int) 2000 / samplingTime];
    float[] last80SamplesAccelerationX = new float[preWindow]; //in realtà ne tengo in memoria di più in modo da evitare il transitorio del filtro
    float[] last80SamplesAccelerationY = new float[preWindow];
    float[] last80SamplesAccelerationZ = new float[preWindow];
    float[] last80SamplesMagneticFieldX = new float[preWindow];
    float[] last80SamplesMagneticFieldY = new float[preWindow];
    float[] last80SamplesMagneticFieldZ = new float[preWindow];
    float[] next80SamplesAccelerationX = new float[preWindow];
    float[] next80SamplesAccelerationY = new float[preWindow];
    float[] next80SamplesAccelerationZ = new float[preWindow];
    float[] next80SamplesMagneticFieldX = new float[preWindow];
    float[] next80SamplesMagneticFieldY = new float[preWindow];
    float[] next80SamplesMagneticFieldZ = new float[preWindow];

    /**
     * Threads and Classes
     */
    ElaborationThreadRotationAndOrientation eThread;
    Algorithms algo;
    int iteration = 0;
    float startTime;

    /**
     * Elaboration data
     */
//	float[] rms=new float[1]; NOT USED

    /**
     * Flags
     */
    boolean sensorsRegistered;
    boolean overTh = false;

    /**
     * Iterators
     */
    int storingIter = 0;

    /**
     * Useful for keep phone working when screen is off
     */
    private PowerManager.WakeLock mWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "FallDetectionService, onBind");
        return null;
    }


    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(TAG, "FallDetectionService, onCreate");

        algo = new Algorithms();

        PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "falldetection");
        mWakeLock.acquire();

        registerSensors();

        eThread = new ElaborationThreadRotationAndOrientation();
        eThread.start();

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "FallDetectionService, onSTartCommand");

        try {
            if (ACTION_FOREGROUND.equals(intent.getAction())) {
                Log.d(TAG, "FallDetectionService, starting service in foreground");
                startForeground(1, getCompatNotification());
                //Intent intentDisplayActiviy=new Intent(FallDetectionService.this)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);

    }

    private Notification getCompatNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Fall Detection Active")
                .setTicker("Running")
                .setWhen(System.currentTimeMillis());
        Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, startIntent, 0);
        builder.setContentIntent(contentIntent);
        Notification notification = builder.build();
        return notification;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mWakeLock.release();
        unregisterSensors();
        stopSelf();
    }


    public void registerSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
//		sensorManager.registerListener(this, sensorManager.getDefaultSensor(
//				Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);

//		for (int i=0; i<sensorTypes.length;i++){
//			if(sensorManager.registerListener(this, sensorManager.getDefaultSensor(
//					sensorTypes[i]), SensorManager.SENSOR_DELAY_FASTEST)){
//				sensorsPresent[i]=true;
//			}
//		}
        sensorsRegistered = true;
    }

    public void unregisterSensors() {
        sensorManager.unregisterListener(this);
        sensorsRegistered = false;
    }

    public void getSensors() {
        for (int i = 0; i < sensorTypes.length; i++) {
            if (sensorsPresent[i]) {
                sensors[i] = sensorManager.getDefaultSensor(sensorTypes[i]);
                sensorsNames[i] = sensors[i].getName();
                sensorsVendors[i] = sensors[i].getVendor();
                sensorsMinDelayes[i] = sensors[i].getMinDelay();
                sensorsMaxRanges[i] = sensors[i].getMaximumRange();
                sensorsPowers[i] = sensors[i].getPower();
                sensorsResolutions[i] = sensors[i].getResolution();
            } else {
                sensorsNames[i] = " assente ";
                sensorsVendors[i] = "";
                sensorsMinDelayes[i] = 0;
                sensorsMaxRanges[i] = 0;
                sensorsPowers[i] = 0;
                sensorsResolutions[i] = 0;
            }
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//			sensData[0]=event.values[0];
//			sensData[1]=event.values[1];
//			sensData[2]=event.values[2];
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//			sensData[3]=event.values[0];
//			sensData[4]=event.values[1];
//			sensData[5]=event.values[2];

            accData[0][alternateAcc] = event.values[0];
            accData[1][alternateAcc] = event.values[1];
            accData[2][alternateAcc] = event.values[2];
            //Log.d(TAG,"FallDetectionService, onSensorChanged, z acc: "+Float.toString(accData[2][alternateAcc]));
            if (alternateAcc == 0) {
                alternateAcc = 1;
            } else alternateAcc = 0;

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//			sensData[6]=event.values[0];
//			sensData[7]=event.values[1];
//			sensData[8]=event.values[2];
            magData[0][alternateMag] = event.values[0];
            magData[1][alternateMag] = event.values[1];
            magData[2][alternateMag] = event.values[2];
            if (alternateMag == 0) {
                alternateMag = 1;
            } else alternateMag = 0;
        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
//			sensData[9]=event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
//			sensData[10]=event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
//			sensData[10]=event.values[0];
//			Log.d(TAG,"FallDetectionService, onSensorChanged, TYPE_LINEAR_ACCELERATION");
//			linAccData[0][alternateLinAcc]=event.values[0];
//			linAccData[1][alternateLinAcc]=event.values[1];
//			linAccData[2][alternateLinAcc]=event.values[2];
//			if(alternateLinAcc==0) {
//				alternateLinAcc=1;
//			} else alternateLinAcc=0;
        } else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
//			ori[0]=event.values[0];
//			ori[1]=event.values[1];
//			ori[2]=event.values[2];
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "FalldetectionService, AccuracyChanged");
    }

    /**
     * @author Luca
     *         Not used
     */
    public class ElaborationThread extends Thread {
        public void run() {
            Log.d(TAG, "FallDetectionService, ElaborationThread");
            boolean running = true;
            iteration = 0;

            while (running) {

                if (!sensorsRegistered) running = false;

                if (iteration == 0) {
                    startTime = System.nanoTime();
                    algo.initAlgorithms();
                }
                iteration++;
                try {
                    sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                float time = (System.nanoTime() - startTime);
                algo.MahonyAHRSUpdate(sensData, time);
                MahonyAHRSUpdateResponse mahonyAHRSUpdateResponse = algo.new MahonyAHRSUpdateResponse();
                float[] angles = mahonyAHRSUpdateResponse.getYpr();
                float[] acc = Arrays.copyOfRange(sensData, 3, 6);
                float[] acc_sep = algo.getAcc_Separate(
                        mahonyAHRSUpdateResponse.getQuat(), acc);
                float[] acc_rot = algo.getEarthFrameVector(mahonyAHRSUpdateResponse.getQuat(), acc_sep);
                algo.print();
            }
        }
    }

    /**
     * Thread for elaboration of accelerometer and magnetometer signals
     *
     * @author Luca
     */
    public class ElaborationThreadRotationAndOrientation extends Thread {

        public void run() {
            Log.d(TAG, "FallDetectionService, ElaborationThreadRotationAndOrientation");
            boolean running = true;
            iteration = 0;
            //WriteDataFile write = new WriteDataFile(); //TODO SCOMMENTARE PER USARLO
            Algorithms algo = new Algorithms();
//			double[] a={1,-2.3695,2.314,-1.0547,0.1874};
//			double[] b={0.0048,0.0193,0.0289,0.0193,0.0048};
            double[] a = {1, -2.5722, 2.8397, -1.5145, 0.3270}; // by matlab: [b,a]=ellip(4,0.1,60,0.2,'low');
            double[] b = {0.0099, 0.0177, 0.0238, 0.0177, 0.0099};
            IirFilterArray iirFilterAcc = new IirFilterArray(a, b, 3);
            IirFilterArray iirFilterMag = new IirFilterArray(a, b, 3);
            IirFilter iirFilterRot = new IirFilter(a, b);
            while (running) {

                if (!sensorsRegistered) running = false;

                if (iteration == 0) {
                    startTime = System.nanoTime();
                    //write.newFile2(); //TODO SCOMMENTARE PER APRIRE IL FILE DA SCRIVERE
                }
                iteration++;
                try {
                    sleep(samplingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (iteration > 1) {
                    float time = (System.nanoTime() - startTime);

                    if (alternateAcc == 0) {
                        for (int i = 0; i < 3; i++) {
                            acc[i] = accData[i][1];
                        }
                    } else {
                        for (int i = 0; i < 3; i++) {
                            acc[i] = accData[i][0];
                        }
                    }

                    if (alternateMag == 0) {
                        for (int i = 0; i < 3; i++) {
                            mag[i] = magData[i][1];
                        }
                    } else {
                        for (int i = 0; i < 3; i++) {
                            mag[i] = magData[i][0];
                        }
                    }

//				//1) GREZZA --> LINEARE --> RUOTATA --> FILTRATA, rotation matrix su accelerazione grezza
//				//calcolo accelerazione lineare
//				float[] acc_=algo.getLinearAcceleration(acc);
//				//calcolo rms da acc lineare
//				float[] rms_=new float[1];
//				rms_[0]=(float) ((Math.sqrt(Math.pow(acc_[0], 2)+Math.pow(acc_[1], 2)+Math.pow(acc_[2], 2)))/9.81);
//				//calcolo rotation Matrix da accelerazione grezza
//				float[] Racc=new float[16];
//				float[] Iacc=new float[16];
//				boolean rotMatrixAcc=SensorManager.getRotationMatrix(Racc, Iacc, acc, mag);
//				//calcolo accelerazione ruotata da accelerazione lineare!?
//				float[] RInvacc=new float[16];
//				Matrix.invertM(RInvacc, 0, Racc, 0);
//				float[] acc_0=new float[4];
//				acc_0[0]=acc_[0]; //prima era su quella lineare
//				acc_0[1]=acc_[1]; 
//				acc_0[2]=acc_[2]; 
//				acc_0[3]=0; 
//				float[] rotacc_=new float[4];
//				
//				Matrix.multiplyMV(rotacc_, 0, RInvacc, 0, acc_0, 0);
//				//float[] rotacc_=algo.getLinearAcceleration(rotacc); //aggiunto insieme al fatto che non lo faccio prima
//				//calcolo variazioni angolari
//				float[] angleChangeacc=new float[3];
//				SensorManager.getAngleChange(angleChangeacc, Racc, prevRacc);
//				prevRacc=Racc;
//				//filtraggio della componente Z dell'accelerazione ruotata
//				double filtZacc_r= iirFilterRot.step(rotacc_[2]); //il filtraggio lo faccio prima o dopo????
//				float[] filtZacc_rV=new float[1];
//				filtZacc_rV[0]= (float) filtZacc_r;
//				//scrivo su file
//				float[] timeA=new float[1];
//				timeA[0]=time;
//				write.writeData(acc,rms_,acc_,rotacc_,mag,angleChangeacc,filtZacc_rV,timeA);

                    //METODO PAPER
                    //compute rms over raw acceleration to find possible fall events.
                    float[] rms_raw = new float[1];
                    rms_raw[0] = (float) ((Math.sqrt(Math.pow(acc[0], 2) + Math.pow(acc[1], 2) + Math.pow(acc[2], 2))) / 9.81);

                    //low pass iir filter of acc and mag, first in double then conversion in float
//                    double[] iir_filt_accD = new double[3];
//                    double[] iir_filt_magD = new double[3];
//                    iir_filt_accD = iirFilterAcc.stepArray(acc);
//                    iir_filt_magD = iirFilterMag.stepArray(mag);
//                    float[] iir_filt_accF = new float[3];
//                    float[] iir_filt_magF = new float[3];
//                    for (int i = 0; i < 3; i++) {
//                        iir_filt_accF[i] = (float) iir_filt_accD[i];
//                        iir_filt_magF[i] = (float) iir_filt_magD[i];
//                    }
//
//                    //calculation of rotation matrix
//                    float[] rot = new float[16];
//                    float[] inc = new float[16];
//                    boolean rotMatrix = SensorManager.getRotationMatrix(rot, inc, iir_filt_accF, iir_filt_magF);
//                    float[] rotInv = new float[16];
//                    Matrix.invertM(rotInv, 0, rot, 0);
//
//                    //calculation of dynamic acceleration (gravity-free)
//                    float[] fir_lin_iir_filt_acc = algo.getLinearAcceleration(iir_filt_accF);
//
//                    //calculation of rms over dynamic filtered acceleration (now here it is not needed)
//                    float[] rms_filt_lin = new float[1];
//                    rms_filt_lin[0] = (float) ((Math.sqrt(Math.pow(fir_lin_iir_filt_acc[0], 2) + Math.pow(fir_lin_iir_filt_acc[1], 2) + Math.pow(fir_lin_iir_filt_acc[2], 2))) / 9.81);
//
//                    //calculation of acceleration referred to earth frame by multiplying with rotation matrix
//                    float[] fir_lin_iir_filt_acc0 = new float[4];
//                    fir_lin_iir_filt_acc0[0] = fir_lin_iir_filt_acc[0];
//                    fir_lin_iir_filt_acc0[1] = fir_lin_iir_filt_acc[1];
//                    fir_lin_iir_filt_acc0[2] = fir_lin_iir_filt_acc[2];
//                    fir_lin_iir_filt_acc0[3] = 0;
//                    float[] rotacc = new float[4];
//                    Matrix.multiplyMV(rotacc, 0, rotInv, 0, fir_lin_iir_filt_acc0, 0);
//
//                    //calculation of angle changes
//                    float[] angleChange = new float[3];
//                    SensorManager.getAngleChange(angleChange, rot, prevRotM);
//                    prevRotM = rot;
//
//                    //time array
//                    float[] timeA = new float[1];
//                    timeA[0] = time;

                    //writing data to .csv file
                    //write.writeData(acc,mag,rms_raw,iir_filt_accF,iir_filt_magF,fir_lin_iir_filt_acc,rotacc,angleChange,rms_filt_lin,timeA);


                    if(!overTh && storingIter==0){
                        storeLastAccAndMag(acc, mag);
                    }

                    if (rms_raw[0] > 2 && !overTh) {
                        Log.d(TAG, "FallDetectionservice: rms over threshold!!!!");
                        overTh = true; //flag che mi serve per sapere che ho superato la soglia e quindi dovr� salvarmi i successivi 60 campioni.

                    }

                    if (overTh && storingIter < postWindow) {
                        storeNextAccAndMag(acc, mag);
                        storingIter++;
                    }

                    if (overTh && storingIter == postWindow) {
                        overTh=false;
                        storingIter++;
                        calcParameters();
                        storingIter=0;
                    }




                    if (iteration == 50) {
//					Intent alertIntent=new Intent();
//					alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//					alertIntent.setClass(FallDetectionService.this, AlertActivity.class);
//					startActivity(alertIntent);
                    }

                    //4) GREZZA --> FILTRATA (filtro anche mag) --> LINEARE --> RUOTATA --> , rotation matrix su accelerazione filtrata
                    //calcolo accelerazione filtrata (tutte e 3 le componenti)
//				double[] accfD=new double[3];
//				float[] accf=new float[3];
//				accfD=iirFilterAcc.stepArray(acc);
//				for(int i=0;i<3;i++){
//					//accfD[i]=iirFilterAcc.stepArray(acc[i]);
//					accf[i]=(float) accfD[i];
//				}
//				//calcolo dati mag filtrati
//				double[] magfD=new double[3];
//				float[] magf=new float[3];
//				magfD=iirFilterMag.stepArray(mag);
//				for(int i=0;i<3;i++){
//					//magfD[i]=iirFilterMag.stepArray(mag[i]);
//					magf[i]=(float) magfD[i];
//				}
//				//calcolo accelerazione lineare
//				float[] accf_=algo.getLinearAcceleration(accf);
//				//calcolo rms da acc lineare
//				float[] rmsf_=new float[1];
//				rmsf_[0]=(float) ((Math.sqrt(Math.pow(accf_[0]/9.8, 2)+Math.pow(accf_[1]/9.8, 2)+Math.pow(accf_[2]/9.8, 2)))/9.81);
//				//calcolo rotation Matrix da accelerazione filtrata
//				float[] Racc=new float[16];
//				float[] Iacc=new float[16];
//				boolean rotMatrixAcc=SensorManager.getRotationMatrix(Racc, Iacc, accf, magf);
//				//calcolo accelerazione ruotata da accelerazione lineare!?
//				float[] RInvacc=new float[16];
//				Matrix.invertM(RInvacc, 0, Racc, 0);
//				float[] acc_0=new float[4];
//				acc_0[0]=accf_[0]; 
//				acc_0[1]=accf_[1]; 
//				acc_0[2]=accf_[2]; 
//				acc_0[3]=0; 
//				float[] rotaccf_=new float[4];
//				Matrix.multiplyMV(rotaccf_, 0, RInvacc, 0, acc_0, 0);
//				//calcolo variazioni angolari
//				float[] angleChangeacc=new float[3];
//				SensorManager.getAngleChange(angleChangeacc, Racc, prevRacc);
//				prevRacc=Racc;
//				float[] timeA=new float[1];
//				timeA[0]=time;
//
//				//scrivo su file
//				write.writeData(acc,accf,accf_,rmsf_,rotaccf_,mag,magf,angleChangeacc,timeA);


                    //5 metodo alberto
                    //algo.MahonyAHRSUpdate(acc,mag,time);
//				MahonyAHRSUpdateResponse mahonyAHRSUpdateResponse = algo.new MahonyAHRSUpdateResponse();
//				float[] angles = mahonyAHRSUpdateResponse.getYpr();
//				float[] acc = Arrays.copyOfRange(sensData, 3, 6);
//				float[] acc_sep = algo.getAcc_Separate(
//						mahonyAHRSUpdateResponse.getQuat(), acc);
//				float[] acc_rot=algo.getEarthFrameVector(mahonyAHRSUpdateResponse.getQuat(),acc_sep);


//				float[] Racc_=new float[16];
//				float[] Iacc_=new float[16];
//				boolean rotMatrixAcc_=SensorManager.getRotationMatrix(Racc_,Iacc_,acc_,mag);
//				
//				
//				
//				
//				linAccData=algo.getLinearAcceleration(acc);
//				
//				rms[0]= (float) ((Math.sqrt(Math.pow(linAccData[0], 2)+Math.pow(linAccData[1], 2)+Math.pow(linAccData[2], 2)))/9.81);
//								
//				boolean rotMatrix=SensorManager.getRotationMatrix(R, I, acc, mag);
//				//Log.d(TAG,"FallDetectionService, ElaborationThreadRotationAndOrientation, acc z: "+Float.toString(acc[2]));
//				
//				
//				
//				
//				float[] RInv=new float[16];
//				Matrix.invertM(RInv, 0, R, 0);
//				float[] linAccDataZeros=new float[4];
//				linAccDataZeros[0]=acc[0]; //quale gli do? quella lineare o quella grezza?
//				linAccDataZeros[1]=acc[1]; 
//				linAccDataZeros[2]=acc[2]; 
//				linAccDataZeros[3]=0; 
//				float[] rotAcc=new float[4];
//				Matrix.multiplyMV(rotAcc, 0, RInv, 0, linAccDataZeros, 0);
////				
//				//SensorManager.getOrientation(R, accRot);
//				
//				//SensorManager.getQuaternionFromVector(Q, accRot);
//				//Log.d(TAG,"FallDetectionService, ElaborationThreadRotationAndOrientation, Q: "+Q.length);
//				//+Float.toString(Q[0])+Float.toString(Q[1])+Float.toString(Q[2])+Float.toString(Q[3]));
//				
//				SensorManager.getAngleChange(angleChange, R, prevR);
//				prevR=R;
////				inclination=SensorManager.getInclination(I);
////				float[] accD=algo.getAcc_Separate(Q, acc);
////				float[] accDR=algo.getEarthFrameVector(Q, accD);
//				
//				filtZAcc= iirFilter.step(rotAcc[2]); //il filtraggio lo faccio prima o dopo????
//				//float filtZAccF=(float) filtZAcc;
//				float[] filtZAccFV=new float[1];
//				filtZAccFV[0]= (float) filtZAcc;
//				//Log.d(TAG,"FallDetectionService, ElaborationThreadRotationAndOrientation, filtZAcc: "+Double.toString(filtZAcc)+"   "+Float.toString(filtZAccF)+"   "+Float.toString(filtZAccFV[0]));
//				
//				storeRotAcc(rotAcc[2]); //quale memorizzo? quella filtrata?
//				if(rms[0]>2) calculateZDistance();
//				//write.writeData(acc,linAccData,rotAcc,mag,orientation,angleChange,Q,accDR,inclination,time);
//				write.writeData(acc,rms,linAccData,rotAcc,mag,angleChange,filtZAccFV);

                }
            }
        }
    }

    void storeRotAcc(float rotAccZ) {
        float[] last2SecondsZAccelerationNew = new float[(int) 2000 / samplingTime];
        last2SecondsZAccelerationNew = shiftLeft(last2SecondsZAcceleration, 1);
        last2SecondsZAccelerationNew[last2SecondsZAccelerationNew.length - 1] = rotAccZ;
        last2SecondsZAcceleration = last2SecondsZAccelerationNew;
    }

    void storeLastAccAndMag(float[] accel, float[] magn) {
//		float[] last60SamplesAccelerationNewX= new float[60];
//		float[] last60SamplesAccelerationNewY= new float[60];
//		float[] last60SamplesAccelerationNewZ= new float[60];
        last80SamplesAccelerationX = shiftLeft(last80SamplesAccelerationX, 1);
        last80SamplesAccelerationY = shiftLeft(last80SamplesAccelerationY, 1);
        last80SamplesAccelerationZ = shiftLeft(last80SamplesAccelerationZ, 1);
//		last60SamplesAccelerationNewX[last60SamplesAccelerationNewX.length-1]=accel[0];
//		last60SamplesAccelerationNewY[last60SamplesAccelerationNewY.length-1]=accel[1];
//		last60SamplesAccelerationNewZ[last60SamplesAccelerationNewZ.length-1]=accel[12];
        last80SamplesAccelerationX[last80SamplesAccelerationX.length - 1] = accel[0];
        last80SamplesAccelerationY[last80SamplesAccelerationY.length - 1] = accel[1];
        last80SamplesAccelerationZ[last80SamplesAccelerationZ.length - 1] = accel[2];

        last80SamplesMagneticFieldX = shiftLeft(last80SamplesMagneticFieldX, 1);
        last80SamplesMagneticFieldY = shiftLeft(last80SamplesMagneticFieldY, 1);
        last80SamplesMagneticFieldZ = shiftLeft(last80SamplesMagneticFieldZ, 1);
        last80SamplesMagneticFieldX[last80SamplesMagneticFieldX.length - 1] = accel[0];
        last80SamplesMagneticFieldY[last80SamplesMagneticFieldY.length - 1] = accel[1];
        last80SamplesMagneticFieldZ[last80SamplesMagneticFieldZ.length - 1] = accel[2];
    }

    static float[] shiftLeft(float[] arr, int shift) {
        float[] tmp = new float[arr.length];
        System.arraycopy(arr, shift, tmp, 0, arr.length - shift);
        System.arraycopy(arr, 0, tmp, arr.length - shift, shift);
        return tmp;
    }

    public void storeNextAccAndMag(float[] acc, float[] mag) {
        next80SamplesAccelerationX[storingIter] = acc[0];
        next80SamplesAccelerationY[storingIter] = acc[1];
        next80SamplesAccelerationZ[storingIter] = acc[2];
        next80SamplesMagneticFieldX[storingIter] = mag[0];
        next80SamplesMagneticFieldY[storingIter] = mag[1];
        next80SamplesMagneticFieldZ[storingIter] = mag[2];
    }


    public void calculateZDistance() {
        //implementare qui la doppia integrazione (sul vettore last2seconds ecc ecc)
        // lo chiamo solo quando l'rms supera una certa soglia
        float[] last2SecondsZSpeed = new float[last2SecondsZAcceleration.length];
        for (int i = 0; i < last2SecondsZAcceleration.length; i++) {

        }


        //mi servir� un vettore dei tempi per�... altrimenti uso un dt fisso di 20 ms... che � samplingTime.

    }

    class ElaborationTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... unused) {

            return (null);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    private void calcParameters() {

        float[] accX = concatArray(last80SamplesAccelerationX, next80SamplesAccelerationX);
        float[] accY = concatArray(last80SamplesAccelerationY, next80SamplesAccelerationY);
        float[] accZ = concatArray(last80SamplesAccelerationZ, next80SamplesAccelerationZ);
        float[] magX = concatArray(last80SamplesMagneticFieldX, next80SamplesMagneticFieldX);
        float[] magY = concatArray(last80SamplesMagneticFieldY, next80SamplesMagneticFieldY);
        float[] magZ = concatArray(last80SamplesMagneticFieldZ, next80SamplesMagneticFieldZ);

        float rms[] = calcRMS(accX,accY,accZ);

        float[] rmsM = maxRMS(rms);
        float rmsMax=rmsM[0]; //TODO use in SVM
        int rmsMaxi=(int) rmsM[1];

        float[] fAccX=filter(accX);
        float[] fAccY=filter(accY);
        float[] fAccZ=filter(accZ);
        float[] fMagX=filter(magX);
        float[] fMagY=filter(magY);
        float[] fMagZ=filter(magZ);

        float[] lfAccX=getLinearAcceleration(fAccX);
        float[] lfAccY=getLinearAcceleration(fAccY);
        float[] lfAccZ=getLinearAcceleration(fAccZ);

        float drms[]=calcRMS(lfAccX,lfAccY,lfAccZ);
        float[] drmsM=maxRMS(drms);
        float drmsMax=drmsM[0]; //TODO use in SVM

        float[][] rotacc= getRotatedAcceleration(fAccX,fAccY,fAccZ,fMagX,fMagY,fMagZ,lfAccX,lfAccY,lfAccZ);
        float[] rlfaccX=rotacc[0];
        float[] rlfaccY=rotacc[1];
        float[] rlfaccZ=rotacc[2];

        float[] angley=rotacc[3];
        float[] anglep=rotacc[4];
        float[] angler=rotacc[5];


        angley=cutArray(angley,rmsMaxi);
        Log.d(TAG,"cutted length: "+angley.length);
        anglep=cutArray(anglep,rmsMaxi);
        angler=cutArray(angler,rmsMaxi);
        accX=cutArray(accX,rmsMaxi);
        accY=cutArray(accY,rmsMaxi);
        accZ=cutArray(accZ,rmsMaxi);
        rms=cutArray(rms,rmsMaxi);
        magX=cutArray(magX,rmsMaxi);
        magY=cutArray(magY,rmsMaxi);
        magZ=cutArray(magZ,rmsMaxi);
        fAccX=cutArray(fAccX,rmsMaxi);
        fAccY=cutArray(fAccY,rmsMaxi);
        fAccZ=cutArray(fAccZ,rmsMaxi);
        fMagX=cutArray(fMagX,rmsMaxi);
        fMagY=cutArray(fMagY,rmsMaxi);
        fMagZ=cutArray(fMagZ,rmsMaxi);
        lfAccX=cutArray(lfAccX,rmsMaxi);
        lfAccY=cutArray(lfAccY,rmsMaxi);
        lfAccZ=cutArray(lfAccZ,rmsMaxi);
        drms=cutArray(drms,rmsMaxi);
        rlfaccX=cutArray(rlfaccX,rmsMaxi);
        rlfaccY=cutArray(rlfaccY,rmsMaxi);
        rlfaccZ=cutArray(rlfaccZ,rmsMaxi);


        float maxA=maxAngle(angley,anglep,angler); //TODO use in SVM

        float maxVarA=getMaxAngleVar(angley,anglep,angler); //TODO use in SVM

        float maxRLFAccZ=maxValue(rlfaccZ)[0]; //TODO use in SVM

        float smaRLFAccz=getTrapZ(rlfaccZ); //TODO use in SVM

        float varRLFAccz=getVariance(rlfaccZ); //TODO use in SVM

        String svm_test="-1 "+"1:"+Float.toString(rmsMax)+" 2:"+Float.toString(drmsMax)+" 3:"+Float.toString(maxA)+" 4:"+
                Float.toString(maxVarA)+" 5:"+Float.toString(maxRLFAccZ)+" 6:"+Float.toString(smaRLFAccz)+" 7:"+Float.toString(varRLFAccz);
//
        Log.d(TAG,"Fall Detection computing --> svm: "+svm_test);
        SVM svm=new SVM(this);
        boolean fall=false;
        try {
            fall=svm.read_test(svm_test);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(fall){
                    Log.d(TAG,"intent to alertactivity");
            		Intent alertIntent=new Intent();
					alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					alertIntent.setClass(FallDetectionService.this, AlertActivity.class);
                    alertIntent.putExtra("svm",svm_test);
					startActivity(alertIntent);
        } else {
            Log.d(TAG,"not a fall");
        }

        WriteDataFile write = new WriteDataFile(); //TODO SCOMMENTARE PER USARLO
        //writing data to .csv file
        write.newFile2(); //TODO SCOMMENTARE PER APRIRE IL FILE DA SCRIVERE

        write.writeCompleteData(accX,accY,accZ,
                magX,magY,magZ,
                rms,
                fAccX,fAccY,fAccZ,
                fMagX,fMagY,fMagZ,
                lfAccX,lfAccY,lfAccZ,
                rlfaccX,rlfaccY,rlfaccZ,
                anglep,angler,angley,
                drms);
                //acc,mag,rms_raw,iir_filt_accF,iir_filt_magF,fir_lin_iir_filt_acc,rotacc,angleChange,rms_filt_lin,timeA);



    }

    /**
     * Concat two arrays of float
     * @param array1
     * @param array2
     * @return concatenated array
     */
    private float[] concatArray(float[] array1, float[] array2) {
        float[] newArray = new float[array1.length + array2.length];
        System.arraycopy(array1, 0, newArray, 0, array2.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    /**
     * Calculate rms array over three arrays of float
     * @param ax
     * @param ay
     * @param az
     * @return rms array
     */
    private float[] calcRMS(float[] ax, float[] ay, float[] az){
        float[] rms=new float[preWindow+postWindow];

        for (int i=0; i<rms.length; i++){
            rms[i]=(float) ((Math.sqrt(Math.pow(ax[i],2)+Math.pow(ay[i],2)+Math.pow(az[i],2)))/9.81);
            //rms_raw[0] = (float) ((Math.sqrt(Math.pow(acc[0], 2) + Math.pow(acc[1], 2) + Math.pow(acc[2], 2))) / 9.81);
        }

        return rms;
    }

    /**
     * Calculate max of array
     * @param rms
     * @return array of float where array[0] is the max value and array[1] is the position
     */
    private float[] maxRMS(float[] rms){
        //Log.d(TAG,"maxRMS, length: "+rms.length);
        float[] maxRMS= new float[2];
        maxRMS[0]=0;
        maxRMS[1]=0;

        for (int i=0;i<rms.length;i++){
            //Log.d(TAG,Float.toString(rms[i]));
            if (rms[i]>maxRMS[0]){

                maxRMS[0]=rms[i];
                maxRMS[1]=i;
            }
        }

        Log.d(TAG,"maxRMS: "+Float.toString(maxRMS[0])+" centered at "+Float.toString(maxRMS[1]));
        return maxRMS;

    }

    private float maxAngle(float[] angley, float[] anglep, float[] angler){
        float[] max = new float[3];
        max[0]=maxValue(angley)[0];
        max[1]=maxValue(anglep)[0];
        max[2]=maxValue(angler)[0];
        float maxA=maxValue(max)[0];
        return maxA;
    }

    /**
     * Calculate max value of array of float
     * @param array
     * @return array of float where array[0] is the max value and array[1] is the position
     */
    private float[] maxValue(float[] array){
        float[] max= new float[2];
        max[0]=0;
        max[1]=1;
        for(int i=0;i<array.length;i++){
            if(array[i]>max[0]){
                max[0]=array[i];
                max[1]=i;
            }
        }
        return max;
    }

    /**
     * Return an array of size 160 (2 seconds) from the center (max of rms)
     * @param array
     * @param center
     * @return cutted array of float
     */
    private float[] cutArray(float[] array, int center){
        Log.d(TAG,"cutArray, center: "+Integer.toString(center));
        return Arrays.copyOfRange(array, center-80, center+80);
    }

    /**
     * iir filter
     * @param sig
     * @return
     */
    private float[] filter(float[] sig){
        float[] fsig=new float[sig.length];

        double[] a = {1, -2.5722, 2.8397, -1.5145, 0.3270}; // by matlab: [b,a]=ellip(4,0.1,60,0.2,'low');
        double[] b = {0.0099, 0.0177, 0.0238, 0.0177, 0.0099};


        IirFilter iirFilter = new IirFilter(a,b);

        for (int i=0; i<fsig.length;i++){
            fsig[i]=(float) iirFilter.step(sig[i]);
            //Log.d(TAG,"filter: "+fsig[i]+" "+sig[i]);
        }

        return fsig;
    }

    /**
     * rotated acceleration
     * @param accx
     * @param accy
     * @param accz
     * @param magx
     * @param magy
     * @param magz
     * @param daccx
     * @param daccy
     * @param daccz
     * @return A MATRIX cointaining rotx roty rotz angley anglep angler arrays of float
     */
    public float[][] getRotatedAcceleration(float[] accx, float[] accy, float[] accz, float[] magx, float[] magy, float[] magz, float[] daccx, float[] daccy, float[] daccz){
        //calculation of rotation matrix
        float[][] ROTACC= new float [6][];
        float[] rotaccx=new float[accx.length];
        float[] rotaccy=new float[accy.length];
        float[] rotaccz=new float[accz.length];
        float[] angleyaw=new float[accx.length];
        float[] anglepitch=new float[accx.length];
        float[] angleroll=new float[accx.length];

        float[] rotPrev= new float[16];

        for (int i=0;i<accx.length;i++){
            float[] rot = new float[16];
            float[] inc = new float[16];
            float[] acc = {accx[i],accy[i],accz[i]};
            float[] mag = {magz[i],magy[i],magz[i]};
            float[] dacc = {daccx[i], daccy[i], daccz[i], 0};
            boolean rotMatrix = SensorManager.getRotationMatrix(rot, inc, acc, mag);
            float[] rotInv = new float[16];
            Matrix.invertM(rotInv, 0, rot, 0);

            float[] rotacc = new float[4];
            Matrix.multiplyMV(rotacc, 0, rotInv, 0, dacc, 0);

            rotaccx[i]=rotacc[0];
            rotaccy[i]=rotacc[1];
            rotaccz[i]=rotacc[2];

            float[] angleChange = new float[3];
            if(i>0){
                SensorManager.getAngleChange(angleChange, rot, rotPrev);
            }

            rotPrev=rot;

            angleyaw[i]=angleChange[0];
            anglepitch[i]=angleChange[1];
            angleroll[i]=angleChange[2];

        }

        ROTACC[0]=rotaccx;
        ROTACC[1]=rotaccy;
        ROTACC[2]=rotaccz;
        ROTACC[3]=angleyaw;
        ROTACC[4]=anglepitch;
        ROTACC[5]=angleroll;

        return ROTACC;

    }

    /**
     * return dynami acceleration (without gravity component)
     * @param acc
     * @return array of float of acceleration
     */
    public float[] getLinearAcceleration(float[] acc){

        final float alpha = (float) 0.8;
        float[] gravity=new float[acc.length];
        float[] linear_acceleration=new float[acc.length];

        //float gravityPrev=0;

        for (int i=0;i<acc.length;i++){

            if(i==0) {
                gravity[i]=(1-alpha)*acc[i];
                linear_acceleration[i]=acc[i]-gravity[i];
            } else {
                gravity[i]=alpha*gravity[i-1]+(1-alpha)*acc[i];
                linear_acceleration[i]=acc[i]-gravity[i];
            }
            //Log.d(TAG,"linear: "+acc[i]+" "+linear_acceleration[i]);

        }

        return linear_acceleration;
    }

    public float getMaxAngleVar(float[] angley, float[] anglep, float[] angler){
        float[] angleVar=new float[3];
        angleVar[0]=getVariance(angley);
        angleVar[1]=getVariance(anglep);
        angleVar[2]=getVariance(angler);
        return maxValue(angleVar)[0];
    }

    public float getVariance(float[] array){

        Statistics stat= new Statistics(array);
        return stat.getVariance();
    }



    public float getTrapZ(float[] array){


        float sum=0;
        float trapz;
        int h=1;

        for (int i=0;i<array.length;i++){
            if(i==0 || i==array.length-1){
                sum+=array[i]/2;
            } else sum += array[i];
        }

        trapz=sum*h;

        return trapz;
    }





	
	
	

	
	

}
