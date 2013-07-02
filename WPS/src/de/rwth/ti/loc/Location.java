package de.rwth.ti.loc;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import android.net.wifi.ScanResult;
import de.rwth.ti.db.AccessPoint;
import de.rwth.ti.db.Building;
import de.rwth.ti.db.Floor;
import de.rwth.ti.db.MeasurePoint;
import de.rwth.ti.db.Scan;
import de.rwth.ti.share.IMeasureDataHandler;

public class Location {

	private IMeasureDataHandler dataHandler;
	private static long timeSinceFloor = 0;
	private static long timeSinceBuilding = 0;
	private static LocationResult lastScan = null;
	private static LocationResult secondToLastScan = null;
	private static long theTime;
	private static Building tempBuilding;
	private static Floor tempFloor;
	Calendar time = Calendar.getInstance();
	private static List<LocationResult> last_ten_results = new LinkedList(); 
	//LocationResult result = null;
	
	public Location(IMeasureDataHandler dataHandler) {
		this.dataHandler = dataHandler;
	}

	public LocationResult getLocation(List<ScanResult> aps, int compass,
			int kontrollvariable) { // kontrollvariable 0,1,2 steuert das
									// Verhalten der Funktion
									// 0 ueberlaesst den Ablauf den
									// Zeitvariablen
		theTime = time.getTime().getTime(); // 1 erzwingt Gebaeudesuche
											// 2 erzwingt FloorSuche
		aps = deleteDoubles(aps);
		
		if (theTime > timeSinceBuilding + 40000 || kontrollvariable == 1) {
			Building lastBuilding=tempBuilding;
			Floor lastFloor=tempFloor;
			tempBuilding = findBuilding(aps);
			tempFloor = findMap(aps, tempBuilding);
			if (lastBuilding!=null){
				if ((lastBuilding.getId()!=tempBuilding.getId()) || (lastFloor.getId()!=tempFloor.getId())){
					last_ten_results.clear();
					secondToLastScan=null;
					lastScan=null;
				}
			}
			timeSinceFloor = theTime;
			timeSinceBuilding = theTime;
		} else if (theTime > timeSinceFloor + 10000 || kontrollvariable == 2) {
			Floor lastFloor=tempFloor;
			tempFloor = findMap(aps, tempBuilding);
			if (lastFloor!=null){
				if (lastFloor.getId()!=tempFloor.getId()){
					last_ten_results.clear();
					secondToLastScan=null;
					lastScan=null;
				}
			}
			timeSinceFloor = theTime;
			timeSinceBuilding = theTime;
		}
		
		LocationResult result = findMP(aps, tempFloor, tempBuilding, compass);
		secondToLastScan = lastScan;
		lastScan = result;
		while (secondToLastScan == null){
			last_ten_results.add(result);
			result = findMP(aps, tempFloor, tempBuilding, compass);
			secondToLastScan = lastScan;
			lastScan = result;
		}
		if(last_ten_results.size()>=10){
			last_ten_results.remove(0);
		}
		
		if (!(lastScan == null || result == null)){
			if (filter_chooser(last_ten_results, result)){
				result = filterLP2(secondToLastScan, lastScan, result);
			}
			else{
				result = filterLP(secondToLastScan, lastScan, result);
			}
			
		}
		last_ten_results.add(result);
		return result;
	}

	private LocationResult filterLP(LocationResult secondToLastScan, LocationResult lastScan, LocationResult scan){
		LocationResult result;
		double x=0;
		double y=0;
//		if (scan.getMap()!=lastScan.getMap() || scan.getMap()!=secondToLastScan.getMap()){
//			return scan;
//		}
		
		{
			x=(0.7*scan.getX()+0.2*lastScan.getX()+0.1*secondToLastScan.getX());
			y=(0.7*scan.getY()+0.2*lastScan.getY()+0.1*secondToLastScan.getY());
		}
		result = new LocationResult(scan.getBuilding(), scan.getMap(), x, y);
		
		return result;
	}
	
	private boolean filter_chooser(List <LocationResult> last_ten_results, LocationResult aktuell){
		double x10=0;
		double y10=0;
		double x1=0;
		double y1=0;
		for (int j=1; j<last_ten_results.size(); j++){
			x10+=Math.abs(last_ten_results.get(j).getX()-last_ten_results.get(j-1).getX());
			y10+=Math.abs(last_ten_results.get(j).getY()-last_ten_results.get(j-1).getY());
		}
		x10=x10/(last_ten_results.size()-1);
		y10=y10/(last_ten_results.size()-1);
		x1=Math.abs(aktuell.getX()-last_ten_results.get(last_ten_results.size()-1).getX());
		y1=Math.abs(aktuell.getY()-last_ten_results.get(last_ten_results.size()-1).getY());
		if ((x1 > 1.5*x10) || (y1 > 1.5*y10)){
			return true;
		}
		else {
			return false;
		}
	
	}
	
	private LocationResult filterLP2(LocationResult secondToLastScan, LocationResult lastScan, LocationResult scan){
		LocationResult result;
		double x=0;
		double y=0;
//		if (scan.getMap()!=lastScan.getMap() || scan.getMap()!=secondToLastScan.getMap()){
//			return scan;
//		}
		
		{
			x=(0.4*scan.getX()+0.4*lastScan.getX()+0.2*secondToLastScan.getX());
			y=(0.4*scan.getY()+0.4*lastScan.getY()+0.2*secondToLastScan.getY());
		}
		result = new LocationResult(scan.getBuilding(), scan.getMap(), x, y);
		
		return result;
	}
	
