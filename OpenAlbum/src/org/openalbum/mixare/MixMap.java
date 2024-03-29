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

import java.util.ArrayList;
import java.util.List;

import org.openalbum.mixare.data.DataHandler;
import org.openalbum.mixare.data.DataSourceList;
import org.openalbum.mixare.marker.Marker;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * This class creates the map view and its overlay. It also adds an overlay with
 * the markers to the map.
 */
public class MixMap extends MapActivity implements OnTouchListener {

	private static List<Overlay> mapOverlays;
	private Drawable drawable;

	private static List<Marker> markerList;
	private static DataView dataView;
	private static GeoPoint startPoint;

	private MixContext mixContext;
	private MapView mapView;
	private static final String debugTag = "WorkFlow";

	//static MixMap map; // ???!! this !!
	private static Context thisContext;
	private static TextView searchNotificationTxt;
	public static List<Marker> originalMarkerList;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(debugTag, "on Create - MapView");
		dataView = MixView.getDataView();
		mixContext = dataView.getContext();
		setMarkerList(dataView.getDataHandler().getMarkerList());
		//map = this;

		setMapContext(this);
		mapView = new MapView(this, this.getString(R.string.GoogleMapAPIKey)); // @devKey
		// mapView= new MapView(this, "Your Google API Key");
		mapView.setBuiltInZoomControls(true);
		mapView.setClickable(true);
		mapView.setSatellite(true);
		mapView.setEnabled(true);

		this.setContentView(mapView);

		setStartPoint();
		createOverlay();

		//Debug
		if (dataView.isFrozen()) {
			searchNotificationTxt = new TextView(this);
			searchNotificationTxt.setWidth(MixView.getdWindow().getWidth());
			searchNotificationTxt.setPadding(10, 2, 0, 0);
			searchNotificationTxt.setText(getString(R.string.search_active_1)
					+ " " + DataSourceList.getDataSourcesStringList()
					+ getString(R.string.search_active_2));
			searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
			searchNotificationTxt.setTextColor(Color.WHITE);

			searchNotificationTxt.setOnTouchListener(this);
			addContentView(searchNotificationTxt, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}

	public void setStartPoint() {
		final Location location = mixContext.getCurrentLocation();
		MapController controller;

		final double latitude = location.getLatitude() * 1E6;
		final double longitude = location.getLongitude() * 1E6;

		controller = mapView.getController();
		startPoint = new GeoPoint((int) latitude, (int) longitude);
		controller.setCenter(startPoint);
		controller.setZoom(15);
	}

	public void createOverlay() {
		mapOverlays = mapView.getOverlays();//@TODO static mapping
		OverlayItem item;
		drawable = this.getResources().getDrawable(R.drawable.icon_map);
		final MixOverlay mixOverlay = new MixOverlay(this, drawable);

		for (final Marker marker : markerList) {
			if (marker.isActive()) {
				final GeoPoint point = new GeoPoint(
						(int) (marker.getLatitude() * 1E6),
						(int) (marker.getLongitude() * 1E6));
				item = new OverlayItem(point, "", "");
//				if (marker.equals(ImageMarker.class)){
//					this.drawable = new BitmapDrawable(((ImageMarker) marker).getImage());
//				}
				mixOverlay.addOverlay(item);
			}
		}
		// Solved issue 39: only one overlay with all marker instead of one
		// overlay for each marker
		mapOverlays.add(mixOverlay);

		MixOverlay myOverlay;
		drawable = this.getResources().getDrawable(R.drawable.loc_icon);
		myOverlay = new MixOverlay(this, drawable);

		item = new OverlayItem(startPoint, "Your Position", "");
		myOverlay.addOverlay(item);
		mapOverlays.add(myOverlay);
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu) {
		final int base = Menu.FIRST;
		/* define the first */
		final MenuItem item1 = menu.add(base, base, base,
				getString(R.string.map_menu_normal_mode));
		final MenuItem item2 = menu.add(base, base + 1, base + 1,
				getString(R.string.map_menu_satellite_mode));
		final MenuItem item3 = menu.add(base, base + 2, base + 2,
				getString(R.string.map_my_location));
		final MenuItem item4 = menu.add(base, base + 3, base + 3,
				getString(R.string.menu_item_2));
		final MenuItem item5 = menu.add(base, base + 4, base + 4,
				getString(R.string.map_menu_cam_mode));

		/* assign icons to the menu items */
		item1.setIcon(android.R.drawable.ic_menu_gallery);
		item2.setIcon(android.R.drawable.ic_menu_mapmode);
		item3.setIcon(android.R.drawable.ic_menu_mylocation);
		item4.setIcon(android.R.drawable.ic_menu_view);
		item5.setIcon(android.R.drawable.ic_menu_camera);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		/* Satellite View */
		case 1:
			mapView.setSatellite(false);
			break;
		/* street View */
		case 2:
			mapView.setSatellite(true);
			break;
		/* go to users location */
		case 3:
			setStartPoint();
			break;
		/* List View */
		case 4:
			createListView();
			//finish(); //doning close this activity -return to it instead
			break;
		/* back to Camera View */
		case 5:
			closeMapViewActivity();
			break;
		}
		return true;
	}

