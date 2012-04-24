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

package org.openalbum.mixare.marker;

import java.text.DecimalFormat;

import org.openalbum.mixare.MixUtils;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.gui.PaintScreen;
import org.openalbum.mixare.gui.TextObj;

import android.graphics.Color;
import android.graphics.Path;
import android.location.Location;

/**
 * This markers represent the points of interest. On the screen they appear as
 * circles, since this class inherits the draw method of the Marker.
 * 
 * @author hannes
 * 
 */
public class POIMarker extends Marker {

	public static final int MAX_OBJECTS = 20;
	public static final int OSM_URL_MAX_OBJECTS = 5;

	public POIMarker(final String title, final double latitude,
			final double longitude, final double altitude, final String URL,
			final DataSource datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);

	}

	@Override
	public void update(final Location curGPSFix) {
		super.update(curGPSFix);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

	@Override
	public void drawCircle(final PaintScreen dw) {
		if (isVisible) {
			final float maxHeight = dw.getHeight();
			dw.setStrokeWidth(maxHeight / 100f);
			dw.setFill(false);

			dw.setColor(getColour());

			// draw circle with radius depending on distance
			// 0.44 is approx. vertical fov in radians
			final double angle = 2.0 * Math.atan2(10, distance);
			final double radius = Math.max(
					Math.min(angle / 0.44 * maxHeight, maxHeight),
					maxHeight / 25f);

			/*
			 * distance 100 is the threshold to convert from circle to another
			 * shape
			 */
			if (distance < 100.0) {
				otherShape(dw);
			} else {
				dw.paintCircle(cMarker.x, cMarker.y, (float) radius);
			}

		}
	}

	@Override
	public void drawTextBlock(final PaintScreen dw) {
		final float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
		// TODO: change textblock only when distance changes

		String textStr = "";

		double d = distance;
		final DecimalFormat df = new DecimalFormat("@#");
		if (d < 1000.0) {
			textStr = title + " (" + df.format(d) + "m)";
		} else {
			d = d / 1000.0;
			textStr = title + " (" + df.format(d) + "km)";
		}

		textBlock = new TextObj(textStr, Math.round(maxHeight / 2f) + 1, 250,
				dw, underline);

		if (isVisible) {
			// based on the distance set the colour
			if (distance < 100.0) {
				textBlock.setBgColor(Color.argb(128, 52, 52, 52));
				textBlock.setBorderColor(Color.rgb(255, 104, 91));
			} else {
				textBlock.setBgColor(Color.argb(128, 0, 0, 0));
				textBlock.setBorderColor(Color.rgb(255, 255, 255));
			}
			// dw.setColor(DataSource.getColor(type));

			final float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
					signMarker.x, signMarker.y);
			txtLab.prepare(textBlock);
			dw.setStrokeWidth(1f);
			dw.setFill(true);
			dw.paintObj(txtLab, signMarker.x - txtLab.getWidth() / 2,
					signMarker.y + maxHeight, currentAngle + 90, 1);

		}
	}

	public void otherShape(final PaintScreen dw) {
		// This is to draw new shape, triangle
		final float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);
		final float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		dw.setColor(getColour());
		final float radius = maxHeight / 1.5f;
		dw.setStrokeWidth(dw.getHeight() / 100f);
		dw.setFill(false);

		final Path tri = new Path();
		final float x = 0;
		final float y = 0;
		tri.moveTo(x, y);
		tri.lineTo(x - radius, y - radius);
		tri.lineTo(x + radius, y - radius);

		tri.close();
		dw.paintPath(tri, cMarker.x, cMarker.y, radius * 2, radius * 2,
				currentAngle + 90, 1);
	}

}
