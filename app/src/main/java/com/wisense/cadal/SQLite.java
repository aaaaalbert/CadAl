package com.wisense.cadal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucapernini on 17/03/15.
 */
public class SQLite extends SQLiteOpenHelper {


    private static final String TAG="FALL DETECTION";

    private static final int DATABASE_VERSION=1;
    private static final String DATABASE_NAME="CADALDB";

    private static final String TABLE_CADAL="cadal";

    private static final String KEY_ID="id";
    private static final String KEY_DATE="date";
    private static final String KEY_CONFIRMED="confirmed";
    private static final String KEY_USED_FOR_TRAIN="train";
    private static final String KEY_NOTIFIED="notified";
    private static final String KEY_MAXRMS="maxrms";
    private static final String KEY_MAXFRMS="maxfrms";
    private static final String KEY_MAXANGLE="maxangle";
    private static final String KEY_VARANGLE="varangle";
    private static final String KEY_MAXAZ="maxaz";
    private static final String KEY_SMA="sma";
    private static final String KEY_VARAZ="varaz";




    public SQLite(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        Log.d(TAG, "SQLITE, onCreate DB");

        String CREATE_CADAL_TABLE="CREATE TABLE "+ TABLE_CADAL+"("+
                KEY_ID+" INTEGER PRIMARY KEY, "+
                KEY_DATE+" TEXT, "+
                KEY_CONFIRMED+" INTEGER, "+ //FALSE=0 TRUE=1
                KEY_USED_FOR_TRAIN+" INTEGER, "+ //""
                KEY_NOTIFIED+" INTEGER, "+ //""
                KEY_MAXRMS+" REAL, "+
                KEY_MAXFRMS+" REAL, "+
                KEY_MAXANGLE+" REAL, "+
                KEY_VARANGLE+" REAL, "+
                KEY_MAXAZ+" REAL, "+
                KEY_SMA+" REAL, "+
                KEY_VARAZ+" REAL);";
        sqLiteDatabase.execSQL(CREATE_CADAL_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE_CADAL);
        this.onCreate(sqLiteDatabase);
    }

    public void addFall(FallEntry fall){
        SQLiteDatabase sqLiteDatabase=this.getWritableDatabase();

        ContentValues values= new ContentValues();

        values.put(KEY_DATE, fall.getDate());
        values.put(KEY_CONFIRMED, fall.getConfirmed());
        values.put(KEY_USED_FOR_TRAIN, fall.getTrain());
        values.put(KEY_NOTIFIED, fall.getNotified());
        values.put(KEY_MAXRMS, fall.getMaxrms());
        values.put(KEY_MAXFRMS, fall.getMaxfrms());
        values.put(KEY_MAXANGLE, fall.getMaxangle());
        values.put(KEY_VARANGLE, fall.getVarangle());
        values.put(KEY_MAXAZ, fall.getMaxaz());
        values.put(KEY_SMA, fall.getSma());
        values.put(KEY_VARAZ, fall.getVaraz());

        sqLiteDatabase.insert(TABLE_CADAL, null, values);

        sqLiteDatabase.close();

    }

    public FallEntry getFall(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor rs =  db.rawQuery( "select * from "+TABLE_CADAL+" where id="+id+"", null );
        rs.moveToFirst();
        FallEntry fall=new FallEntry();
        fall.setDate(rs.getString(rs.getColumnIndex(KEY_DATE)));
        fall.setConfirmed(rs.getInt(rs.getColumnIndex(KEY_CONFIRMED)));
        fall.setTrain(rs.getInt(rs.getColumnIndex(KEY_USED_FOR_TRAIN)));
        fall.setNotified(rs.getInt(rs.getColumnIndex(KEY_NOTIFIED)));
        fall.setMaxrms(rs.getFloat(rs.getColumnIndex(KEY_MAXRMS)));
        fall.setMaxfrms(rs.getFloat(rs.getColumnIndex(KEY_MAXFRMS)));
        fall.setMaxangle(rs.getFloat(rs.getColumnIndex(KEY_MAXANGLE)));
        fall.setVarangle(rs.getFloat(rs.getColumnIndex(KEY_VARANGLE)));
        fall.setMaxaz(rs.getFloat(rs.getColumnIndex(KEY_MAXAZ)));
        fall.setSma(rs.getFloat(rs.getColumnIndex(KEY_SMA)));
        fall.setVaraz(rs.getFloat(rs.getColumnIndex(KEY_VARAZ)));

        return fall;

    }

    public ArrayList<FallEntry> getAllFall(){
        ArrayList array_list = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_CADAL, null );
        res.moveToFirst();
        while(res.isAfterLast() == false){
            array_list.add(getFall(res.getPosition()));
            res.moveToNext();
        }
        return array_list;
    }

    public int getFallsNumber(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_CADAL, null );
        return res.getCount();
    }

    public int getCancFallsNumber(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_CADAL, null );
        int tot=0;
        res.moveToFirst();
        while(res.isAfterLast()==false){
            if(res.getInt(res.getColumnIndex(KEY_CONFIRMED))==0) tot++;
        }
        return tot;
    }





}
