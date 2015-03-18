package com.wisense.cadal;

import java.util.ArrayList;

import android.content.Context;

public class Algorithms {
	
	/**
	 * Variables
	 */
	float gx,gy,gz,ax,ay,az,mx,my,mz,rms;
	float lastnow,now,sampleFreq;
	float tempoIntAlt;
	float q0=1.0f,q1=0.0f,q2=0.0f,q3=0.0f,integralFBx = 0,  integralFBy = 0
			, integralFBz = 0,twoKpDef=(2* 0.5f),twoKiDef=(2* 0.1f),twoKp=twoKpDef,twoKi=twoKiDef;
	float[] quat=new float[4];
	float[] acc_static=new float[3];
	float[] acc_dynamic=new float[3];
	float[] temp=new float[4];
	float[] EarthFrameVect=new float[4];
	float[] ypr=new float[3];
	float time;
	
	float[] gravityPrev={0,0,0};
	float[] gravityPrevRot={0,0,0};
	
	
	/**
	 * Classes
	 */
	WriteDataFile writeDataFile;
	
	public void initAlgorithms(){
		writeDataFile=new WriteDataFile();
		writeDataFile.newFile();
	}
	
	/**
	 * Returns inverse sqrt
	 * @param x
	 * @return x
	 */
	
	public float invSqrt(float x){
		float xhalf=0.5f*x;
		int i=Float.floatToIntBits(x);
		i=0x5f3759d5-(i>>1);
		x=Float.intBitsToFloat(i);
		x=x*(1.5f-xhalf*x*x);
		return x;
	}
	
	
	/**
	 * Returns quaternions
	 * @param dat
	 * @return angles
	 */

