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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.data.Json;
import org.openalbum.mixare.data.XMLHandler;
import org.openalbum.mixare.marker.Marker;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * This class establishes a connection and downloads the data for each entry in
 * its todo list one after another.
 * 
 * @TODO Customize Class + decouple it, fix performance issues.
 * @TODO http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android
 *       /android/2.3.1_r1/android/app/DownloadManager.java#DownloadManager
 */
public class DownloadManager implements Runnable {

	private boolean stop = false, pause = false, proceed = false;
	//@FIXME Dangorus mutable static fields, Change ASAP
	private final static int NOT_STARTED = 0, CONNECTING = 1, CONNECTED = 2,
			PAUSED = 3, STOPPED = 4;
	private static int state = NOT_STARTED;
	private volatile Thread runner;

	private static final String debugTag = "WorkFlow";
	private int id = 0;
	private HashMap<String, DownloadRequest> todoList = new HashMap<String, DownloadRequest>();
	private HashMap<String, DownloadResult> doneList = new HashMap<String, DownloadResult>();
	private InputStream is;

	private String currJobId = null;

	public DownloadManager(MixContext ctx) {
		Log.d(debugTag, "DownloadManager - created");
		 if(runner == null){
			    runner = new Thread(this);
			  }
	}


	public void run() {
		Log.d(debugTag, "DownloadManager - running");
		String jobId;
		DownloadRequest request;
		DownloadResult result;

		stop = false;
		pause = false;
		proceed = false;
		state = CONNECTING;

		while ( Thread.currentThread() == runner) {
			jobId = "";
			request = null;
			result = null;

			// Wait for proceed
			while (!stop && !pause) {
				synchronized (this) {
					if (todoList.size() > 0) {
						jobId = getNextReqId();
						request = todoList.get(jobId);
						proceed = true;
					}
				}
				// Do proceed
				if (proceed) {
					state = CONNECTED;
					currJobId = jobId;

					result = processRequest(request);

					synchronized (this) {
						todoList.remove(jobId);
						doneList.put(jobId, result);
						proceed = false;
					}
				}
				state = CONNECTING;

				if (!stop && !pause) {
					sleep(100);
				}
			}

			state = CONNECTING;
		}
		// Do stop
		state = STOPPED;
	
		Log.d(debugTag, "DownloadManager - running Stopped");
		return;
	}

	public int checkForConnection() {
		return state;
	}

	private void sleep(final long ms) {
		try {
			Thread.sleep(ms);
		} catch (final java.lang.InterruptedException ex) {
			Log.w("OpenAlbum - Mixare",
					"Thread interrupted, sleep() -> DownloadMang");
		}
	}

	//http://docs.oracle.com/javase/1.4.2/docs/guide/misc/threadPrimitiveDeprecation.html
	public synchronized void startThread(){
		Log.d(debugTag, "DownloadManager - startThread"); 
		if(runner == null){
		    runner = new Thread(this);
		  }
		  runner.start();
		}
	public synchronized void stopThread(){
		Log.d(debugTag, "DownloadManager - stopThread");
		  if(runner != null){
		    Thread moribund = runner;
		    runner = null;
		    moribund.interrupt();
		  }
		}
	private String getNextReqId() {
		return todoList.keySet().iterator().next();
	}

	private DownloadResult processRequest(final DownloadRequest request) {
		final DownloadResult result = new DownloadResult();
		// assume an error until everything is fine
		result.error = true;

		try {
			if (request != null && request.source.getUrl() != null) {

				is = getHttpGETInputStream(request.source.getUrl()
						+ request.params);
				final String tmp = getHttpInputString(is);

				final Json layer = new Json(); // //@FIXME OpenStreetMap
												// Recieved data is XML

				// try loading JSON DATA
				try {

					Log.v(MixView.TAG, "try to load JSON data");

					final JSONObject root = new JSONObject(tmp);

					Log.d(MixView.TAG, "loading JSON data");

					final List<Marker> markers = layer.load(root,
							request.source);
					result.setMarkers(markers);

					result.source = request.source;
					result.error = false;
					result.errorMsg = "";

				}
				catch (JSONException e) {

					Log.v(MixView.TAG, "no JSON data");
					Log.v(MixView.TAG, "try to load XML data");

					try {
						final DocumentBuilder builder = DocumentBuilderFactory
								.newInstance().newDocumentBuilder();
						// Document doc = builder.parse(is);d
						final Document doc = builder.parse(new InputSource(
								new StringReader(tmp)));

						// Document doc = builder.parse(is);

						final XMLHandler xml = new XMLHandler();

						Log.i(MixView.TAG, "loading XML data");

						final List<Marker> markers = xml.load(doc,
								request.source);

						result.setMarkers(markers);

						result.source = request.source;
						result.error = false;
						result.errorMsg = "";
					} catch (Exception e1) {
						Log.d("OpenAlbum - Mixare", e1.getMessage(), e1);
					}
				}
			}
		}
		catch (Exception ex) {
			result.errorMsg = ex.getMessage();
			result.errorRequest = request;
//
//			try {
//				closeHttpInputStream(is);
//			} catch (Exception ignore) {
//				Log.e("OpenAlbum - Mixare", ignore.getMessage());
//			}

			Log.d("OpenAlbum - Mixare", ex.getMessage(), ex);
		} finally {
			try {
				closeHttpInputStream(is);
			} catch (Exception e) {
				e.printStackTrace();
			}
			is = null;
		}

		currJobId = null;

		return result;
	}

