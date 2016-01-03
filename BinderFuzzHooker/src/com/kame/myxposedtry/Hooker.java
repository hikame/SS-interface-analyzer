package com.kame.myxposedtry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.robv.android.xposed.XposedHelpers;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hooker implements IXposedHookLoadPackage {
	private static int GETPID_TRANSACTION = ('_'<<24)|('I'<<16)|('N'<<8)|'F';
	private static ClassLoader classloader;
	private static List<String> serInter = new ArrayList<String>();
	private static Unhook uh_FinishBoot;
	/** hook到service的ixxx.Stub类的onTransact方法上*/
//	private static XC_MethodHook xcmh_onTrans = new XC_MethodHook() {
//		@Override
//		protected void beforeHookedMethod(final MethodHookParam param) throws Throwable{
//			int code = (int) param.args[0];
//			Parcel data = (Parcel) param.args[1];
//			Parcel reply = (Parcel) param.args[2];
//			int flags = (int) param.args[3];
//			int dataS = data.dataSize(), replyS = reply.dataSize();
//			boolean dataZip = (dataS > 1024), replyZip = (replyS > 1024);
//			byte[] dataB = marshalZipParcel(data, dataZip), replyB = marshalZipParcel(reply, replyZip);
//            Log.i("Service_onTransact", String.format(
//            		"Time: [L]%d. Server: [S]%s. Code: [I]%d. " +
//            		"Data Size: [I]%d. Data Hash: [I]%d. Data: [A]%s. " +
//            		"Reply Size: [I]%d. Reply Hash: [I]%d. Reply: [A]%s. " +
//            		"Flags: [I]%d",
//            		new Date().getTime(),
//            		param.thisObject.getClass().getName(),
//            		code, 
//            		
//            		data.dataSize(),
//            		hashBytes(dataB),
//            		dataZip ? "Z" + Hex.encodeHexStr(dataB) : Hex.encodeHexStr(dataB),
//            		
//            		reply.dataSize(),
//            		hashBytes(replyB),
//            		replyZip ? "Z" + Hex.encodeHexStr(replyB) : Hex.encodeHexStr(replyB),
//            		
//            		flags)
//            ); 
//		}
//	};
	/** hook ServiceManager的addService，并将捕获到的servicename添加到serInter中*/
	private static XC_MethodHook xcmh_addSer = new XC_MethodHook() {
		@Override
		protected void afterHookedMethod(final MethodHookParam param) throws Throwable{
			new Thread(){		//addService被调用后的所有hook数据处理线程
				public void run() {
					//hook各个Service的准备工作
		           String name = (String) param.args[0];
	               IBinder ib = (IBinder) param.args[1];
	               String inter;
				   try {inter = ib.getInterfaceDescriptor();} catch (RemoteException e) { inter = null;	}
	               Log.i("ASH", String.format(
	            		   "[%d] - [T] %d, [N]: %s. [I]: %s. [C]: %s",
	            		   serInter.size(), android.os.Process.myPid(), name, inter, ib.getClass().getName()));
//	               if(inter != null){
	            	   serInter.add(ib.getClass().getSuperclass().getName().replace("$", "."));
//	               }
				}
			}.start();
		}
	};	

	/** hook android.os.Binder.onTransact, 当请求的code为我们自定义的GETPID_TRANSACTION时，返回当前进程PID*/
	private static XC_MethodHook xcmh_getPID = new XC_MethodHook(){
		protected void beforeHookedMethod(final MethodHookParam param) throws Throwable{
			int code = (Integer) param.args[0];
			if(code != GETPID_TRANSACTION){
				return;
			}
			Parcel reply = (Parcel) param.args[2];
			reply.writeInt(android.os.Process.myPid());
			reply.writeString(param.thisObject.getClass().toString());
			param.setResult(true);
			return;
		}
	};
	
//	private static XC_MethodHook xcmh_finishBoot = new XC_MethodHook(){
//		@Override
//		protected void afterHookedMethod(final MethodHookParam param) throws Throwable{
//			for(String name : serInter){
//				Log.i("KM", "Hook Method: [" + name + "]");
//				XposedHelpers.findAndHookMethod(name, classloader, "onTransact", objs_onTrans);
//			}
//			uh_FinishBoot.unhook();
//		}
//	};
	
//	private static XC_MethodHook xcmh_checkPer = new XC_MethodHook(){
//		@Override
//		protected void afterHookedMethod(final MethodHookParam param) throws Throwable{
//            int uid = (Integer) param.args[2];
//            if (uid == 2000 | uid==0) {
//                String permission = (String) param.args[0];
//                Log.d("ContextImpl.checkPermission", "Shell gets permission: " + permission);
//                param.setResult(0);
//            }
//		}
//	};
//	
//	private static XC_MethodHook xcmh_checkUriPer = new XC_MethodHook(){
//		@Override
//		protected void afterHookedMethod(final MethodHookParam param) throws Throwable{
//            int uid = (Integer) param.args[2];
//            if (uid == 2000|uid==0) {
//                Uri uri = (Uri) param.args[0];
//                int modeFlags = (Integer) param.args[3];
//                Log.d("ContextImpl.checkUriPermission", "Shell gets uri permission: " + uri
//                        + "(modeFlags-" + modeFlags + ").");
//                param.setResult(0);
//            }
//		}
//	};
	
//	private static Object[] objs_onTrans = {int.class, Parcel.class, Parcel.class, int.class, xcmh_onTrans};
	private static Object[] objs_addSer = {String.class, IBinder.class, xcmh_addSer};
//	private static Object[] objs_finishBoot = {xcmh_finishBoot};
//	private static Object[] objs_checkPer = {String.class, int.class, int.class, xcmh_checkPer};
//	private static Object[] objs_checkUriPer = {Uri.class, int.class, int.class, int.class, xcmh_checkUriPer}; 
	private static Object[] objs_onTransact = {int.class, Parcel.class, Parcel.class, int.class, xcmh_getPID};
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if(!lpparam.packageName.equals("android"))
			return;
		classloader = lpparam.classLoader;
		XposedHelpers.findAndHookMethod("android.os.ServiceManager", classloader, "addService", objs_addSer);
		XposedHelpers.findAndHookMethod("android.os.Binder", classloader, "onTransact", objs_onTransact);
//		uh_FinishBoot = XposedHelpers.findAndHookMethod("com.android.server.am.ActivityManagerService", classloader, "finishBooting", objs_finishBoot);
//		XposedHelpers.findAndHookMethod("android.app.ContextImpl", classloader, "checkPermission", objs_checkPer);
//		XposedHelpers.findAndHookMethod("android.app.ContextImpl", classloader, "checkUriPermission", objs_checkUriPer);
	}
	
	/**flag: 为true时进行压缩*/
	private static byte[] marshalZipParcel(Parcel data, boolean flag) throws IOException {
		byte[] dataB;
		try { 
			dataB = data.marshall();
		} catch(Exception e)
		{
			return new byte[]{};
		}
		if(dataB == null){
			return new byte[]{};
		}
		else
			return flag ? zip(dataB) : dataB;
	}
	
	private static int hashBytes(byte[] byteD){
        final int p = 16777619;   
        int hash = (int)2166136261L;   
        for(byte b: byteD)   
            hash = (hash ^ b) * p;   
        hash += hash << 13;   
        hash ^= hash >> 7;   
        hash += hash << 3;   
        hash ^= hash >> 17;   
        hash += hash << 5;   
        return hash;  
	}
	
	private static byte[] zip(byte[] data) throws IOException {
		if(data.length == 0)
			return data;
Log.i("KM", "ZIP org: " + Hex.encodeHexStr(data));
		 byte[] b = null;
		  ByteArrayOutputStream bos = new ByteArrayOutputStream();
		  ZipOutputStream zip = new ZipOutputStream(bos);
		  ZipEntry entry = new ZipEntry("zip");
		  entry.setSize(data.length);
		  zip.putNextEntry(entry);
		  zip.write(data);
		  zip.closeEntry();
		  zip.close();
		  b = bos.toByteArray();
		  bos.close();
Log.i("KM", "ZIP fin: " + Hex.encodeHexStr(b));
		  return b;
		}
}