	/**
	 * 
	 */
	private void closeMapViewActivity() {
		Intent closeAndRelauch = new Intent();
		closeAndRelauch.putExtra("settingChanged", false);
		setResult(RESULT_OK, closeAndRelauch);
		finish();
	}

	public void createListView() {
		MixListView.setList(2);
		if (dataView.getDataHandler().getMarkerCount() > 0) {
			final Intent intent1 = new Intent(MixMap.this, MixListView.class);
			startActivity(intent1);
		}
		/* if the list is empty */
		else {
			Toast.makeText(this, R.string.empty_list,
					Toast.LENGTH_LONG).show();
		}
	}

	// public static ArrayList<Marker> getMarkerList(){
	// return markerList;
	// }

	public void setMarkerList(final List<Marker> maList) {
		markerList = maList;
	}

	public DataView getDataView() {
		return dataView;
	}

	// public static void setDataView(DataView view){
	// dataView= view;
	// }

	// public static void setMixContext(MixContext context){
	// ctx= context;
	// }
	//
	// public static MixContext getMixContext(){
	// return ctx;
	// }

	public List<Overlay> getMapOverlayList() {
		return mapOverlays;
	}

	public void setMapContext(Context context) {
		thisContext = context;
	}

	public Context getMapContext() {
		return thisContext;
	}

	public void startPointMsg() {
		Toast.makeText(getMapContext(), R.string.map_current_location_click,
				Toast.LENGTH_LONG).show();
	}

	private void handleIntent(final Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	@Override
	public void onNewIntent(final Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void doMixSearch(final String query) {
		final DataHandler jLayer = dataView.getDataHandler();
		if (!dataView.isFrozen()) {
			originalMarkerList = jLayer.getMarkerList();
			MixListView.originalMarkerList = jLayer.getMarkerList();
		}
		markerList = new ArrayList<Marker>();

		for (int i = 0; i < jLayer.getMarkerCount(); i++) {
			final Marker ma = jLayer.getMarker(i);

			if (ma.getTitle().toLowerCase().indexOf(query.toLowerCase()) != -1) {
				markerList.add(ma);
			}
		}
		if (markerList.size() == 0) {
			Toast.makeText(this,
					getString(R.string.search_failed_notification),
					Toast.LENGTH_LONG).show();
		} else {
			jLayer.setMarkerList(markerList);
			dataView.setFrozen(true);

			finish();
			final Intent intent1 = new Intent(this, MixMap.class);
			startActivityForResult(intent1, 42);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}else if (keyCode == KeyEvent.KEYCODE_MENU){
				openOptionsMenu();
			} else {
				return false;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		
		}
		return true;
	}
	public boolean onTouch(final View v, final MotionEvent event) {
		dataView.setFrozen(false);
		dataView.getDataHandler().setMarkerList(originalMarkerList);

		searchNotificationTxt.setVisibility(View.INVISIBLE);
		searchNotificationTxt = null;
		finish();
		final Intent intent1 = new Intent(this, MixMap.class);
		startActivityForResult(intent1, 42);

		return false;
	}

}

class MixOverlay extends ItemizedOverlay<OverlayItem> {

	private final ArrayList<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
	private final MixMap mixMap;

	public MixOverlay(final MixMap mixMap, final Drawable marker) {
		super(boundCenterBottom(marker));
		// need to call populate here. See
		// http://code.google.com/p/android/issues/detail?id=2035
		populate();
		this.mixMap = mixMap;
	}

	@Override
	protected OverlayItem createItem(final int i) {
		return overlayItems.get(i);
	}

	@Override
	public int size() {
		return overlayItems.size();
	}

	@Override
	protected boolean onTap(final int index) {
		if (size() == 1) {
			mixMap.startPointMsg();
		} else if (mixMap.getDataView().getDataHandler().getMarker(index)
				.getURL() != null) {
			final String url = mixMap.getDataView().getDataHandler()
					.getMarker(index).getURL();
			Log.d("MapView", "opern url: " + url);
			try {
				if (url != null && url.startsWith("webpage")) {
					final String newUrl = MixUtils.parseAction(url);
					mixMap.getDataView().getContext()
							.loadWebPage(newUrl, mixMap.getMapContext());
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	public void addOverlay(final OverlayItem overlay) {
		overlayItems.add(overlay);
		populate();
	}
}
