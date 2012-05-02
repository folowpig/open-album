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
 * This class has the ability to calculate the declination of a line between two
 * points. It is able to check if a point is in a given rectangle and it also
 * can make a String out of a given distance-value which contains number and
 * unit.
 */
public class MixUtils {
	public static String parseAction(final String action) {
		return (action.substring(action.indexOf(':') + 1, action.length()))
				.trim();
	}

	public static String formatDist(final float meters) {
		if (meters < 1000) {
			return ((int) meters) + "m";
		} else if (meters < 10000) {
			return formatDec(meters / 1000f, 1) + "km";
		} else {
			return ((int) (meters / 1000f)) + "km";
		}
	}

	static String formatDec(final float val, final int dec) {
		final int factor = (int) Math.pow(10, dec);

		final int front = (int) (val);
		final int back = (int) Math.abs(val * (factor)) % factor;

		return front + "." + back;
	}

	public static boolean pointInside(final float P_x, final float P_y,
			final float r_x, final float r_y, final float r_w, final float r_h) {
		return (P_x > r_x && P_x < r_x + r_w && P_y > r_y && P_y < r_y + r_h);
	}

	public static float getAngle(final float center_x, final float center_y,
			final float post_x, final float post_y) {
		final float tmpv_x = post_x - center_x;
		final float tmpv_y = post_y - center_y;
		final float d = (float) Math.sqrt(tmpv_x * tmpv_x + tmpv_y * tmpv_y);
		final float cos = tmpv_x / d;
		float angle = (float) Math.toDegrees(Math.acos(cos));

		angle = (tmpv_y < 0) ? angle * -1 : angle;

		return angle;
	}

	public static double getAngle(final double center_x,final double center_y,
			final double post_x,final double post_y) {
		final double d = (double) Math.sqrt((post_x - center_x)*(post_x - center_x)+ 
				(post_y - center_y) * (post_y - center_y));
		final double cos = (post_x - center_x) / d;
		double angle = (double) Math.toDegrees(Math.acos(cos));

		angle = ((post_y - center_y) < 0.) ? angle * -1. : angle;

		return angle;
	}
}
