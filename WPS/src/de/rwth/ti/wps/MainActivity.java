package de.rwth.ti.wps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.rwth.ti.common.Cardinal;
import de.rwth.ti.common.CompassManager;
import de.rwth.ti.common.Constants;
import de.rwth.ti.common.IPMapView;
import de.rwth.ti.common.QualityCheck;
import de.rwth.ti.db.Floor;
import de.rwth.ti.db.MeasurePoint;
import de.rwth.ti.loc.Location;
import de.rwth.ti.loc.LocationResult;

/**
 * This is the main activity class
 * 
 */
public class MainActivity extends SuperActivity implements
		OnCheckedChangeListener {

	private ToggleButton checkLoc;
	private IPMapView viewMap;
	private ImageButton btCenter;
	private Button btZoom;
	private ImageButton forceRefresh;
	private BroadcastReceiver wifiReceiver;
	private int control;
	private TextView measureTimeView;
	private TextView locInfoView;
	private Location myLoc;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// create app sd directory
		File sdDir = new File(Constants.SD_APP_DIR);
		if (sdDir.exists() == false) {
			if (sdDir.mkdirs() == false) {
				makeToast(R.string.error_sd_dir);
			}
		}
		checkLoc = (ToggleButton) findViewById(R.id.toggleLocalization);
		checkLoc.setOnCheckedChangeListener(this);
		viewMap = (IPMapView) findViewById(R.id.viewMap);
		viewMap.setMeasureMode(false);
		viewMap.setOnScaleChangeListener(new ScaleChangeListener());
		btCenter = (ImageButton) findViewById(R.id.centerButton);
		btZoom = (Button) findViewById(R.id.zoomButton);
		forceRefresh = (ImageButton) findViewById(R.id.forceRefreshButton);
		measureTimeView = (TextView) findViewById(R.id.measureTime);
		locInfoView = (TextView) findViewById(R.id.debugInfo);
		btZoom.setText("x1.0");
		wifiReceiver = new MyReceiver();
		control = 0;
		myLoc = new Location(getStorage());
	}

	/** Called when the activity is first created or restarted */
	@Override
	public void onStart() {
		super.onStart();
		checkLoc.setChecked(true);
		if (checkLoc.isChecked() == true) {
			getScanManager().startAutoScan(Constants.AUTO_SCAN_SEC);
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			this.registerReceiver(wifiReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}
	}

	/** Called when the activity is finishing or being destroyed by the system */
	@Override
	public void onStop() {
		checkLoc.setChecked(false);
		super.onStop();
		getScanManager().stopAutoScan();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		try {
			this.unregisterReceiver(wifiReceiver);
		} catch (IllegalArgumentException ex) {
			// just ignore it
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton view, boolean state) {
		if (view == checkLoc) {
			if (state == true) {
				getScanManager().startAutoScan(Constants.AUTO_SCAN_SEC);
				getWindow().addFlags(
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else {
				getScanManager().stopAutoScan();
				getWindow().clearFlags(
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
			// research for building and floor
			control = 1;
		}
	}

	private class MyReceiver extends BroadcastReceiver {

		private WifiManager wifi = MainActivity.this.getScanManager().getWifi();
		private CompassManager comp = MainActivity.this.getCompassManager();
		private Floor lastMap;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (checkLoc != null && checkLoc.isChecked() == true) {
				try {
					final List<ScanResult> results = wifi.getScanResults();
					final Cardinal direction = Cardinal.getFromAzimuth(comp
							.getMeanAzimut());
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							final long start = System.currentTimeMillis();
							final LocationResult myLocRes = myLoc.getLocation(
									results, direction, control);
							control = 0;
							final long stop = System.currentTimeMillis();
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									String measureTime = "APs: "
											+ results.size() + "\nLocTime: "
											+ (stop - start) + "ms";
									int errorCode = myLocRes.getError();
									if (errorCode != 0) {
										String errorMessage = "";
										switch (errorCode) {
										case 1:
											errorMessage = String
													.format(getString(R.string.error_not_found),
															getString(R.string.building));
											break;
										case 2:
											errorMessage = String
													.format(getString(R.string.error_not_found),
															getString(R.string.floor));
											break;
										case 3:
											errorMessage = getString(R.string.error_loc_aps_not_found);
											break;
										case 4:
											errorMessage = getString(R.string.error_loc_empty_map);
											break;
										case 5:
											errorMessage = String
													.format(getString(R.string.error_not_found),
															getString(R.string.position));
											break;
										default:
											errorMessage = "Error: "
													+ errorCode;
											break;
										}
										locInfoView.setText(errorMessage);
									} else {
										Floor map = myLocRes.getFloor();
										long mStart = System
												.currentTimeMillis();
										if (lastMap == null
												|| map.getId() != lastMap
														.getId()) {
											// map has changed reload it
											byte[] file = myLocRes.getFloor()
													.getFile();
											if (file != null) {
												ByteArrayInputStream bin = new ByteArrayInputStream(
														file);
												viewMap.newMap(bin);
												List<MeasurePoint> mpl = getStorage()
														.getMeasurePoints(map);
												for (MeasurePoint mp : mpl) {
													mp.setQuality(QualityCheck
															.getQuality(
																	getStorage(),
																	mp));
													viewMap.addOldPoint(mp);
												}
											} else {
												makeToast(R.string.error_no_floor_file);
											}
										}
										long mStop = System.currentTimeMillis();
										viewMap.setPoint(myLocRes);
										if (lastMap == null
												|| map.getId() != lastMap
														.getId()) {
											// map has changed focus position
											// once
											lastMap = map;
											viewMap.zoomLocationPoint();
										}
										measureTime += "\nMap: "
												+ (mStop - mStart) + "ms";
										String locationInfo = myLocRes
												.getFloor().getName()
												+ " in "
												+ myLocRes.getBuilding()
														.getName();
										locInfoView.setText(locationInfo);
									}
									measureTimeView.setText(measureTime);
								}
							});
						}
					});
					t.start();
				} catch (Exception ex) {
					locInfoView.setText(ex.toString());
				}
			}
		}
	}

	public void centerPosition(View view) {
		if (view == btCenter) {
			viewMap.focusLocationPoint();
		}
	}

	public void zoomPosition(View view) {
		if (view == btZoom) {
			viewMap.zoomLocationPoint();
		}
	}

	public void forceBuilding(View view) {
		if (view == forceRefresh) {
			control = 1;
		}
	}

	private class ScaleChangeListener implements
			IPMapView.OnScaleChangeListener {

		@Override
		public void onScaleChange(float scale) {
			if (btZoom == null) {
				// do nothing
				return;
			}
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append(Float.toString(scale));
			int ind = strBuilder.indexOf(".");
			final String zStr;
			if (ind != -1) {
				int len = Math.min(ind + 3, strBuilder.length());
				zStr = strBuilder.substring(0, len);
			} else {
				zStr = "x" + strBuilder.toString();
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					btZoom.setText(zStr);
				}
			});
		}

	}

}
