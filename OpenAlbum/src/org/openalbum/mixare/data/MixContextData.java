package org.openalbum.mixare.data;

import java.util.ArrayList;
import java.util.Random;

import org.openalbum.mixare.DownloadManager;
import org.openalbum.mixare.MixView;
import org.openalbum.mixare.render.Matrix;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class MixContextData {
	protected static final String TAG = "Open Album";
	private MixView mixView;
	private Context ctx;
	private boolean isURLvalid;
	private Random rand;
	private DownloadManager downloadManager;
	private Matrix rotationM;
	private float declination;
	private LocationManager lm;
	private Location curLoc;
	private Location locationAtLastDownload;
	private ArrayList<DataSource> allDataSources;
	private LocationListener lbounce = new LocationListener() {

		public void onLocationChanged(final Location location) {
			Log.d(TAG,
					"bounce Location Changed: " + location.getProvider()
							+ " lat: " + location.getLatitude() + " lon: "
							+ location.getLongitude() + " alt: "
							+ location.getAltitude() + " acc: "
							+ location.getAccuracy());
			// Toast.makeText(ctx,
			// "BOUNCE: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(),
			// Toast.LENGTH_LONG).show();

			// @FIXME Check location changes before purging, make use of cache -
			// leak
			downloadManager.purgeLists();

			if (location.getAccuracy() < 40) {
				lm.removeUpdates(lcoarse);
				lm.removeUpdates(lbounce);
			}
		}

		public void onProviderDisabled(final String arg0) {
			Log.d(TAG, "bounce disabled");
		}

		public void onProviderEnabled(final String arg0) {
			Log.d(TAG, "bounce enabled");

		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			// TODO Auto-generated method stub

		}

	};
	private LocationListener lcoarse = new LocationListener() {

		public void onLocationChanged(final Location location) {
			try {
				Log.d(TAG,
						"coarse Location Changed: " + location.getProvider()
								+ " lat: " + location.getLatitude() + " lon: "
								+ location.getLongitude() + " alt: "
								+ location.getAltitude() + " acc: "
								+ location.getAccuracy());
				// Toast.makeText(ctx,
				// "COARSE: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(),
				// Toast.LENGTH_LONG).show();
				 if (lm != null)
				 lm.removeUpdates(lcoarse); //?
				downloadManager.purgeLists();
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}

		public void onProviderDisabled(final String arg0) {
			
			//@TODO Display warning or redirect to provider
		}

		public void onProviderEnabled(final String arg0) {
		}

		public void onStatusChanged(final String arg0, final int arg1,
				final Bundle arg2) {
		}

	};
	private LocationListener lnormal = new LocationListener() {
		public void onProviderDisabled(final String provider) {
		}

		public void onProviderEnabled(final String provider) {
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
		}

		public void onLocationChanged(final Location location) {
			Log.d(TAG,
					"normal Location Changed: " + location.getProvider()
							+ " lat: " + location.getLatitude() + " lon: "
							+ location.getLongitude() + " alt: "
							+ location.getAltitude() + " acc: "
							+ location.getAccuracy());
			// Toast.makeText(ctx,
			// "NORMAL: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(),
			// Toast.LENGTH_LONG).show();
			try {

				downloadManager.purgeLists();
				Log.v(TAG,
						"Location Changed: " + location.getProvider()
								+ " lat: " + location.getLatitude() + " lon: "
								+ location.getLongitude() + " alt: "
								+ location.getAltitude() + " acc: "
								+ location.getAccuracy());
				synchronized (curLoc) {
					curLoc = location;
				}
				mixView.repaint();
				final Location lastLoc = getLocationAtLastDownload();
				if (lastLoc == null) {
					setLocationAtLastDownload(location);
				}
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}

	};

	public MixContextData(final boolean isURLvalid, final Matrix rotationM,
			final float declination, final ArrayList<DataSource> allDataSources) {
		this.isURLvalid = isURLvalid;
		this.rotationM = rotationM;
		this.declination = declination;
		this.allDataSources = allDataSources;
	}

	public MixView getMixView() {
		return mixView;
	}

	public void setMixView(final MixView mixView) {
		this.mixView = mixView;
	}

	public Context getCtx() {
		return ctx;
	}

	public void setCtx(final Context ctx) {
		this.ctx = ctx;
	}

	public boolean isURLvalid() {
		return isURLvalid;
	}

	public void setURLvalid(final boolean isURLvalid) {
		this.isURLvalid = isURLvalid;
	}

	public Random getRand() {
		return rand;
	}

	public void setRand(final Random rand) {
		this.rand = rand;
	}

	public DownloadManager getDownloadManager() {
		return downloadManager;
	}

	public void setDownloadManager(final DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

	public Matrix getRotationM() {
		return rotationM;
	}

	public void setRotationM(final Matrix rotationM) {
		this.rotationM = rotationM;
	}

	public float getDeclination() {
		return declination;
	}

	public void setDeclination(final float declination) {
		this.declination = declination;
	}

	public LocationManager getLm() {
		return lm;
	}

	public void setLm(final LocationManager lm) {
		this.lm = lm;
	}

	public Location getCurLoc() {
		return curLoc;
	}

	public void setCurLoc(final Location curLoc) {
		this.curLoc = curLoc;
	}

	public Location getLocationAtLastDownload() {
		return locationAtLastDownload;
	}

	public void setLocationAtLastDownload(final Location locationAtLastDownload) {
		this.locationAtLastDownload = locationAtLastDownload;
	}

	public ArrayList<DataSource> getAllDataSources() {
		return allDataSources;
	}

	public void setAllDataSources(final ArrayList<DataSource> allDataSources) {
		this.allDataSources = allDataSources;
	}

	public LocationListener getLbounce() {
		return lbounce;
	}

	public void setLbounce(final LocationListener lbounce) {
		this.lbounce = lbounce;
	}

	public LocationListener getLcoarse() {
		return lcoarse;
	}

	public void setLcoarse(final LocationListener lcoarse) {
		this.lcoarse = lcoarse;
	}

	public LocationListener getLnormal() {
		return lnormal;
	}

	public void setLnormal(final LocationListener lnormal) {
		this.lnormal = lnormal;
	}
}