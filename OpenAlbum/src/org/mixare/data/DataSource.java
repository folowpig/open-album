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

package org.mixare.data;

import org.mixare.R;

import android.app.Activity;
//import android.content.Intent; //@del
//import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The DataSource class is able to create the URL where the information about a
 * place can be found.
 * 
 * @author hannes
 * 
 */
public class DataSource extends Activity {
	
	
	private String name;
	private String url;
	public enum TYPE { WIKIPEDIA, TWITTER, OSM, PANORAMIO, MIXARE,OPENSTREETMAP,BUZZ }; //@TODO add panoramio
	public enum DISPLAY { CIRCLE_MARKER, NAVIGATION_MARKER, THUMBNAILS };
	private boolean isenabled;
	private TYPE type;
	private DISPLAY display;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.datasourcedetails);
		final EditText nameField = (EditText) findViewById(R.id.name);
		final EditText urlField = (EditText) findViewById(R.id.url);
		final Spinner typeSpinner = (Spinner) findViewById(R.id.type);
		final Spinner displaySpinner = (Spinner) findViewById(R.id.displaytype);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			if (extras.containsKey("DataSourceId")) {
				SharedPreferences settings = getSharedPreferences(DataSourceList.SHARED_PREFS, 0);
				String fields[] = settings.getString("DataSource" + extras.getInt("DataSourceId"), "").split("\\|", -1);
				nameField.setText(fields[0], TextView.BufferType.EDITABLE);
				urlField.setText(fields[1], TextView.BufferType.EDITABLE);
				typeSpinner.setSelection(Integer.parseInt(fields[2])-3);
				displaySpinner.setSelection(Integer.parseInt(fields[3]));
			}
		}

	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			final EditText nameField = (EditText) findViewById(R.id.name);
			String name = nameField.getText().toString();
			final EditText urlField = (EditText) findViewById(R.id.url);
			String url = urlField.getText().toString();
			final Spinner typeSpinner = (Spinner) findViewById(R.id.type);
			int typeId = (int) typeSpinner.getItemIdAtPosition(typeSpinner.getSelectedItemPosition());
			final Spinner displaySpinner = (Spinner) findViewById(R.id.displaytype);
			int displayId = (int) displaySpinner.getItemIdAtPosition(displaySpinner.getSelectedItemPosition());

			//TODO: fix the weird hack for type!
			DataSource newDS = new DataSource(name, url, typeId+3, displayId, true);

			SharedPreferences settings = getSharedPreferences(DataSourceList.SHARED_PREFS, 0);
			SharedPreferences.Editor editor = settings.edit();
			int index = settings.getAll().size();
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				if (extras.containsKey("DataSourceId")) {
					index = extras.getInt("DataSourceId");
				}
			}
			editor.putString("DataSource"+index, newDS.serialize());
			editor.commit();
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int base = Menu.FIRST;
		menu.add(base, base, base, R.string.cancel);
		return super.onCreateOptionsMenu(menu);

	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item){
		switch(item.getItemId()){
		case Menu.FIRST:
			finish();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	public DataSource() {
		
	}
	
	public DataSource(String name, String url, TYPE type, DISPLAY display, boolean enabled) {
		this.name = name;
		this.url = url;
		this.type = type;
		this.display = display;
		this.isenabled = enabled;
		Log.d("mixare", "New Datasource!" + name +" "+url+" "+type+" "+display+" "+ enabled);
	}
	
	public DataSource(String name, String url, int typeInt, int displayInt, boolean enabled) {
		TYPE typeEnum = TYPE.valueOf(name.toUpperCase());
		DISPLAY displayEnum = DISPLAY.values()[displayInt];
		this.name = name;
		this.url = url;
		this.type = typeEnum;
		this.display = displayEnum;
		this.isenabled = enabled;
	}
	public DataSource(String name, String url, String typeString, String displayString, String enabledString) {
		TYPE typeEnum = TYPE.valueOf(name.toUpperCase());  
		DISPLAY displayEnum = DISPLAY.values()[Integer.parseInt(displayString)];
		Boolean enabledBool = Boolean.parseBoolean(enabledString);
		this.name = name;
		this.url = url;
		this.type = typeEnum;
		this.display = displayEnum;
		this.isenabled = enabledBool;
	}

	public String createRequestParams(double lat, double lon, double alt, float radius,String locale) {
		String ret="";
		if (!ret.startsWith("file://")) {
			switch(this.type) {
			
			case WIKIPEDIA: 
				float geoNamesRadius = radius > 20 ? 20 : radius; //Free service limited to 20km
				ret+=
				"?lat=" + lat +
				"&lng=" + lon + 
				"&radius="+ geoNamesRadius +
				"&maxRows=50" +
				"&lang=" + locale+
				"&username=devopenalbum"; //this.getString(R.string.GeonamesUsername);
			break;
			
			case BUZZ: 
				ret+= 
				"&lat=" + lat+
				"&lon=" + lon + 
				"&radius="+ radius*1000;
			break;
			
			case TWITTER: 
				ret+=
				"?geocode=" + lat + "%2C" + lon + "%2C" + 
				Math.max(radius, 1.0) + "km" ;				
			break;
			
			case MIXARE:
				ret+=
				"?latitude=" + Double.toString(lat) + 
				"&longitude=" + Double.toString(lon) + 
				"&altitude=" + Double.toString(alt) +
				"&radius=" + Double.toString(radius);
			break;
			
			case OSM: case OPENSTREETMAP: 
				ret+= XMLHandler.getOSMBoundingBox(lat, lon, radius);
			break;
			case PANORAMIO: 
				float minLong = (float) (lon-radius/100.0);
				float minLat = (float) (lat-radius/100.0);
				float maxLong = (float) (lon+radius/100.0);
				float maxLat = (float) (lat+radius/100.0);
				ret+= "?set=public&from=0&to=20&minx="+ minLong + 
				"&miny="+ minLat+ 
				"&maxx=" + maxLong +
				"&maxy=" + maxLat + 
				"&size=thumbnail&mapfilter=true";
			}
			
		}
		
		return ret;
	}
	
	/******** Getters and Setters *******/
	public int getColor() {
		int ret;
		switch(this.type) {
			case BUZZ:		ret=Color.rgb(4, 228, 20); break;
			case TWITTER:	ret=Color.rgb(50, 204, 255); break;
			case WIKIPEDIA:	ret=Color.RED; break;
			case PANORAMIO: ret=Color.GRAY; break;
			default:		ret=Color.WHITE; break;
		}
		return ret;
	}

	public int getDataSourceIcon() {
		int ret;
		switch(this.type) {
			case BUZZ:		
				ret=R.drawable.buzz; 
				break;
			case TWITTER:	
				ret=R.drawable.twitter; 
				break;
			case OSM:		
				ret=R.drawable.osm;
				break;
			 case OPENSTREETMAP:
					ret=R.drawable.osm;
					break;
			case WIKIPEDIA:	
				ret=R.drawable.wikipedia; 
				break;
			case PANORAMIO:	
				ret=R.drawable.panoramio; 
				break;
			default:		
				ret=R.drawable.ic_launcher; 
				break;
		}
		return ret;
	}
	
	public int getDisplayId() {
		return this.display.ordinal();
	}
	
	public int getTypeId() {
		return this.type.ordinal();
	}
	
	public DISPLAY getDisplay() {
		return this.display;
	}
	
	public TYPE getType() {
		return this.type;
	}
	
	public boolean getEnabled() {
		return this.isenabled;
	}

	public String getName() {
		return this.name;
	}

	public String getUrl() {
		return this.url;
	}
	
	public String serialize() {
		return this.getName() + "|"
		+ this.getUrl() + "|"
		+ this.getTypeId() + "|"
		+ this.getDisplayId() + "|"
		+ this.getEnabled();
	}
	public void setEnabled(boolean isChecked) {
		this.isenabled = isChecked;
	}
	
}
