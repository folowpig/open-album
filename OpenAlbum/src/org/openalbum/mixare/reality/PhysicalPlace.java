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
package org.openalbum.mixare.reality;

import org.openalbum.mixare.render.MixVector;

import android.location.Location;

/**
 * The class stores the geographical information (latitude, longitude and
 * altitude) and represents a Place on the map. It also calculates the
 * destination using the theory of the great-circle-distance. Further it is able
 * to convert the distances between locations into vectors and vice versa.
 * 
 */

public class PhysicalPlace {

	double latitude;
	double longitude;
	double altitude;

	public PhysicalPlace() {

	}

	public PhysicalPlace(final PhysicalPlace pl) {
		this.setTo(pl.latitude, pl.longitude, pl.altitude);
	}

	public PhysicalPlace(final double latitude, final double longitude,
			final double altitude) {
		this.setTo(latitude, longitude, altitude);
	}

	public void setTo(final double latitude, final double longitude,
			final double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	public void setTo(final PhysicalPlace pl) {
		this.latitude = pl.latitude;
		this.longitude = pl.longitude;
		this.altitude = pl.altitude;
	}

	@Override
	public String toString() {
		return "(lat=" + latitude + ", lng=" + longitude + ", alt=" + altitude
				+ ")";
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(final double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(final double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(final double altitude) {
		this.altitude = altitude;
	}

	public static void calcDestination(final double lat1Deg,
			final double lon1Deg, final double bear, final double d,
			final PhysicalPlace dest) {
		/** see http://en.wikipedia.org/wiki/Great-circle_distance */

		final double brng = Math.toRadians(bear);
		final double lat1 = Math.toRadians(lat1Deg);
		final double lon1 = Math.toRadians(lon1Deg);
		final double R = 6371.0 * 1000.0;

		final double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		final double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
						Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));

		dest.setLatitude(Math.toDegrees(lat2));
		dest.setLongitude(Math.toDegrees(lon2));
	}

	public static void convLocToVec(final Location org, final PhysicalPlace gp,
			final MixVector v) {
		final float[] z = new float[1];
		z[0] = 0;
		Location.distanceBetween(org.getLatitude(), org.getLongitude(),
				gp.getLatitude(), org.getLongitude(), z);
		final float[] x = new float[1];
		Location.distanceBetween(org.getLatitude(), org.getLongitude(),
				org.getLatitude(), gp.getLongitude(), x);
		final double y = gp.getAltitude() - org.getAltitude();
		if (org.getLatitude() < gp.getLatitude()) {
			z[0] *= -1;
		}
		if (org.getLongitude() > gp.getLongitude()) {
			x[0] *= -1;
		}

		v.set(x[0], (float) y, z[0]);
	}

	public static void convertVecToLoc(final MixVector v, final Location org,
			final Location gp) {
		double brngNS = 0, brngEW = 90;
		if (v.z > 0) {
			brngNS = 180;
		}
		if (v.x < 0) {
			brngEW = 270;
		}

		final PhysicalPlace tmp1Loc = new PhysicalPlace();
		final PhysicalPlace tmp2Loc = new PhysicalPlace();
		PhysicalPlace.calcDestination(org.getLatitude(), org.getLongitude(),
				brngNS, Math.abs(v.z), tmp1Loc);
		PhysicalPlace.calcDestination(tmp1Loc.getLatitude(),
				tmp1Loc.getLongitude(), brngEW, Math.abs(v.x), tmp2Loc);

		gp.setLatitude(tmp2Loc.getLatitude());
		gp.setLongitude(tmp2Loc.getLongitude());
		gp.setAltitude(org.getAltitude() + v.y);
	}
}
