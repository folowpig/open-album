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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * This class has the ability to set up the main view and it paints objects on
 * the screen
 */

public class PaintScreen {
	Canvas canvas;
	int width, height;
	Paint paint = new Paint();
	Paint bPaint = new Paint();

	public PaintScreen() {
		paint.setTextSize(16);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
	}

	public void paintLine(final float x1, final float y1, final float x2,
			final float y2) {
		canvas.drawLine(x1, y1, x2, y2, paint);
	}

	public void paintRect(final float x, final float y, final float width,
			final float height) {
		canvas.drawRect(x, y, x + width, y + height, paint);
	}

	public void paintRoundedRect(final float x, final float y,
			final float width, final float height) {
		// rounded edges. patch by Ignacio Avellino
		final RectF rect = new RectF(x, y, x + width, y + height);
		canvas.drawRoundRect(rect, 15F, 15F, paint);
	}

	public void paintBitmap(final Bitmap bitmap, final float left,
			final float top) {
		canvas.save();
		canvas.drawBitmap(bitmap, left, top, paint);
		canvas.restore();
	}

	public void paintPath(final Path path, final float x, final float y,
			final float width, final float height, final float rotation,
			final float scale) {
		canvas.save();
		canvas.translate(x + width / 2, y + height / 2);
		canvas.rotate(rotation);
		canvas.scale(scale, scale);
		canvas.translate(-(width / 2), -(height / 2));
		canvas.drawPath(path, paint);
		canvas.restore();
	}

	public void paintCircle(final float x, final float y, final float radius) {
		canvas.drawCircle(x, y, radius, paint);
	}

	public void paintText(final float x, final float y, final String text,
			final boolean underline) {
		paint.setUnderlineText(underline);
		canvas.drawText(text, x, y, paint);
	}

	public void paintObj(final ScreenObj obj, final float x, final float y,
			final float rotation, final float scale) {
		canvas.save();
		canvas.translate(x + obj.getWidth() / 2, y + obj.getHeight() / 2);
		canvas.rotate(rotation);
		canvas.scale(scale, scale);
		canvas.translate(-(obj.getWidth() / 2), -(obj.getHeight() / 2));
		obj.paint(this);
		canvas.restore();
	}

	/* ********** Getters and Setters ************ */
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setFill(final boolean fill) {
		if (fill) {
			paint.setStyle(Paint.Style.FILL);
		} else {
			paint.setStyle(Paint.Style.STROKE);
		}
	}

	public void setColor(final int c) {
		paint.setColor(c);
	}

	public void setStrokeWidth(final float w) {
		paint.setStrokeWidth(w);
	}

	public void setWidth(final int width) {
		this.width = width;
	}

	public void setHeight(final int height) {
		this.height = height;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setCanvas(final Canvas canvas) {
		this.canvas = canvas;
	}

	public float getTextWidth(final String txt) {
		return paint.measureText(txt);
	}

	public float getTextAsc() {
		return -paint.ascent();
	}

	public float getTextDesc() {
		return paint.descent();
	}

	public float getTextLead() {
		return 0;
	}

	public void setFontSize(final float size) {
		paint.setTextSize(size);
	}
}
