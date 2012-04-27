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
package org.openalbum.mixare;

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

import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.data.DataSourceList;
import org.openalbum.mixare.data.MixContextData;
import org.openalbum.mixare.render.Matrix;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Cares about location management and about the data (source, inputstream)
 * 
 * @TODO decouple class, ...
 */
public class MixContext extends ContextWrapper {

	// TAG for logging
	public static final String TAG = "Open Album";
	
	public MixContextData data = new MixContextData(true, new Matrix(), 0f,
			new ArrayList<DataSource>());
	
	public MixContext(final Context appCtx) {
		super(appCtx);
		this.data.setMixView((MixView) appCtx);
		this.data.setCtx(appCtx.getApplicationContext());

		refreshDataSources();

		boolean atLeastOneDatasourceSelected = false;

		for (final DataSource ds : this.data.getAllDataSources()) {
			if (ds.getEnabled()) {
				atLeastOneDatasourceSelected = true;
				break;
			}
		}
		// select Wikipedia if nothing was previously selected
		if (!atLeastOneDatasourceSelected) {
			// TODO>: start intent data source select
		}

		data.getRotationM().toIdentity();

		data.setLm((LocationManager) getSystemService(Context.LOCATION_SERVICE));

		searchForGPSProvider();

		setLocationAtLastDownload(data.getCurLoc());

	}
	
	/**
	 * Internal function that search's for the best GPS provider.
	 * 1- looks for more precise (fine)
	 * 2- looks for approimation first (Coarse)
	 * 3- Hard code location (reverse geo)
	 * 
	 */
	private void searchForGPSProvider() {
		final Criteria c = new Criteria();

		// need to be precise
		c.setAccuracy(Criteria.ACCURACY_FINE);
		// fineProvider will be used for the initial phase (requesting fast
		// updates)
		// as well as during normal program usage
		// NB: using "true" as second parameters means we get the provider only
		// if it's enabled
		final String fineProvider = data.getLm().getBestProvider(c, true);
		try {
			data.getLm().requestLocationUpdates(fineProvider, 0, 0,
					data.getLbounce());
		} catch (final Exception e) {
			Log.d(TAG, "Could not initialize the bounce provider");
		}
		
		// try to use the coarse provider first to get a rough position
		// Sidenote: rough estimate approach is not being used.
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		final String coarseProvider = data.getLm().getBestProvider(c, true);
		try {
			data.getLm().requestLocationUpdates(coarseProvider, 0, 0,
					data.getLcoarse());
		} catch (final Exception e) {
			Log.d(TAG, "Could not initialize the coarse provider");
		}

		// fallback for the case where GPS and network providers are disabled
		final Location hardFix = new Location("reverseGeocoded");

		// Frangart, Eppan, Bozen, Italy
		hardFix.setLatitude(46.480302);
		hardFix.setLongitude(11.296005);
		hardFix.setAltitude(300);

		// /*New York*/
		// // hardFix.setLatitude(40.731510);
		// // hardFix.setLongitude(-73.991547);
		//
		// // TU Wien
		// // hardFix.setLatitude(48.196349);
		// // hardFix.setLongitude(16.368653);
		// // hardFix.setAltitude(180);
		//
		// //frequency and minimum distance for update
		// //this values will only be used after there's a good GPS fix
		// //see back-off pattern discussion
		// //http://stackoverflow.com/questions/3433875/how-to-force-gps-provider-to-get-speed-in-android
		// //thanks Reto Meier for his presentation at gddde 2010
		// long lFreq = 60000; //60 seconds
		// float lDist = 50; //20 meters
		// try {
		// lm.requestLocationUpdates(fineProvider, lFreq , lDist, lnormal);
		// } catch (Exception e) {
		// Log.d(TAG, "Could not initialize the normal provider");
		// Toast.makeText( this, getString(DataView.CONNECTION_GPS_DIALOG_TEXT),
		// Toast.LENGTH_LONG ).show();
		// }

		try {
			final Location lastFinePos = data.getLm().getLastKnownLocation(
					fineProvider);
			final Location lastCoarsePos = data.getLm().getLastKnownLocation(
					coarseProvider);
			if (lastFinePos != null) {
				data.setCurLoc(lastFinePos);
			} else if (lastCoarsePos != null) {
				data.setCurLoc(lastCoarsePos);
			} else {
				data.setCurLoc(hardFix);
			}

		} catch (final Exception ex2) {
			// ex2.printStackTrace();
			data.setCurLoc(hardFix);
			Toast.makeText(this,
					getString(DataView.CONNECTION_GPS_DIALOG_TEXT),
					Toast.LENGTH_LONG).show();
		}
	}

