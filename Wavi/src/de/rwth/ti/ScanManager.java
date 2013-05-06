package de.rwth.ti;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import de.rwth.ti.R.string;
import de.rwth.ti.db.Checkpoint;
import de.rwth.ti.db.Scan;

/**
 * This class handles wifi scanning, provides auto scanning and is activated if
 * scan results are available
 * 
 * @author tcuje
 * 
 */
public class ScanManager extends BroadcastReceiver {

	private Wavi app;
	private WifiManager wifi;
	private WifiLock wl;
	private AlertDialog.Builder builder;
	private Timer tim;
	private TimerTask scantask;
	private boolean onlineMode;
	private Checkpoint cpoint;

	public ScanManager(Wavi app) {
		this.app = app;
		// Setup WiFi
		wifi = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
		wl = wifi.createWifiLock("Wavi");

		if (builder == null) {
			builder = new AlertDialog.Builder(app);
			android.content.DialogInterface.OnClickListener dialogOnClick = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						wifi.setWifiEnabled(true);
						wifi.startScan();
						break;
					case DialogInterface.BUTTON_NEGATIVE:
						break;
					}
				}
			};
			builder.setPositiveButton(string.text_yes, dialogOnClick);
			builder.setNegativeButton(string.text_no, dialogOnClick);
		}

		// Setup auto scan timer
		if (tim == null) {
			tim = new Timer();
		}
		if (scantask == null) {
			scantask = new TimerTask() {
				@Override
				public void run() {
					wifi.startScan();
				}
			};
		}
	}

	public void onStart() {
		app.registerReceiver(this, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wl.acquire();
	}

	public void onStop() {
		try {
			app.unregisterReceiver(this);
		} catch (IllegalArgumentException ex) {
			// just ignore it
		}
		wl.release();
	}

	public void startSingleScan(Checkpoint mp) {
		cpoint = mp;
		// check if wifi is enabled or not
		if (wifi.isWifiEnabled() == false) {
			builder.setMessage(string.text_wifistate).show();
		} else {
			wifi.startScan();
		}
	}

	public void startAutoScan(int period) {
		tim.schedule(scantask, 0, period);
	}

	public void stopAutoScan() {
		tim.cancel();
	}

	public List<ScanResult> getScanResults() {
		List<ScanResult> result = wifi.getScanResults();
		return result;
	}

	@Override
	public void onReceive(Context c, Intent intent) {
		List<ScanResult> results = app.getScanManager().getScanResults();
		if (onlineMode == false) {
			app.textStatus.setText("Folgende Wifi's gefunden:\n");
			if (results != null && !results.isEmpty()) {
				Date d = new Date();
				Scan scan = app.getStorage()
						.addScan(cpoint.getId(), d.getTime() / 1000,
								app.getCompassManager().getAzimut());
				for (ScanResult result : results) {
					app.textStatus.append(result.BSSID + "\t" + result.level
							+ "\t" + result.frequency + "\t'" + result.SSID
							+ "'\n");
					app.getStorage().addAp(scan, result.BSSID, result.level,
							result.frequency, result.SSID, result.capabilities);
				}
			} else {
				app.textStatus.append("keine");
			}
		}
	}

	public void setOnlineMode(boolean state) {
		onlineMode = state;
	}

}
