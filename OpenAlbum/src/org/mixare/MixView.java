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

/**
 * This class is the main application which uses the other classes for different
 * functionalities.
 * It sets up the camera screen and the augmented screen which is in front of the
 * camera screen.
 * It also handles the main sensor events, touch events and location events.
 * 
 * @TODO decouple class, ...
 * @FIXME displaying wikipedia, if user returns from list view.
 */

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.mixare.R.drawable;
import org.mixare.data.DataHandler;
import org.mixare.data.DataSourceList;
import org.mixare.gui.PaintScreen;
import org.mixare.render.Matrix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
//import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
//import android.view.Display; //@del
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
//import android.view.WindowManager;//@del
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MixView extends Activity implements SensorEventListener,
		OnTouchListener {

	private static boolean isInited;
	static PaintScreen dWindow;
	static DataView dataView;
	// TAG for logging
	public static final String TAG = "Open Album";

	/* string to name & access the preference file in the internal storage */
	public static final String PREFS_NAME = "MyPrefsFileForMenuItems";

	public void doError(Exception ex1) {
		if (!data.isfError()) {
			data.setfError(true);

			setErrorDialog();

			ex1.printStackTrace();
			try {
			} catch (Exception ex2) {
				ex2.printStackTrace();
			}
		}

		try {
			getAugScreen().invalidate();
		} catch (Exception ignore) {
			Log.d(TAG, ignore.getMessage());
		}
	}

	public void killOnError() throws Exception {
		if (data.isfError())
			throw new Exception();
	}

	public void repaint() {
		dataView.clearEvents();
		dataView = null; //smell code practices but enforce garbage collector to release data
		dataView = new DataView(getMixContext()); 
		dWindow = new PaintScreen();
		setZoomLevel();
	}

	public void setErrorDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(DataView.CONNECTION_ERROR_DIALOG_TEXT));
		builder.setCancelable(false);

		/* Retry */
		builder.setPositiveButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						data.setfError(false);
						// TODO improve
						try {
							repaint();
						} catch (Exception ex) {
							// Don't call doError, it will be a recursive call.
							// doError(ex);
							Log.d(TAG, ex.getMessage());
						}
					}
				});
		/* Open settings */
		builder.setNeutralButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON2,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent1 = new Intent(
								Settings.ACTION_WIRELESS_SETTINGS);
						startActivityForResult(intent1, 42);
					}
				});
		/* Close application */
		builder.setNegativeButton(DataView.CONNECTION_ERROR_DIALOG_BUTTON3,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// System.exit(0);
						finish();

					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			handleIntent(getIntent());

			final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			this.data.setmWakeLock(pm.newWakeLock(
					PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag"));

			// killOnError();
			requestWindowFeature(Window.FEATURE_NO_TITLE);

			/*
			 * Get the preference file PREFS_NAME stored in the internal memory
			 * of the phone
			 */
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();

			SharedPreferences DataSourceSettings = getSharedPreferences(
					DataSourceList.SHARED_PREFS, 0);

			data.setMyZoomBar(new SeekBar(this));
			//myZoomBar.setVisibility(View.INVISIBLE);
			data.getMyZoomBar().setMax(100);
			data.getMyZoomBar().setProgress(settings.getInt("zoomLevel", 65));
			data.getMyZoomBar()
					.setOnSeekBarChangeListener(data.getMyZoomBarOnSeekBarChangeListener());
			data.getMyZoomBar().setVisibility(View.INVISIBLE);
			
			FrameLayout frameLayout = new FrameLayout(this);

			frameLayout.setMinimumWidth(3000);
			frameLayout.addView(data.getMyZoomBar());
			frameLayout.setPadding(10, 0, 10, 10);

			setCamScreen(new CameraSurface(this));
			setAugScreen(new AugmentedView(this));
			setContentView(getCamScreen());

			addContentView(getAugScreen(), new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			addContentView(frameLayout, new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM));

			if (!isInited) {
				setMixContext(new MixContext(this)); // ?
				getMixContext().data.setDownloadManager(new DownloadManager(
						getMixContext()));// ?
				dWindow = new PaintScreen();
				dataView = new DataView(getMixContext());

				/* set the radius in data view to the last selected by the user */
				setZoomLevel();
				isInited = true;
			}

			/* check if the application is launched for the first time */
			if (settings.getBoolean("firstAccess", false) == false) {
				AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
				builder1.setMessage(getString(DataView.LICENSE_TEXT));
				builder1.setNegativeButton(getString(DataView.CLOSE_BUTTON),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
				AlertDialog alert1 = builder1.create();
				alert1.setTitle(getString(DataView.LICENSE_TITLE));
				alert1.show();
				editor.putBoolean("firstAccess", true);

				// value for maximum POI for each selected OSM URL to be active
				// by default is 5
				editor.putInt("osmMaxObject", 5);
				editor.commit();

				SharedPreferences.Editor dataSourceEditor = DataSourceSettings
						.edit();
				dataSourceEditor
						.putString("DataSource0",
								"Wikipedia|http://api.geonames.org/findNearbyWikipediaJSON|0|0|true");
				dataSourceEditor
						.putString("DataSource1",
								"Twitter|http://search.twitter.com/search.json|2|0|false");
				dataSourceEditor
						.putString(
								"DataSource2",
								"OpenStreetmap|http://open.mapquestapi.com/xapi/api/0.6/node[railway=station]|3|1|false");
				dataSourceEditor
						.putString("DataSource3",
								"Panoramio|http://www.panoramio.com/map/get_panoramas.php|4|0|true");
				dataSourceEditor.commit();

			}// end if first access

		} catch (Exception ex) {
			doError(ex);
		}
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void doMixSearch(String query) {
		DataHandler jLayer = dataView.getDataHandler();
		if (!dataView.isFrozen()) {
			MixListView.originalMarkerList = jLayer.getMarkerList();
			MixMap.originalMarkerList = jLayer.getMarkerList();
		}

		ArrayList<Marker> searchResults = new ArrayList<Marker>();
		Log.d("SEARCH-------------------0", query);
		if (jLayer.getMarkerCount() > 0) {
			for (int i = 0; i < jLayer.getMarkerCount(); i++) {
				Marker ma = jLayer.getMarker(i);
				if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1) {
					searchResults.add(ma);
					/* the website for the corresponding title */
				}
			}
		}
		if (searchResults.size() > 0) {
			dataView.setFrozen(true);
			jLayer.setMarkerList(searchResults);
		} else
			Toast.makeText(this,
					getString(DataView.SEARCH_FAILED_NOTIFICATION),
					Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onPause() {
		super.onPause();

		try {
			this.data.getmWakeLock().release();

			try {
				data.getSensorMgr().unregisterListener(this, data.getSensorGrav());
				data.getSensorMgr().unregisterListener(this, data.getSensorMag());
				// sensorMgr = null;

				getMixContext().unregisterLocationManager();
				getMixContext().data.getDownloadManager().stop();
			} catch (Exception ignore) {
				Log.w(TAG, ignore.getMessage());
			}

			if (data.isfError()) {
				finish();
			}
		} catch (Exception ex) {
			doError(ex);
		}
	}

	// @TODO optimize onResume for faster transitions
	@Override
	protected void onResume() {
		super.onResume();

		try {
			this.data.getmWakeLock().acquire();

			// killOnError();
			// SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			getMixContext().data.setMixView(this);
			dataView.doStart();
			dataView.clearEvents();

			getMixContext().refreshDataSources();

			double angleX, angleY;

			int marker_orientation = -90; // why -90

			int rotation = Compatibility.getRotation(this);

			// display text from left to right and keep it horizontal
			angleX = Math.toRadians(marker_orientation);
			data.getM1().set(1f, 0f, 0f, 0f, (float) Math.cos(angleX),
					(float) -Math.sin(angleX), 0f, (float) Math.sin(angleX),
					(float) Math.cos(angleX));
			angleX = Math.toRadians(marker_orientation);
			angleY = Math.toRadians(marker_orientation);
			if (rotation == 1) {
				data.getM2().set(1f, 0f, 0f, 0f, (float) Math.cos(angleX),
						(float) -Math.sin(angleX), 0f,
						(float) Math.sin(angleX), (float) Math.cos(angleX));
				data.getM3().set((float) Math.cos(angleY), 0f, (float) Math.sin(angleY),
						0f, 1f, 0f, (float) -Math.sin(angleY), 0f,
						(float) Math.cos(angleY));
			} else {
				data.getM2().set((float) Math.cos(angleX), 0f, (float) Math.sin(angleX),
						0f, 1f, 0f, (float) -Math.sin(angleX), 0f,
						(float) Math.cos(angleX));
				data.getM3().set(1f, 0f, 0f, 0f, (float) Math.cos(angleY),
						(float) -Math.sin(angleY), 0f,
						(float) Math.sin(angleY), (float) Math.cos(angleY));

			}

			data.getM4().toIdentity();
			// @FIXME large Memory consumtion, 60 Matrix's will be created
			// within a matrix
			for (int i = 0; i < data.getHistR().length; i++) {
				data.getHistR()[i] = new Matrix();
			}

			data.setSensorMgr((SensorManager) getSystemService(SENSOR_SERVICE));

			data.setSensors(data.getSensorMgr().getSensorList(Sensor.TYPE_ACCELEROMETER));
			if (data.getSensors().size() > 0) {
				data.setSensorGrav(data.getSensors().get(0));
			}

			data.setSensors(data.getSensorMgr().getSensorList(Sensor.TYPE_MAGNETIC_FIELD));
			if (data.getSensors().size() > 0) {
				data.setSensorMag(data.getSensors().get(0));
			}

			data.getSensorMgr().registerListener(this, data.getSensorGrav(), SENSOR_DELAY_GAME);
			data.getSensorMgr().registerListener(this, data.getSensorMag(), SENSOR_DELAY_GAME);

			try {

				GeomagneticField gmf = new GeomagneticField(
						(float) getMixContext().data.getCurLoc().getLatitude(),
						(float) getMixContext().data.getCurLoc().getLongitude(),
						(float) getMixContext().data.getCurLoc().getAltitude(),
						System.currentTimeMillis());

				angleY = Math.toRadians(-gmf.getDeclination());
				data.getM4().set((float) Math.cos(angleY), 0f, (float) Math.sin(angleY),
						0f, 1f, 0f, (float) -Math.sin(angleY), 0f,
						(float) Math.cos(angleY));
				getMixContext().data.setDeclination(gmf.getDeclination());
			} catch (Exception ex) {
				Log.d("mixare", "GPS Initialize Error", ex);
			}
			data.setDownloadThread(new Thread(getMixContext().data.getDownloadManager()));
			data.getDownloadThread().start();
		} catch (Exception ex) {
			doError(ex);
			try {
				if (data.getSensorMgr() != null) {
					data.getSensorMgr().unregisterListener(this, data.getSensorGrav());
					data.getSensorMgr().unregisterListener(this, data.getSensorMag());
					// sensorMgr = null;
				}

				if (getMixContext() != null) {
					getMixContext().unregisterLocationManager();
					if (getMixContext().data.getDownloadManager() != null) {
						getMixContext().data.getDownloadManager().stop();
					}
				}
			} catch (Exception ignore) {
				Log.e(TAG, ignore.getMessage());
			}
		}

		Log.d("-------------------------------------------", "resume");
		if (dataView.isFrozen() && data.getSearchNotificationTxt() == null) {
			data.setSearchNotificationTxt(new TextView(this));
			data.getSearchNotificationTxt().setWidth(dWindow.getWidth());
			data.getSearchNotificationTxt().setPadding(10, 2, 0, 0);
			data.getSearchNotificationTxt().setText(getString(DataView.SEARCH_ACTIVE_1)
					+ " " + DataSourceList.getDataSourcesStringList()
					+ getString(DataView.SEARCH_ACTIVE_2));
			data.getSearchNotificationTxt().setBackgroundColor(Color.DKGRAY);
			data.getSearchNotificationTxt().setTextColor(Color.WHITE);

			data.getSearchNotificationTxt().setOnTouchListener(this);
			addContentView(data.getSearchNotificationTxt(), new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		} else if (!dataView.isFrozen() && data.getSearchNotificationTxt() != null) {
			data.getSearchNotificationTxt().setVisibility(View.GONE);
			// searchNotificationTxt = null;
			data.getSearchNotificationTxt().clearComposingText();
		}
	}

	@Override
	protected void onStop(){
//		if (downloadThread.isInterrupted()){
//			downloadThread.interrupt();
//		}
		//@todo move destroy to onDestroy (user can relaunch app after it stops)
		//downloadThread.destroy();
		data.getMixContext().stopService(getIntent());
		data.getMixContext().onStopContext();
		super.onStop();
	}
	
	@Override
	protected void onDestroy(){
		//downloadThread.destroy();
		data.getMixContext().stopService(getIntent());
		data.getMixContext().onDestroyContext();
		data.setDownloadThread(null);
		data.setMixContext(null);
		data = null;
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int base = Menu.FIRST;
		/* define the first */
		MenuItem item1 = menu.add(base, base, base,
				getString(DataView.MENU_ITEM_1));
		MenuItem item2 = menu.add(base, base + 1, base + 1,
				getString(DataView.MENU_ITEM_2));
		MenuItem item3 = menu.add(base, base + 2, base + 2,
				getString(DataView.MENU_ITEM_3));
		MenuItem item4 = menu.add(base, base + 3, base + 3,
				getString(DataView.MENU_ITEM_4));
		MenuItem item5 = menu.add(base, base + 4, base + 4,
				getString(DataView.MENU_ITEM_5));
		MenuItem item6 = menu.add(base, base + 5, base + 5,
				getString(DataView.MENU_ITEM_6));
		MenuItem item7 = menu.add(base, base + 6, base + 6,
				getString(DataView.MENU_ITEM_7));

		/* assign icons to the menu items */
		item1.setIcon(drawable.icon_plates);
		item2.setIcon(android.R.drawable.ic_menu_view);
		item3.setIcon(android.R.drawable.ic_menu_mapmode);
		item4.setIcon(android.R.drawable.ic_menu_zoom);
		item5.setIcon(android.R.drawable.ic_menu_search);
		item6.setIcon(android.R.drawable.ic_menu_info_details);
		item7.setIcon(android.R.drawable.ic_menu_share);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		/* Data sources */
		case 1:
			if (!dataView.isLauncherStarted()) {
				MixListView.setList(1);
				Intent intent = new Intent(MixView.this, DataSourceList.class);
				startActivityForResult(intent, 40);
			} else {
				Toast.makeText(this,
						getString(DataView.OPTION_NOT_AVAILABLE_STRING_ID),
						Toast.LENGTH_LONG).show();
			}
			break;
		/* List view */
		case 2:

			MixListView.setList(2);
			/*
			 * if the list of titles to show in alternative list view is not
			 * empty
			 */
			if (dataView.getDataHandler().getMarkerCount() > 0) {
				Intent intent1 = new Intent(MixView.this, MixListView.class);
				startActivityForResult(intent1, 42);
			}
			/* if the list is empty */
			else {
				Toast.makeText(this, DataView.EMPTY_LIST_STRING_ID,
						Toast.LENGTH_LONG).show();
			}
			break;
		/* Map View */
		case 3:
			Intent intent2 = new Intent(MixView.this, MixMap.class);
			startActivityForResult(intent2, 20);
			break;
		/* zoom level */
		case 4:
			data.getMyZoomBar().setVisibility(View.VISIBLE);
			data.setZoomProgress(data.getMyZoomBar().getProgress());
			break;
		/* Search */
		case 5:
			onSearchRequested();
			break;
		/* GPS Information */
		case 6:
			Location currentGPSInfo = getMixContext().getCurrentLocation();
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(DataView.GENERAL_INFO_TEXT) + "\n\n"
					+ getString(DataView.GPS_LONGITUDE)
					+ currentGPSInfo.getLongitude() + "\n"
					+ getString(DataView.GPS_LATITUDE)
					+ currentGPSInfo.getLatitude() + "\n"
					+ getString(DataView.GPS_ALTITUDE)
					+ currentGPSInfo.getAltitude() + "m\n"
					+ getString(DataView.GPS_SPEED) + currentGPSInfo.getSpeed()
					+ "km/h\n" + getString(DataView.GPS_ACCURACY)
					+ currentGPSInfo.getAccuracy() + "m\n"
					+ getString(DataView.GPS_LAST_FIX)
					+ new Date(currentGPSInfo.getTime()).toString() + "\n");
			builder.setNegativeButton(getString(DataView.CLOSE_BUTTON),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			AlertDialog alert = builder.create();
			alert.setTitle(getString(DataView.GENERAL_INFO_TITLE));
			alert.show();
			break;
		/* Case 6: license agreements */
		case 7: //@TODO Add app Info
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setMessage(getString(DataView.LICENSE_TEXT));
			/* Retry */
			builder1.setNegativeButton(getString(DataView.CLOSE_BUTTON),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			AlertDialog alert1 = builder1.create();
			alert1.setTitle(getString(DataView.LICENSE_TITLE));
			alert1.show();
			break;

		}
		return true;
	}

	public float calcZoomLevel() {

		int myZoomLevel = data.getMyZoomBar().getProgress();
		float myout = 5;

		if (myZoomLevel <= 26) {
			myout = myZoomLevel / 25f;
		} else if (25 < myZoomLevel && myZoomLevel < 50) {
			myout = (1 + (myZoomLevel - 25)) * 0.38f;
		} else if (25 == myZoomLevel) {
			myout = 1;
		} else if (50 == myZoomLevel) {
			myout = 10;
		} else if (50 < myZoomLevel && myZoomLevel < 75) {
			myout = (10 + (myZoomLevel - 50)) * 0.83f;
		} else {
			myout = (30 + (myZoomLevel - 75) * 2f);
		}

		return myout;
	}

	private MixViewData data = new MixViewData(new float[9], new float[9], new float[9],
			new float[3], new float[3], 0, new Matrix(), new Matrix(),
			new Matrix(), new Matrix[60], new Matrix(), new Matrix(),
			new Matrix(), new Matrix(), 0);

	public void onSensorChanged(SensorEvent evt) {
		try {

			if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				data.getGrav()[0] = evt.values[0];
				data.getGrav()[1] = evt.values[1];
				data.getGrav()[2] = evt.values[2];

				getAugScreen().postInvalidate();
			} else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				data.getMag()[0] = evt.values[0];
				data.getMag()[1] = evt.values[1];
				data.getMag()[2] = evt.values[2];

				getAugScreen().postInvalidate();
			}

			SensorManager.getRotationMatrix(data.getRTmp(), data.getI(), data.getGrav(), data.getMag());

			int rotation = Compatibility.getRotation(this);

			if (rotation == 1) {
				SensorManager.remapCoordinateSystem(data.getRTmp(), SensorManager.AXIS_X,
						SensorManager.AXIS_MINUS_Z, data.getRot());
			} else {
				SensorManager.remapCoordinateSystem(data.getRTmp(), SensorManager.AXIS_Y,
						SensorManager.AXIS_MINUS_Z, data.getRot());
			}
			data.getTempR().set(data.getRot()[0], data.getRot()[1], data.getRot()[2], data.getRot()[3], data.getRot()[4], data.getRot()[5], data.getRot()[6],
					data.getRot()[7], data.getRot()[8]);

			data.getFinalR().toIdentity();
			data.getFinalR().prod(data.getM4());
			data.getFinalR().prod(data.getM1());
			data.getFinalR().prod(data.getTempR());
			data.getFinalR().prod(data.getM3());
			data.getFinalR().prod(data.getM2());
			data.getFinalR().invert();

			data.getHistR()[data.getrHistIdx()].set(data.getFinalR());
			data.setrHistIdx(data.getrHistIdx() + 1);
			if (data.getrHistIdx() >= data.getHistR().length)
				data.setrHistIdx(0);

			data.getSmoothR().set(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
			for (int i = 0; i < data.getHistR().length; i++) {
				data.getSmoothR().add(data.getHistR()[i]);
			}
			data.getSmoothR().mult(1 / (float) data.getHistR().length);

			synchronized (getMixContext().data.getRotationM()) {
				getMixContext().data.getRotationM().set(data.getSmoothR());
			}
		} catch (Exception ex) {
			Log.d(TAG, ex.getMessage(), ex);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		try {
			killOnError();

			float xPress = me.getX();
			float yPress = me.getY();
			if (me.getAction() == MotionEvent.ACTION_UP) {
				dataView.clickEvent(xPress, yPress);
			}
		} catch (Exception ex) {
			// doError(ex);
			ex.printStackTrace();
			return super.onTouchEvent(me);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			killOnError();

			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (dataView.isDetailsView()) {
					dataView.keyEvent(keyCode);
					dataView.setDetailsView(false);
					return true;
				} else {
					return super.onKeyDown(keyCode, event);
				}
			} else if (keyCode == KeyEvent.KEYCODE_MENU) {
				return super.onKeyDown(keyCode, event);
			} else {
				dataView.keyEvent(keyCode);
				return false;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return super.onKeyDown(keyCode, event);
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
				&& accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
				&& data.getCompassErrorDisplayed() == 0) {
			for (int i = 0; i < 2; i++) {
				Toast.makeText(getMixContext(),
						"Compass data unreliable. Please recalibrate compass.",
						Toast.LENGTH_LONG).show();
			}
			data.setCompassErrorDisplayed(data.getCompassErrorDisplayed() + 1);
		}
	}

	public boolean onTouch(View v, MotionEvent event) {
		dataView.setFrozen(false);
		if (data.getSearchNotificationTxt() != null) {
			data.getSearchNotificationTxt().setVisibility(View.GONE);
			// searchNotificationTxt = null;
			data.getSearchNotificationTxt().clearComposingText();
		}
		return false;
	}

	/*********** Getters and Setters ************/
	public boolean isZoombarVisible() {
		return data.getMyZoomBar() != null && data.getMyZoomBar().getVisibility() == View.VISIBLE;
	}

	private void setZoomLevel() {
		float myout = calcZoomLevel();

		dataView.setRadius(myout);

		data.getMyZoomBar().setVisibility(View.INVISIBLE);
		data.setZoomLevel(String.valueOf(myout));

		dataView.doStart();
		dataView.clearEvents();
		data.setDownloadThread(new Thread(getMixContext().data.getDownloadManager())); // Does
																		// this
																		// set's
																		// zoom
																		// level?
		data.getDownloadThread().start();

	};

	public int getZoomProgress() {
		return data.getZoomProgress();
	}

	public String getZoomLevel() {
		return data.getZoomLevel();
	}

	/**
	 * @return the camScreen
	 */
	private CameraSurface getCamScreen() {
		return data.getCamScreen();
	}

	/**
	 * @param camScreen
	 *            the camScreen to set
	 */
	private void setCamScreen(CameraSurface camScreen) {
		this.data.setCamScreen(camScreen);
	}

	/**
	 * @return the augScreen
	 */
	private AugmentedView getAugScreen() {
		return data.getAugScreen();
	}

	/**
	 * @param augScreen
	 *            the augScreen to set
	 */
	private void setAugScreen(AugmentedView augScreen) {
		this.data.setAugScreen(augScreen);
	}

	/**
	 * @return the mixContext
	 */
	private MixContext getMixContext() {
		return data.getMixContext();
	}

	/**
	 * @param mixContext
	 *            the mixContext to set
	 */
	private void setMixContext(MixContext mixContext) {
		this.data.setMixContext(mixContext);
	}
}

/**
 * @author daniele
 * 
 */
class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
	MixView app; // ?
	SurfaceHolder holder;
	Camera camera;

	CameraSurface(Context context) {
		super(context);

		try {
			app = (MixView) context;

			holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		} catch (Exception ex) {
			Log.e(VIEW_LOG_TAG, ex.getMessage());
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			// release camera if it's in use
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
					Log.i(VIEW_LOG_TAG, ignore.getMessage());
				}
				try {
					camera.release();
				} catch (Exception ignore) {
					Log.i(VIEW_LOG_TAG, ignore.getMessage());
				}
				// camera = null;
			}

			camera = Camera.open();
			camera.setPreviewDisplay(holder);
		} catch (Exception ex) {
			Log.w(VIEW_LOG_TAG, ex.getMessage());
			try {
				if (camera != null) {
					try {
						camera.stopPreview();
					} catch (Exception ignore) {
						Log.e(VIEW_LOG_TAG, ignore.getMessage());
					}
					try {
						camera.release();
					} catch (Exception ignore) {
						Log.e(VIEW_LOG_TAG, ignore.getMessage());
					}
					camera = null;
				}
			} catch (Exception ignore) {
				Log.i(VIEW_LOG_TAG, ignore.getMessage());
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			if (camera != null) {
				try {
					camera.stopPreview();
				} catch (Exception ignore) {
					Log.i(VIEW_LOG_TAG, ignore.getMessage());
				}
				try {
					camera.release();
				} catch (Exception ignore) {
					Log.i(VIEW_LOG_TAG, ignore.getMessage());
				}
				camera = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		try {
			Camera.Parameters parameters = camera.getParameters();
			try {
				List<Camera.Size> supportedSizes = null;
				// On older devices (<1.6) the following will fail
				// the camera will work nevertheless
				supportedSizes = Compatibility
						.getSupportedPreviewSizes(parameters);

				// preview form factor
				float ff = (float) w / h;
				Log.d("OpenAlbum - Mixare", "Screen res: w:" + w + " h:" + h
						+ " aspect ratio:" + ff);

				// holder for the best form factor and size
				float bff = 0;
				int bestw = 0;
				int besth = 0;
				Iterator<Camera.Size> itr = supportedSizes.iterator();

				// we look for the best preview size, it has to be the closest
				// to the
				// screen form factor, and be less wide than the screen itself
				// the latter requirement is because the HTC Hero with update
				// 2.1 will
				// report camera preview sizes larger than the screen, and it
				// will fail
				// to initialize the camera
				// other devices could work with previews larger than the screen
				// though
				while (itr.hasNext()) {
					Camera.Size element = itr.next();
					// current form factor
					float cff = (float) element.width / element.height;
					// check if the current element is a candidate to replace
					// the best match so far
					// current form factor should be closer to the bff
					// preview width should be less than screen width
					// preview width should be more than current bestw
					// this combination will ensure that the highest resolution
					// will win
					Log.d("Mixare", "Candidate camera element: w:"
							+ element.width + " h:" + element.height
							+ " aspect ratio:" + cff);
					if ((ff - cff <= ff - bff) && (element.width <= w)
							&& (element.width >= bestw)) {
						bff = cff;
						bestw = element.width;
						besth = element.height;
					}
				}
				Log.d("Mixare", "Chosen camera element: w:" + bestw + " h:"
						+ besth + " aspect ratio:" + bff);
				// Some Samsung phones will end up with bestw and besth = 0
				// because their minimum preview size is bigger then the screen
				// size.
				// In this case, we use the default values: 480x320
				if ((bestw == 0) || (besth == 0)) {
					Log.d("Mixare", "Using default camera parameters!");
					bestw = 480;
					besth = 320;
				}
				parameters.setPreviewSize(bestw, besth);
			} catch (Exception ex) {
				parameters.setPreviewSize(480, 320);
			}

			camera.setParameters(parameters);
			camera.startPreview();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

class AugmentedView extends View {
	MixView app; // ?
	int xSearch = 200;
	int ySearch = 10;
	int searchObjWidth = 0;
	int searchObjHeight = 0;

	public AugmentedView(Context context) {
		super(context);

		try {
			app = (MixView) context;

			app.killOnError();
		} catch (Exception ex) {
			app.doError(ex);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		try {
			// if (app.fError) {
			//
			// Paint errPaint = new Paint();
			// errPaint.setColor(Color.RED);
			// errPaint.setTextSize(16);
			//
			// /*Draws the Error code*/
			// canvas.drawText("ERROR: ", 10, 20, errPaint);
			// canvas.drawText("" + app.fErrorTxt, 10, 40, errPaint);
			//
			// return;
			// }

			app.killOnError();

			MixView.dWindow.setWidth(canvas.getWidth());
			MixView.dWindow.setHeight(canvas.getHeight());

			MixView.dWindow.setCanvas(canvas);

			if (!MixView.dataView.isInited()) {
				MixView.dataView.init(MixView.dWindow.getWidth(),
						MixView.dWindow.getHeight());
			}
			if (app.isZoombarVisible()) {
				Paint zoomPaint = new Paint();
				zoomPaint.setColor(Color.WHITE);
				zoomPaint.setTextSize(14);
				String startKM, endKM;
				endKM = "80km";
				startKM = "0km";
				/*
				 * if(MixListView.getDataSource().equals("Twitter")){ startKM =
				 * "1km"; }
				 */
				canvas.drawText(startKM, canvas.getWidth() / 100 * 4,
						canvas.getHeight() / 100 * 85, zoomPaint);
				canvas.drawText(endKM, canvas.getWidth() / 100 * 99 + 25,
						canvas.getHeight() / 100 * 85, zoomPaint);

				int height = canvas.getHeight() / 100 * 85;
				int zoomProgress = app.getZoomProgress();
				if (zoomProgress > 92 || zoomProgress < 6) {
					height = canvas.getHeight() / 100 * 80;
				}
				canvas.drawText(app.getZoomLevel(), (canvas.getWidth()) / 100
						* zoomProgress + 20, height, zoomPaint);
			}

			MixView.dataView.draw(MixView.dWindow);
		} catch (Exception ex) {
			app.doError(ex);
		}
	}
}
