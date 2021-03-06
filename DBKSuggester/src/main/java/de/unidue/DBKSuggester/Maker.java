package de.unidue.DBKSuggester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Makes megadocuments from a folder of study XML files
 */
public class Maker {

	public static final String[] ZACATEGORIES = {
			"Government, Political System",
			"Political Institutions",
			"International Institutions, Relations, Conditions",
			"Political Ideology",
			"Political Issues",
			"Political Attitudes and Behavior",
			"Political Parties, Organizations",
			"Armed Forces, Defense, Military Affairs",
			"Legal system, Legislation, Law",
			"Economic Systems",
			"Economic Policy, National Economic Situation",
			"Social Policy",
			"Budget and Fiscal Policy",
			"Public Expenditures",
			"Public Revenue",
			"Branches of Economy, Services and Transport",
			"Work and Industry",
			"Occupation, Profession",
			"Income",
			"Patterns of Consumption",
			"Saving, Investment of Money",
			"Stock Market and Monetary Transactions",
			"Society, Culture",
			"Community, Living Environment",
			"Group",
			"Family",
			"Person, Personality, Role",
			"Education, School Systems",
			"University, Research, the Sciences",
			"Religion and Weltanschauung",
			"Technology, Energy",
			"Medicine",
			"Leisure",
			"Communication, Public Opinion, Media",
			"Natural Environment, Nature",
			"Historical Social Research",
			"Historical Studies Data"
	};

