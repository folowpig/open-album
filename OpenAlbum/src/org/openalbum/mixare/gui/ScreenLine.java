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
package org.openalbum.mixare.gui;

/**
 * The class stores a point of a two-dimensional coordinate system. (values of
 * the x and y axis)
 */

public class ScreenLine {
	public float x, y;

	public ScreenLine() {
		set(0, 0);
	}

	public ScreenLine(final float x, final float y) {
		set(x, y);
	}

	public void set(final float x, final float y) {
		this.x = x;
		this.y = y;
	}

	public void rotate(final double t) {
		final float xp = (float) Math.cos(t) * x - (float) Math.sin(t) * y;
		final float yp = (float) Math.sin(t) * x + (float) Math.cos(t) * y;

		x = xp;
		y = yp;
	}

	public void add(final float x, final float y) {
		this.x += x;
		this.y += y;
	}
}
