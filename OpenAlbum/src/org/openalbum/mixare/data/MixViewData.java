package org.openalbum.mixare.data;

import java.util.List;

import org.openalbum.mixare.MixContext;
import org.openalbum.mixare.reality.AugmentedView;
import org.openalbum.mixare.reality.CameraSurface;
import org.openalbum.mixare.render.Matrix;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager.WakeLock;
import android.widget.SeekBar;
import android.widget.TextView;

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
	public static final String PREFS_NAME = "OpenAlbumPrefsFileForMenuItems";


	public float calcZoomLevel() {

		final int myZoomLevel = getMyZoomBar().getProgress();
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

	public MixViewData(final float[] rTmp, final float[] rot, final float[] i,
			final float[] grav, final float[] mag, final int rHistIdx,
			final Matrix tempR, final Matrix finalR, final Matrix smoothR,
			final Matrix[] histR, final Matrix m1, final Matrix m2,
			final Matrix m3, final Matrix m4, final int compassErrorDisplayed) {
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

	public void setCamScreen(final CameraSurface camScreen) {
		this.camScreen = camScreen;
	}

	public AugmentedView getAugScreen() {
		return augScreen;
	}

	public void setAugScreen(final AugmentedView augScreen) {
		this.augScreen = augScreen;
	}

	public MixContext getMixContext() {
		return mixContext;
	}

	public void setMixContext(final MixContext mixContext) {
		this.mixContext = mixContext;
	}

	public Thread getDownloadThread() {
		return downloadThread;
	}

	public void setDownloadThread(final Thread downloadThread) {
		this.downloadThread = downloadThread;
	}

	public float[] getRTmp() {
		return RTmp;
	}

	public void setRTmp(final float[] rTmp) {
		RTmp = rTmp;
	}

	public float[] getRot() {
		return Rot;
	}

	public void setRot(final float[] rot) {
		Rot = rot;
	}

	public float[] getI() {
		return I;
	}

	public void setI(final float[] i) {
		I = i;
	}

	public float[] getGrav() {
		return grav;
	}

	public void setGrav(final float[] grav) {
		this.grav = grav;
	}

	public float[] getMag() {
		return mag;
	}

	public void setMag(final float[] mag) {
		this.mag = mag;
	}

	public SensorManager getSensorMgr() {
		return sensorMgr;
	}

	public void setSensorMgr(final SensorManager sensorMgr) {
		this.sensorMgr = sensorMgr;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(final List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public Sensor getSensorGrav() {
		return sensorGrav;
	}

	public void setSensorGrav(final Sensor sensorGrav) {
		this.sensorGrav = sensorGrav;
	}

	public Sensor getSensorMag() {
		return sensorMag;
	}

	public void setSensorMag(final Sensor sensorMag) {
		this.sensorMag = sensorMag;
	}

	public int getrHistIdx() {
		return rHistIdx;
	}

	public void setrHistIdx(final int rHistIdx) {
		this.rHistIdx = rHistIdx;
	}

	public Matrix getTempR() {
		return tempR;
	}

	public void setTempR(final Matrix tempR) {
		this.tempR = tempR;
	}

	public Matrix getFinalR() {
		return finalR;
	}

	public void setFinalR(final Matrix finalR) {
		this.finalR = finalR;
	}

	public Matrix getSmoothR() {
		return smoothR;
	}

	public void setSmoothR(final Matrix smoothR) {
		this.smoothR = smoothR;
	}

	public Matrix[] getHistR() {
		return histR;
	}

	public void setHistR(final Matrix[] histR) {
		this.histR = histR;
	}

	public Matrix getM1() {
		return m1;
	}

	public void setM1(final Matrix m1) {
		this.m1 = m1;
	}

	public Matrix getM2() {
		return m2;
	}

	public void setM2(final Matrix m2) {
		this.m2 = m2;
	}

	public Matrix getM3() {
		return m3;
	}

	public void setM3(final Matrix m3) {
		this.m3 = m3;
	}

	public Matrix getM4() {
		return m4;
	}

	public void setM4(final Matrix m4) {
		this.m4 = m4;
	}

	public SeekBar getMyZoomBar() {
		return myZoomBar;
	}

	public void setMyZoomBar(final SeekBar myZoomBar) {
		this.myZoomBar = myZoomBar;
	}

	public WakeLock getmWakeLock() {
		return mWakeLock;
	}

	public void setmWakeLock(final WakeLock mWakeLock) {
		this.mWakeLock = mWakeLock;
	}

	public boolean isfError() {
		return fError;
	}

	public void setfError(final boolean fError) {
		this.fError = fError;
	}

	public int getCompassErrorDisplayed() {
		return compassErrorDisplayed;
	}

	public void setCompassErrorDisplayed(final int compassErrorDisplayed) {
		this.compassErrorDisplayed = compassErrorDisplayed;
	}

	public String getZoomLevel() {
		return zoomLevel;
	}

	public void setZoomLevel(final String zoomLevel) {
		this.zoomLevel = zoomLevel;
	}

	public int getZoomProgress() {
		return zoomProgress;
	}

	public void setZoomProgress(final int zoomProgress) {
		this.zoomProgress = zoomProgress;
	}

	public TextView getSearchNotificationTxt() {
		return searchNotificationTxt;
	}

	public void setSearchNotificationTxt(final TextView searchNotificationTxt) {
		this.searchNotificationTxt = searchNotificationTxt;
	}

}