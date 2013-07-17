package de.rwth.ti.common;

import de.rwth.ti.db.MeasurePoint;

public class WPSQuadKnot {

	private MeasurePoint value = null;
	private WPSQuadKnot tl, tr, bl, br;
	private float xMin, xMax, yMin, yMax;

	public WPSQuadKnot(float xMi, float xMa, float yMi, float yMa) {
		xMin = xMi;
		xMax = xMa;
		yMin = yMi;
		yMax = yMa;
		tl = null;
		tr = null;
		bl = null;
		br = null;
	}

	public MeasurePoint getValue() {
		return value;
	}

	public void setValue(MeasurePoint mp) {
		value = mp;
	}

	public void addMPoint(MeasurePoint mp) {
		if (tl == null && value == null) {
			value = mp;
		} else {
			if (tl == null) {
				tl = new WPSQuadKnot(xMin, (xMax + xMin) / 2, yMin,
						(yMin + yMax) / 2);
				tr = new WPSQuadKnot((xMax + xMin) / 2, xMax, yMin,
						(yMin + yMax) / 2);
				bl = new WPSQuadKnot(xMin, (xMax + xMin) / 2,
						(yMin + yMax) / 2, yMax);
				br = new WPSQuadKnot((xMax + xMin) / 2, xMax,
						(yMin + yMax) / 2, yMax);
			}
			if (value != null) {
				if (value.getPosx() > (xMax + xMin) / 2) {
					if (value.getPosy() > (yMax + yMin) / 2) {
						br.addMPoint(value);
					} else {
						tr.addMPoint(value);
					}
				} else {
					if (value.getPosy() > (yMax + yMin) / 2) {
						bl.addMPoint(value);
					} else {
						tl.addMPoint(value);
					}
				}
			}
			value = null;
			if (mp.getPosx() > (xMax + xMin) / 2) {
				if (mp.getPosy() > (yMax + yMin) / 2) {
					br.addMPoint(mp);
				} else {
					tr.addMPoint(mp);
				}
			} else {
				if (mp.getPosy() > (yMax + yMin) / 2) {
					bl.addMPoint(mp);
				} else {
					tl.addMPoint(mp);
				}
			}
		}
	}

	public WPSQuadKnot getKnot(float x, float y) {
		if (x > ((xMax + xMin) / 2)) {
			if (y > ((yMax + yMin) / 2)) {
				return br;
			} else {
				return tr;
			}
		} else {
			if (y > ((yMax + yMin) / 2)) {
				return bl;
			} else {
				return tl;
			}
		}
	}

}
