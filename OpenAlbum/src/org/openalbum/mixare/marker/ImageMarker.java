/**
 * 
 */
package org.openalbum.mixare.marker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

import org.openalbum.mixare.MixUtils;
import org.openalbum.mixare.data.DataSource;
import org.openalbum.mixare.gui.PaintScreen;
import org.openalbum.mixare.gui.TextObj;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;

/**
 * @author A.Egal
 * 
 */
public class ImageMarker extends Marker {

	public static final int MAX_OBJECTS = 20;
	private final Bitmap image; // @TODO Should not be static
	public static final int OSM_URL_MAX_OBJECTS = 5;
	private static final boolean FLAG_DECODE_PHOTO_STREAM_WITH_SKIA = false;
	private static final int IO_BUFFER_SIZE = 4 * 1024;
	private final int rectangleBackgroundColor = Color.WHITE;

	public ImageMarker(final String title, final double latitude,
			final double longitude, final double altitude, final String URL,
			final DataSource datasource, final Bitmap image) {
		super(title, latitude, longitude, altitude, URL, datasource);
		this.image = image;
	}

	public ImageMarker(final String title, final double latitude,
			final double longitude, final double altitude, final String URL,
			final DataSource datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);

		this.image = getBitmapFromURL(URL);
	}

	public Bitmap getBitmapFromURL(final String src) {

		InputStream input = null;
		BufferedOutputStream out = null;
		Bitmap myBitmap = null;
		try {
			final URL url = new URL(src);
			final HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoInput(true);
			connection.connect();
			input = new BufferedInputStream(connection.getInputStream(),
					IO_BUFFER_SIZE);
			if (FLAG_DECODE_PHOTO_STREAM_WITH_SKIA) {
				myBitmap = BitmapFactory.decodeStream(input);
			} else {
				final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
				out = new BufferedOutputStream(dataStream, IO_BUFFER_SIZE);
				copy(input, out);
				out.flush();

				final byte[] data = dataStream.toByteArray();
				myBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			closeStream(input);
			closeStream(out);
		}
		return myBitmap;
	}

	private static void copy(final InputStream input,
			final BufferedOutputStream out) {
		final byte[] b = new byte[IO_BUFFER_SIZE];
		int read;
		try {
			while ((read = input.read(b)) != -1) {
				out.write(b, 0, read);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the specified stream.
	 * 
	 * @param stream
	 *            The stream to close.
	 */
	private static void closeStream(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (final IOException e) {
				android.util.Log
						.e("ImageMaker-OA", "Could not close stream", e);
			}
		}
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
	public void draw(final PaintScreen dw) {
		this.drawImage(dw);
		this.drawTitle(dw);
	}

	/**
	 * Draw a title for image. It displays full title if title's length is less
	 * than 10 chars, otherwise, it displays the first 10 chars and concatenate
	 * three dots "..."
	 * 
	 * @param PaintScreen
	 *            dw
	 */
	public void drawTitle(final PaintScreen dw) {
		final float maxHeight = Math.round(dw.getHeight() / 10f) + 1;

		// TODO: change textblock only when distance changes
		String textStr = "";
		double d = distance;
		final DecimalFormat df = new DecimalFormat("@#");
		String imageTitle = "";
		if (title.length() > 10) {
			imageTitle = title.substring(0, 10) + "...";
		} else {
			imageTitle = title;
		}
		if (d < 1000.0) {
			textStr = imageTitle + " (" + df.format(d) + "m)";
		} else {
			d = d / 1000.0;
			textStr = imageTitle + " (" + df.format(d) + "km)";
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

	public void drawImage(final PaintScreen dw) {
		if (isVisible) {
			dw.setStrokeWidth(dw.getHeight() / 100f);
			dw.setFill(false);
			dw.setColor(rectangleBackgroundColor);
			dw.paintBitmap(getImage(), signMarker.x - (getImage().getWidth() / 2),
					signMarker.y - (getImage().getHeight() / 2));

		}
	}

	/**
	 * @return the image
	 */
	public Bitmap getImage() {
		return image;
	}
}