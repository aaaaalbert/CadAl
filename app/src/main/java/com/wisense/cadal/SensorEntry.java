package com.wisense.cadal;

public class SensorEntry {
	
	public String sensorType;
	public String sensorVendor;
	public String sensorName;
	public int sensorMinDelay;
	public float sensorMaxRange;
	public float sensorResolution;
	public float sensorPower;
	
	public SensorEntry(){}
	
	public SensorEntry(String type, String vendor, String name, int minDelay, float maxRange, float resolution, float power) {
        super();
        this.sensorType=type;
        this.sensorVendor=vendor;
        this.sensorName=name;
        this.sensorMinDelay=minDelay;
        this.sensorMaxRange=maxRange;
        this.sensorResolution=resolution;
        this.sensorPower=power;
    }
	
	public String getType(){
		return sensorType;
	}
	
	public String getVendor(){
		return sensorVendor;
	}
	
	public String getName(){
		return sensorName;
	}
	
	public int getMinDelay(){
		return sensorMinDelay;
	}
	
	public float getMaxRange(){
		return sensorMaxRange;
	}
	
	public float getResolution(){
		return sensorResolution;
	}
	
	public float getPower(){
		return sensorPower;
	}
	
	public void setType(String type){
		sensorType=type;
	}
	
	public void setVendor(String vendor){
		sensorVendor=vendor;
	}
	
	public void setName(String name){
		sensorName=name;
	}
	
	public void setMinDelay(int minDelay){
		sensorMinDelay=minDelay;
	}
	
	public void setMaxRange(float maxRange){
		sensorMaxRange=maxRange;
	}
	
	public void setResolution(float resolution){
		sensorResolution=resolution;
	}
	
	public void setPower(float power){
		sensorPower=power;
	}
	
	
	

}
