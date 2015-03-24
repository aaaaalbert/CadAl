package com.wisense.cadal;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * NOT USED
 */

//public class ProgressDialogWithTimeout {
//
//	private static Timer mTimer = new Timer();
//	private static ProgressDialog dialog;
//
//	public ProgressDialogWithTimeout(Context context) {
//	    super();
//	    // TODO Auto-generated constructor stub
//	}
//
//	public ProgressDialogWithTimeout(Context context, int theme) {
//	    super();
//	    // TODO Auto-generated constructor stub
//	}
//
//	public static ProgressDialog show (Context context, CharSequence title, CharSequence message)
//	{
//	    MyTask task = new MyTask();
//	            // Run task after 10 seconds
//	    mTimer.schedule(task, 0, 10000);
//
//	    dialog = ProgressDialog.show(context, title, message);
//	    return dialog;
//	}
//
//	static class MyTask extends TimerTask {
//
//	    public void run() {
//	        // Do what you wish here with the dialog
//	        if (dialog != null)
//	        {
//	            dialog.cancel();
//	        }
//	    }
//	}
//}
