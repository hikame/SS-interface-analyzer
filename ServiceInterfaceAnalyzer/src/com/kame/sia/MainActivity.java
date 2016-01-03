package com.kame.sia;

import java.lang.reflect.Method;

import android.app.Activity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private final String TAG = "SIA";
	private static int GETPID_TRANSACTION = ('_'<<24)|('I'<<16)|('N'<<8)|'F';
android.os.Binder b;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TextView tv = (TextView) findViewById(R.id.textView1);
		Button bt = (Button) findViewById(R.id.button1);
		bt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					startAnalyze();
				} catch (Exception e) {
					e.printStackTrace();
					Log.i(TAG, e.toString());
				}
			}
		});
	}

	private IBinder startAnalyze() throws Exception{
		Class smcls = Class.forName("android.os.ServiceManager");
		Method listServices = smcls.getMethod("listServices", null);
		String[] services = (String[])listServices.invoke(null, null);
		int count = -1;
		for(String ser : services){
//			Log.i(TAG, String.format("[%d] %s", count, ser));
			count++;
			Method getService = smcls.getMethod("getService", String.class);
			IBinder ib = (IBinder)getService.invoke(null, ser);
			if(ib == null){
				Log.i(TAG, String.format(
					"[%d] - [P] ?, [N] %s: there is no ibinder object.", count, ser));
				continue;
			}
			String inter;
			try {inter = ib.getInterfaceDescriptor();} catch (RemoteException e) { inter = null;	}
			if(inter == null){
				Log.i(TAG, String.format(
						"[%d] - [P] ?, [N] %s: there is no interface descriptor.", count, ser));
				continue;
			}
			
//			Class interStubClass;
//			try{
//				interStubClass = Class.forName(inter + "$Stub");
//			}catch(ClassNotFoundException e){
//				Log.i(TAG, String.format(
//						"[%d] - [P] ?, [N] %s, [I] %s: interface descriptor is not a class.", count, ser, inter));
//				continue;
//			}
//			Method asInterface = interStubClass.getMethod("asInterface", IBinder.class);
//			String serClass = asInterface.invoke(null, ib).getClass().get();
//			
//			String serClass = (ib == null ? null  ib.getClass().getName());
			int pid =  -1;
			String serClass = null;
			Parcel data = Parcel.obtain();
			Parcel reply = Parcel.obtain();
			try {
				ib.transact(GETPID_TRANSACTION, data, reply, 0);
				pid = reply.readInt();
				serClass = reply.readString();
				if(serClass != null)
					serClass = serClass.substring("class ".length());
			} catch (RemoteException e) {}
			
            Log.i(TAG, String.format(
         		   "[%d] - [P] %d, [N]: %s. [I]: %s. [C]: %s",
         		   count, pid, ser, inter, serClass));
		}
		return null;
//		return (IBinder) mth.invoke(null, sername);	
	}
	
	private int getPID(IBinder ib) throws RemoteException {
		Parcel data = Parcel.obtain();
		Parcel reply = Parcel.obtain();
		ib.transact(GETPID_TRANSACTION, data, reply, 0);
		return reply.readInt();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
