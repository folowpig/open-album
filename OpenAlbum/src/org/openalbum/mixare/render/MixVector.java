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
package org.openalbum.mixare.render;

/**
 * This class holds information of a point in a three-dimensional coordinate
 * system. It holds the values for the x-, y- and z-axis, which can be modified
 * through several methods. (for example adding and subtracting points) The
 * distance from the origin of the coordinate system to the point represents the
 * vector. The application uses vectors to describe distances on the map.
 * 
 * @author daniele
 * 
 */
public class MixVector {
	public float x;
	public float y;
	public float z;

	public MixVector() {
		this(0, 0, 0);
	}

	public MixVector(final MixVector v) {
		this(v.x, v.y, v.z);
	}

	public MixVector(final float v[]) {
		this(v[0], v[1], v[2]);
	}

	public MixVector(final float x, final float y, final float z) {
		set(x, y, z);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj != null){
		final MixVector v = (MixVector) obj;
		return (v.x == x && v.y == y && v.z == z);
		}else {
			return false;
		}
		
	}

	public boolean equals(final float x, final float y, final float z) {
		return (this.x == x && this.y == y && this.z == z);
	}

	@Override
	public String toString() {
		return "<" + x + ", " + y + ", " + z + ">";
	}

	public void set(final MixVector v) {
		set(v.x, v.y, v.z);
	}

	public void set(final float x, final float y, final float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void add(final float x, final float y, final float z) {
		this.x += x;
		this.y += y;
		this.z += z;
	}

	public void add(final MixVector v) {
		add(v.x, v.y, v.z);
	}

	public void sub(final float x, final float y, final float z) {
		add(-x, -y, -z);
	}

	public void sub(final MixVector v) {
		add(-v.x, -v.y, -v.z);
	}

	public void mult(final float s) {
		x *= s;
		y *= s;
		z *= s;
	}

	public void divide(final float s) {
		x /= s;
		y /= s;
		z /= s;
	}

	public float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public float length2D() {
		return (float) Math.sqrt(x * x + z * z);
	}

	public void norm() {
		divide(length());
	}

	public float dot(final MixVector v) {
		return x * v.x + y * v.y + z * v.z;
	}

	public void cross(final MixVector u, final MixVector v) {
		final float x = u.y * v.z - u.z * v.y;
		final float y = u.z * v.x - u.x * v.z;
		final float z = u.x * v.y - u.y * v.x;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void prod(final Matrix m) {
		final float xTemp = m.a1 * x + m.a2 * y + m.a3 * z;
		final float yTemp = m.b1 * x + m.b2 * y + m.b3 * z;
		final float zTemp = m.c1 * x + m.c2 * y + m.c3 * z;

		x = xTemp;
		y = yTemp;
		z = zTemp;
	}
}