	public  void MahonyAHRSUpdate(float[] dat,float tim){
		
		time=tim;
		float recipNorm;
	    float q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;  
		float hx, hy, bx, bz;
		float halfvx, halfvy, halfvz, halfwx, halfwy, halfwz;
		float halfex=0, halfey=0, halfez=0;
		float qa, qb, qc;
		
		gx=dat[0];
		gy=dat[1];
		gz=dat[2];
		ax=(float) (dat[3]/9.81);
		ay=(float) (dat[4]/9.81);
		az=(float) (dat[5]/9.81);
		mx=dat[6];
		my=dat[7];
		mz=dat[8];
		
		rms= (float) ((Math.sqrt(Math.pow(dat[3], 2)+Math.pow(dat[4], 2)+Math.pow(dat[5], 2)))/9.81);
		
		now = System.nanoTime();
		sampleFreq = (float) (1.0 / ((now - lastnow) / 1000000000.0));
		tempoIntAlt=((now-lastnow)/1000000000);
		lastnow=now;
		
	    // Auxiliary variables to avoid repeated arithmetic
        q0q0 = q0 * q0;
        q0q1 = q0 * q1;
        q0q2 = q0 * q2;
        q0q3 = q0 * q3;
        q1q1 = q1 * q1;
        q1q2 = q1 * q2;
        q1q3 = q1 * q3;
        q2q2 = q2 * q2;
        q2q3 = q2 * q3;
        q3q3 = q3 * q3; 
		
        if((mx != 0) && (my != 0) && (mz != 0)) {
      		// Normalise magnetometer measurement
      		recipNorm = invSqrt(mx * mx + my * my + mz * mz);
      		mx *= recipNorm;
      		my *= recipNorm;
      		mz *= recipNorm; 
      		
      		   //PROVARE AD UTILIZZARE QUESTA RELAZIONE PER CALCOLARE I VALORI DI CAMPO MAGNETICO RIFERITI AL FRAME EARTH
      	    // Reference direction of Earth's magnetic field (direzione del campo magnetico terrestre in earth frame, con m che pu� essere soggetto a distorsioni)
      	    hx = 2.0f * (mx * (0.5f - q2q2 - q3q3) + my * (q1q2 - q0q3) + mz * (q1q3 + q0q2));
      	    hy = 2.0f * (mx * (q1q2 + q0q3) + my * (0.5f - q1q1 - q3q3) + mz * (q2q3 - q0q1));
      	    bx = (float) Math.sqrt(hx * hx + hy * hy);
      	    bz = 2.0f * (mx * (q1q3 - q0q2) + my * (q2q3 + q0q1) + mz * (0.5f - q1q1 - q2q2));
      	    
      	    // Estimated direction of magnetic field (vettore w) in sensor frame
      	    halfwx = bx * (0.5f - q2q2 - q3q3) + bz * (q1q3 - q0q2);
      	    halfwy = bx * (q1q2 - q0q3) + bz * (q0q1 + q2q3);
      	    halfwz = bx * (q0q2 + q1q3) + bz * (0.5f - q1q1 - q2q2);
      	    
      	    // Error is sum of cross product between estimated direction and measured direction of field vectors
      	    halfex = (my * halfwz - mz * halfwy);
      	    halfey = (mz * halfwx - mx * halfwz);
      	    halfez = (mx * halfwy - my * halfwx);
          }
          // Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
          if((ax != 0) && (ay != 0) && (az != 0)) {
          	   // Normalise accelerometer measurement
              recipNorm = invSqrt(ax * ax + ay * ay + az * az);
              ax *= recipNorm;
              ay *= recipNorm;
              az *= recipNorm;
              
              // Estimated direction of gravity (vettore v) in sensor frame (ipotesi di assenza di accelerazioni lineari)
              halfvx = q1q3 - q0q2;
              halfvy = q0q1 + q2q3;
              halfvz = q0q0 - 0.5f + q3q3;
              
              // Error is sum of cross product between estimated direction and measured direction of field vectors
              halfex += (ay * halfvz - az * halfvy);
              halfey += (az * halfvx - ax * halfvz);
              halfez += (ax * halfvy - ay * halfvx);
          }
       // Apply feedback only when valid data has been gathered from the accelerometer or magnetometer
          if(halfex != 0.0f && halfey != 0.0f && halfez != 0.0f) {
          	if(twoKi > 0.0f) {
          	     integralFBx += twoKi * halfex * (1 / sampleFreq);  // integral error scaled by Ki
          	      integralFBy += twoKi * halfey * (1 / sampleFreq);
          	      integralFBz += twoKi * halfez * (1 / sampleFreq);
          	      gx += integralFBx;  // apply integral feedback
          	      gy += integralFBy;
          	      gz += integralFBz;
          	}else {
          	      integralFBx = 0; // prevent integral windup
          	      integralFBy = 0;
          	      integralFBz = 0;
          	}
          	   // Apply proportional feedback
              gx += twoKp * halfex;
              gy += twoKp * halfey;
              gz += twoKp * halfez;
          }
          // Integrate rate of change of quaternion
          gx *= (0.5f * (1.0f / sampleFreq));   // pre-multiply common factors
          gy *= (0.5f * (1.0f / sampleFreq));
          gz *= (0.5f * (1.0f / sampleFreq));
          qa = q0;
          qb = q1;
          qc = q2;
          //stima orientazione fornita dall'algoritmo di Madgwick
          q0 += (-qb * gx - qc * gy - q3 * gz);
          q1 += (qa * gx + qc * gz - q3 * gy);
          q2 += (qa * gy - qb * gz + q3 * gx);
          q3 += (qa * gz + qb * gy - qc * gx);
          
          // Normalise quaternion
          recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
          q0 *= recipNorm;
          q1 *= recipNorm;
          q2 *= recipNorm;
          q3 *= recipNorm;
        
  		gx = 2 * (q1*q3 - q0*q2);
  		gy = 2 * (q0*q1 + q2*q3);
  		gz = q0*q0 - q1*q1 - q2*q2 + q3*q3;
  		
  		ypr[0] = (float) Math.atan2(2 * q1 * q2 - 2 * q0 * q3, 2 * q0*q0 + 2 * q1 * q1 - 1);
  		ypr[1] = (float) Math.atan(gx / Math.sqrt(gy*gy + gz*gz));
  		ypr[2] = (float) Math.atan(gy / Math.sqrt(gx*gx + gz*gz));
  		
  		ypr[0]*=(180/3.14);
  		ypr[1]*=(180/3.14);
  		ypr[2]*=(180/3.14);
  		
  		quat[0]=q0;
  		quat[1]=q1;
  		quat[2]=q2;
  		quat[3]=q3;
  		
  		MahonyAHRSUpdateResponse mahonyAHRSUpdateResponse = new MahonyAHRSUpdateResponse();
  		mahonyAHRSUpdateResponse.setQuaternions(quat);
  		mahonyAHRSUpdateResponse.setYPRAngles(ypr);
  		
		
	}
	
	
	/**
	 * Returns dynami acceleration separated from static acceleration
	 * @param q
	 * @param acc_tot
	 * @return dynamic acc
	 */
	
