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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.DataSource;
import org.mixare.data.Json;
import org.mixare.data.XMLHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.mixare.MixView;
import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * This class establishes a connection and downloads the data for
 * each entry in its todo list one after another.
 * 
 * @TODO Customize Class + decouple it, fix performance issues.
 * @TODO http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.1_r1/android/app/DownloadManager.java#DownloadManager  
 */
public class DownloadManager implements Runnable {

	private boolean stop = false, pause = false, proceed = false;
	public static int NOT_STARTED = 0, CONNECTING = 1, CONNECTED = 2, PAUSED = 3, STOPPED = 4;
	private int state = NOT_STARTED;

	private int id = 0;
	private HashMap<String, DownloadRequest> todoList = new HashMap<String, DownloadRequest>();
	private HashMap<String, DownloadResult> doneList = new HashMap<String, DownloadResult>();
	InputStream is;

	private String currJobId = null;

	MixContext ctx;

	public DownloadManager(MixContext ctx) {
		this.ctx = ctx;
	}

	public void run() {
		String jobId;
		DownloadRequest request;
		DownloadResult result;

		stop = false;
		pause = false;
		proceed = false;
		state = CONNECTING;

		while (!stop) {
			jobId = null;
			request = null;
			result = null;

			// Wait for proceed
			while (!stop && !pause) {
				synchronized (this) { //@FIXME Important - Thread deadLock
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

				if (!stop && !pause)
					sleep(100);
			}

			// Do pause
			while (!stop && pause) {
				state = PAUSED;
				sleep(100);
			}
			state = CONNECTING;
		}
		// Do stop
		state = STOPPED;
	}

	public int checkForConnection(){
		return state;
	}

	private void sleep(long ms){
		try {
			Thread.sleep(ms);
		} catch (java.lang.InterruptedException ex) {

		}
	}

	private String getNextReqId() {
		return todoList.keySet().iterator().next();
	}

	private DownloadResult processRequest(DownloadRequest request) {
		DownloadResult result = new DownloadResult();
		//assume an error until everything is fine
		result.error = true;
		
		try {
			if( request!=null && request.source.getUrl() !=null){

				is = getHttpGETInputStream(request.source.getUrl() + request.params);
				String tmp = getHttpInputString(is);

				Json layer = new Json(); ////@FIXME OpenStreetMap Recieved data is XML

				// try loading JSON DATA
				try {

					Log.v(MixView.TAG, "try to load JSON data");

					JSONObject root = new JSONObject(tmp);

					Log.d(MixView.TAG, "loading JSON data");

					List<Marker> markers = layer.load(root,request.source);
					result.setMarkers(markers);

					result.source = request.source;
					result.error = false;
					result.errorMsg = null;

				}
				catch (JSONException e) {

					Log.v(MixView.TAG, "no JSON data");
					Log.v(MixView.TAG, "try to load XML data");

					try {
						DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						//Document doc = builder.parse(is);d
						Document doc = builder.parse(new InputSource(new StringReader(tmp)));

						//Document doc = builder.parse(is);

						XMLHandler xml = new XMLHandler();

						Log.i(MixView.TAG, "loading XML data");	
						

						List<Marker> markers = xml.load(doc, request.source);
						
						result.setMarkers(markers);

						result.source = request.source;
						result.error = false;
						result.errorMsg = null;
					} catch (Exception e1) {
						e1.printStackTrace();
					}				
				}
				closeHttpInputStream(is);
				is = null;
			}
		}
		catch (Exception ex) {
			result.errorMsg = ex.getMessage();
			result.errorRequest = request;

			try {
				closeHttpInputStream(is);
			} catch (Exception ignore) {
			}

			ex.printStackTrace();
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
			//ensure that we only have one download per each datasource
			String currDSname = job.source.getName();
			boolean found = false;
			if (!todoList.isEmpty()){
			for (String k: todoList.keySet()) {
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
					+ job.source.getType() + ", params: " + job.params + ", url: "
					+ job.source.getUrl());
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
	}

	public void restart() {
		pause = false;
	}

	public void stop() {
		stop = true;
	}
	
	public synchronized DownloadResult getNextResult() {
		if(!doneList.isEmpty()) {
			String nextId=doneList.keySet().iterator().next();
			DownloadResult result = doneList.get(nextId);
			doneList.remove(nextId);
			return result;
		}
		return null;
	}
	public Boolean isDone() {
		return todoList.isEmpty();
	}

	//Moved from mixContext
	public InputStream getHttpPOSTInputStream(String urlStr,
			String params) throws Exception {
		InputStream is = null;
		OutputStream os = null;
		HttpURLConnection conn = null;

		if (urlStr.startsWith("content://"))
			return getContentInputStream(urlStr, params);

		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);

			if (params != null) {
				conn.setDoOutput(true);
				os = conn.getOutputStream();
				OutputStreamWriter wr = new OutputStreamWriter(os);
				wr.write(params);
				wr.close();
			}

			is = conn.getInputStream();
			
			return is;
		} catch (Exception ex) {

			try {
				is.close();
			} catch (Exception ignore) {			

			}
			try {
				os.close();
			} catch (Exception ignore) {			

			}
			try {
				conn.disconnect();
			} catch (Exception ignore) {
			}

			if (conn != null && conn.getResponseCode() == 405) {
				return getHttpGETInputStream(urlStr);
			} else {		

				throw ex;
			}
		}
	}
	public InputStream getHttpGETInputStream(String urlStr)
			throws Exception {
				InputStream is = null;
				URLConnection conn = null;

			    // HTTP connection reuse which was buggy pre-froyo
			    if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			        System.setProperty("http.keepAlive", "false");
			    }
			    
				if (urlStr.startsWith("file://"))			
					return new FileInputStream(urlStr.replace("file://", ""));

				if (urlStr.startsWith("content://"))
					return getContentInputStream(urlStr, null);

				if (urlStr.startsWith("https://")) {
					HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
		    			public boolean verify(String hostname, SSLSession session) {
		    				return true;
		    			}});
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(null, new X509TrustManager[]{new X509TrustManager(){
					public void checkClientTrusted(X509Certificate[] chain,
							String authType) throws CertificateException {}
					public void checkServerTrusted(X509Certificate[] chain,
							String authType) throws CertificateException {}
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}}}, new SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(
						context.getSocketFactory());
				}
				
				try {
					URL url = new URL(urlStr);
					conn =  url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(10000);

					is = conn.getInputStream(); //@TODO fix openMap error (URL Params)
					
					return is;
				} catch (Exception ex) {
					try {
						is.close();
					} catch (Exception ignore) {			
					}
					try {
						if(conn instanceof HttpURLConnection)
							((HttpURLConnection)conn).disconnect();
					} catch (Exception ignore) {			
					}
					
					throw ex;				

				}
			}

	public InputStream getContentInputStream(String urlStr, String params)
	throws Exception {
		ContentResolver cr = MixView.class.newInstance().getContentResolver();
		Cursor cur = cr.query(Uri.parse(urlStr), null, params, null, null);

		cur.moveToFirst();
		int mode = cur.getInt(cur.getColumnIndex("MODE"));

		if (mode == 1) {
			String result = cur.getString(cur.getColumnIndex("RESULT"));
			cur.deactivate();

			return new ByteArrayInputStream(result
					.getBytes());
		} else {
			cur.deactivate();

			throw new Exception("Invalid content:// mode " + mode);
		}
	}

	public String getHttpInputString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8 * 1024);
		StringBuilder sb = new StringBuilder();

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public void closeHttpInputStream(InputStream is) throws Exception {
		if (is != null) {
			is.close();
		}
	}

	public InputStream getResourceInputStream(String name) throws Exception {
		AssetManager mgr = MixView.class.newInstance().getAssets();
		return mgr.open(name);
	}

	public void closeResourceInputStream(InputStream is) throws Exception {
		if (is != null)
			is.close();
	}
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
	String errorMsg="";
	DownloadRequest errorRequest;


	public List<Marker> getMarkers() {
		return markers;
	}
	public void setMarkers(List<Marker> markers) {
		this.markers = markers;
	}
	
}
