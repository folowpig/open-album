package org.mixare.data;

import java.util.List;

import org.mixare.MixContext;
import org.mixare.reality.AugmentedView;
import org.mixare.reality.CameraSurface;
import org.mixare.render.Matrix;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MixViewData {
	private CameraSurface camScreen;
	private AugmentedView augScreen;
	private MixContext mixContext;
	private Thread downloadThread;
	private float[] RTmp;
	private float[] Rot;
	private float[] I;
	private float[] grav;
	private float[] mag;
	private SensorManager sensorMgr;
	private List<Sensor> sensors;
	private Sensor sensorGrav;
	private Sensor sensorMag;
	private int rHistIdx;
	private Matrix tempR;
	private Matrix finalR;
	private Matrix smoothR;
	private Matrix[] histR;
	private Matrix m1;
	private Matrix m2;
	private Matrix m3;
	private Matrix m4;
	private SeekBar myZoomBar;
	private WakeLock mWakeLock;
	private boolean fError;
	private int compassErrorDisplayed;
	private String zoomLevel;
	private int zoomProgress;
	private TextView searchNotificationTxt;
	public static final String PREFS_NAME = "MyPrefsFileForMenuItems";
	private OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
		Toast t;
	
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			float myout = calcZoomLevel();
	
			zoomLevel = String.valueOf(myout);
			zoomProgress = myZoomBar.getProgress();
	
			t.setText("Radius: " + String.valueOf(myout));
			t.show();
		}
	
		public void onStartTrackingTouch(SeekBar seekBar) {
			Context ctx = seekBar.getContext();
			t = Toast.makeText(ctx, "Radius: ", Toast.LENGTH_LONG);
			// zoomChanging= true;
		}
	
		public void onStopTrackingTouch(SeekBar seekBar) {
			SharedPreferences settings = getMixContext().getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			/* store the zoom range of the zoom bar selected by the user */
			editor.putInt("zoomLevel", myZoomBar.getProgress());
			editor.commit();
			myZoomBar.setVisibility(View.INVISIBLE);
			// zoomChanging= false;
	
			myZoomBar.getProgress();
	
			t.cancel();
			//refreashZoomLevel();
		}

		//private void //refreashZoomLevel() {
			// TODO Auto-generated method stub
			
		//}
	
	};

	public float calcZoomLevel() {

		int myZoomLevel = getMyZoomBar().getProgress();
		float myout = 5;

		if (myZoomLevel <= 26) {
			myout = myZoomLevel / 25f;
		} else if (25 < myZoomLevel && myZoomLevel < 50) {
			myout = (1 + (myZoomLevel - 25)) * 0.38f;
		} else if (25 == myZoomLevel) {
			myout = 1;
		} else if (50 == myZoomLevel) {
			myout = 10;
		} else if (50 < myZoomLevel && myZoomLevel < 75) {
			myout = (10 + (myZoomLevel - 50)) * 0.83f;
		} else {
			myout = (30 + (myZoomLevel - 75) * 2f);
		}

		return myout;
	}
	public MixViewData(float[] rTmp, float[] rot, float[] i, float[] grav,
			float[] mag, int rHistIdx, Matrix tempR, Matrix finalR,
			Matrix smoothR, Matrix[] histR, Matrix m1, Matrix m2, Matrix m3,
			Matrix m4, int compassErrorDisplayed) {
		RTmp = rTmp;
		Rot = rot;
		I = i;
		this.grav = grav;
		this.mag = mag;
		this.rHistIdx = rHistIdx;
		this.tempR = tempR;
		this.finalR = finalR;
		this.smoothR = smoothR;
		this.histR = histR;
		this.m1 = m1;
		this.m2 = m2;
		this.m3 = m3;
		this.m4 = m4;
		this.compassErrorDisplayed = compassErrorDisplayed;
	}

	public CameraSurface getCamScreen() {
		return camScreen;
	}

	public void setCamScreen(CameraSurface camScreen) {
		this.camScreen = camScreen;
	}

	public AugmentedView getAugScreen() {
		return augScreen;
	}

	public void setAugScreen(AugmentedView augScreen) {
		this.augScreen = augScreen;
	}

	public MixContext getMixContext() {
		return mixContext;
	}

	public void setMixContext(MixContext mixContext) {
		this.mixContext = mixContext;
	}

	public Thread getDownloadThread() {
		return downloadThread;
	}

	public void setDownloadThread(Thread downloadThread) {
		this.downloadThread = downloadThread;
	}

	public float[] getRTmp() {
		return RTmp;
	}

	public void setRTmp(float[] rTmp) {
		RTmp = rTmp;
	}

	public float[] getRot() {
		return Rot;
	}

	public void setRot(float[] rot) {
		Rot = rot;
	}

	public float[] getI() {
		return I;
	}

	public void setI(float[] i) {
		I = i;
	}

	public float[] getGrav() {
		return grav;
	}

	public void setGrav(float[] grav) {
		this.grav = grav;
	}

	public float[] getMag() {
		return mag;
	}

	public void setMag(float[] mag) {
		this.mag = mag;
	}

	public SensorManager getSensorMgr() {
		return sensorMgr;
	}

	public void setSensorMgr(SensorManager sensorMgr) {
		this.sensorMgr = sensorMgr;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public Sensor getSensorGrav() {
		return sensorGrav;
	}

	public void setSensorGrav(Sensor sensorGrav) {
		this.sensorGrav = sensorGrav;
	}

	public Sensor getSensorMag() {
		return sensorMag;
	}

	public void setSensorMag(Sensor sensorMag) {
		this.sensorMag = sensorMag;
	}

	public int getrHistIdx() {
		return rHistIdx;
	}

	public void setrHistIdx(int rHistIdx) {
		this.rHistIdx = rHistIdx;
	}

	public Matrix getTempR() {
		return tempR;
	}

	public void setTempR(Matrix tempR) {
		this.tempR = tempR;
	}

	public Matrix getFinalR() {
		return finalR;
	}

	public void setFinalR(Matrix finalR) {
		this.finalR = finalR;
	}

	public Matrix getSmoothR() {
		return smoothR;
	}

	public void setSmoothR(Matrix smoothR) {
		this.smoothR = smoothR;
	}

	public Matrix[] getHistR() {
		return histR;
	}

	public void setHistR(Matrix[] histR) {
		this.histR = histR;
	}

	public Matrix getM1() {
		return m1;
	}

	public void setM1(Matrix m1) {
		this.m1 = m1;
	}

	public Matrix getM2() {
		return m2;
	}

	public void setM2(Matrix m2) {
		this.m2 = m2;
	}

	public Matrix getM3() {
		return m3;
	}

	public void setM3(Matrix m3) {
		this.m3 = m3;
	}

	public Matrix getM4() {
		return m4;
	}

	public void setM4(Matrix m4) {
		this.m4 = m4;
	}

	public SeekBar getMyZoomBar() {
		return myZoomBar;
	}

	public void setMyZoomBar(SeekBar myZoomBar) {
		this.myZoomBar = myZoomBar;
	}

	public WakeLock getmWakeLock() {
		return mWakeLock;
	}

	public void setmWakeLock(WakeLock mWakeLock) {
		this.mWakeLock = mWakeLock;
	}

	public boolean isfError() {
		return fError;
	}

	public void setfError(boolean fError) {
		this.fError = fError;
	}

	public int getCompassErrorDisplayed() {
		return compassErrorDisplayed;
	}

	public void setCompassErrorDisplayed(int compassErrorDisplayed) {
		this.compassErrorDisplayed = compassErrorDisplayed;
	}

	public String getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(String zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public int getZoomProgress() {
		return zoomProgress;
	}

	public void setZoomProgress(int zoomProgress) {
		this.zoomProgress = zoomProgress;
	}

	public TextView getSearchNotificationTxt() {
		return searchNotificationTxt;
	}

	public void setSearchNotificationTxt(TextView searchNotificationTxt) {
		this.searchNotificationTxt = searchNotificationTxt;
	}

	public OnSeekBarChangeListener getMyZoomBarOnSeekBarChangeListener() {
		return myZoomBarOnSeekBarChangeListener;
	}

	public void setMyZoomBarOnSeekBarChangeListener(
			OnSeekBarChangeListener myZoomBarOnSeekBarChangeListener) {
		this.myZoomBarOnSeekBarChangeListener = myZoomBarOnSeekBarChangeListener;
	}
}