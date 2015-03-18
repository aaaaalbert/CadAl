package com.wisense.cadal;

/**
 * Created by lucapernini on 17/03/15.
 */
public class FallEntry {

    private static final String TAG="FALL DETECTION";

    private String date;
    private int confirmed;
    private int train;
    private int notified;
    private float maxrms;
    private float maxfrms;
    private float maxangle;
    private float varangle;
    private float maxaz;
    private float sma;
    private float varaz;

    public FallEntry() {
    }

    public FallEntry(String date, int confirmed, int train,
    int notified, float maxrms, float maxfrms, float maxangle,
    float varangle, float maxaz, float sma, float varaz) {
        this.date=date;
        this.confirmed=confirmed;
        this.train=train;
        this.notified=notified;
        this.maxrms=maxrms;
        this.maxfrms=maxfrms;
        this.maxangle=maxangle;
        this.varangle=varangle;
        this.maxaz=maxaz;
        this.sma=sma;
        this.varaz=varaz;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setConfirmed(int confirmed) {
        this.confirmed = confirmed;
    }

    public void setTrain(int train) {
        this.train = train;
    }

    public void setNotified(int notified) {
        this.notified = notified;
    }

    public void setMaxrms(float maxrms) {
        this.maxrms = maxrms;
    }

    public void setMaxfrms(float maxfrms) {
        this.maxfrms = maxfrms;
    }

    public void setMaxangle(float maxangle) {
        this.maxangle = maxangle;
    }

    public void setVarangle(float varangle) {
        this.varangle = varangle;
    }

    public void setMaxaz(float maxaz) {
        this.maxaz = maxaz;
    }

    public void setSma(float sma) {
        this.sma = sma;
    }

    public void setVaraz(float varaz) {
        this.varaz = varaz;
    }

    public String getDate() {
        return date;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public int getTrain() {
        return train;
    }

    public int getNotified() {
        return notified;
    }

    public float getMaxrms() {
        return maxrms;
    }

    public float getMaxfrms() {
        return maxfrms;
    }

    public float getMaxangle() {
        return maxangle;
    }

    public float getVarangle() {
        return varangle;
    }

    public float getMaxaz() {
        return maxaz;
    }

    public float getSma() {
        return sma;
    }

    public float getVaraz() {
        return varaz;
    }
}
