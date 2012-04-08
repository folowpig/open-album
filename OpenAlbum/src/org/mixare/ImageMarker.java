/**
 * 
 */
package org.mixare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.mixare.data.DataSource;
import org.mixare.gui.PaintScreen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import java.net.URL;

/**
 * @author A.Egal
 * 
 */
public class ImageMarker extends Marker {

	public static final int MAX_OBJECTS = 20;
	private Bitmap image; //@TODO Should not be static
	public static final int OSM_URL_MAX_OBJECTS = 5;
	private static final boolean FLAG_DECODE_PHOTO_STREAM_WITH_SKIA = false;
	private static final int IO_BUFFER_SIZE = 4 * 1024;
	private int rectangleBackgroundColor = Color.argb(155, 255, 255, 255);

	public ImageMarker(String title, double latitude, double longitude,
			double altitude, String URL, DataSource datasource, Bitmap image) {
		super(title, latitude, longitude, altitude, URL, datasource);
		this.setImage(image);
	}

	public ImageMarker(String title, double latitude, double longitude,
			double altitude, String URL, DataSource datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);

		setImage(getBitmapFromURL(URL));
	}

	private  Bitmap getBitmapFromURL(String src) {
		
		InputStream input = null;
		BufferedOutputStream out = null;
		Bitmap myBitmap = null;
		try {
//			// workaround DNS lookup, resolves unknown host issue
//			try {
//				InetAddress.getByName(src);
//				// InetAddress i = InetAddress.getByName(URLName);
//			} catch (UnknownHostException e1) {
//				e1.printStackTrace();
//			}
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
		} catch (IOException e) {
			Log.e("OpenAlbum - Mixare", e.getMessage(), e);
			//return null;
		}finally {
            closeStream(input);
            closeStream(out);
        }
		return myBitmap;
	}

	private static void copy(InputStream input, BufferedOutputStream out) {
		 byte[] byteTemp = new byte[IO_BUFFER_SIZE];
	        int read;
	        try {
				while ((read = input.read(byteTemp)) != -1) {
				    out.write(byteTemp, 0, read);
				}
			} catch (IOException e) {
				Log.e("OpenAlbum - Mixare", e.getMessage(), e);
			}		
	}
	
    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e("ImageMaker-OA", "Could not close stream", e);
            }
        }
    }

    //uncomment if there is local processing
//	@Override
//	public void update(Location curGPSFix) {
//		super.update(curGPSFix);
//	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

	@Override
	public void draw(PaintScreen dw) {
		this.drawImage(dw);
		super.drawTextBlock(dw);
	}

	public void drawImage(PaintScreen dw) {
		if (isVisible) {
			dw.setColor(rectangleBackgroundColor);
			dw.paintBitmap(getImage(), signMarker.x - (getImage().getWidth() / 2),
					signMarker.y - (getImage().getHeight() / 2));
			
		}
	}

	/**
	 * @return the image
	 */
	private Bitmap getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	private void setImage(Bitmap image) {
		this.image = image;
	}
}