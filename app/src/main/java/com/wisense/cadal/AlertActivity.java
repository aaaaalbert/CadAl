package com.wisense.cadal;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class AlertActivity extends Activity {

    static String TAG="FALL_DETECTION";
	
	CountDownTimer cdown;
	
	TextView timer;

    String svm;

    Button cancel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alert);

		timer=(TextView) findViewById(R.id.timeout);

        Intent intent = getIntent();

        svm=intent.getStringExtra("svm");

        cancel=(Button) findViewById(R.id.cancelbutton);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cdown.cancel();
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
                            Log.d(TAG, svm_old.toString());
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
        });
		
		CountDown();
	}
	
	private void CountDown(){
		
		cdown=new CountDownTimer(20000,1000){
			
			public void onTick(long millisUntilFinished){
				timer.setText(Long.toString(millisUntilFinished / 1000));
			}
			public void onFinish(){
				
				timer.setText("0");
//				telephoneState=tm.getCallState();
//				while(telephoneState==TelephonyManager.CALL_STATE_RINGING){
//					telephoneState=tm.getCallState();
//				}
//				if (!phoneCall.isEmpty()){
//					phoneCallIntent=new Intent(Intent.ACTION_CALL);
//					phoneCallIntent.setData(Uri.parse(phoneCallUri));
//					control=false;
//					startActivityForResult(phoneCallIntent, CALLING);
//					pop_up.cancel();
//				} else {
//					control=false;
//					thresholdControlCounter=0;
//					prenote=false;
//					TextView alarmTV=(TextView)findViewById(R.id.overTh);
//					alarmTV.setText(" ");
					
//				}
			}
		};
		cdown.start();
	}

    public boolean svmNewExists(){
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
        File file = new File(dir, "fall_training_set___new");



        if(file.exists()) return true;
        else return false;
    }

	

}
