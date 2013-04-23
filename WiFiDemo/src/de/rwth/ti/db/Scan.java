package de.rwth.ti.db;

/**
 * This class represents the table where each scan is stored
 * 
 * @author tcuje
 * 
 */
public class Scan {

	public static final String TABLE_NAME = "scans";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";

	public static final String[] ALL_COLUMNS = { COLUMN_ID, COLUMN_NAME };

	public static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME
			+ "(" + COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_NAME + " text null);";
	public static final String TABLE_DROP = "DROP TABLE IF EXISTS "
			+ TABLE_NAME;

	private long id;
	private String name;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
