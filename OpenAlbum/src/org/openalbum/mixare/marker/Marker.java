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

import java.net.URLDecoder;
import java.text.DecimalFormat;

import org.openalbum.mixare.MixContext;
import org.openalbum.mixare.MixState;
import org.openalbum.mixare.MixUtils;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.gui.PaintScreen;
import org.openalbum.mixare.gui.ScreenLine;
import org.openalbum.mixare.gui.ScreenObj;
import org.openalbum.mixare.gui.TextObj;
import org.openalbum.mixare.reality.PhysicalPlace;
import org.openalbum.mixare.render.Camera;
import org.openalbum.mixare.render.MixVector;

import android.location.Location;

/**
 * The class represents a marker and contains its information. It draws the
 * marker itself and the corresponding label. All markers are specific markers
 * like SocialMarkers or NavigationMarkers, since this class is abstract
 */

public abstract class Marker implements Comparable<Marker> {

	private String ID;
	protected String title;
	protected boolean underline = false;
	private String URL;
	protected PhysicalPlace mGeoLoc;
	// distance from user to mGeoLoc in meters
	protected double distance;
	// From which type does this marker originate
	protected DataSource datasource;
	private boolean active;

	// Draw properties
	protected boolean isVisible;
	// private boolean isLookingAt;
	// private boolean isNear;
	// private float deltaCenter;
	public MixVector cMarker = new MixVector();
	protected MixVector signMarker = new MixVector();
	// private MixVector oMarker = new MixVector();

	protected MixVector locationVector = new MixVector();
	private final MixVector origin = new MixVector(0, 0, 0);
	private final MixVector upV = new MixVector(0, 1, 0);
	private final ScreenLine pPt = new ScreenLine();

	protected Label txtLab = new Label();
	protected TextObj textBlock;

	public Marker(final String title, final double latitude,
			final double longitude, final double altitude, final String link,
			final DataSource datasource) {
		super();

		this.active = false;
		this.title = title;
		this.mGeoLoc = new PhysicalPlace(latitude, longitude, altitude);
		if (link != null && link.length() > 0) {
			URL = "webpage:" + URLDecoder.decode(link);
			this.underline = true;
		}
		this.datasource = datasource;

		this.ID = datasource.getTypeId() + "##" + title;

	}

	private void cCMarker(final MixVector originalPoint, final Camera viewCam,
			final float addX, final float addY) {

		// Temp properties
		final MixVector tmpa = new MixVector(originalPoint);
		final MixVector tmpc = new MixVector(upV);
		tmpa.add(locationVector); // 3
		tmpc.add(locationVector); // 3
		tmpa.sub(viewCam.lco); // 4
		tmpc.sub(viewCam.lco); // 4
		tmpa.prod(viewCam.transform); // 5
		tmpc.prod(viewCam.transform); // 5

		final MixVector tmpb = new MixVector();
		viewCam.projectPoint(tmpa, tmpb, addX, addY); // 6
		cMarker.set(tmpb); // 7
		viewCam.projectPoint(tmpc, tmpb, addX, addY); // 6
		signMarker.set(tmpb); // 7
	}

	private void calcV(final Camera viewCam) {
		isVisible = false;
		// isLookingAt = false;
		// deltaCenter = Float.MAX_VALUE;

		if (cMarker.z < -1f) {
			isVisible = true;

			if (MixUtils.pointInside(cMarker.x, cMarker.y, 0, 0, viewCam.width,
					viewCam.height)) {

				// float xDist = cMarker.x - viewCam.width / 2;
				// float yDist = cMarker.y - viewCam.height / 2;
				// float dist = xDist * xDist + yDist * yDist;

				// deltaCenter = (float) Math.sqrt(dist);
				//
				// if (dist < 50 * 50) {
				// isLookingAt = true;
				// }
			}
		}
	}

	public void update(final Location curGPSFix) {
		// An elevation of 0.0 probably means that the elevation of the
		// POI is not known and should be set to the users GPS height
		// Note: this could be improved with calls to
		// http://www.geonames.org/export/web-services.html#astergdem
		// to estimate the correct height with DEM models like SRTM, AGDEM or
		// GTOPO30
		if (mGeoLoc.getAltitude() == 0.0) {
			mGeoLoc.setAltitude(curGPSFix.getAltitude());
		}

		// compute the relative position vector from user position to POI
		// location
		PhysicalPlace.convLocToVec(curGPSFix, mGeoLoc, locationVector);
	}

