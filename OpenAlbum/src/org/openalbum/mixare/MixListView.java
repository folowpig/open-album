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
import java.util.Vector;

import org.openalbum.mixare.data.DataHandler;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.data.DataSourceList;
import org.openalbum.mixare.marker.Marker;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class holds vectors with informaction about sources, their description
 * and whether they have been selected.
 * 
 * @TODO Performance improvement
 */
public class MixListView extends ListActivity {

	private static int list;

	private Vector<SpannableString> listViewMenu;
	private Vector<String> selectedItemURL;
	private Vector<String> dataSourceMenu;
	private Vector<String> dataSourceDescription;
	private Vector<Boolean> dataSourceChecked;
	private Vector<Integer> dataSourceIcon;

	private MixContext mixContext;

	private DataView dataView;
	// private static String selectedDataSource = "Wikipedia";
	/* to check which data source is active */
	// private int clickedDataSourceItem = 0;
	private ListItemAdapter adapter;
	public static String customizedURL = "http://mixare.org/geotest.php";
	private static Context ctx;
	private static String searchQuery = "";
	private static SpannableString underlinedTitle;
	public static List<Marker> searchResultMarkers;
	public static List<Marker> originalMarkerList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mixCtx = MixView.ctx;
		dataView = MixView.getDataView();
		ctx = this;
		setMixContext(dataView.getContext());

