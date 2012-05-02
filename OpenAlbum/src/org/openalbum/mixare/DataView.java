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

/*
 *TODO's 
 *1-Photo's text title 
 *2-Fix rador accuracy with images. (optional twitter and open street)
 *3-Elevation level by location http://asterweb.jpl.nasa.gov/gdem.asp
 *4-Rador meter
 *5-View Enhancements.
 */
package org.openalbum.mixare;

import static android.view.KeyEvent.KEYCODE_CAMERA;
import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

import java.util.ArrayList;
import java.util.Locale;

import org.openalbum.mixare.data.DataHandler;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.gui.PaintScreen;
import org.openalbum.mixare.gui.RadarPoints;
import org.openalbum.mixare.gui.ScreenLine;
import org.openalbum.mixare.marker.ImageMarker;
import org.openalbum.mixare.marker.Marker;
import org.openalbum.mixare.marker.MarkerInterface;
import org.openalbum.mixare.render.Camera;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is able to update the markers and the radar. It also handles some
 * user events
 * 
 * @author daniele
 * 
 */
public class DataView {

	/** current context */
	private  MixContext mixContext;
	/** is the view Inited? */
	private boolean isInit;
	private static int  countertmp = 0;

	/** width and height of the view */
	private int width, height;
	
	/**
	 * _NOT_ the android camera, the class that takes care of the transformation
	 */
	private Camera cam;

	private final MixState state = new MixState();

	/** The view can be "frozen" for debug purposes */
	private boolean frozen;

	/** how many times to re-attempt download */
	private int retry;

	private Location curFix;
	private final DataHandler dataHandler = new DataHandler();
	private float radius = 20; 

	private boolean isLauncherStarted;
	
	private ArrayList<UIEvent> uiEvents = new ArrayList<UIEvent>();


	private final RadarPoints radarPoints = new RadarPoints();
	private final ScreenLine lrl = new ScreenLine();
	private final ScreenLine rrl = new ScreenLine();
	private final float rx = 10, ry = 20;
	private float addX = 0, addY = 0;
	private static final String debugTag = "WorkFlow";
	/**
	 * Constructor
	 */
	public DataView(MixContext ctx) {
		//final Throwable stack = new Throwable();
		Log.d(debugTag, "DataView - Created");
		this.mixContext = (MixContext) ctx;
	}

