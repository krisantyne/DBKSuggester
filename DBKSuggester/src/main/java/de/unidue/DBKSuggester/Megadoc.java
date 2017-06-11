package de.unidue.DBKSuggester;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * The main class that connects the UI with the parts of the application
 */
public class Megadoc {


	private TransportClient client;

	private Indexer indexer;
	private Classifier classifier;
	private Evaluation evaluation;

	private File currentFile;

	public String[] ZACATEGORIES = {
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
	public String[] CESSDATOPICS = {
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

	public String path = "";
	

	/**
	 * Initialization creates the TransportClient for communicating with Elasticsearch,
	 * reads the properties file and initializes the other application parts
	 */
	public Megadoc(){

		Settings settings = Settings.builder()
				.put("client.transport.sniff", true).build();
		try {
			client = new PreBuiltTransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InputStream inputStream = null;
		
		try {
			Properties prop = new Properties();
			String propFileName = "dbksuggester.properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
 
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			// get the property value and print it out
			path = prop.getProperty("path");
 
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		indexer = new Indexer(client, this);

		//classifier = new DotClassifier(client);
		classifier = new MLTClassifier(client);

		evaluation = new Evaluation(client, classifier, this);


	}


	/**
	 * Tells Indexer to create or reset Megadoc index
	 */
	public void resetIndex() {
		indexer.makeIndex();
	}

	/**
	 * Tells Evaluation to run automatic evaluation with Megadoc
	 */
	public void evaluate() {
		evaluation.evaluate();
	}

	/**
	 * Tells Indexer to create or reset NonMegadoc index
	 */
	public void nonMegaResetIndex() {
		evaluation.makeIndexNonMega();
	}

	/**
	 * Tells Evaluation to run automatic evaluation with NonMegadoc
	 */
	public void nonMegaEvaluate() {
		evaluation.nonMegaEvaluate();
	}

	/**
	 * Tells Indexer to create or reset Megadoc without fields index
	 */
	public void noFieldsResetIndex() {
		evaluation.makeIndexNoFields();
	}

	/**
	 * Tells Evaluation to run automatic evaluation with Megadoc without fields
	 */
	public void noFieldsEvaluate() {
		evaluation.noFieldsEvaluate();
	}


	/**
	 * Extracts Study object from uploaded file
	 * @param inFile The uploaded file
	 * @return The extracted Study
	 */
	public Study getStudy(File inFile) {
		currentFile = inFile;
		return classifier.parse(inFile.getPath());
	}

	/**
	 * Classifies the study from the uploaded file to create the category suggestions and
	 * the tooltips from the ids.
	 * @param inFile The uploaded file
	 * @return A Suggestion object that includes the category suggestions and the text for
	 * the tooltips
	 */
	public Suggestion makeSuggestion(File inFile) {

		List<List<Map<String, String>>> result = classifier.classify(inFile.getPath());

		if (result != null) {

			List<Map<String, String>> cessdaMap = result.get(0);
			List<Map<String, String>> zaMap = result.get(1);

			List<String> cessdaList = new ArrayList<String>();
			List<String> zaList = new ArrayList<String>();


			String tooltipC = "";
			String tooltipZ = "";

			Pattern pattern = Pattern.compile("\\d{4}");
			Random randomGenerator = new Random();

			for (Map<String, String> c : cessdaMap) {
				for (Map.Entry<String, String> entry : c.entrySet()) {

					String cat = entry.getKey();
					cessdaList.add(cat);

					String ids = entry.getValue();

					tooltipC += "<b>" + cat + "</b>: ";

					Matcher matcher = pattern.matcher(ids);
					List<String> matches = new ArrayList<String>();

					while (matcher.find()) {
						matches.add(matcher.group());
					}

					for (int i=0; i<=matches.size(); i++) {
						int randomno = randomGenerator.nextInt(matches.size());
						String randomdoc = matches.get(randomno);
						String addition = "<a target=\"_blank\" href=\"https://dbk.gesis.org/dbksearch/SDesc2.asp?no="+randomdoc+"\">" + randomdoc + "</a> ";
						tooltipC += addition;
						if (i == 4) break;
					}

					tooltipC += "<br>";

				}
			}

			for (Map<String, String> z : zaMap) {
				for (Map.Entry<String, String> entry : z.entrySet()) {

					String cat = entry.getKey();
					zaList.add(cat);

					String ids = entry.getValue();

					tooltipZ += "<b>" + cat + "</b>: ";

					Matcher matcher = pattern.matcher(ids);
					List<String> matches = new ArrayList<String>();

					while (matcher.find()) {
						matches.add(matcher.group());
					}

					for (int i=0; i<=5; i++) {
						int randomno = randomGenerator.nextInt(matches.size());
						String randomdoc = matches.get(randomno);
						String addition = "<a target=\"_blank\" href=\"https://dbk.gesis.org/dbksearch/SDesc2.asp?no="+randomdoc+"\">" + randomdoc + "</a> ";
						tooltipZ += addition;
					}

					tooltipZ += "<br>";

				}
			}


			return new Suggestion(cessdaList, zaList, tooltipC, tooltipZ);

		} else {
			return null;
		}

	}


	/**
	 * Adds the new study to the index after the user has selected the categories.
	 * @param cessda Selected CESSDA categories
	 * @param za Selected ZA categories
	 */
	public void indexStudy(List<String> cessda, List<String> za) {
		indexer.indexStudy(currentFile, cessda, za);

	}




}
