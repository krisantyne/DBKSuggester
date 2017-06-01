package de.unidue.DBKSuggester;

import java.util.List;

public class Study {

	String id;
	String titleDE;
	String titleEN;
	List<String> creators;
	String contentDE;
	String contentEN;
	List<String> zaCategory;
	List<String> cessdaTopics;

	public Study (String iId, String iTitleDE, String iTitleEN, List<String> iCreators, String iContentDE, String iContentEN, List<String> iZaCategory, List<String> iCessdaTopics) {
		id = iId;
		titleDE = iTitleDE;
		titleEN = iTitleEN;
		creators = iCreators;
		contentDE = iContentDE;
		contentEN = iContentEN;
		zaCategory = iZaCategory;
		cessdaTopics = iCessdaTopics;
	}

	public String id() {return id;}
	public String titleDE() {return titleDE;}
	public String titleEN() {return titleEN;}
	public List<String> creators() {return creators;}
	public String contentDE() {return contentDE;}
	public String contentEN() {return contentEN;}
	public List<String> zaCategory() {return zaCategory;}
	public List<String> cessdaTopics() {return cessdaTopics;}
}