	public void refreshDataSources() {
		this.data.getAllDataSources().clear();
		 SharedPreferences settings = getSharedPreferences(
				DataSourceList.SHARED_PREFS, 0);
		int size = settings.getAll().size();
		if (size == 0) {
			//@TODO make all access to setting through this class only
			data.getMixView().storeDefaultSources();
			settings = getSharedPreferences(
					DataSourceList.SHARED_PREFS, 0);
			size = settings.getAll().size();
		}
		// copy the value from shared preference to adapter
		for (int i = 0; i < size; i++) {
			final String fields[] = settings.getString("DataSource" + i, "")
					.split("\\|", -1);
			this.data.getAllDataSources().add(
					new DataSource(fields[0], fields[1], fields[2], fields[3],
							fields[4]));
		}
	}



	/***** Getters and Setters ********/
	public ArrayList<DataSource> getAllDataSources() {
		return this.data.getAllDataSources();
	}

	public void setAllDataSourcesforLauncher(final DataSource datasource) {
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
		final Intent intent = ((Activity) data.getMixView()).getIntent();
		if (intent.getAction() != null
				&& intent.getAction().equals(Intent.ACTION_VIEW)) {
			return intent.getData().toString();
		} else {
			return "";
		}
	}

	public void onStopContext() {
		data.getDownloadManager().pause();
		// @todo move destroy to onDestroy (user can relaunch app after it
		// stops)
		// downloadThread.destroy();
	}

	public void onDestroyContext() {
		data.getDownloadManager().stop();
		data.setDownloadManager(null);
		unregisterLocationManager();
	}

	/**
	 * sets rotation manager of dest
	 * @param dest
	 */
	public void setRM(final Matrix dest) {
		synchronized (data.getRotationM()) {
			dest.set(data.getRotationM());
		}
	}

	public Location getCurrentLocation() {
		synchronized (data.getCurLoc()) {
			return data.getCurLoc();
		}
	}

	public void loadMixViewWebPage(final String url) throws Exception {
		final WebView webview = new WebView(data.getMixView());
		webview.getSettings().setJavaScriptEnabled(true);
		webview.getSettings().setAppCacheEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view,
					final String url) {
				view.loadUrl(url);
				return true;
			}

		});

		final Dialog d = new Dialog(data.getMixView()) {
			@Override
			public boolean onKeyDown(final int keyCode, final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					this.dismiss();
					webview.destroy();
				}
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.setCancelable(true);
		d.setCanceledOnTouchOutside(true);
		d.show();
		webview.loadUrl(url);
	}

	public void loadWebPage(final String url, final Context context)
			throws Exception {
		final WebView webview = new WebView(context);
		webview.getSettings().setAppCacheEnabled(true);
		webview.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(final WebView view,
					final String url) {
				view.loadUrl(url);
				return true;
			}

		});

		final Dialog d = new Dialog(context) {
			@Override
			public boolean onKeyDown(final int keyCode, final KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					this.dismiss();
					webview.destroy();
				}
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.setCancelable(true);
		d.setCanceledOnTouchOutside(true);
		d.show();

		webview.loadUrl(url);
	}

	public Location getLocationAtLastDownload() {
		return data.getLocationAtLastDownload();
	}

	public void setLocationAtLastDownload(final Location locationAtLastDownload) {
		this.data.setLocationAtLastDownload(locationAtLastDownload);
	}


}
