package com.wisense.cadal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;



/**
 * Class for write sensors and elaborations data on a file
 * @author Luca
 *
 */

public class WriteDataFile {

    final static boolean DEBUG=true;
	String TAG="FALL_DETECTION";
	
	/**
	 * Variables
	 */
	String fileName="";
	
	/**
	 * 
	 * @param dati
	 * @param acc_D
	 * @param acc_R
	 * @param altB
	 * @param altC
	 * @param q
	 * @param ang
	 * @param te
	 * @param an
	 * @param cad
	 * @param rms
	 */

    /**
     * Not used now
     * @param gx
     * @param gy
     * @param gz
     * @param ax
     * @param ay
     * @param az
     * @param mx
     * @param my
     * @param mz
     * @param rms
     * @param quat0
     * @param quat1
     * @param quat2
     * @param quat3
     * @param as0
     * @param as1
     * @param as2
     * @param ad0
     * @param ad1
     * @param ad2
     * @param efv0
     * @param efv1
     * @param efv2
     * @param efv3
     * @param ypr0
     * @param ypr1
     * @param ypr2
     * @param time
     */
	public void write(float gx, float gy,float gz,float ax,float ay,float az,float mx,float my,float mz,float rms,float quat0,float quat1,float quat2,float quat3, float as0, float as1, float as2, float ad0, float ad1, float ad2, float efv0, float efv1, float efv2, float efv3, float ypr0, float ypr1, float ypr2,float time){
		
		String data=Float.toString(gx)+";"+Float.toString(gy)+Float.toString(gz)+";"+Float.toString(ax)+";"+Float.toString(ay)+";"+Float.toString(az)+";"+Float.toString(mx)+";"+Float.toString(my)+";"+Float.toString(mz)+";"+Float.toString(rms)+";"+Float.toString(quat0)+";"+Float.toString(quat1)+";"+Float.toString(quat2)+";"+Float.toString(quat3)+";"+Float.toString(as0)+";"+Float.toString(as1)+";"+Float.toString(as2)+";"+Float.toString(ad0)+";"+Float.toString(ad1)+";"+Float.toString(ad2)+";"+Float.toString(efv0)+";"+Float.toString(efv1)+";"+Float.toString(efv2)+";"+Float.toString(efv3)+";"+Float.toString(ypr0)+";"+Float.toString(ypr1)+";"+Float.toString(ypr2)+";"+Float.toString(time)+"\n";
		File root = android.os.Environment.getExternalStorageDirectory(); 
		File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
		dir.mkdirs();
	    File file = new File(dir, fileName);  
	    FileWriter fw;
	    
	    try {
	    	FileOutputStream fileOut = new FileOutputStream(file,true);
	    	PrintWriter pw = new PrintWriter(fileOut);

		
			pw.append(data);
		
			pw.close();	
			fileOut.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    /**
     * writeData, accepts multiple args o f type float[]. Designed for write data at each acquisition by sensors
     * @param args
     */
	public void writeData(float[]... args){
		String data="";
		//if(DEBUG) Log.d(TAG,"WriteDataFile, writeData, args.length: "+Integer.toString(args.length));
		for (int i=0; i<args.length;i++){
			float[] dato=args[i];
			//if(DEBUG) Log.d(TAG,"WriteDataFile, writeData, dato.length: "+Integer.toString(dato.length));
			for (int j = 0; j < dato.length; j++) {
				data = data + Float.toString(dato[j]) + ";";
			}
		}
		data=data+"\n";
		data=data.replace(".", ",");
		
		File root = android.os.Environment.getExternalStorageDirectory(); 
		File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
		dir.mkdirs();
	    File file = new File(dir, fileName);  
	    FileWriter fw;
	    
	    try {
	    	FileOutputStream fileOut = new FileOutputStream(file,true);
	    	PrintWriter pw = new PrintWriter(fileOut);

		
			pw.append(data);
		
			pw.close();	
			fileOut.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

    /**
     * Write a complete file. Accepts an array of array of float. Do recursively (for n rows) the each things of writeData2
     * @param args
     */
    public void writeCompleteData(float[]... args){

        float[][] matrix = transposeMatrix(args);

        if(DEBUG) Log.d(TAG,"matrix.length: "+matrix.length+"  matrix[0].length: "+matrix[0].length+"  args.length: "+args.length+"  args[0].length: "+args[0].length);
        float[] row=new float[args.length];
        for (int i=0;i<matrix.length;i++){
            row=matrix[i];
            writeData(row);
        }

//        for (int i=0;i<args[i].length;i++){
//            if(DEBUG) Log.d(TAG,"writeCompleteData: nr args: "+args.length+" length args: "+args[i].length);
//            float[] array=new float[args.length];
//            for (int j=0;j<args.length;j++){
//                if(DEBUG) Log.d(TAG,"writeCompleteData i: "+i+" j: "+j);
//
//                array[j]=args[i][j];
//                writeData(array);
//            }
//
//        }
    }

    /**
     * Transpose a matrix, useful for write a new file row-to-row
     * @param m
     * @return
     */
    public static float[][] transposeMatrix(float [][] m){
        float[][] temp = new float[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }

    /**
     * not used
     * @param acc
     * @param linAcc
     * @param rotAcc
     * @param mag
     * @param orientation
     * @param angle
     * @param quat
     * @param accDynR
     * @param inclination
     * @param time
     */
	public void writeData2(float[] acc, 
			float[] linAcc, 
			float[] rotAcc,
			float[] mag, 
			float[] orientation,
			float[] angle,
			float[] quat,
			float[] accDynR, 
			float inclination,
			float time){
		
		String data=Float.toString(acc[0])+";"+Float.toString(acc[1])+";"+Float.toString(acc[2])+";"+
				Float.toString(linAcc[0])+";"+Float.toString(linAcc[1])+";"+Float.toString(linAcc[2])+";"+
				Float.toString(rotAcc[0])+";"+Float.toString(rotAcc[1])+";"+Float.toString(rotAcc[2])+";"+Float.toString(rotAcc[3])+";"+
				Float.toString(mag[0])+";"+Float.toString(mag[1])+";"+Float.toString(mag[2])+";"+
				Float.toString(orientation[0])+";"+Float.toString(orientation[1])+";"+Float.toString(orientation[2])+";"+
				Float.toString(angle[0])+";"+Float.toString(angle[1])+";"+Float.toString(angle[2])+";"+
				Float.toString(quat[0])+";"+Float.toString(quat[1])+";"+Float.toString(quat[2])+";"+Float.toString(quat[3])+";"+
				Float.toString(accDynR[0])+";"+Float.toString(accDynR[1])+";"+Float.toString(accDynR[2])+";"+Float.toString(accDynR[3])+";"+
				Float.toString(inclination)+";"+
				Float.toString(time)+"\n";
		data=data.replace(".", ",");
		
		File root = android.os.Environment.getExternalStorageDirectory(); 
		File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
		dir.mkdirs();
	    File file = new File(dir, fileName);  
	    FileWriter fw;
	    
	    try {
	    	FileOutputStream fileOut = new FileOutputStream(file,true);
	    	PrintWriter pw = new PrintWriter(fileOut);

		
			pw.append(data);
		
			pw.close();	
			fileOut.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    /**
     * not used
     */
public void newFile(){
	if(DEBUG) Log.d(TAG,"WriteDataFile, newFile");
	
	fileName="SensorsData_"+getDate()+".csv";
	
	//check if exists
	File root = android.os.Environment.getExternalStorageDirectory(); 
	File dir = new File (root.getAbsolutePath()+"/CadAlFiles/");
	File file = new File(dir, fileName); 
	if(file.exists())  fileName="SensorsData_"+getDate()+"_2.csv";
}

    /**
     * create a new file from the actual date
     */
public void newFile2(){
	if(DEBUG) Log.d(TAG,"WriteDataFile, newFile2");
	fileName="SensorsData2_"+getDate()+".csv";
}

    /**
     * get actual date in yyyyMMdd_HHmm format
     * @return date String
     */
public String getDate(){
    	    	
    	Date date=new Date();
    	String format="yyyyMMdd_HHmm";
    	SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.ITALY);
    	
    	String now=sdf.format(date);
    	return now;
    }

}
