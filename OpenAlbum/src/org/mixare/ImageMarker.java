/**
 * 
 */
package org.mixare;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.mixare.data.DataSource;
import org.mixare.gui.PaintScreen;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import java.net.URL;

/**
 * @author A.Egal
 *
 */
public class ImageMarker extends Marker{

	public static final int MAX_OBJECTS = 20;
	private Bitmap image; 
	public static final int OSM_URL_MAX_OBJECTS = 5;
	private int rectangleBackgroundColor = Color.argb(155, 255, 255, 255);


	public ImageMarker(String title, double latitude, double longitude,
			double altitude, String URL, DataSource datasource, Bitmap image) {
		super(title, latitude, longitude, altitude, URL, datasource);
		this.image = image;
	}

	public ImageMarker(String title, double latitude, double longitude,
			double altitude, String URL, DataSource datasource) {
		super(title, latitude, longitude, altitude, URL, datasource);
		
		this.image = getBitmapFromURL(URL);
	}
	
	public static Bitmap getBitmapFromURL(String src) {
	    try {
	    	//workaround DNS lookup, resolves unknown host issue
	    	 try {
	    	       InetAddress.getByName(src);
	    	       //InetAddress i = InetAddress.getByName(URLName);
	    	    } catch (UnknownHostException e1) {
	    	      e1.printStackTrace();
	    	    }
	    	 
	        URL url = new URL(src);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        return myBitmap;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	@Override
	public void update(Location curGPSFix) {
		super.update(curGPSFix);
	}

	@Override
	public int getMaxObjects() {
		return MAX_OBJECTS;
	}

	@Override
	public void draw(PaintScreen dw) {
		this.drawImage(dw);
		super.drawTextBlock(dw);
	}

	public void drawImage(PaintScreen dw){
		if (isVisible) {
			dw.setColor(rectangleBackgroundColor);
			dw.paintBitmap(image, signMarker.x - (image.getWidth()/2), signMarker.y - (image.getHeight() / 2));
		}
	}
}