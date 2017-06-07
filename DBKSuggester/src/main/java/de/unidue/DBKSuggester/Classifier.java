package de.unidue.DBKSuggester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;
import org.elasticsearch.index.query.QueryBuilders;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;


public abstract class Classifier {

	protected TransportClient client;

	public Classifier(TransportClient client) {
		this.client = client;
	}

	public Study parse(String inFile) {

		String id = "";
		String titleDE = "";
		String titleEN = "";
		List<String> creators = new ArrayList<String>();
		String contentDE = "";
		String contentEN = "";
		List<String> zaCategory = new ArrayList<String>();
		List<String> cessdaTopics = new ArrayList<String>();

		org.jdom2.Document doc = new org.jdom2.Document();
		try {
			doc = new SAXBuilder().build(inFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
			return null;
		}
		Element instance = doc.getRootElement();
		Namespace sNS = Namespace.getNamespace("s","ddi:studyunit:3_1");
		Element studyUnit = instance.getChild("StudyUnit",sNS);
		Namespace rNS = Namespace.getNamespace("r","ddi:reusable:3_1");

		Element userID = studyUnit.getChild("UserID", rNS);
		id = userID.getText();

		Element citation = studyUnit.getChild("Citation", rNS);

		List<Element> titles = citation.getChildren("Title", rNS);
		for (int i=0; i<titles.size(); i++) {
			Element title = titles.get(i);
			Attribute attribute = title.getAttribute("lang", Namespace.XML_NAMESPACE);
			if (attribute.getValue().equals("en")) {
				titleEN = title.getText();
			} else if (attribute.getValue().equals("de")) {
				titleDE = title.getText();
			}
		}

		List<Element> creatorsE = citation.getChildren("Creator", rNS);
		for (int i=0; i<creatorsE.size(); i++) {
			Element creator = creatorsE.get(i);
			creators.add(creator.getText());
		}

		List<Element> abstracts = studyUnit.getChildren("Abstract", sNS);
		for (int i=0; i<abstracts.size(); i++) {
			Element content = abstracts.get(i).getChild("Content", rNS);
			Attribute attribute = content.getAttribute("lang", Namespace.XML_NAMESPACE);
			if (attribute.getValue().equals("en")) {
				contentEN = content.getText();
			} else if (attribute.getValue().equals("de")) {
				contentDE = content.getText();
			}
		}

		Element coverage = studyUnit.getChild("Coverage", rNS);
		Element topicalCoverage = coverage.getChild("TopicalCoverage", rNS);
		List<Element> subjects = topicalCoverage.getChildren("Subject", rNS);
		for (int i=0; i<subjects.size(); i++) {
			Element subject = subjects.get(i);
			Attribute attribute = subject.getAttribute("codeListID");
			Attribute lang = subject.getAttribute("lang", Namespace.XML_NAMESPACE);
			if (attribute.getValue().equals("ZA-Categories") && lang.getValue().equals("en")) {
				zaCategory.add(subject.getText());
			} else if (attribute.getValue().equals("CESSDA Topic Classification") && lang.getValue().equals("en")) {
				cessdaTopics.add(subject.getText());
			}
		}

		return new Study(id, titleDE, titleEN, creators, contentDE, contentEN, zaCategory, cessdaTopics);	
	}



	public XContentBuilder buildXContent(Study study) {
		XContentBuilder newDoc = null;
		
		if (study != null) {
			try {
				newDoc = XContentFactory.jsonBuilder()
						.startObject()
						.field("category", "")
						.field("ids", study.id())
						.field("titlesDE", study.titleDE())
						.field("titlesEN", study.titleEN())
						.field("creators", study.creators())
						.field("contentsDE", study.contentDE())
						.field("contentsEN", study.contentEN())
						.endObject();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		return newDoc;
	}


	abstract List<List<Map<String, String>>> classify(String inFile);






}