	public static final String[] CESSDATOPICS = {
			"Labour and employment",
			"Working conditions",
			"Labour relations / conflict",
			"Unemployment",
			"In-job training",
			"Employment",
			"Retirement",
			"Demography and population",
			"Fertility",
			"Migration",
			"Censuses",
			"Education",
			"Educational policy",
			"Life-long / continuing education",
			"Teaching profession",
			"Vocational education",
			"Post-compulsory education",
			"Compulsory and pre-school education",
			"History",
			"Society and culture",
			"Leisure, tourism and sport",
			"Community, urban and rural life",
			"Cultural activities and participation",
			"Cultural and national identity",
			"Religion and values",
			"Social conditions and indicators",
			"Social change",
			"Social behaviour and attitudes",
			"Time use",
			"Law, crime and legal systems",
			"Legislation",
			"Crime",
			"Legal systems",
			"Rehabilitation / reintegration into society",
			"Law enforcement",
			"Health",
			"Drug abuse, alcohol and smoking",
			"Nutrition",
			"General health",
			"Health care and medical treatment",
			"Health policy",
			"Physical fitness and exercise",
			"Childbearing, family planning and abortion",
			"Specific diseases and medical conditions",
			"Accidents and injuries",
			"Trade, industry and markets",
			"Agricultural, forestry and rural industry",
			"Business / industrial management and organisation",
			"Information and communication",
			"Information society",
			"Mass media",
			"Language and linguistics",
			"Advertising",
			"Teaching packages and test datasets",
			"Politics",
			"Domestic political issues",
			"International politics and organisation",
			"Conflict, security and peace",
			"Political ideology",
			"Mass political behaviour, attitudes / opinion",
			"Government, political systems and organisation",
			"Elections",
			"Psychology",
			"Social stratification and groupings",
			"Elderly",
			"Elites and leadership",
			"Family life and marriage",
			"Gender and gender roles",
			"Equality and inequality",
			"Youth",
			"Children",
			"Minorities",
			"Social exclusion",
			"Social and occupational mobility",
			"Social welfare policy and systems",
			"Specific social services: use and provision",
			"Social welfare policy",
			"Social welfare systems / structures",
			"Transport, travel and mobility",
			"Natural environment",
			"Natural landscapes",
			"Natural resources and energy",
			"Plant and animal distribution",
			"Environmental degradation / pollution and protection",
			"Economics",
			"Rural economics",
			"Income, property and investment / saving",
			"Consumption / consumer behaviour",
			"Economic conditions and indicators",
			"Economic policy",
			"Economic systems and development",
			"Science and technology",
			"Biotechnology",
			"Information technology",
			"Housing and land use planning",
			"Land use and planning",
			"Housing"
	};


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		makeMegadocs();
		//makeMegadocsNoFields();


	}


	/**
	 * For each category in the list: Reads all study XML files from the training directory, takes the
	 * field contents from those who belong to that category and then writes the megadoc to the megadoc
	 * category
	 * (You have to edit the folder paths here, I couldn't get it to work with String[] args input)
	 */
	static void makeMegadocs() {
		File sourceDirectoryCESSDA = new File("/Users/Martina/Desktop/alldata Kopie/cessda/train");
		String megaDirectoryCESSDA = "/Users/Martina/Desktop/alldata Kopie/cessda/mega";

		File sourceDirectoryZA = new File("/Users/Martina/Desktop/alldata Kopie/za/train");
		String megaDirectoryZA = "/Users/Martina/Desktop/alldata Kopie/za/mega";


		File[] xmldocs = sourceDirectoryZA.listFiles();
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));

		for (String category: ZACATEGORIES) {          
			String ids = "";
			String titlesDE = "";
			String titlesEN = "";
			String creators = "";
			String contentsDE = "";
			String contentsEN = "";

			for(int i=0; i < fileList.size(); i++) {
				String filepath = fileList.get(i).getPath();
				Study study = parse(filepath);

				if (study.zaCategory().contains(category)) {
					ids = ids + " " + study.id();
					titlesDE = titlesDE + " " + study.titleDE();
					titlesEN = titlesEN + " " + study.titleEN();
					creators = creators + " " + study.creators();
					contentsDE = contentsDE + " " + study.contentDE();
					contentsEN = contentsEN + " " + study.contentEN();
				};

			}


			Element megadoc = new Element("megadoc");
			Document document = new Document(megadoc); 

			megadoc.addContent(new Element("ids").setText(ids));
			megadoc.addContent(new Element("titlesDE").setText(titlesDE)); 
			megadoc.addContent(new Element("titlesEN").setText(titlesEN));
			megadoc.addContent(new Element("creators").setText(creators));
			megadoc.addContent(new Element("contentsDE").setText(contentsDE));
			megadoc.addContent(new Element("contentsEN").setText(contentsEN));

			XMLOutputter xmlOutput = new XMLOutputter();  
			try {
				String cleancategory = category.replace("/ ", "");
				System.out.println(cleancategory);
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(document, new FileWriter(  
						megaDirectoryZA + "/" + cleancategory + ".xml"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
		
		
		File[] xmldocs2 = sourceDirectoryCESSDA.listFiles();
		ArrayList<File> fileList2 = new ArrayList<File>(Arrays.asList(xmldocs2));

		for (String category: CESSDATOPICS) {          
			String ids = "";
			String titlesDE = "";
			String titlesEN = "";
			String creators = "";
			String contentsDE = "";
			String contentsEN = "";

			for(int i=0; i < fileList2.size(); i++) {
				String filepath = fileList2.get(i).getPath();
				Study study = parse(filepath);

				if (study.cessdaTopics().contains(category)) {
					ids = ids + " " + study.id();
					titlesDE = titlesDE + " " + study.titleDE();
					titlesEN = titlesEN + " " + study.titleEN();
					creators = creators + " " + study.creators();
					contentsDE = contentsDE + " " + study.contentDE();
					contentsEN = contentsEN + " " + study.contentEN();
				};

			}


			Element megadoc = new Element("megadoc");
			Document document = new Document(megadoc); 

			megadoc.addContent(new Element("ids").setText(ids));
			megadoc.addContent(new Element("titlesDE").setText(titlesDE)); 
			megadoc.addContent(new Element("titlesEN").setText(titlesEN));
			megadoc.addContent(new Element("creators").setText(creators));
			megadoc.addContent(new Element("contentsDE").setText(contentsDE));
			megadoc.addContent(new Element("contentsEN").setText(contentsEN));

			XMLOutputter xmlOutput = new XMLOutputter();  
			try {
				String cleancategory = category.replace("/ ", "");
				System.out.println(cleancategory);
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(document, new FileWriter(  
						megaDirectoryCESSDA + "/" + cleancategory + ".xml"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
	}

	
	/**
	 * Turns training document into Study object for easier handling
	 * @param inFile The training document
	 * @return The Study object
	 */
	static Study parse(String inFile) {
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



	/**
	 * Makes megadocuments without fields
	 */
	static void makeMegadocsNoFields() {

		File sourceDirectory = new File("/Users/Martina/Desktop/alldata/cessda/train");
		String megaDirectory = "/Users/Martina/Desktop/alldata/cessda/meganofields";

		//File sourceDirectory = new File("/Users/Martina/Desktop/alldata/za/train");
		//String megaDirectory = "/Users/Martina/Desktop/alldata/za/meganofields";

		File[] xmldocs = sourceDirectory.listFiles();
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));

		for (String category: CESSDATOPICS) {

			String doccontent = "";

			for(int i=0; i < fileList.size(); i++) {
				String filepath = fileList.get(i).getPath();
				Study study = parse(filepath);

				if (study.cessdaTopics().contains(category)) {
					try {
						doccontent = doccontent + " " + new String(Files.readAllBytes(Paths.get(filepath)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				};
			}

			Element megadoc = new Element("megadoc");
			Document document = new Document(megadoc);
			megadoc.addContent(new Element("doccontent").setText(doccontent));

			XMLOutputter xmlOutput = new XMLOutputter();  
			try {
				String cleancategory = category.replace("/ ", "");
				System.out.println(cleancategory);
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(document, new FileWriter(  
						megaDirectory + "/" + cleancategory + ".xml"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  

		}


	}

}
