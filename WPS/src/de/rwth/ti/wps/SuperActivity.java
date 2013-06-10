package de.rwth.ti.wps;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import de.rwth.ti.common.CompassManager;
import de.rwth.ti.common.Constants;
import de.rwth.ti.common.ScanManager;
import de.rwth.ti.db.StorageHandler;

public abstract class SuperActivity extends Activity {

	public static final String PACKAGE_NAME = "de.rwth.ti.wps";

	/*
	 * Own classes
	 */
	protected ScanManager scm;
	protected StorageHandler storage;
	protected CompassManager cmgr;

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
			storage = new StorageHandler(this);
		}

		// Setup compass manager
		if (cmgr == null && hasCompass) {
			cmgr = new CompassManager(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	/** Called when the activity is first created or restarted */
	@Override
	public void onStart() {
		super.onStart();
		storage.onStart();
		scm.onStart();
		cmgr.onStart();
	}

	/** Called when the activity is finishing or being destroyed by the system */
	@Override
	public void onStop() {
		super.onStop();
		storage.onStop();
		scm.onStop();
		cmgr.onStop();
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Start other Activities, when the related MenuItem is selected
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_localisation:
			intent = new Intent(this, MainActivity.class);
			break;
		case R.id.action_measure:
			intent = new Intent(this, MeasureActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		case R.id.action_new_floor:
			intent = new Intent(this, NewFloorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			break;
		case R.id.menu_export:
			try {
				storage.exportDatabase(Constants.LOCAL_DB_NAME);
				Toast.makeText(getBaseContext(),
						R.string.database_export_success, Toast.LENGTH_SHORT)
						.show();
			} catch (IOException e) {
				Toast.makeText(getBaseContext(), e.toString(),
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_import:
			storage.importDatabase(Environment.getDataDirectory()
					+ File.separator + Constants.LOCAL_DB_NAME);
			Toast.makeText(getBaseContext(), R.string.database_import_success,
					Toast.LENGTH_SHORT).show();
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