		switch (list) {
		case 1:

			adapter = new ListItemAdapter(this);
			// adapter.colorSource(getDataSource());
			getListView().setTextFilterEnabled(true);

			setListAdapter(adapter);
			break;

		case 2:
			selectedItemURL = new Vector<String>();
			listViewMenu = new Vector<SpannableString>();
			final DataHandler jLayer = dataView.getDataHandler();
			if (dataView.isFrozen() && jLayer.getMarkerCount() > 0) {
				selectedItemURL.add("search");
			}
			/* add all marker items to a title and a URL Vector */
			for (int i = 0; i < jLayer.getMarkerCount(); i++) {
				final Marker ma = jLayer.getMarker(i);
				if (ma.isActive()) {
					if (ma.getURL() != null) {
						/* Underline the title if website is available */
						underlinedTitle = new SpannableString(ma.getTitle());
						underlinedTitle.setSpan(new UnderlineSpan(), 0,
								underlinedTitle.length(), 0);
						listViewMenu.add(underlinedTitle);
					} else {
						listViewMenu.add(new SpannableString(ma.getTitle()));
					}
					/* the website for the corresponding title */
					if (ma.getURL() != null) {
						selectedItemURL.add(ma.getURL());
						/* if no website is available for a specific title */
					} else {
						selectedItemURL.add("");
					}
				}
			}

			if (dataView.isFrozen()) {

				final TextView searchNotificationTxt = new TextView(this);
				searchNotificationTxt.setVisibility(View.VISIBLE);
				searchNotificationTxt
						.setText(getString(R.string.search_active_1) + " "
								+ DataSourceList.getDataSourcesStringList()
								+ getString(R.string.search_active_2));
				searchNotificationTxt.setWidth(MixView.getdWindow().getWidth());

				searchNotificationTxt.setPadding(10, 2, 0, 0);
				searchNotificationTxt.setBackgroundColor(Color.DKGRAY);
				searchNotificationTxt.setTextColor(Color.WHITE);

				getListView().addHeaderView(searchNotificationTxt);

			}

			setListAdapter(new ArrayAdapter<SpannableString>(this,
					android.R.layout.simple_list_item_1, listViewMenu));
			getListView().setTextFilterEnabled(true);
			break;

		}
	}

	private void handleIntent(final Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String query = intent.getStringExtra(SearchManager.QUERY);
			doMixSearch(query);
		}
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void doMixSearch(final String query) {
		final DataHandler jLayer = dataView.getDataHandler();
		if (!dataView.isFrozen()) {
			originalMarkerList = jLayer.getMarkerList();
			MixMap.originalMarkerList = jLayer.getMarkerList();
		}
		originalMarkerList = jLayer.getMarkerList();
		searchResultMarkers = new ArrayList<Marker>();
		Log.d("SEARCH-------------------0", "" + query);
		setSearchQuery(query);

		selectedItemURL = new Vector<String>();
		listViewMenu = new Vector<SpannableString>();
		for (int i = 0; i < jLayer.getMarkerCount(); i++) {
			final Marker ma = jLayer.getMarker(i);

			if (ma.getTitle().toLowerCase().indexOf(searchQuery.toLowerCase()) != -1) {
				searchResultMarkers.add(ma);
				listViewMenu.add(new SpannableString(ma.getTitle()));
				/* the website for the corresponding title */
				if (ma.getURL() != null) {
					selectedItemURL.add(ma.getURL());
					/* if no website is available for a specific title */
				} else {
					selectedItemURL.add("");
				}
			}
		}
		if (listViewMenu.size() == 0) {
			Toast.makeText(this,
					getString(R.string.search_failed_notification),
					Toast.LENGTH_LONG).show();
		} else {
			jLayer.setMarkerList(searchResultMarkers);
			dataView.setFrozen(true);
			setList(2);
			finish();
			final Intent intent1 = new Intent(this, MixListView.class);
			startActivityForResult(intent1, 42);
		}
	}

	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		super.onListItemClick(l, v, position, id);
		switch (list) {
		/* Data Sources */
		case 1:
			// clickOnDataSource(position);
			final CheckBox cb = (CheckBox) v.findViewById(R.id.list_checkbox);
			cb.toggle();
			break;

		/* List View */
		case 2:
			clickOnListView(position);
			break;
		}

	}

	public void clickOnListView(final int position) {
		/* if no website is available for this item */
		final String selectedURL = position < selectedItemURL.size() ? selectedItemURL
				.get(position) : null;
		if (selectedURL == null || selectedURL.length() <= 0) {
			Toast.makeText(this, getString(R.string.no_website_available),
					Toast.LENGTH_LONG).show();
		} else if ("search".equals(selectedURL)) {
			dataView.setFrozen(false);
			dataView.getDataHandler().setMarkerList(originalMarkerList);
			setList(2);
			finish();
			final Intent intent1 = new Intent(this, MixListView.class);
			startActivityForResult(intent1, 42);
		} else {
			try {
				if (selectedURL.startsWith("webpage")) {
					final String newUrl = MixUtils.parseAction(selectedURL);
					dataView.getContext().loadWebPage(newUrl, this);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void createContextMenu(final ImageView icon) {
		icon.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			public void onCreateContextMenu(final ContextMenu menu,
					final View v, final ContextMenuInfo menuInfo) {
				final int index = 0;
				switch (ListItemAdapter.itemPosition) {
				case 0:
					menu.setHeaderTitle("Wiki Menu");
					menu.add(index, index, index, "We are working on it...");
					break;
				case 1:
					menu.setHeaderTitle("Twitter Menu");
					menu.add(index, index, index, "We are working on it...");
					break;
				case 2:
					menu.setHeaderTitle("Buzz Menu");
					menu.add(index, index, index, "We are working on it...");
					break;
				case 3:
					menu.setHeaderTitle("OpenStreetMap Menu");
					menu.add(index, index, index, "We are working on it...");
					break;
				case 4:
					final AlertDialog.Builder alert = new AlertDialog.Builder(
							ctx);
					alert.setTitle("insert your own URL:");

					final EditText input = new EditText(ctx);
					input.setText(customizedURL);
					alert.setView(input);

					alert.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									final Editable value = input.getText();
									customizedURL = "" + value;
								}
							});
					alert.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,
										final int id) {
									dialog.dismiss();
								}
							});
					alert.show();
					break;
				}
			}
		});

	}

	public void clickOnDataSource(final int position) {
		// if(dataView.isFrozen())
		// dataView.setFrozen(false);
		// switch(position){
		// /*WIKIPEDIA*/
		// case 0:
		// mixContext.toogleDataSource(DATASOURCE.WIKIPEDIA);
		// break;
		//
		// /*TWITTER*/
		// case 1:
		// mixContext.toogleDataSource(DATASOURCE.TWITTER);
		// break;
		//
		// /*BUZZ*/
		// case 2:
		// mixContext.toogleDataSource(DATASOURCE.BUZZ);
		// break;
		//
		// /*OSM*/
		// case 3:
		// mixContext.toogleDataSource(DATASOURCE.OSM);
		// break;
		//
		// /*Own URL*/
		// case 4:
		// mixContext.toogleDataSource(DATASOURCE.OWNURL);
		// break;
		// }
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final int base = Menu.FIRST;

		/* define menu items */
		final MenuItem item1 = menu.add(base, base, base,
				getString(R.string.menu_item_3));
		final MenuItem item2 = menu.add(base, base + 1, base + 1,
				getString(R.string.map_menu_cam_mode));
		final MenuItem item3 = menu.add(base, base + 2, base + 2,
				"Add data source");
		/* assign icons to the menu items */
		item1.setIcon(android.R.drawable.ic_menu_mapmode);
		item2.setIcon(android.R.drawable.ic_menu_camera);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		/* Map View */
		case 1:
			createMixMap();
			finish();
			break;
		/* back to Camera View */
		case 2:
			finish();
			break;
		case 3:
			final Intent addDataSource = new Intent(this, DataSource.class);
			startActivity(addDataSource);
			break;

		}
		return true;
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			break;
		case 2:
			break;
		case 3:
			break;
		}
		return false;
	}

	public void createMixMap() {
		final Intent intent2 = new Intent(MixListView.this, MixMap.class);
		startActivityForResult(intent2, 20);
	}

	/***************** Getters and Setters *****************/

	public static void setSearchQuery(final String query) {
		searchQuery = query;
	}

	/*
	 * public void setDataSource(String source){ selectedDataSource = source; }
	 * 
	 * public static String getDataSource(){ return selectedDataSource; }
	 */

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		try {
			
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		
		}
		return true;
	}
	public static void setList(final int l) {
		list = l;
	}

	public static String getSearchQuery() {
		return searchQuery;
	}

	/**
	 * @return the mixContext
	 */
	public MixContext getMixContext() {
		return mixContext;
	}

	/**
	 * @param mixContext
	 *            the mixContext to set
	 */
	public void setMixContext(final MixContext mixContext) {
		this.mixContext = mixContext;
	}

	public Vector<String> getDataSourceMenu() {
		return dataSourceMenu;
	}

	public Vector<String> getDataSourceDescription() {
		return dataSourceDescription;
	}

	public Vector<Boolean> getDataSourceChecked() {
		return dataSourceChecked;
	}

	public Vector<Integer> getDataSourceIcon() {
		return dataSourceIcon;
	}

}