	public void init(final int widthInit, final int heightInit) {
		try {
			//final Throwable stack = new Throwable();
			Log.d(debugTag, "DataView - Created");
			width = widthInit;
			height = heightInit;

			cam = new Camera(width, height, true);
			cam.setViewAngle(Camera.DEFAULT_VIEW_ANGLE);

			lrl.set(0, -RadarPoints.getRADIUS());
			lrl.rotate(Camera.DEFAULT_VIEW_ANGLE / 2);
			lrl.add(rx + RadarPoints.getRADIUS(), ry + RadarPoints.getRADIUS());
			rrl.set(0, -RadarPoints.getRADIUS());
			rrl.rotate(-Camera.DEFAULT_VIEW_ANGLE / 2);
			rrl.add(rx + RadarPoints.getRADIUS(), ry + RadarPoints.getRADIUS());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		frozen = false;
		isInit = true;
	}

	public void requestData(final String url) {
		final DownloadRequest request = new DownloadRequest();
		request.source = new DataSource("LAUNCHER", url,
				DataSource.TYPE.MIXARE, DataSource.DISPLAY.CIRCLE_MARKER, true);
		request.params = "";
		mixContext.setAllDataSourcesforLauncher(request.source);
		mixContext.getDownloader().submitJob(request);
		state.nextLStatus = MixState.PROCESSING;

	}

	public void requestData(DataSource datasource, double lat, double lon, double alt, float radius, String locale) {
		DownloadRequest request = new DownloadRequest();
		request.params = datasource.createRequestParams(lat, lon, alt, radius, locale);
		request.source = datasource;

		mixContext.getDownloader().submitJob(request); // @FIXME logic and pano
														// don't go through
		state.nextLStatus = MixState.PROCESSING;

	}
	public void draw(PaintScreen dw) {
		mixContext.setRM(cam.transform);
		curFix = mixContext.getCurrentLocation();

		state.calcPitchBearing(cam.transform);

		
		// Load Layer
		if (state.nextLStatus == MixState.NOT_STARTED && !frozen) {
			if (mixContext.getStartUrl().length() > 0) {
				requestData(mixContext.getStartUrl());
				isLauncherStarted = true;
				
			}
			else {
				final double lat = curFix.getLatitude(), lon = curFix
						.getLongitude(), alt = curFix.getAltitude();
				final ArrayList<DataSource> allDataSources = mixContext
						.getAllDataSources();
				for (final DataSource ds : allDataSources) {
					/*
					 * when type is OpenStreetMap iterate the URL list and for
					 * selected URL send data request
					 */
					if (ds.getEnabled()) {
						requestData(ds, lat, lon, alt, radius, Locale
								.getDefault().getLanguage());
					}
				}
			}
			// if no datasources are activated
			if (state.nextLStatus == MixState.NOT_STARTED) {
				state.nextLStatus = MixState.DONE;
			}

			// TODO:
			// state.downloadId = mixContext.getDownloader().submitJob(request);

		} else if (state.nextLStatus == MixState.PROCESSING) {
			final DownloadManager dm = mixContext.getDownloader();
			DownloadResult dRes;

			while ((dRes = dm.getNextResult()) != null) {
				if (dRes.error && retry < 3) {
					retry++;
					mixContext.getDownloader().submitJob(dRes.errorRequest);
					// Notification
					// Toast.makeText(mixContext, dRes.errorMsg,
					// Toast.LENGTH_SHORT).show();
				}

				if (!dRes.error) {
					// jLayer = (DataHandler) dRes.obj;
					Log.i(MixView.TAG, "Adding Markers");
					dataHandler.addMarkers(dRes.getMarkers());
					dataHandler.onLocationChanged(curFix);
					// Notification
					Toast.makeText(
							mixContext,
							mixContext.getResources().getString(
									R.string.download_received)
									+ " " + dRes.source.getName(),
							Toast.LENGTH_SHORT).show();
					//@TODO group markers
					for (int i = dataHandler.getMarkerCount() - 1 ; i >= 0; i--) {
						final int tm = (i > 0)? i-1 : i;
						final int tm2 = (i >1)? i-2 : i;
						final Marker ma = dataHandler.getMarker(i);
						final Marker ma2 = dataHandler.getMarker(tm);
						final Marker ma3 = dataHandler.getMarker(tm2);
						if (ImageMarker.class.isInstance(ma) ){
							final double comp = MixUtils.getAngle(ma.getLongitude(), ma.getLatitude(), 
									ma2.getLongitude(), ma2.getLatitude());
							Log.d(debugTag, "DataView - Draw - before cal"+
									" i= " + String.valueOf(i) +
						" compare " + String.valueOf(comp) +
								//"dataHandler = " + String.valueOf(dataHandler.getMarkerCount()) +
								" Latitude = " + String.valueOf(ma.getLatitude()) +
								" long = " + String.valueOf(ma.getLongitude())+
								" title " + ma.getTitle());
							
							
							//if (MixUtils.getAngle(ma., center_y, post_x, post_y))
						}
					
					}
					

				}
			}
			if (dm.isDone()) {
				retry = 0;
				state.nextLStatus = MixState.DONE;
			}
		}

		// Update markers
		dataHandler.updateActivationStatus(mixContext);
		for (int i = dataHandler.getMarkerCount() - 1; i >= 0; i--) {
			final int tmp = (i > 0)? i-1: i;
			final Marker ma = dataHandler.getMarker(i);
			final Marker ma2 = dataHandler.getMarker(tmp);
			// if (ma.isActive() && (ma.getDistance() / 1000f < radius || ma
			// instanceof NavigationMarker || ma instanceof SocialMarker)) {
			if (ma.isActive() && (ma.getDistance() / 1000f < radius)) {

				// To increase performance don't recalculate position vector
				// for every marker on every draw call, instead do this only
				// after onLocationChanged and after downloading new marker
				// if (!frozen)
				// ma.update(curFix);
//				Log.d(debugTag, "DataView - Draw - calcPaint addx=" + String.valueOf(addX)
//						+ " addy= " + String.valueOf(addY));
				if (!frozen) {
					ma.calcPaint(cam, addX, addY);
					 if ( countertmp < 100){
						Log.d(debugTag, "DataView - after Calpaint" +
					" AsignMarkerX = "+ String.valueOf(ma.signMarker.x) +
					" ASignMarkerY = "+ String.valueOf(ma.signMarker.y)+
					" BsignMarkerX = "+ String.valueOf(ma2.signMarker.x) +
					" BsignMarkerY = "+ String.valueOf(ma2.signMarker.x) +
					" ACmarkerX = " + String.valueOf(ma.cMarker.x) +
					" ACmarkerY = " + String.valueOf(ma.cMarker.y) +
					" BCmarkerX = " + String.valueOf(ma2.cMarker.x) +
					" BCmarkerY = " + String.valueOf(ma2.cMarker.y) +
					" distance = " + String.valueOf(ma.getDistance()) +
					" Compare = " + String.valueOf(Marker.doubleCompareTo(ma, ma2)) +
					" title ma " + ma.getTitle() +
						" title ma2 " + ma2.getTitle());
						countertmp++;
					}
					 if (ImageMarker.class.isInstance(ma) && ImageMarker.class.isInstance(ma2)){
						 
					 }
					
				}
				ma.draw(dw);
			}
		}

		// Draw Radar
		drawRadar(dw);

		// Get next event
		UIEvent evt = null;
		synchronized (uiEvents) {
			if (uiEvents.size() > 0) {
				evt = uiEvents.get(0);
				uiEvents.remove(0);
			}
		}
		if (evt != null) {
			switch (evt.type) {
			case UIEvent.KEY:
				handleKeyEvent((KeyEvent) evt);
				break;
			case UIEvent.CLICK:
				handleClickEvent((ClickEvent) evt);
				break;
			}
		}
		state.nextLStatus = MixState.PROCESSING;
	}

	/**
	 * Draw Radar
	 */
	public void drawRadar(PaintScreen dw) {
		String	dirTxt = ""; 
		int bearing = (int) state.getCurBearing(); 
		int range = (int) (state.getCurBearing() / (360f / 16f)); 
		if (range == 15 || range == 0) dirTxt = this.getContext().getString(R.string.N); 
		else if (range == 1 || range == 2) dirTxt = this.getContext().getString(R.string.NE); 
		else if (range == 3 || range == 4) dirTxt = this.getContext().getString(R.string.E); 
		else if (range == 5 || range == 6) dirTxt = this.getContext().getString(R.string.SE);
		else if (range == 7 || range == 8) dirTxt= this.getContext().getString(R.string.S); 
		else if (range == 9 || range == 10) dirTxt = this.getContext().getString(R.string.SW); 
		else if (range == 11 || range == 12) dirTxt = this.getContext().getString(R.string.W); 
		else if (range == 13 || range == 14) dirTxt = this.getContext().getString(R.string.NW);

		if (radarPoints.view != null) {
			radarPoints.view = null;
		}
		radarPoints.view = this;
		dw.paintObj(radarPoints, rx, ry, -state.getCurBearing(), 1);
		dw.setFill(false);
		dw.setColor(Color.argb(150, 0, 0, 220));
		dw.paintLine(lrl.x, lrl.y, rx + RadarPoints.getRADIUS(), ry
				+ RadarPoints.getRADIUS());
		dw.paintLine(rrl.x, rrl.y, rx + RadarPoints.getRADIUS(), ry
				+ RadarPoints.getRADIUS());
		dw.setColor(Color.rgb(255, 255, 255));
		dw.setFontSize(12);

		radarText(dw, MixUtils.formatDist(radius * 1000),
				rx + RadarPoints.getRADIUS(), ry + RadarPoints.getRADIUS() * 2
						- 10, false);
		radarText(dw, "" + bearing + ((char) 176) + " " + dirTxt, rx
				+ RadarPoints.getRADIUS(), ry - 5, true);
	}

	private void handleKeyEvent(KeyEvent evt) {
		/** Adjust marker position with keypad */
		final float CONST = 10f;
		switch (evt.keyCode) {
		case KEYCODE_DPAD_LEFT:
			addX -= CONST;
			break;
		case KEYCODE_DPAD_RIGHT:
			addX += CONST;
			break;
		case KEYCODE_DPAD_DOWN:
			addY += CONST;
			break;
		case KEYCODE_DPAD_UP:
			addY -= CONST;
			break;
		case KEYCODE_DPAD_CENTER:
			frozen = !frozen;
			break;
		case KEYCODE_CAMERA:
			frozen = !frozen;
			break; // freeze the overlay with the camera button
		}
	}
	
	boolean handleClickEvent(ClickEvent evt) {
		boolean evtHandled = false;

		// Handle event
		if (state.nextLStatus == MixState.DONE) {
			// the following will traverse the markers in ascending order (by
			// distance) the first marker that
			// matches triggers the event.
			for (int i = 0; i < dataHandler.getMarkerCount() && !evtHandled; i++) {
				final MarkerInterface pm = dataHandler.getMarker(i);

				evtHandled = pm.fClick(evt.x, evt.y, mixContext, state);
			}
		}
		return evtHandled;
	}

	public void doStart() {
		state.nextLStatus = MixState.NOT_STARTED;
		mixContext.setLocationAtLastDownload(curFix);
	}

	private void radarText(PaintScreen dw, final String txt,
			final float x, final float y, final boolean bg) {
		final float padw = 4, padh = 2;
		final float w = dw.getTextWidth(txt) + padw * 2;
		final float h = dw.getTextAsc() + dw.getTextDesc() + padh * 2;
		if (bg) {
			dw.setColor(Color.rgb(0, 0, 0));
			dw.setFill(true);
			dw.paintRect(x - w / 2, y - h / 2, w, h);
			dw.setColor(Color.rgb(255, 255, 255));
			dw.setFill(false);
			dw.paintRect(x - w / 2, y - h / 2, w, h);
		}
		dw.paintText(padw + x - w / 2, padh + dw.getTextAsc() + y - h / 2, txt,
				false);
	}

	/**************** Getters And Setters ******************/
	public MixContext getContext() {
		return mixContext;
	}
	public void setMixContext (final MixContext mixContxt){
		mixContext = mixContxt;
	}

	public boolean isLauncherStarted() {
		return isLauncherStarted;
	}

	public boolean isFrozen() {
		return frozen;
	}

	public void setFrozen(final boolean frozen) {
		this.frozen = frozen;
	}

	public float getRadius() {
		return radius;
	}

	public void setRadius(final float radius) {
		this.radius = radius;
	}

	public DataHandler getDataHandler() {
		return dataHandler;
	}

	public boolean isDetailsView() {
		return state.isDetailsView();
	}

	public void setDetailsView(final boolean detailsView) {
		state.setDetailsView(detailsView);
	}

	public boolean isInited() {
		return isInit;
	}

	public void clickEvent(final float x, final float y) {
		synchronized (uiEvents) {
			uiEvents.add(new ClickEvent(x, y));
		}
	}

	public void keyEvent(final int keyCode) {
		synchronized (uiEvents) {
			uiEvents.add(new KeyEvent(keyCode));
		}
	}

	public void clearEvents() {
		synchronized (uiEvents) {
			uiEvents.clear();
		}
	}
}

class UIEvent {
	public static final int CLICK = 0;
	public static final int KEY = 1;

	public int type;
}

class ClickEvent extends UIEvent {
	public float x, y;

	public ClickEvent(float x, float y) {
		this.type = CLICK;
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}

class KeyEvent extends UIEvent {
	public int keyCode;

	public KeyEvent(int keyCode) {
		this.type = KEY;
		this.keyCode = keyCode;
	}

	@Override
	public String toString() {
		return "(" + keyCode + ")";
	}
}
