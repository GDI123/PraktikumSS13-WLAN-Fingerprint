package de.rwth.ti.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class is responsible for creating and upgrading the database
 * 
 */
public class Storage extends SQLiteOpenHelper {

	private static final int DB_VERSION = 1;

	public Storage(Context context, String dbName) {
		super(context, dbName, null, DB_VERSION);
	}

	/** Called when the database is created for the first time. */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(Building.TABLE_CREATE);
		db.execSQL(Floor.TABLE_CREATE);
		db.execSQL(MeasurePoint.TABLE_CREATE);
		db.execSQL(Scan.TABLE_CREATE);
		db.execSQL(AccessPoint.TABLE_CREATE);
	}

	/** Called when the database needs to be upgraded */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO make a nice upgrade
		clearDatabase();
	}

	public void clearDatabase() {
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL(AccessPoint.TABLE_DROP);
		db.execSQL(Scan.TABLE_DROP);
		db.execSQL(MeasurePoint.TABLE_DROP);
		db.execSQL(Floor.TABLE_DROP);
		db.execSQL(Building.TABLE_DROP);
		onCreate(db);
		db.close();
	}

}
