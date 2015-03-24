package com.wisense.cadal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * This Activity is launched when a fall is detected; allows the user to cancel the fall within a certain preset time
 */

public class AlertActivity extends Activity  {
    
    /*
    Debug
     */
    static final boolean DEBUG=true;
    static String TAG="FALL_DETECTION";
	
	/*
	Countdown Timer
	 */
    CountDownTimer cdown;
	TextView timer;
    public int countdown;

    /*
    View elements
     */
    TextView titleTV;
    Button cancel;

    /*
    The svm result about the fall
     */
    String svm;

    /*
    Database Class and fall element for the database
     */
    SQLite sql;
    FallEntry fall;

    /*
    Preferences
     */
    public String name;
    public boolean train;
    public boolean trainfalse;
    public boolean traintrue;
    public boolean notification;
    public String relnr1;
    public String relnr2;
    public String msg;

    /*
    Location variables
     */
    protected double latitude,longitude;
    public String locality;

    /*
    Mediaplayer object for alert sound
     */
    MediaPlayer mMediaPlayer;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alert);

        loadPreferences();
        
        IntentFilter myFilter = new IntentFilter("LOC_REQUEST");
        registerReceiver(mReceiver, myFilter);

        sendToLocationService();

        alarmSound();

        sql=new SQLite(AlertActivity.this);

		timer=(TextView) findViewById(R.id.timeout);
        titleTV=(TextView)findViewById(R.id.titleAlertTV);

        Intent intent = getIntent();

        svm=intent.getStringExtra("svm");
        fall=new FallEntry();
        fall=(FallEntry) intent.getSerializableExtra("fallentry");
        if(DEBUG) Log.d(TAG,"Alert data: "+fall.getDate());

        //Setting the cancel button onclicklistener
        cancel=(Button) findViewById(R.id.cancelbutton);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if onclick the the fall is canceled, the timer stopped, the player stopped, and the fall added to db
                cdown.cancel();
                mMediaPlayer.stop();

                titleTV.setText("Fall Canceled");
                titleTV.setTextColor(Color.GREEN);
                if(trainfalse){
                    //If false falls training is activated this fall is used for update the training model
                    fall.setTrain(1);
                    training();
                    Toast.makeText(AlertActivity.this,"Training Updated",Toast.LENGTH_LONG).show();
                }
                fall.setConfirmed(0);
                sql.addFall(fall);
                stopThread.run();
            }
        });
		
		CountDown();
	}

    @Override
    protected void onPause() {
        super.onPause();
        mMediaPlayer.stop();

    }

    /**
     * Void that describes the countdown. When Countdown expires alert sms is sended
     * (if there are saved preset telephone numbers), and the fall is added to db
     * If true falls training is activated the fall is used to update the SVM model
     */
    private void CountDown(){
		
		cdown=new CountDownTimer(countdown*1000,1000){
			
			public void onTick(long millisUntilFinished){
				timer.setText(Long.toString(millisUntilFinished / 1000));
			}
			public void onFinish(){
				
				timer.setText("0");

                cancel.setEnabled(false);

                titleTV.setText("Fall Confirmed");
                titleTV.setTextColor(Color.MAGENTA);

                if(notification){

                    boolean notified=false;
                    Toast.makeText(AlertActivity.this,"Sending Message to relatives!",Toast.LENGTH_LONG).show();

                    String message="Alert!!! "+name+" is falled down!!!";
                    if(locality!=null) message=message+" I'm near "+locality+" long:"+Double.toString(longitude)+
                            " lat:"+Double.toString(latitude);
                    message=message+" Msg: "+msg;
                    if(DEBUG) Log.d(TAG,"Message: "+message);
                    if(DEBUG) Log.d(TAG,"Numbers: "+relnr1+" "+relnr2);
                    if(!relnr1.equals("")){
                        try {
                            sendSMS(relnr1, message);
                            notified=true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(AlertActivity.this,"Unable to send SMS to "+relnr1+"!!!",Toast.LENGTH_LONG).show();
                        }
                    }
                    if(!relnr2.equals("")){
                        try {
                            sendSMS(relnr2,message);
                            notified=true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(AlertActivity.this,"Unable to send SMS to "+relnr2+"!!!",Toast.LENGTH_LONG).show();

                        }
                    }
                    titleTV.setText("Fall Confirmed and Notified");
                    titleTV.setTextColor(Color.MAGENTA);

                    if(notified)fall.setNotified(1);
                }



                if(traintrue){

                    fall.setTrain(1);
                    training();
                    Toast.makeText(AlertActivity.this,"Training Updated",Toast.LENGTH_LONG).show();

                }

                fall.setConfirmed(1);

                sql.addFall(fall);

			}
		};
		cdown.start();
	}

    /**
     * Check if already exists a custom file for the svm training model
     * @return
     */
    public boolean svmNewExists(){
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
        File file = new File(dir, "fall_training_set___new");



        if(file.exists()) return true;
        else return false;
    }

    /**
     * Update the svm training model
     */
    public void training(){
        StringBuffer svm_old = new StringBuffer("");
        InputStream inputStream = null;
        InputStreamReader isr;
        BufferedReader buffreader;
        try {

            if(!svmNewExists()) {
                inputStream = getResources().openRawResource(R.raw.fall_training_set___);
                isr = new InputStreamReader(inputStream);
                buffreader = new BufferedReader(isr);
            } else {
                File root = android.os.Environment.getExternalStorageDirectory();
                File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
                File file = new File(dir, "fall_training_set___new");
                inputStream=new FileInputStream(file);
                isr=new InputStreamReader(inputStream);
                buffreader=new BufferedReader(isr);
            }
            String readString = buffreader.readLine();
            while (readString != null) {
                svm_old.append(readString+"\n");
                readString = buffreader.readLine();
            }

            isr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (svm_old != null) {

            try {
                PrintWriter pw;
                FileOutputStream fileOut;

                if(svmNewExists()){
                    File root = android.os.Environment.getExternalStorageDirectory();
                    File dir = new File(root.getAbsolutePath() + "/CadAlFiles/");
                    dir.mkdirs();
                    File file = new File(dir, "fall_training_set___new");
                    fileOut = new FileOutputStream(file, true);
                    pw = new PrintWriter(fileOut);
                    pw.append(svm+"\n");
                }
                else {
                    File root = android.os.Environment.getExternalStorageDirectory();
                    File dir = new File(root.getAbsolutePath() + "/CadAlFiles/");
                    dir.mkdirs();
                    File file = new File(dir, "fall_training_set___new");

                    fileOut = new FileOutputStream(file, true);
                    pw = new PrintWriter(fileOut);
//                            InputStream in = getResources().openRawResource(R.raw.fall_training_set___);
//                            byte[] buff = new byte[1024];
//                            int read = 0;
//
//                            try {
//                                while ((read = in.read(buff)) > 0) {
//                                    fileOut.write(buff, 0, read);
//                                }
//                            } finally {
//                                in.close();
//
//
//                            }
                    if(DEBUG) Log.d(TAG, svm_old.toString());
                    pw.write(svm_old.toString());
                    pw.flush();
                    pw.append(svm+"\n");
                }
//                        pw.append(svm_old);
//                        pw.append("/n" + svm + "/n");

                pw.close();
                fileOut.close();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            SVM svm = new SVM(AlertActivity.this);
            try {
                boolean update_svm = true;
                svm.train(update_svm);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load the user preferences
     */
    private void loadPreferences(){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        countdown=Integer.parseInt(mySharedPreferences.getString("countdown","20"));
        train=mySharedPreferences.getBoolean("training", false);
        traintrue=mySharedPreferences.getBoolean("training_true",false);
        trainfalse=mySharedPreferences.getBoolean("training_false",false);
        notification=mySharedPreferences.getBoolean("notification",false);
        name=mySharedPreferences.getString("myname","");
        relnr1=mySharedPreferences.getString("relative_number1","0");
        relnr2=mySharedPreferences.getString("relative_number2","0");
        msg=mySharedPreferences.getString("custom_msg","Fall Detected!");


        if(DEBUG) Log.d(TAG,"preferences: "+countdown+" "+name+" "+msg);
    }

    /**
     * Play the alarm sound
     */
    public void alarmSound(){
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();

        Vibrator vibe = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE) ;
        vibe.vibrate(150);
    }

    private void sendSMS(String phoneNumber, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    /**
     * Thread used to automatic close the activity after cancelbutton click
     */
    Thread stopThread = new Thread(){
        @Override
        public void run() {
            try {
                Thread.sleep(2000); // As I am using LENGTH_LONG in Toast
                AlertActivity.this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Sends a location request to the location service
     */
    private void sendToLocationService(){
        String VOID=" sendToSHMService ";
        if(DEBUG) Log.d(TAG,VOID);
        Intent sendIntent=new Intent();
        sendIntent.setAction("LOC_REQUEST");
        sendIntent.putExtra("LOC_REQ","request");
        sendBroadcast(sendIntent);
    }

    /**
     * Receive the requested location by the location service
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override

        public void onReceive(Context context, Intent intent) {
            String VOID = " mReceiver ";
            if(DEBUG) Log.d(TAG, VOID);
            if (intent.getStringExtra("LOCALITY") != null) {
                locality=intent.getStringExtra("LOCALITY");
            }
            if (intent.getDoubleExtra("LONGITUDE",0)!=0){
                longitude=intent.getDoubleExtra("LONGITUDE",0);

            }
            if (intent.getDoubleExtra("LATITUDE",0)!=0){
                latitude=intent.getDoubleExtra("LATITUDE",0);

            }

        }
    };
	

}
