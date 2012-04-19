package org.mixare;

import java.util.ArrayList;
import java.util.Random;

import org.mixare.data.DataSource;
import org.mixare.render.Matrix;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MixContextData {
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
	
		public void onLocationChanged(Location location) {
			Log.d(TAG, "bounce Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
			//Toast.makeText(ctx, "BOUNCE: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(), Toast.LENGTH_LONG).show();
	
			//@FIXME Check location changes before purging, make use of cache - leak
			downloadManager.purgeLists();
			
			if (location.getAccuracy() < 40) {
				lm.removeUpdates(lcoarse);
				lm.removeUpdates(lbounce);			
			}
		}
	
		public void onProviderDisabled(String arg0) {
			Log.d(TAG, "bounce disabled");
		}
	
		public void onProviderEnabled(String arg0) {
			Log.d(TAG, "bounce enabled");
	
		}
	
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
		
	};
	private LocationListener lcoarse = new LocationListener() {
	
		public void onLocationChanged(Location location) {
			try {
				Log.d(TAG, "coarse Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
				//Toast.makeText(ctx, "COARSE: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(), Toast.LENGTH_LONG).show();
	//				if (lm != null)
	//					lm. =  location;
	//				lm.removeUpdates(lcoarse); //?
				//@FIXME Check location changes before purging, make use of cache - leak
				downloadManager.purgeLists();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	
		public void onProviderDisabled(String arg0) {}
	
		public void onProviderEnabled(String arg0) {}
	
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
		
	};
	private LocationListener lnormal = new LocationListener() {
		public void onProviderDisabled(String provider) {}
	
		public void onProviderEnabled(String provider) {}
	
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	
		public void onLocationChanged(Location location) {
			Log.d(TAG, "normal Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
			//Toast.makeText(ctx, "NORMAL: Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy(), Toast.LENGTH_LONG).show();
			try {
	
				downloadManager.purgeLists();
				Log.v(TAG,"Location Changed: "+location.getProvider()+" lat: "+location.getLatitude()+" lon: "+location.getLongitude()+" alt: "+location.getAltitude()+" acc: "+location.getAccuracy());
					synchronized (curLoc) {
						curLoc = location;
					}
					mixView.repaint();
					Location lastLoc=getLocationAtLastDownload();
					if(lastLoc==null)
						setLocationAtLastDownload(location);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	
	};

	public MixContextData(boolean isURLvalid, Matrix rotationM,
			float declination, ArrayList<DataSource> allDataSources) {
		this.isURLvalid = isURLvalid;
		this.rotationM = rotationM;
		this.declination = declination;
		this.allDataSources = allDataSources;
	}

	public MixView getMixView() {
		return mixView;
	}

	public void setMixView(MixView mixView) {
		this.mixView = mixView;
	}

	public Context getCtx() {
		return ctx;
	}

	public void setCtx(Context ctx) {
		this.ctx = ctx;
	}

	public boolean isURLvalid() {
		return isURLvalid;
	}

	public void setURLvalid(boolean isURLvalid) {
		this.isURLvalid = isURLvalid;
	}

	public Random getRand() {
		return rand;
	}

	public void setRand(Random rand) {
		this.rand = rand;
	}

	public DownloadManager getDownloadManager() {
		return downloadManager;
	}

	public void setDownloadManager(DownloadManager downloadManager) {
		this.downloadManager = downloadManager;
	}

	public Matrix getRotationM() {
		return rotationM;
	}

	public void setRotationM(Matrix rotationM) {
		this.rotationM = rotationM;
	}

	public float getDeclination() {
		return declination;
	}

	public void setDeclination(float declination) {
		this.declination = declination;
	}

	public LocationManager getLm() {
		return lm;
	}

	public void setLm(LocationManager lm) {
		this.lm = lm;
	}

	public Location getCurLoc() {
		return curLoc;
	}

	public void setCurLoc(Location curLoc) {
		this.curLoc = curLoc;
	}

	public Location getLocationAtLastDownload() {
		return locationAtLastDownload;
	}

	public void setLocationAtLastDownload(Location locationAtLastDownload) {
		this.locationAtLastDownload = locationAtLastDownload;
	}

	public ArrayList<DataSource> getAllDataSources() {
		return allDataSources;
	}

	public void setAllDataSources(ArrayList<DataSource> allDataSources) {
		this.allDataSources = allDataSources;
	}

	public LocationListener getLbounce() {
		return lbounce;
	}

	public void setLbounce(LocationListener lbounce) {
		this.lbounce = lbounce;
	}

	public LocationListener getLcoarse() {
		return lcoarse;
	}

	public void setLcoarse(LocationListener lcoarse) {
		this.lcoarse = lcoarse;
	}

	public LocationListener getLnormal() {
		return lnormal;
	}

	public void setLnormal(LocationListener lnormal) {
		this.lnormal = lnormal;
	}
}