	public void calcPaint(final Camera viewCam, final float addX,
			final float addY) {
		cCMarker(origin, viewCam, addX, addY);
		calcV(viewCam);
	}

	// private void calcPaint(Camera viewCam) {
	// cCMarker(origin, viewCam, 0, 0);
	// }

	private boolean isClickValid(final float x, final float y) {
		final float currentAngle = MixUtils.getAngle(cMarker.x, cMarker.y,
				signMarker.x, signMarker.y);
		// if the marker is not active (i.e. not shown in AR view) we don't have
		// to check it for clicks
		if (!isActive()) {
			return false;
		}

		// TODO adapt the following to the variable radius!
		pPt.x = x - signMarker.x;
		pPt.y = y - signMarker.y;
		pPt.rotate(Math.toRadians(-(currentAngle + 90)));
		pPt.x += txtLab.getX();
		pPt.y += txtLab.getY();

		final float objX = txtLab.getX() - txtLab.getWidth() / 2;
		final float objY = txtLab.getY() - txtLab.getHeight() / 2;
		final float objW = txtLab.getWidth();
		final float objH = txtLab.getHeight();

		if (pPt.x > objX && pPt.x < objX + objW && pPt.y > objY
				&& pPt.y < objY + objH) {
			return true;
		} else {
			return false;
		}
	}

	public void draw(final PaintScreen dw) {
		drawCircle(dw);
		drawTextBlock(dw);
	}

	public void drawCircle(final PaintScreen dw) {

		if (isVisible) {
			// float maxHeight = Math.round(dw.getHeight() / 10f) + 1;
			final float maxHeight = dw.getHeight();
			dw.setStrokeWidth(maxHeight / 100f);
			dw.setFill(false);
			// dw.setColor(DataSource.getColor(type));

			// draw circle with radius depending on distance
			// 0.44 is approx. vertical fov in radians
			final double angle = 2.0 * Math.atan2(10, distance);
			final double radius = Math.max(
					Math.min(angle / 0.44 * maxHeight, maxHeight),
					maxHeight / 25f);
			// double radius = angle/0.44d * (double)maxHeight;

			dw.paintCircle(cMarker.x, cMarker.y, (float) radius);
		}
	}

	public void drawTextBlock(final PaintScreen dw) {
		// TODO: grandezza cerchi e trasparenza
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

	public boolean fClick(final float x, final float y, final MixContext ctx,
			final MixState state) {
		boolean evtHandled = false;

		if (isClickValid(x, y)) {
			evtHandled = state.handleEvent(ctx, URL);
		}
		return evtHandled;
	}

	public int compareTo(final Marker another) {

		final Marker leftPm = this;
		final Marker rightPm = another;

		return Double.compare(leftPm.getDistance(), rightPm.getDistance());

	}

	@Override
	public boolean equals(final Object marker) {//@FIXME Marker casting Dangours
		return (marker != null)? this.ID.equals(((Marker) marker).getID()) : false;
	}

	abstract public int getMaxObjects();

	/************ Getters and Setters ****************/
	public String getTitle() {
		return title;
	}

	// get Colour for OpenStreetMap based on the URL number
	public int getColour() {
		return this.datasource.getColor();
	}

	public String getURL() {
		return URL;
	}

	public double getLatitude() {
		return mGeoLoc.getLatitude();
	}

	public double getLongitude() {
		return mGeoLoc.getLongitude();
	}

	public double getAltitude() {
		return mGeoLoc.getAltitude();
	}

	public MixVector getLocationVector() {
		return locationVector;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(final double distance) {
		this.distance = distance;
	}

	public String getID() {
		return ID;
	}

	public void setID(final String iD) {
		ID = iD;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

}

class Label implements ScreenObj {
	private float x, y;
	private float width, height;
	private ScreenObj obj;

	public void prepare(final ScreenObj drawObj) {
		obj = drawObj;
		final float w = obj.getWidth();
		final float h = obj.getHeight();

		x = w / 2;
		y = 0;

		width = w * 2;
		height = h * 2;
	}

	public void paint(final PaintScreen dw) {
		dw.paintObj(obj, x, y, 0, 1);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}
}