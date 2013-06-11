package de.rwth.ti.common;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * This class reads the magnetic sensor and provides azimut angle information
 * 
 */
public class CompassManager implements SensorEventListener {

	public enum Direction {
		NORTH, EAST, SOUTH, WEST
	}

	private SensorManager sensor;
	private Sensor accel;
	private Sensor magnet;
	private float[] gravity;
	private float[] geomagnetic;
	private double azimut;

	public interface OnCustomEventListener {
		public void onEvent();
	}

	public CompassManager(Activity app) {
		sensor = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
		accel = sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnet = sensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		azimut = 0;
	}

	public void onStart() {
		sensor.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
		sensor.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI);
	}

	public void onStop() {
		sensor.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			gravity = event.values;
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			geomagnetic = event.values;
		}
		if (gravity != null && geomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, gravity,
					geomagnetic);
			if (success == true) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[0] * 360.0 / (2.0 * Math.PI);
			}
		}
	}

	/**
	 * 
	 * @return Returns the last known azimut in degrees, initialized with zero
	 */
	public double getAzimut() {
		return azimut;
	}

}
