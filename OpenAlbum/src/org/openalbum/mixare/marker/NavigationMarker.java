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

import org.openalbum.mixare.MixUtils;
import org.openalbum.mixare.MixView;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.gui.PaintScreen;

import android.graphics.Path;
import android.location.Location;

/**
 * 
 * A NavigationMarker is displayed as an arrow at the bottom of the screen. It
 * indicates directions using the OpenStreetMap as type.
 * 
 * @author hannes
 * 
 */
public class NavigationMarker extends Marker {

	public static final int MAX_OBJECTS = 10;

	public NavigationMarker(final String title, final double latitude,
			final double longitude, final double altitude, final String URL,
			final DataSource datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);
	}

	@Override
	public void update(final Location curGPSFix) {

		super.update(curGPSFix);

		// we want the navigation markers to be on the lower part of
		// your surrounding sphere so we set the height component of
		// the position vector radius/2 (in meter) below the user

		locationVector.y -= MixView.getDataView().getRadius() * 500f;
		// locationVector.y+=-1000;
	}

	@Override
	public void draw(final PaintScreen dw) {
		drawArrow(dw);
		drawTextBlock(dw);
	}

	public void drawArrow(final PaintScreen dw) {
		if (isVisible) {
			final float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
					signMarker.x, signMarker.y);
			final float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

			// dw.setColor(DataSource.getColor(type));
			dw.setStrokeWidth(maxHeight / 10f);
			dw.setFill(false);

			final Path arrow = new Path();
			final float radius = maxHeight / 1.5f;
			final float x = 0;
			final float y = 0;
			arrow.moveTo(x - radius / 3, y + radius);
			arrow.lineTo(x + radius / 3, y + radius);
			arrow.lineTo(x + radius / 3, y);
			arrow.lineTo(x + radius, y);
			arrow.lineTo(x, y - radius);
			arrow.lineTo(x - radius, y);
			arrow.lineTo(x - radius / 3, y);
			arrow.close();
			dw.paintPath(arrow, cMarker.x, cMarker.y, radius * 2, radius * 2,
					currentAngle + 90, 1);
		}
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

}