	public float[] getAcc_Separate(float[] q,float[] acc_tot){

		acc_tot[0]*=(1/9.81);
		acc_tot[1]*=(1/9.81);
		acc_tot[2]*=(1/9.81);

		acc_static[0] = 2 * (q[1] * q[3] - q[0] * q[2]);
		acc_static[1] = 2 * (q[0] * q[1] + q[2] * q[3]);
		acc_static[2] = q[0] * q[0] - q[1] * q[1] - q[2] * q[2] + q[3] * q[3];
		
		acc_dynamic[0] = acc_tot[0] - acc_static[0];
		acc_dynamic[1] = acc_tot[1] - acc_static[1];
		acc_dynamic[2] = acc_tot[2] - acc_static[2];
		
		return acc_dynamic;
	}
	
	public void print (){
		writeDataFile.write(gx,gy,gz,ax,ay,az,mx,my,mz,rms,quat[0],quat[1],quat[2],quat[3],acc_static[0],acc_static[1],acc_static[2],acc_dynamic[0],acc_dynamic[1],acc_dynamic[2],EarthFrameVect[0],EarthFrameVect[1],EarthFrameVect[2],EarthFrameVect[3],ypr[0],ypr[1],ypr[2],time);
	}
	
	
	/**
	 * Returns accelerations referred to earth frame
	 * @param q
	 * @param vect
	 * @return earth frame vector
	 */
	
	public float[] getEarthFrameVector(float[] q,float[] vect){

		
		temp[0] = q[0]*0 - (-q[1])*vect[0] - (-q[2])*vect[1] - (-q[3])*vect[2];
		temp[1] = q[0]*vect[0] + (-q[1])*0 +  (-q[2])*vect[2] - (-q[3])*vect[1];
		temp[2] = q[0]*vect[1] - (-q[1])*vect[2] + (-q[2])*0       + (-q[3])*vect[0];
		temp[3] = q[0]*vect[2] + (-q[1])*vect[1] - (-q[2])*vect[0] + (-q[3])*0;
		
		EarthFrameVect[0] = temp[0]*q[0] - temp[1]*q[1] - temp[2]*q[2] - temp[3]*q[3];
		EarthFrameVect[1] = temp[0]*q[1] + temp[1]*q[0] + temp[2]*q[3] - temp[3]*q[2];
		EarthFrameVect[2] = temp[0]*q[2] - temp[1]*q[3] + temp[2]*q[0] + temp[3]*q[1];
		EarthFrameVect[3] = temp[0]*q[3] + temp[1]*q[2] - temp[2]*q[1] + temp[3]*q[0];
		
		return EarthFrameVect;
	}
	
	
	/**
	 * class for the return of the double values output of MahonyAHRSUpdate method
	 * @author Luca
	 *
	 */
	
	public class MahonyAHRSUpdateResponse {

		 float[] quaternions = new float[4];
		 float[] yprAngles= new float[3];
				 
		 public void setQuaternions(float[] quat) {
		   quaternions=quat;
		 }

		 public void setYPRAngles(float[] ypr) {
		   yprAngles=ypr;;
		 }

		 public float[] getQuat() {
		   return quaternions;
		 }

		 public float[] getYpr() {
		   return yprAngles;
		 }

		}
	
	public float[] getLinearAcceleration(float[] acc){
		
		final float alpha = (float) 0.8;
		float[] gravity=new float[3];
		float[] linear_acceleration=new float[3];
		
          gravity[0] = alpha * gravityPrev[0] + (1 - alpha) * acc[0];
          gravity[1] = alpha * gravityPrev[1] + (1 - alpha) * acc[1];
          gravity[2] = alpha * gravityPrev[2] + (1 - alpha) * acc[2];

          gravityPrev=gravity;
          
          linear_acceleration[0] = acc[0] - gravity[0];
          linear_acceleration[1] = acc[1] - gravity[1];
          linear_acceleration[2] = acc[2] - gravity[2];
          
          return linear_acceleration;
	}
	
public float[] getLinearRotAcceleration(float[] acc){
		
		final float alpha = (float) 0.8;
		float[] gravity=new float[3];
		float[] linear_acceleration=new float[3];
		
          gravity[0] = alpha * gravityPrevRot[0] + (1 - alpha) * acc[0];
          gravity[1] = alpha * gravityPrevRot[1] + (1 - alpha) * acc[1];
          gravity[2] = alpha * gravityPrevRot[2] + (1 - alpha) * acc[2];

          gravityPrevRot=gravity;
          
          linear_acceleration[0] = acc[0] - gravity[0];
          linear_acceleration[1] = acc[1] - gravity[1];
          linear_acceleration[2] = acc[2] - gravity[2];
          
          return linear_acceleration;
	}
	
}