	private Building findBuilding(List<ScanResult> aps) {
		if (aps.isEmpty()) {
			return null;
		}
		String mac = aps.get(0).BSSID;
		List<AccessPoint> entries = dataHandler.getAccessPoint(mac);
		if (entries.isEmpty()) {
			return null;
		}
		AccessPoint ap = entries.get(0);
		Scan scan = dataHandler.getScan(ap);
		if (scan == null) {
			return null;
		}
		MeasurePoint mp = dataHandler.getMeasurePoint(scan);
		if (mp == null) {
			return null;
		}
		Floor map = dataHandler.getFloor(mp);
		if (map == null) {
			return null;
		}
		Building result = dataHandler.getBuilding(map);
		return result;
	}

	private Floor findMap(List<ScanResult> aps, Building b) {
		if (aps.isEmpty() || b == null) {
			return null;
		}
		String mac = aps.get(0).BSSID;
		List<AccessPoint> entries = dataHandler.getAccessPoint(mac);
		AccessPoint ap = entries.get(0);
		Scan scan = dataHandler.getScan(ap);
		MeasurePoint mp = dataHandler.getMeasurePoint(scan);
		Floor floor = dataHandler.getFloor(mp);
		return floor;
	}

	private LocationResult findMP(List<ScanResult> aps, Floor map,
			Building building, int compass) {
		if (aps.isEmpty() || map == null) {
			return null;
		}
		List<Scan> scanEntries = dataHandler.getScans(map, compass);
		List<ScanError> errorList = new LinkedList<ScanError>();
		for (int j = 0; j < scanEntries.size(); j++) {
			double errorValue = 0;
			List<AccessPoint> entries = dataHandler.getAccessPoints(scanEntries
					.get(j));
			for (int k = 0; k < 3 && k < aps.size(); k++) {
				String mac = aps.get(k).BSSID;
				int l;
				boolean success = false;
				for (l = 0; l < entries.size(); l++) {
					if (mac.compareTo(entries.get(l).getBssid()) == 0) {
						success = true;
						break;
					}
				}
				if (success) {
					errorValue += (double) ((100 + (double) aps.get(k).level) / 100)
							* (Math.abs((int) ((aps.get(k).level) - entries
									.get(l).getLevel())));
				} else {
					errorValue += (double) ((100 + (double)aps.get(k).level) / 100)
							* (Math.abs((int) ((aps.get(k).level) + 100)));
				}
			}
			if (errorValue==0){
				LocationResult result = new LocationResult(building, map, dataHandler.getMeasurePoint(scanEntries.get(j)).getPosx(),
						dataHandler.getMeasurePoint(scanEntries.get(j)).getPosy());
				return result;
			}
			
			ScanError scanErrorObject = new ScanError();
			scanErrorObject.setScanError(scanEntries.get(j), errorValue);
			errorList.add(scanErrorObject);
		}
		errorList = sortScanError(errorList);
		double x = 0;
		double y = 0;
		double errorSum = 0;
		for (int h = 0; h < errorList.size(); h++) {
			if (h > 3) {
				break;
			}
			x += (1 / (errorList.get(h).getError()))
					* dataHandler.getMeasurePoint(errorList.get(h).getScan())
							.getPosx();
			y += (1 / (errorList.get(h).getError()))
					* dataHandler.getMeasurePoint(errorList.get(h).getScan())
							.getPosy();
			errorSum += (1 / (errorList.get(h).getError()));
		}
		if (errorSum != 0) {
			x = x / errorSum;
			y = y / errorSum;
		}
		LocationResult result = new LocationResult(building, map, x, y);
		return result;

	}

	private List<ScanResult> deleteDoubles (List <ScanResult> aps){
		List <ScanResult> tempListe = new LinkedList<ScanResult>();
		//tempListe.add(aps.get(0));
		for (int i=0; i<aps.size(); i++){
			boolean elementVorhanden=false;
			for (int j=0; j<tempListe.size(); j++){				
				if ((aps.get(i).BSSID.substring(0, (aps.get(i).BSSID.length()-1)).compareTo(
						tempListe.get(j).BSSID.substring(0, (tempListe.get(j).BSSID.length()-1)))==0)){
					elementVorhanden=true;
				}
				if(elementVorhanden){
					break;
				}
			}
			if(!elementVorhanden){
				tempListe.add(aps.get(i));
			}
		}
		return tempListe;
	}
	
	private List<ScanError> sortScanError(List<ScanError> scanErrorList) {
		for (int i = 0; i < scanErrorList.size(); i++) {
			for (int j = 0; j < scanErrorList.size() - 1; j++) {
				if (scanErrorList.get(j).getError() > scanErrorList.get(j + 1)
						.getError()) {
					ScanError tempobject = scanErrorList.get(j);
					scanErrorList.set(j, scanErrorList.get(j + 1));
					scanErrorList.set(j + 1, tempobject);
				}
			}
		}

		return scanErrorList;
	}
}
