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

package org.openalbum.mixare.data;

import java.util.ArrayList;
import java.util.List;

import org.openalbum.mixare.MixView;
import org.openalbum.mixare.marker.Marker;
import org.openalbum.mixare.marker.NavigationMarker;
import org.openalbum.mixare.marker.POIMarker;
import org.openalbum.mixare.reality.PhysicalPlace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

/**
 * @author hannes
 * 
 */
public class XMLHandler extends DataHandler {

	private List<Marker> processOSM(final Element root,
			final DataSource datasource) {

		final List<Marker> markers = new ArrayList<Marker>();
		final NodeList nodes = root.getElementsByTagName("node");

		for (int i = 0; i < nodes.getLength(); i++) {
			final Node node = nodes.item(i);
			final NamedNodeMap att = node.getAttributes();
			final NodeList tags = node.getChildNodes();
			for (int j = 0; j < tags.getLength(); j++) {
				final Node tag = tags.item(j);
				if (tag.getNodeType() != Node.TEXT_NODE) {
					final String key = tag.getAttributes().getNamedItem("k")
							.getNodeValue();
					if (key.equals("name")) {

						final String name = tag.getAttributes()
								.getNamedItem("v").getNodeValue();
						final double lat = Double.valueOf(att.getNamedItem(
								"lat").getNodeValue());
						final double lon = Double.valueOf(att.getNamedItem(
								"lon").getNodeValue());

						Log.v(MixView.TAG, "OSM Node: " + name + " lat " + lat
								+ " lon " + lon + "\n");

						// This check will be done inside the createMarker
						// method
						// if(markers.size()<MAX_OBJECTS)
						if (datasource.getDisplay() == DataSource.DISPLAY.CIRCLE_MARKER) {
							final Marker ma = new POIMarker(name, lat, lon, 0,
									"http://www.openstreetmap.org/?node="
											+ att.getNamedItem("id")
													.getNodeValue(), datasource);
							markers.add(ma);
						} else {
							final Marker ma = new NavigationMarker(name, lat,
									lon, 0,
									"http://www.openstreetmap.org/?node="
											+ att.getNamedItem("id")
													.getNodeValue(), datasource);
							markers.add(ma);
						}

						// skip to next node
						continue;
					}
				}
			}
		}
		return markers;
	}

	public static String getOSMBoundingBox(final double lat, final double lon,
			final double radius) {
		String bbox = "[bbox=";
		final PhysicalPlace lb = new PhysicalPlace(); // left bottom
		final PhysicalPlace rt = new PhysicalPlace(); // right top
		PhysicalPlace.calcDestination(lat, lon, 225, radius * 1414, lb); // 1414:
																			// sqrt(2)*1000
		PhysicalPlace.calcDestination(lat, lon, 45, radius * 1414, rt);
		bbox += lb.getLongitude() + "," + lb.getLatitude() + ","
				+ rt.getLongitude() + "," + rt.getLatitude() + "]";
		return bbox;

		// return "[bbox=16.365,48.193,16.374,48.199]";
	}

	public List<Marker> load(final Document doc, final DataSource datasource) {
		final Element root = doc.getDocumentElement();

		// If the root tag is called "osm" we got an
		// openstreetmap .osm xml document
		if ("osm".equals(root.getTagName())) {
			return processOSM(root, datasource);
		}
		return null;
	}

}
