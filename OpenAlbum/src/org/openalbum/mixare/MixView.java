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

import org.openalbum.mixare.R.drawable;
import org.openalbum.mixare.data.DataHandler;
import org.openalbum.mixare.data.DataSourceList;
import org.openalbum.mixare.data.MixViewData;
import org.openalbum.mixare.gui.PaintScreen;
import org.openalbum.mixare.marker.Marker;
import org.openalbum.mixare.reality.AugmentedView;
import org.openalbum.mixare.reality.CameraSurface;
import org.openalbum.mixare.render.Matrix;

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
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MixView extends Activity implements SensorEventListener,
		OnTouchListener {
	// TAG for logging
	public static final String TAG = "Open Album";
	private static final String debugTag = "WorkFlow";

	private static boolean isInited;
	private static PaintScreen dWindow;
	private static DataView dataView;

	/* string to name & access the preference file in the internal storage */
	// change to not be conflicted with Mixare AR
	public static final String PREFS_NAME = "OpenAlbumPrefsFileForMenuItems";
	private OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		Toast t;

		public void onProgressChanged(final SeekBar seekBar,
				final int progress, final boolean fromUser) {
			final float myout = calcZoomLevel();

			data.setZoomLevel(String.valueOf(myout));
			data.setZoomProgress(data.getMyZoomBar().getProgress());

			t.setText("Radius: " + String.valueOf(myout));
			t.show();
		}

		public void onStartTrackingTouch(final SeekBar seekBar) {
			final Context ctx = seekBar.getContext();
			t = Toast.makeText(ctx, "Radius: ", Toast.LENGTH_LONG);
			// zoomChanging= true;
		}

		public void onStopTrackingTouch(final SeekBar seekBar) {
			final SharedPreferences settings = getMixContext()
					.getSharedPreferences(PREFS_NAME, 0);
			final SharedPreferences.Editor editor = settings.edit();
			/* store the zoom range of the zoom bar selected by the user */
			editor.putInt("zoomLevel", data.getMyZoomBar().getProgress());
			editor.commit();
			data.getMyZoomBar().setVisibility(View.INVISIBLE);
			// zoomChanging= false;
			setZoomLevel();
			// data.getMyZoomBar().getProgress();

			t.cancel();
			// repaint after changing the level
			repaint();
			refreshDownload();

		}

		// private void //refreashZoomLevel() {
		// TODO Auto-generated method stub

		// }

	};

	/* Data holder class */
	private MixViewData data = new MixViewData();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(debugTag, "on Create - MixView");
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
			final SharedPreferences settings = getSharedPreferences(PREFS_NAME,
					0);
			/* check if the application is launched for the first time */
			if (settings.getBoolean("firstAccess", false) == false) {
				firstAccess(settings);
			}
			maintainView();
			
			final FrameLayout frameLayout = createZoomBar(settings);
			
			addContentView(getAugScreen(), new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			addContentView(frameLayout, new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM));

			if (!isInited) {
				setMixContext(new MixContext(this));
				getMixContext().data.setDownloadManager(new DownloadManager(
						getMixContext()));
				setdWindow(new PaintScreen());
				setDataView(new DataView(getMixContext()));

				/* set the radius in data view to the last selected by the user */
				setZoomLevel();
				isInited = true;
				refreshDownload();
			}

		} catch (Exception ex) {
			doError(ex);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(debugTag, "MixView - On Start started");

		if (!isInited) {
			Log.d(debugTag, "MixView - On Start - isInited is not true");
		}
		maintainView();
	}

	/**
	 * checks if there is inctances of view
	 */
	private void maintainView() {
		if (getAugScreen() == null || getCamScreen() == null){
			setCamScreen(new CameraSurface(this));
			setAugScreen(new AugmentedView(this));
			setContentView(getCamScreen());
		}
		if (getDataView() == null) {
			Log.d(debugTag, "MixView - on Start - DataView is null");
		}
		if (getMixContext() == null) {
			Log.d(debugTag, "MixView - on Start - MixContext is null");
			setMixContext(new MixContext(this));
		}
	}

	// @TODO optimize onResume for faster transitions
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(debugTag, "MixView - On Resume - starting");
		try {
			this.data.getmWakeLock().acquire();

			// reinit context and dataview
			getMixContext().data.setMixView(this);
			getDataView().setMixContext(getMixContext());
			// then update context
			data.setMixContext(getDataView().getContext());
			// refreash data
			getMixContext().refreshDataSources();

			getDataView().doStart();
			getDataView().clearEvents();

			double angleX, angleY;

			final int marker_orientation = -90; // why -90

			final int rotation = Compatibility.getRotation(this);

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
				data.getM3()
						.set((float) Math.cos(angleY), 0f,
								(float) Math.sin(angleY), 0f, 1f, 0f,
								(float) -Math.sin(angleY), 0f,
								(float) Math.cos(angleY));
			} else {
				data.getM2()
						.set((float) Math.cos(angleX), 0f,
								(float) Math.sin(angleX), 0f, 1f, 0f,
								(float) -Math.sin(angleX), 0f,
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

			data.setSensors(data.getSensorMgr().getSensorList(
					Sensor.TYPE_ACCELEROMETER));
			if (data.getSensors().size() > 0) {
				data.setSensorGrav(data.getSensors().get(0));
			}

			data.setSensors(data.getSensorMgr().getSensorList(
					Sensor.TYPE_MAGNETIC_FIELD));
			if (data.getSensors().size() > 0) {
				data.setSensorMag(data.getSensors().get(0));
			}

			data.getSensorMgr().registerListener(this, data.getSensorGrav(),
					SENSOR_DELAY_GAME);
			data.getSensorMgr().registerListener(this, data.getSensorMag(),
					SENSOR_DELAY_GAME);

			try {

				final GeomagneticField gmf = new GeomagneticField(
						(float) getMixContext().data.getCurLoc().getLatitude(),
						(float) getMixContext().data.getCurLoc().getLongitude(),
						(float) getMixContext().data.getCurLoc().getAltitude(),
						System.currentTimeMillis());

				angleY = Math.toRadians(-gmf.getDeclination());
				data.getM4()
						.set((float) Math.cos(angleY), 0f,
								(float) Math.sin(angleY), 0f, 1f, 0f,
								(float) -Math.sin(angleY), 0f,
								(float) Math.cos(angleY));
				getMixContext().data.setDeclination(-gmf.getDeclination());
			} catch (Exception ex) {
				Log.d("Open Album", "GPS Initialize Error", ex);
			}
			// data.setDownloadThread(new Thread(getMixContext().data
			// .getDownloadManager()));
			// data.getDownloadThread().start();
			// refreshDownload();
		} catch (Exception ex) {
			doError(ex);
			try {
				if (data.getSensorMgr() != null) {
					data.getSensorMgr().unregisterListener(this,
							data.getSensorGrav());
					data.getSensorMgr().unregisterListener(this,
							data.getSensorMag());
					// sensorMgr = null;
				}

			} catch (Exception ignore) {
				Log.e(TAG, ignore.getMessage());
			}
		}

		Log.d("-------------------------------------------", "resume");
		if (getDataView().isFrozen() && data.getSearchNotificationTxt() == null) {
			debugFrozen();
		} else if (!getDataView().isFrozen()
				&& data.getSearchNotificationTxt() != null) {
			data.getSearchNotificationTxt().setVisibility(View.GONE);
			// searchNotificationTxt = null;
			data.getSearchNotificationTxt().clearComposingText();
		}
		// @TODO referesh augmented
		// data.getAugScreen().refreshDrawableState();
		// setZoomLevel();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// repaint screen if other activity requested it
		if (data.getBooleanExtra("settingChanged", false)) {
			Log.d(debugTag, "MixView - Reseved Setting Change");
			repaint();
			refreshDownload();
		}
	}

	/**
	 * Clears "Events" and repaint screen. *Data are not cleared, caller wishes
	 * to clear, clear data first then call repaint.
	 * 
	 */
	public void repaint() {
		Log.d(debugTag, "repaint - called");
		getDataView().clearEvents();
		setDataView(null); // smell code practices but enforce garbage collector
							// to release data
		setDataView(new DataView(getMixContext()));
		setdWindow(new PaintScreen());
		setZoomLevel();
	}

	/**
	 * Creates zoom Seek bar on FrameLayout. Seek bar is set to invisble by
	 * default.
	 * 
	 * @param SharedPreferences
	 *            settings
	 * @return FrameLayout
	 */
	private FrameLayout createZoomBar(final SharedPreferences settings) {
		data.setMyZoomBar(new SeekBar(this));
		// myZoomBar.setVisibility(View.INVISIBLE);
		data.getMyZoomBar().setMax(100);
		data.getMyZoomBar().setProgress(settings.getInt("zoomLevel", 65));
		data.getMyZoomBar().setOnSeekBarChangeListener(
				getMyZoomBarOnSeekBarChangeListener());
		data.getMyZoomBar().setVisibility(View.INVISIBLE);

		final FrameLayout frameLayout = new FrameLayout(this);

		frameLayout.setMinimumWidth(3000);
		frameLayout.addView(data.getMyZoomBar());
		frameLayout.setPadding(10, 0, 10, 10);
		return frameLayout;
	}

	/**
	 * First Access users. display the license agreement.
	 * 
	 * @param SharedPreferences
	 *            settings
	 */
	private void firstAccess(final SharedPreferences settings) {
		final SharedPreferences.Editor editor = settings.edit();

		final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);

		builder1.setMessage(getString(R.string.license));
		builder1.setNegativeButton(getString(R.string.close_button),
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						dialog.dismiss();
					}
				});
		final AlertDialog alert1 = builder1.create();
		alert1.setTitle(getString(R.string.license_title));
		alert1.show();
		editor.putBoolean("firstAccess", true);

		// value for maximum POI for each selected OSM URL to be active
		// by default is 5
		editor.putInt("osmMaxObject", 5);
		editor.commit();

		storeDefaultSources();
	}

	/**
	 * Stores default Data Source into "Shared_Prefs"
	 */
	public void storeDefaultSources() {
		final SharedPreferences DataSourceSettings = getSharedPreferences(
				DataSourceList.SHARED_PREFS, 0);
		// setting the default values
		final SharedPreferences.Editor dataSourceEditor = DataSourceSettings
				.edit();
		dataSourceEditor
				.putString("DataSource0",
						"Wikipedia|http://api.geonames.org/findNearbyWikipediaJSON|0|0|true");
		dataSourceEditor.putString("DataSource1",
				"Twitter|http://search.twitter.com/search.json|2|0|false");
		dataSourceEditor
				.putString(
						"DataSource2",
						"OpenStreetmap|http://open.mapquestapi.com/xapi/api/0.6/node[railway=station]|3|1|false");
		dataSourceEditor
				.putString("DataSource3",
						"Panoramio|http://www.panoramio.com/map/get_panoramas.php|4|0|true");
		dataSourceEditor.commit();
	}

	@Override
	protected void onPause() {
		try {
			this.data.getmWakeLock().release();

			try {
				data.getSensorMgr().unregisterListener(this,
						data.getSensorGrav());
				data.getSensorMgr().unregisterListener(this,
						data.getSensorMag());
			} catch (Exception ignore) {
				Log.w(TAG, ignore.getMessage());
			}
		} catch (Exception ex) {
			Log.w("MixView - On pause", ex.getMessage(), ex);
			doError(ex);
		} finally {
			super.onPause();
		}

	}

	@Override
	protected void onStop() {
		Log.d(debugTag, "MixView - On Stop called");
		data.getMixContext().onStopContext();
		//data.getMixContext().stopService(getIntent());
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// downloadThread.destroy();
		Log.d(debugTag, "MixView - On Destroy called");
		data.getMixContext().onDestroyContext();
		//data.getMixContext().stopService(getIntent());
		//data.setDownloadThread(null);
		//data.setMixContext(null);
		//data = null;
		Log.d(debugTag, "MixView - onDestroy to super now");
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final int base = Menu.FIRST;
		/* define the first */
		final MenuItem item1 = menu.add(base, base, base,
				getString(R.string.menu_item_1));
		final MenuItem item2 = menu.add(base, base + 1, base + 1,
				getString(R.string.menu_item_2));
		final MenuItem item3 = menu.add(base, base + 2, base + 2,
				getString(R.string.menu_item_3));
		final MenuItem item4 = menu.add(base, base + 3, base + 3,
				getString(R.string.menu_item_4));
		final MenuItem item5 = menu.add(base, base + 4, base + 4,
				getString(R.string.menu_item_5));
		final MenuItem item6 = menu.add(base, base + 5, base + 5,
				getString(R.string.menu_item_6));
		final MenuItem item7 = menu.add(base, base + 6, base + 6,
				getString(R.string.menu_item_7));

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
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		/* Data sources */
		case 1:
			if (!getDataView().isLauncherStarted()) {
				MixListView.setList(1);
				Intent intent = new Intent(MixView.this, DataSourceList.class);
				startActivityForResult(intent, 40);
			} else {
				Toast.makeText(this, getString(R.string.option_not_available),
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
				Toast.makeText(this, R.string.empty_list, Toast.LENGTH_LONG)
						.show();
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
			final Location currentGPSInfo = getMixContext()
					.getCurrentLocation();
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.general_info_text) + "\n\n"
					+ getString(R.string.longitude)
					+ currentGPSInfo.getLongitude() + "\n"
					+ getString(R.string.latitude)
					+ currentGPSInfo.getLatitude() + "\n"
					+ getString(R.string.altitude)
					+ currentGPSInfo.getAltitude() + "m\n"
					+ getString(R.string.speed) + currentGPSInfo.getSpeed()
					+ "km/h\n" + getString(R.string.accuracy)
					+ currentGPSInfo.getAccuracy() + "m\n"
					+ getString(R.string.gps_last_fix)
					+ new Date(currentGPSInfo.getTime()).toString() + "\n");
			builder.setNegativeButton(getString(R.string.close_button),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			final AlertDialog alert = builder.create();
			alert.setTitle(getString(R.string.general_info_title));
			alert.show();
			break;
		/* Case 6: license agreements */
		case 7: // @TODO Add app Info
			final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setMessage(getString(R.string.license));
			/* Retry */
			builder1.setNegativeButton(getString(R.string.close_button),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			AlertDialog alert1 = builder1.create();
			alert1.setTitle(getString(R.string.license_title));
			alert1.show();
			break;
		default:
			// do nothing
			break;

		}
		return true;
	}

	public float calcZoomLevel() {

		final int myZoomLevel = data.getMyZoomBar().getProgress();
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

	/* ********* Handlers ************ */

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

	private void doMixSearch(final String query) {
		final DataHandler jLayer = getDataView().getDataHandler();
		if (!getDataView().isFrozen()) {
			MixListView.originalMarkerList = jLayer.getMarkerList();
			MixMap.originalMarkerList = jLayer.getMarkerList();
		}

		final ArrayList<Marker> searchResults = new ArrayList<Marker>();
		Log.d("SEARCH-------------------0", query);
		if (jLayer.getMarkerCount() > 0) {
			for (int i = 0; i < jLayer.getMarkerCount(); i++) {
				final Marker ma = jLayer.getMarker(i);
				if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1) {
					searchResults.add(ma);
					/* the website for the corresponding title */
				}
			}
		}
		if (searchResults.size() > 0) {
			getDataView().setFrozen(true);
			jLayer.setMarkerList(searchResults);
		} else {
			Toast.makeText(this,
					getString(R.string.search_failed_notification),
					Toast.LENGTH_LONG).show();
		}
	}

	public void setErrorDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.connection_error_dialog));
		builder.setCancelable(false);

		/* Retry */
		builder.setPositiveButton(R.string.connection_error_dialog_button1,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						data.setfError(false); // ?
						// TODO improve
						try {
							maintainView();
							repaint();
							refreshDownload();
						} catch (Exception ex) {
							// Don't call doError, it will be a recursive call.
							// doError(ex);
							Log.d(TAG, ex.getMessage(), ex.fillInStackTrace());
						}
					}
				});
		/* Open settings */
		builder.setNeutralButton(R.string.connection_error_dialog_button2,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent1 = new Intent(
								Settings.ACTION_WIRELESS_SETTINGS);
						startActivityForResult(intent1, 42);
					}
				});
		/* Close application */
		builder.setNegativeButton(R.string.connection_error_dialog_button3,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// System.exit(0);
						finish();

					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void doError(final Exception ex1) {
		if (!data.isfError()) {
			data.setfError(true); // ?
			setErrorDialog();
			Log.d(TAG, ex1.getMessage(), ex1.fillInStackTrace());// @debug
		}

		try {
			getAugScreen().invalidate();
		} catch (Exception ignore) {
			Log.d(TAG, ignore.getMessage(), ignore.fillInStackTrace());// @debug
		}
	}

	public void killOnError() throws Exception {
		if (data.isfError()) {
			throw new Exception();
		}
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

			SensorManager.getRotationMatrix(data.getRTmp(), data.getI(),
					data.getGrav(), data.getMag());

			final int rotation = Compatibility.getRotation(this);

			if (rotation == 1) {
				SensorManager.remapCoordinateSystem(data.getRTmp(),
						SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z,
						data.getRot());
			} else {
				SensorManager.remapCoordinateSystem(data.getRTmp(),
						SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_Z,
						data.getRot());
			}
			data.getTempR().set(data.getRot()[0], data.getRot()[1],
					data.getRot()[2], data.getRot()[3], data.getRot()[4],
					data.getRot()[5], data.getRot()[6], data.getRot()[7],
					data.getRot()[8]);

			data.getFinalR().toIdentity();
			data.getFinalR().prod(data.getM4());
			data.getFinalR().prod(data.getM1());
			data.getFinalR().prod(data.getTempR());
			data.getFinalR().prod(data.getM3());
			data.getFinalR().prod(data.getM2());
			data.getFinalR().invert();

			data.getHistR()[data.getrHistIdx()].set(data.getFinalR());
			data.setrHistIdx(data.getrHistIdx() + 1);
			if (data.getrHistIdx() >= data.getHistR().length) {
				data.setrHistIdx(0);
			}

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

	/**
	 * For Debugging purposes
	 */
	private void debugFrozen() {
		data.setSearchNotificationTxt(new TextView(this));
		data.getSearchNotificationTxt().setWidth(getdWindow().getWidth());
		data.getSearchNotificationTxt().setPadding(10, 2, 0, 0);
		data.getSearchNotificationTxt().setText(
				getString(R.string.search_active_1) + " "
						+ DataSourceList.getDataSourcesStringList()
						+ getString(R.string.search_active_2));
		data.getSearchNotificationTxt().setBackgroundColor(Color.DKGRAY);
		data.getSearchNotificationTxt().setTextColor(Color.WHITE);

		data.getSearchNotificationTxt().setOnTouchListener(this);
		addContentView(data.getSearchNotificationTxt(), new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		try {
			killOnError();

			final float xPress = me.getX();
			final float yPress = me.getY();
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

	/* ********** Getters and Setters *********** */
	public boolean isZoombarVisible() {
		return data.getMyZoomBar() != null
				&& data.getMyZoomBar().getVisibility() == View.VISIBLE;
	}

	private void setZoomLevel() {
		final float myout = calcZoomLevel();

		getDataView().setRadius(myout);

		// data.getMyZoomBar().setVisibility(View.INVISIBLE);
		data.setZoomLevel(String.valueOf(myout));

	}

	/**
	 * Refreshes Download
	 */
	private void refreshDownload() {
		Log.d(debugTag, "MixView - refreshing Download");
		getDataView().doStart();
		getDataView().clearEvents();
		try {
			if (data.getDownloadThread() != null) {

				if (!data.getDownloadThread().isInterrupted()) {
					Log.d(debugTag, "MixView - refreshing Download - interrputing thread");
					data.getDownloadThread().interrupt();
					getMixContext().data.getDownloadManager().restart();
				}

			} else { //if no download thread found
				Log.d(debugTag, "MixView - refreshing Download - creating new one");
				data.setDownloadThread(new Thread(getMixContext().data
						.getDownloadManager()));
				getMixContext().data.getDownloadManager().startThread();
			}
		} catch (Exception ex) {
			Log.w(TAG, ex.getMessage(), ex.fillInStackTrace());
		}
	}

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
	 * @param dWindow
	 *            the dWindow to set
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
	 * @param dataView
	 *            the dataView to set
	 */
	public static void setDataView(DataView dataView) {
		MixView.dataView = dataView;
	}

	/**
	 * @return the myZoomBarOnSeekBarChangeListener
	 */
	public OnSeekBarChangeListener getMyZoomBarOnSeekBarChangeListener() {
		return myZoomBarOnSeekBarChangeListener;
	}

	/**
	 * @param myZoomBarOnSeekBarChangeListener
	 *            the myZoomBarOnSeekBarChangeListener to set
	 */
	public void setMyZoomBarOnSeekBarChangeListener(
			OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener) {
		this.myZoomBarOnSeekBarChangeListener = myZoomBarOnSeekBarChangeListener;
	}
}
