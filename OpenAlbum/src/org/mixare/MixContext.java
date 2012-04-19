/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.mixare;

//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.OutputStreamWriter;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLConnection;
//import java.security.SecureRandom;
//import java.security.cert.CertificateException;
//import java.security.cert.X509Certificate;
import java.util.ArrayList;

//import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSession;
//import javax.net.ssl.X509TrustManager;

import org.mixare.data.DataSource;
import org.mixare.data.DataSourceList;
import org.mixare.render.Matrix;

import android.app.Activity;
import android.app.Dialog;
//import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.content.res.AssetManager;
//import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
//import android.net.Uri;
//import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Cares about location management and about
 * the data (source, inputstream)
 * 
 * @TODO decouple class, ...
 */
public class MixContext extends ContextWrapper {

	//TAG for logging
	public static final String TAG = "Open Album";
	
	//@TODO reorganize + centralize datasource input 
	public void refreshDataSources() {
		this.data.getAllDataSources().clear();
		SharedPreferences settings = getSharedPreferences(
				DataSourceList.SHARED_PREFS, 0);
		int size = settings.getAll().size();  
		if (size == 0){
			SharedPreferences.Editor dataSourceEditor = settings.edit();
			dataSourceEditor.putString("DataSource0", "Wikipedia|http://api.geonames.org/findNearbyWikipediaJSON|0|0|true");
			dataSourceEditor.putString("DataSource1", "Twitter|http://search.twitter.com/search.json|2|0|false");
			dataSourceEditor.putString("DataSource2", "OpenStreetmap|http://open.mapquestapi.com/xapi/api/0.6/node[railway=station]|3|1|true");
			dataSourceEditor.putString("DataSource3", "Panoramio|http://www.panoramio.com/map/get_panoramas.php|4|2|true");
			dataSourceEditor.commit();
			size = settings.getAll().size();
		}
		// copy the value from shared preference to adapter
		for (int i = 0; i < size; i++) {
			String fields[] = settings.getString("DataSource" + i, "").split("\\|", -1);
			this.data.getAllDataSources().add(new DataSource(fields[0], fields[1], fields[2], fields[3], fields[4]));
		}
	}
	
	public MixContext(Context appCtx) {
	
		super(appCtx);
		this.data.setMixView((MixView) appCtx);
		this.data.setCtx(appCtx.getApplicationContext());

		refreshDataSources();
		
		boolean atLeastOneDatasourceSelected=false;
		
		for(DataSource ds: this.data.getAllDataSources()) {
			if(ds.getEnabled()){
				atLeastOneDatasourceSelected=true; //? Why do we need that, why here
				break;
			}
		}
		// select Wikipedia if nothing was previously selected  
		if(!atLeastOneDatasourceSelected){
			//TODO>: start intent data source select
		}
		
		data.getRotationM().toIdentity();
		
		data.setLm((LocationManager) getSystemService(Context.LOCATION_SERVICE));
		
		Criteria c = new Criteria();
		//try to use the coarse provider first to get a rough position
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		String coarseProvider = data.getLm().getBestProvider(c, true);
		try {
			data.getLm().requestLocationUpdates(coarseProvider, 0 , 0, data.getLcoarse());
		} catch (Exception e) {
			Log.d(TAG, "Could not initialize the coarse provider");
		}
		
		//need to be precise
		c.setAccuracy(Criteria.ACCURACY_FINE);				
		//fineProvider will be used for the initial phase (requesting fast updates)
		//as well as during normal program usage
		//NB: using "true" as second parameters means we get the provider only if it's enabled
		String fineProvider = data.getLm().getBestProvider(c, true);
		try {
			data.getLm().requestLocationUpdates(fineProvider, 0 , 0, data.getLbounce());
		} catch (Exception e) {
			Log.d(TAG, "Could not initialize the bounce provider");
		}
		
		//fallback for the case where GPS and network providers are disabled
		Location hardFix = new Location("reverseGeocoded");

		//Frangart, Eppan, Bozen, Italy
		hardFix.setLatitude(46.480302);
		hardFix.setLongitude(11.296005);
		hardFix.setAltitude(300);

//		/*New York*/
////		hardFix.setLatitude(40.731510);
////		hardFix.setLongitude(-73.991547);
//		
//		// TU Wien
////		hardFix.setLatitude(48.196349);
////		hardFix.setLongitude(16.368653);
////		hardFix.setAltitude(180);
//
//		//frequency and minimum distance for update
//		//this values will only be used after there's a good GPS fix
//		//see back-off pattern discussion 
//		//http://stackoverflow.com/questions/3433875/how-to-force-gps-provider-to-get-speed-in-android
//		//thanks Reto Meier for his presentation at gddde 2010
//		long lFreq = 60000;	//60 seconds
//		float lDist = 50;		//20 meters
//		try {
//			lm.requestLocationUpdates(fineProvider, lFreq , lDist, lnormal);
//		} catch (Exception e) {
//			Log.d(TAG, "Could not initialize the normal provider");
//			Toast.makeText( this, getString(DataView.CONNECTION_GPS_DIALOG_TEXT), Toast.LENGTH_LONG ).show();
//		}
		
		try {
			Location lastFinePos=data.getLm().getLastKnownLocation(fineProvider);
			Location lastCoarsePos=data.getLm().getLastKnownLocation(coarseProvider);
			if(lastFinePos!=null)
				data.setCurLoc(lastFinePos);
			else if (lastCoarsePos!=null)
				data.setCurLoc(lastCoarsePos);
			else
				data.setCurLoc(hardFix);
			
		} catch (Exception ex2) {
			//ex2.printStackTrace();
			data.setCurLoc(hardFix);
			Toast.makeText( this, getString(DataView.CONNECTION_GPS_DIALOG_TEXT), Toast.LENGTH_LONG ).show();
		}
		
		setLocationAtLastDownload(data.getCurLoc());

//@TODO fix logic

	
	}
	
	
	/***** Getters and Setters ********/
	public ArrayList<DataSource> getAllDataSources() {
		return this.data.getAllDataSources();
	}
	
