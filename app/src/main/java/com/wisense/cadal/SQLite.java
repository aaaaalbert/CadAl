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
 * This class handle the CADALDB database, table cadal
 */
public class SQLite extends SQLiteOpenHelper {

    final static boolean DEBUG=true;
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

        if(DEBUG) Log.d(TAG, "SQLITE, onCreate DB");

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

    /**
     * Add a fall to the db
     * @param fall (FallEntry)
     */
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

    /**
     * get a fall from the cursor and the id
     * @param res : cursor
     * @param id : int
     * @return a FallEntry
     */
    public FallEntry getFall(Cursor res, int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor rs =  db.rawQuery( "select * from "+TABLE_CADAL+" where id="+id+"", null );
        rs.moveToFirst();
        FallEntry fall=new FallEntry();
        //if (rs.moveToFirst()) {
            fall.setDate(res.getString(res.getColumnIndex(KEY_DATE)));
            fall.setConfirmed(res.getInt(res.getColumnIndex(KEY_CONFIRMED)));
            fall.setTrain(res.getInt(res.getColumnIndex(KEY_USED_FOR_TRAIN)));
            fall.setNotified(res.getInt(res.getColumnIndex(KEY_NOTIFIED)));
            fall.setMaxrms(res.getFloat(res.getColumnIndex(KEY_MAXRMS)));
            fall.setMaxfrms(res.getFloat(res.getColumnIndex(KEY_MAXFRMS)));
            fall.setMaxangle(res.getFloat(res.getColumnIndex(KEY_MAXANGLE)));
            fall.setVarangle(res.getFloat(res.getColumnIndex(KEY_VARANGLE)));
            fall.setMaxaz(res.getFloat(res.getColumnIndex(KEY_MAXAZ)));
            fall.setSma(res.getFloat(res.getColumnIndex(KEY_SMA)));
            fall.setVaraz(res.getFloat(res.getColumnIndex(KEY_VARAZ)));
        //}
        //rs.close();
        return fall;

    }

    /**
     * return all the falls
     * @return ArrayList of FallEntry
     */
    public ArrayList<FallEntry> getAllFall(){
        ArrayList array_list = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_CADAL, null );
        if (getFallsNumber()>0) {
            res.moveToFirst();
            do{

                array_list.add(getFall(res, res.getPosition()));

            } while(res.moveToNext());
            res.close();
        }
        return array_list;
    }

    /**
     * return the number of falls in the db
     * @return falls : int
     */
    public int getFallsNumber(){
        String countQuery = "SELECT  * FROM " + TABLE_CADAL;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }

    /**
     * return the number of not confirmed falls
     * @return int falls number
     */
    public int getCancFallsNumber(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_CADAL, null );
        int tot=0;
        if (getFallsNumber()>0) {
            res.moveToFirst();
            do{
                if(DEBUG) Log.d(TAG,"GETCANCFALLSNUMBER LOOP");
                if(res.getInt(res.getColumnIndex(KEY_CONFIRMED))==0) tot++;
            } while(res.moveToNext());
            res.close();
        }
        return tot;
    }

    /**
     * Clear all the table of falls
     */
    public void deleteAllFalls(){
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_CADAL + ";");
    }




}
