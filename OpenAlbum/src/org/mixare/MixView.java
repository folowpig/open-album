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

import org.mixare.R.drawable;
import org.mixare.data.DataHandler;
import org.mixare.data.DataSourceList;
import org.mixare.data.MixViewData;
import org.mixare.gui.PaintScreen;
import org.mixare.marker.Marker;
import org.mixare.reality.AugmentedView;
import org.mixare.reality.CameraSurface;
import org.mixare.render.Matrix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MixView extends Activity implements SensorEventListener,
		OnTouchListener {

	private static boolean isInited;
	private static PaintScreen dWindow;
	private static DataView dataView;
	// TAG for logging
	public static final String TAG = "Open Album";

	/* string to name & access the preference file in the internal storage */
	public static final String PREFS_NAME = "MyPrefsFileForMenuItems";
	
	/* Data holder class */
	private MixViewData data = new MixViewData(new float[9], new float[9], new float[9],
			new float[3], new float[3], 0, new Matrix(), new Matrix(),
			new Matrix(), new Matrix[60], new Matrix(), new Matrix(),
			new Matrix(), new Matrix(), 0);

	public void doError(Exception ex1) {
		if (!data.isfError()) {
			data.setfError(true); //?
			setErrorDialog();
			Log.d(TAG, ex1.getMessage(), ex1.fillInStackTrace());//@debug
		}

		try {
			getAugScreen().invalidate();
		} catch (Exception ignore) {
			Log.d(TAG, ignore.getMessage(), ignore.fillInStackTrace());//@debug
		}
	}

	public void killOnError() throws Exception {
		if (data.isfError())
			throw new Exception();
	}

	/**
	 * Clears "Events" and repaint screen.
	 * *Data are not cleared, caller wishes to clear, clear data first then call repaint.
	 * 
	 */
	public void repaint() {
		getDataView().clearEvents();
		setDataView(null); //smell code practices but enforce garbage collector to release data
		setDataView(new DataView(getMixContext())); 
		setdWindow(new PaintScreen());
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
						data.setfError(false); //?
						// TODO improve
						try {
							repaint();
						} catch (Exception ex) {
							// Don't call doError, it will be a recursive call.
							// doError(ex);
							Log.d(TAG, ex.getMessage(), ex.fillInStackTrace());
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
				setdWindow(new PaintScreen());
				setDataView(new DataView(getMixContext()));

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
		DataHandler jLayer = getDataView().getDataHandler();
		if (!getDataView().isFrozen()) {
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
			getDataView().setFrozen(true);
			jLayer.setMarkerList(searchResults);
		} else
			Toast.makeText(this,
					getString(DataView.SEARCH_FAILED_NOTIFICATION),
					Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onPause() {
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
		}finally {
			super.onPause();
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
			getDataView().doStart();
			getDataView().clearEvents();

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
				getMixContext().data.setDeclination(-gmf.getDeclination());
			} catch (Exception ex) {
				Log.d("Open Album", "GPS Initialize Error", ex);
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
		if (getDataView().isFrozen() && data.getSearchNotificationTxt() == null) {
			data.setSearchNotificationTxt(new TextView(this));
			data.getSearchNotificationTxt().setWidth(getdWindow().getWidth());
			data.getSearchNotificationTxt().setPadding(10, 2, 0, 0);
			data.getSearchNotificationTxt().setText(getString(DataView.SEARCH_ACTIVE_1)
					+ " " + DataSourceList.getDataSourcesStringList()
					+ getString(DataView.SEARCH_ACTIVE_2));
			data.getSearchNotificationTxt().setBackgroundColor(Color.DKGRAY);
			data.getSearchNotificationTxt().setTextColor(Color.WHITE);

			data.getSearchNotificationTxt().setOnTouchListener(this);
			addContentView(data.getSearchNotificationTxt(), new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		} else if (!getDataView().isFrozen() && data.getSearchNotificationTxt() != null) {
			data.getSearchNotificationTxt().setVisibility(View.GONE);
			// searchNotificationTxt = null;
			data.getSearchNotificationTxt().clearComposingText();
		}
		//@TODO referesh augmented 
		data.getAugScreen().refreshDrawableState();
		//setZoomLevel();
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
		//Destroy data and this class data if any.
		if (getMixContext() != null) {
			getMixContext().unregisterLocationManager();
			if (getMixContext().data.getDownloadManager() != null) {
				getMixContext().data.getDownloadManager().stop();
			}
		}
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
			if (!getDataView().isLauncherStarted()) {
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
			if (getDataView().getDataHandler().getMarkerCount() > 0) {
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
	@Override
	public void onOptionsMenuClosed (Menu menu){
		  getMixContext().data.setMixView(this);
			getDataView().doStart();
			getDataView().clearEvents();
			getDataView().drawRadar(getdWindow());
			//getMixContext().refreshDataSources();
			// super.onOptionsMenuClosed(menu);

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
				getDataView().clickEvent(xPress, yPress);
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
				if (getDataView().isDetailsView()) {
					getDataView().keyEvent(keyCode);
					getDataView().setDetailsView(false);
					return true;
				} else {
					return super.onKeyDown(keyCode, event);
				}
			} else if (keyCode == KeyEvent.KEYCODE_MENU) {
				return super.onKeyDown(keyCode, event);
			} else {
				getDataView().keyEvent(keyCode);
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
		getDataView().setFrozen(false);
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

		getDataView().setRadius(myout);

		data.getMyZoomBar().setVisibility(View.INVISIBLE);
		data.setZoomLevel(String.valueOf(myout));

		getDataView().doStart();
		getDataView().clearEvents();
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

	/**
	 * @return the dWindow
	 */
	public static PaintScreen getdWindow() {
		return dWindow;
	}

	/**
	 * @param dWindow the dWindow to set
	 */
	public static void setdWindow(PaintScreen dWindow) {
		MixView.dWindow = dWindow;
	}

	/**
	 * @return the dataView
	 */
	public static DataView getDataView() {
		return dataView;
	}

	/**
	 * @param dataView the dataView to set
	 */
	public static void setDataView(DataView dataView) {
		MixView.dataView = dataView;
	}
}