/**
 * The ListItemAdapter is can store properties of list items, like background or
 * text color
 */
class ListItemAdapter extends BaseAdapter {

	private final MixListView mixListView;

	private final LayoutInflater myInflater;
	static ViewHolder holder;
	private final int[] bgcolors = new int[] { 0, 0, 0, 0, 0 };
	private final int[] textcolors = new int[] { Color.WHITE, Color.WHITE,
			Color.WHITE, Color.WHITE, Color.WHITE };
	private final int[] descriptioncolors = new int[] { Color.GRAY, Color.GRAY,
			Color.GRAY, Color.GRAY, Color.GRAY };

	public static int itemPosition = 0;

	public ListItemAdapter(final MixListView mixListView) {
		this.mixListView = mixListView;
		myInflater = LayoutInflater.from(mixListView);
	}

	public View getView(final int position, View convertView,
			final ViewGroup parent) {
		itemPosition = position;
		if (convertView == null) {
			convertView = myInflater.inflate(R.layout.main, null);

			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.list_text);
			holder.description = (TextView) convertView
					.findViewById(R.id.description_text);
			holder.checkbox = (CheckBox) convertView
					.findViewById(R.id.list_checkbox);
			holder.datasource_icon = (ImageView) convertView
					.findViewById(R.id.datasource_icon);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.datasource_icon.setImageResource(mixListView.getDataSourceIcon()
				.get(position));
		holder.checkbox.setChecked(mixListView.getDataSourceChecked().get(
				position));

		holder.checkbox
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					public void onCheckedChanged(
							final CompoundButton buttonView,
							final boolean isChecked) {
						mixListView.clickOnDataSource(position);
					}

				});

		holder.text.setPadding(20, 8, 0, 0);
		holder.description.setPadding(20, 40, 0, 0);

		holder.text.setText(mixListView.getDataSourceMenu().get(position));
		holder.description.setText(mixListView.getDataSourceDescription().get(
				position));

		final int colorPos = position % bgcolors.length;
		convertView.setBackgroundColor(bgcolors[colorPos]);
		holder.text.setTextColor(textcolors[colorPos]);
		holder.description.setTextColor(descriptioncolors[colorPos]);

		return convertView;
	}

	public void changeColor(final int index, final int bgcolor,
			final int textcolor) {
		if (index < bgcolors.length) {
			bgcolors[index] = bgcolor;
			textcolors[index] = textcolor;
		} else {
			Log.d("Color Error", "too large index");
		}
	}

	public void colorSource(final String source) {
		for (int i = 0; i < bgcolors.length; i++) {
			bgcolors[i] = 0;
			textcolors[i] = Color.WHITE;
		}

		if (source.equals("Wikipedia")) {
			changeColor(0, Color.WHITE, Color.DKGRAY);
		} else if (source.equals("Twitter")) {
			changeColor(1, Color.WHITE, Color.DKGRAY);
		} else if (source.equals("Buzz")) {
			changeColor(2, Color.WHITE, Color.DKGRAY);
		} else if (source.equals("OpenStreetMap")) {
			changeColor(3, Color.WHITE, Color.DKGRAY);
		} else if (source.equals("OwnURL")) {
			changeColor(4, Color.WHITE, Color.DKGRAY);
		}
	}

	public int getCount() {
		return mixListView.getDataSourceMenu().size();
	}

	public Object getItem(final int position) {
		return this;
	}

	public long getItemId(final int position) {
		return position;
	}

	private class ViewHolder {
		TextView text;
		TextView description;
		CheckBox checkbox;
		ImageView datasource_icon;
	}
}
