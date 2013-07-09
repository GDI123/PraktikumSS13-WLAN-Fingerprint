package de.rwth.ti.wps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import de.rwth.ti.common.CompassManager;
import de.rwth.ti.common.Constants;
import de.rwth.ti.common.ScanManager;
import de.rwth.ti.db.StorageHandler;

/**
 * This is the super activity for all other acitivities
 * 
 */
public abstract class SuperActivity extends Activity {

	private ScanManager scm;
	private StorageHandler storage;
	private CompassManager cmgr;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		onCreate(savedInstanceState, true, true, true);
	}

	protected void onCreate(Bundle savedInstanceState, boolean hasCompass) {
		onCreate(savedInstanceState, hasCompass, true, true);
	}

	protected void onCreate(Bundle savedInstanceState, boolean hasCompass,
			boolean hasScan) {
		onCreate(savedInstanceState, hasCompass, hasScan, true);
	}

	protected void onCreate(Bundle savedInstanceState, boolean hasCompass,
			boolean hasScan, boolean hasStorage) {
		super.onCreate(savedInstanceState);

		// Setup Wifi
		if (scm == null && hasScan) {
			scm = new ScanManager(this);
		}

		// Setup database storage
		if (storage == null && hasStorage) {
			storage = new StorageHandler(this, Constants.DB_NAME);
		}

		// Setup compass manager
		if (cmgr == null && hasCompass) {
			cmgr = new CompassManager(this, Constants.COMPASS_TIMESPAN);
		}
	}

	/** Called when the activity is first created or restarted */
	@Override
	public void onStart() {
		super.onStart();
		if (cmgr != null) {
			cmgr.onStart();
		}
		if (scm != null) {
			scm.onStart();
		}
		if (storage != null) {
			storage.onStart();
		}
//		// launch default activity for debugging only
//		Intent intent = new Intent(this, DebugActivity.class);
//		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//		startActivity(intent);
	}

	/** Called when the activity is finishing or being destroyed by the system */
	@Override
	public void onStop() {
		super.onStop();
		if (cmgr != null) {
			cmgr.onStop();
		}
		if (scm != null) {
			scm.onStop();
		}
		if (storage != null) {
			storage.onStop();
		}
	}

	public ScanManager getScanManager() {
		return scm;
	}

	public StorageHandler getStorage() {
		return storage;
	}

	public CompassManager getCompassManager() {
		return cmgr;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Start other Activities, when the related MenuItem is selected
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_new_floor:
			intent = new Intent(this, NewFloorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		case R.id.action_measure:
			intent = new Intent(this, MeasureActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		case R.id.action_data:
			intent = new Intent(this, DataActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		case R.id.action_debug:
			intent = new Intent(this, DebugActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		if (intent != null) {
			startActivity(intent);
		}
		return true;
	}
}