	public void setAllDataSourcesforLauncher(DataSource datasource) {
		this.data.getAllDataSources().clear();
		this.data.getAllDataSources().add(datasource);
	}

	public void unregisterLocationManager() {
		if (data.getLm() != null) {
			data.getLm().removeUpdates(data.getLnormal());
			data.getLm().removeUpdates(data.getLcoarse());
			data.getLm().removeUpdates(data.getLbounce());
			data.setLm(null);
		}
	}

	public DownloadManager getDownloader() {
		return data.getDownloadManager();
	}

	public String getStartUrl() {
		Intent intent = ((Activity) data.getMixView()).getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) { 
			return intent.getData().toString(); 
		} 
		else { 
			return ""; 
		}
	}

	public void onStopContext(){
		data.getDownloadManager().pause();
		//@todo move destroy to onDestroy (user can relaunch app after it stops)
		//downloadThread.destroy();
	}
	
	public void onDestroyContext(){
		data.getDownloadManager().stop();
		data.setDownloadManager(null);
	}
	public void getRM(Matrix dest) {
		synchronized (data.getRotationM()) {
			dest.set(data.getRotationM());
		}
	}
	
	public Location getCurrentLocation() {
		synchronized (data.getCurLoc()) {
			return data.getCurLoc();
		}
	}

	public void loadMixViewWebPage(String url) throws Exception {
		WebView webview = new WebView(data.getMixView());
		webview.getSettings().setJavaScriptEnabled(true);

		webview.setWebViewClient(new WebViewClient() {
			public boolean  shouldOverrideUrlLoading  (WebView view, String url) {
			     view.loadUrl(url);
				return true;
			}

		});
				
		Dialog d = new Dialog(data.getMixView()) {
			public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK)
					this.dismiss();
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.show();
		
		webview.loadUrl(url);
	}

	public void loadWebPage(String url, Context context) throws Exception {
		WebView webview = new WebView(context);
		
		webview.setWebViewClient(new WebViewClient() {
			public boolean  shouldOverrideUrlLoading  (WebView view, String url) {
			     view.loadUrl(url);
				return true;
			}

		});
				
		Dialog d = new Dialog(context) {
			public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK)
					this.dismiss();
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.show();
		
		webview.loadUrl(url);
	}

	public Location getLocationAtLastDownload() {
		return data.getLocationAtLastDownload();
	}

	public void setLocationAtLastDownload(Location locationAtLastDownload) {
		this.data.setLocationAtLastDownload(locationAtLastDownload);
	}
	
	public MixContextData data = new MixContextData(true, new Matrix(), 0f,
			new ArrayList<DataSource>());
}