	public synchronized void purgeLists() {
		todoList.clear();
		doneList.clear();
	}

	public synchronized String submitJob(DownloadRequest job) {
		if(job!=null) {
			String jobId = "";
			// ensure that we only have one download per each datasource
			final String currDSname = job.source.getName();
			boolean found = false;
			if (!todoList.isEmpty()) {
				for (final String k : todoList.keySet()) {
					if (currDSname.equals(todoList.get(k).source.getName())) {
						found = true;
						jobId = k;
					}
				}
			}
			if (!found) {
				jobId = "ID_" + (id++);
				todoList.put(jobId, job);
				Log.i(MixView.TAG, "Submitted Job with " + jobId + ", type: "
						+ job.source.getType() + ", params: " + job.params
						+ ", url: " + job.source.getUrl());
			}
			return jobId;
		}
		return null;
	}
	

	public synchronized boolean isReqComplete(String jobId) {
		return doneList.containsKey(jobId);
	}

	public synchronized DownloadResult getReqResult(String jobId) {
		DownloadResult result = doneList.get(jobId);
		doneList.remove(jobId);

		return result;
	}

	public String getActiveReqId() {
		return currJobId;
	}

	public void pause() {
		pause = true;
		Log.d(debugTag, "DownloadManager - Paused");
	}

	public void restart() {
		pause = false;
	}

	public void stop() {
		stop = true;
		final Throwable stack = new Throwable().fillInStackTrace();//debugTag
		Log.d(debugTag, "DownloadManager - stop request",stack);
	}

	public synchronized DownloadResult getNextResult() {
		if (!doneList.isEmpty()) {
			final String nextId = doneList.keySet().iterator().next();
			final DownloadResult result = doneList.get(nextId);
			doneList.remove(nextId);
			return result;
		}
		return null;
	}

	public Boolean isDone() {
		return todoList.isEmpty();
	}



	public InputStream getHttpGETInputStream(final String urlStr)
			throws Exception {
		InputStream is = null;
		URLConnection conn = null;

		// HTTP connection reuse which was buggy pre-froyo
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}

		if (urlStr.startsWith("file://")) {
			return new FileInputStream(urlStr.replace("file://", ""));
		}

		if (urlStr.startsWith("content://")) {
			return getContentInputStream(urlStr, null);
		}

		if (urlStr.startsWith("https://")) {
			HttpsURLConnection
					.setDefaultHostnameVerifier(new HostnameVerifier() {
						public boolean verify(final String hostname,
								final SSLSession session) {
							return true;
						}
					});
			final SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new X509TrustManager[] { new X509TrustManager() {
				public void checkClientTrusted(final X509Certificate[] chain,
						final String authType) throws CertificateException {
				}

				public void checkServerTrusted(final X509Certificate[] chain,
						final String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			} }, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(context
					.getSocketFactory());
		}

		try {
			final URL url = new URL(urlStr);
			conn = url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);

			is = conn.getInputStream(); // @TODO fix openMap error (URL Params)

			return is;
		} catch (final Exception ex) {
			try {
				is.close();
			} catch (final Exception ignore) {
			}
			try {
				if (conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				}
			} catch (final Exception ignore) {
			}

			throw ex;

		}
	}

	public InputStream getContentInputStream(final String urlStr,
			final String params) throws Exception {
		final ContentResolver cr = MixView.class.newInstance()
				.getContentResolver();
		final Cursor cur = cr
				.query(Uri.parse(urlStr), null, params, null, null);

		cur.moveToFirst();
		final int mode = cur.getInt(cur.getColumnIndex("MODE"));

		if (mode == 1) {
			final String result = cur.getString(cur.getColumnIndex("RESULT"));
			cur.deactivate();

			return new ByteArrayInputStream(result.getBytes());
		} else {
			cur.deactivate();

			throw new Exception("Invalid content:// mode " + mode);
		}
	}

	public String getHttpInputString(final InputStream is) {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				is), 8 * 1024);
		final StringBuilder sb = new StringBuilder();

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			Log.d("OpenAlbum - Mixare", e.getMessage(), e);
		}
//		 finally { // read, don't close.
//			try {
//				is.close();
//			} catch (IOException ex) {
//				Log.d("OpenAlbum - Mixare", ex.getMessage(), ex);
//			}
//		}
		return sb.toString();
	}

	public void closeHttpInputStream(InputStream is) throws Exception {
		if (is != null) {
			is.close();
		}
	}
	// Moved from mixContext
//	public InputStream getHttpPOSTInputStream(String urlStr,
//			String params) throws Exception {}

	/**
	 * @return the stopped
	 */
	public static boolean isStopped() {
		return (state == STOPPED)? true : false;
	}

//	public InputStream getResourceInputStream(String name)
//			throws Exception {
//		final AssetManager mgr = MixView.class.newInstance().getAssets();
//		return mgr.open(name);
//	}

//	public void closeResourceInputStream(InputStream is) throws Exception {
//		if (is != null) {
//			is.close();
//		}
//	}
}

class DownloadRequest {

	public DataSource source;
	String params;

}

class DownloadResult {
	public DataSource source;
	String params;
	List<Marker> markers;

	boolean error;
	String errorMsg = "";
	DownloadRequest errorRequest;

	public List<Marker> getMarkers() {
		return markers;
	}

	public void setMarkers(final List<Marker> markers) {
		this.markers = markers;
	}

}
