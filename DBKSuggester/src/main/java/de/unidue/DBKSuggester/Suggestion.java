package de.unidue.DBKSuggester;

import java.util.List;

public class Suggestion {

	List<String> cessdaCats;
	List<String> zaCats;
	String tooltipC;
	String tooltipZ;
	
	
	public Suggestion(List<String> cc, List<String> zc, String tc, String tz) {
		cessdaCats = cc;
		zaCats = zc;
		tooltipC = tc;
		tooltipZ = tz;
	}


	public List<String> getCessdaCats() {
		return cessdaCats;
	}


	public List<String> getZaCats() {
		return zaCats;
	}


	public String getTooltipC() {
		return tooltipC;
	}


	public String getTooltipZ() {
		return tooltipZ;
	}
	
}
