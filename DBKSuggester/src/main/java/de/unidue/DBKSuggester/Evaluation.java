package de.unidue.DBKSuggester;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;
import org.elasticsearch.rest.RestStatus;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class Evaluation {

	TransportClient client;
	Classifier classifier;
	Megadoc megadoc;

	String cessdatest = "/Users/Martina/Desktop/alldata/cessda/test";
	String zatest = "/Users/Martina/Desktop/alldata/za/test";

	String path = "/Users/Martina/Desktop/alldata/";


	public Evaluation(TransportClient client, Classifier classifier, Megadoc megadoc) {
		this.client = client;
		this.classifier = classifier;
		this.megadoc = megadoc;
	}


	public void evaluate() {

		//List<Float> zaResults = evalPart("za");
		List<Float> cessdaResults = evalPart("cessda");
		
		//System.out.println("ZA: Precision: " + zaResults.get(0) + " Recall: " + zaResults.get(1) + " F: " + zaResults.get(2));
		System.out.println("CESSDA: Precision: " + cessdaResults.get(0) + " Recall: " + cessdaResults.get(1) + " F: " + cessdaResults.get(2));

	}
	
	private List<Float> evalPart(String type) {
		
		float sumPrecicison = 0;
		float sumRecall = 0;
		File[] xmldocs;

		if (type.equals("cessda"))  {
			xmldocs = new File(cessdatest).listFiles();
		} else {
			xmldocs = new File(zatest).listFiles();
		}
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));

		//Random randomGenerator = new Random();
		
		for(int i=0; i < fileList.size(); i++) {
			//int randomno = randomGenerator.nextInt(fileList.size());
			
			String filepath = fileList.get(i).getPath();
			Study study = classifier.parse(filepath);
			
			List<String> realCategoriesUnprocessed;
			if (type.equals("cessda")) {
				realCategoriesUnprocessed = study.cessdaTopics();
			} else {
				realCategoriesUnprocessed = study.zaCategory();
			}
			List<String> realCategories = new ArrayList<String>();

			for (String c : realCategoriesUnprocessed) {
				String cleanc = c.replace("/ ", "");
				realCategories.add(cleanc);
			}

			List<String> suggestedCategories;
			
			if (type.equals("cessda")) {
				suggestedCategories = megadoc.makeSuggestion(fileList.get(i)).getCessdaCats();
			} else {
				suggestedCategories = megadoc.makeSuggestion(fileList.get(i)).getZaCats();
			}
			
			float precision = calcPrecision(realCategories, suggestedCategories);
			float recall = calcRecall(realCategories, suggestedCategories);

			sumPrecicison = sumPrecicison + precision;
			sumRecall = sumRecall + recall;

			System.out.println(i + " " + study.id() + " " + study.titleDE() + " | " + "Real: " + realCategories
					+ " Suggested: " + suggestedCategories + " Precision: " + precision + " Recall: " + recall);
		}

		float avgPrecision = sumPrecicison / fileList.size();
		float avgRecall = sumRecall / fileList.size();

		float fbalance = 2;
		float fmeasure = (float) ((float) ((1 + Math.pow(fbalance, 2)) * avgPrecision * avgRecall) / (Math.pow(fbalance, 2) * avgPrecision + avgRecall));

		List<Float> results = new ArrayList<Float>();
		results.add(avgPrecision);
		results.add(avgRecall);
		results.add(fmeasure);
		return results;
		
	}


	private float calcPrecision(List<String> real, List<String> suggested) {

		int retrieved = suggested.size();
		int relevantretrieved = 0;

		for (String r : real) {
			if (suggested.contains(r)) {
				relevantretrieved++;
			}
		}

		float precision = 0;
		
		if (retrieved != 0) {
			precision = (float) relevantretrieved / retrieved;
		}
		return precision;

	}


	private float calcRecall(List<String> real, List<String> suggested) {

		int relevant = real.size();
		int relevantretrieved = 0;

		for (String r : real) {
			if (suggested.contains(r)) {
				relevantretrieved++;
			}
		}

		float recall = (float) relevantretrieved / relevant;
		return recall;
	}


	public void nonMegaEvaluate() {

		List<Float> zaResults = nonMegaEvalPart("za");
		List<Float> cessdaResults = nonMegaEvalPart("cessda");
		
		System.out.println("ZA: Precision: " + zaResults.get(0) + " Recall: " + zaResults.get(1) + " F: " + zaResults.get(2));
		System.out.println("CESSDA: Precision: " + cessdaResults.get(0) + " Recall: " + cessdaResults.get(1) + " F: " + cessdaResults.get(2));

	}
	
	
	private List<Float> nonMegaEvalPart(String type){
		float sumPrecicison = 0;
		float sumRecall = 0;
		File[] xmldocs;

		if (type.equals("cessda"))  {
			xmldocs = new File(cessdatest).listFiles();
		} else {
			xmldocs = new File(zatest).listFiles();
		}
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));

		for(int i=0; i < fileList.size(); i++) {
			String filepath = fileList.get(i).getPath();
			Study study = classifier.parse(filepath);

			List<String> realCategoriesUnprocessed;
			if (type.equals("cessda")) {
				realCategoriesUnprocessed = study.cessdaTopics();
			} else {
				realCategoriesUnprocessed = study.zaCategory();
			}
			List<String> realCategories = new ArrayList<String>();

			for (String c : realCategoriesUnprocessed) {
				String cleanc = c.replace("/ ", "");
				realCategories.add(cleanc);
			}

			XContentBuilder xContent = classifier.buildXContent(study);

			List<String> suggestedCategories = nonMegaClassify(xContent, type);

			float precision = calcPrecision(realCategories, suggestedCategories);
			float recall = calcRecall(realCategories, suggestedCategories);

			sumPrecicison = sumPrecicison + precision;
			sumRecall = sumRecall + recall;

			System.out.println(study.id() + " " + study.titleDE() + " | " + "Real: " + realCategories
					+ " Suggested: " + suggestedCategories + " Precision: " + precision + " Recall: " + recall);
		}

		float avgPrecision = sumPrecicison / fileList.size();
		float avgRecall = sumRecall / fileList.size();

		float fbalance = 2;
		float fmeasure = (float) ((float) ((1 + Math.pow(fbalance, 2)) * avgPrecision * avgRecall) / (Math.pow(fbalance, 2) * avgPrecision + avgRecall));
		
		List<Float> results = new ArrayList<Float>();
		results.add(avgPrecision);
		results.add(avgRecall);
		results.add(fmeasure);
		return results;
	}



	public void makeIndexNonMega(){

		boolean exists = client.admin().indices()
			    .prepareExists("nonmegadoc")
			    .execute().actionGet().isExists();
		
		if (exists == true) {
			DeleteIndexRequest request = new DeleteIndexRequest("nonmegadoc");
		    try {
		        DeleteIndexResponse response = client.admin().indices().delete(request).actionGet();
		        if (!response.isAcknowledged()) {
		            throw new Exception("Failed to delete index " + "nonmegadoc");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		client.admin().indices().prepareCreate("nonmegadoc")
		.setSettings(Settings.builder()             
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0))
		.get();

		makeIndexPartNonMega("cessda");
		makeIndexPartNonMega("za");


	}


	private void makeIndexPartNonMega(String part) {

		try {

			XContentBuilder indexMappings = XContentFactory.jsonBuilder().
					startObject().
					startObject(part).
					startObject("properties").
					startObject("id").
					field("type", "text").field("index", "false").
					endObject().
					startObject("titleDE").
					field("type", "text").field("analyzer", "german").field("term_vector", "yes").
					endObject().
					startObject("titleEN").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					startObject("creators").
					field("type", "text").
					endObject().
					startObject("contentDE").
					field("type", "text").field("analyzer", "german").field("term_vector", "yes").
					endObject().
					startObject("contentEN").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					startObject("categories").
					field("type", "keyword").
					endObject().
					endObject().
					endObject().
					endObject();


			client.admin().indices().preparePutMapping("nonmegadoc").setType(part).setSource(indexMappings).get();


			File[] fileList = new File(path + part + "/train").listFiles();

			for(int i=0; i < fileList.length; i++) {
				String filepath = fileList[i].getPath();
				String filename = fileList[i].getName();
				filename = filename.substring( 0, filename.indexOf( ".xml" ) );
				Study study = classifier.parse(filepath);

				Map<String, Object> jsonDocument = new HashMap<String, Object>();

				jsonDocument.put("id", study.id());
				jsonDocument.put("titleDE", study.titleDE());
				jsonDocument.put("titleEN", study.titleEN());
				jsonDocument.put("creators", study.creators());
				jsonDocument.put("contentDE", study.contentDE());
				jsonDocument.put("contentEN", study.contentEN());
				if (part.equals("cessda")) {
					jsonDocument.put("categories", study.cessdaTopics());
				}
				if (part.equals("za")) {
					jsonDocument.put("categories", study.zaCategory());
				}


				IndexResponse response = client.prepareIndex("nonmegadoc", part).setSource(jsonDocument)
						.get();

				String _index = response.getIndex();
				String _type = response.getType();
				String _id = response.getId();
				long _version = response.getVersion();
				RestStatus status = response.status();

				System.out.println(_index + " " + _type + " " + _id + " " + _version + " " + status);

			}


		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<String> nonMegaClassify(XContentBuilder xContent, String type) {

		XContentBuilder newDoc = xContent;

		Item item = new Item("nonmegadoc", type, newDoc);
		Item[] items = {item};

		SearchResponse response = client.prepareSearch("nonmegadoc")
				.setQuery(QueryBuilders
						.moreLikeThisQuery(items)
						.minTermFreq(1)
						.maxQueryTerms(10)
						.minDocFreq(1))
				.setTypes(type)
				.get();

		List<String> suggestedCategories = new ArrayList<String>();
		
		if (response.getHits().totalHits() != 0) {
		double topscore = response.getHits().getAt(0).getScore();

		for (int i=0; i<response.getHits().getTotalHits(); i++) {
			double score = response.getHits().getAt(i).getScore();
			if (score*1.3 < topscore) break;
			Map<String, Object> field = response.getHits().getAt(i).sourceAsMap();
			List<String> categories = (List<String>) field.get("categories");
			for (String c : categories) {
				if (!suggestedCategories.contains(c)) {
					suggestedCategories.add(c);
					}
			}
			
			if (i==9) break;
		}
		}

		return suggestedCategories;

	}


	public void noFieldsEvaluate() {
		List<Float> zaResults = noFieldsEvalPart("za");
		List<Float> cessdaResults = noFieldsEvalPart("cessda");
		
		System.out.println("ZA: Precision: " + zaResults.get(0) + " Recall: " + zaResults.get(1) + " F: " + zaResults.get(2));
		System.out.println("CESSDA: Precision: " + cessdaResults.get(0) + " Recall: " + cessdaResults.get(1) + " F: " + cessdaResults.get(2));

	}
	
	public List<Float> noFieldsEvalPart(String type) {
		float sumPrecicison = 0;
		float sumRecall = 0;
		File[] xmldocs;

		if (type.equals("cessda"))  {
			xmldocs = new File(cessdatest).listFiles();
		} else {
			xmldocs = new File(zatest).listFiles();
		}
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));

		for(int i=0; i < fileList.size(); i++) {
			String filepath = fileList.get(i).getPath();
			Study study = classifier.parse(filepath);

			List<String> realCategoriesUnprocessed;
			if (type.equals("cessda")) {
				realCategoriesUnprocessed = study.cessdaTopics();
			} else {
				realCategoriesUnprocessed = study.zaCategory();
			}
			List<String> realCategories = new ArrayList<String>();

			for (String c : realCategoriesUnprocessed) {
				String cleanc = c.replace("/ ", "");
				realCategories.add(cleanc);
			}

			XContentBuilder xContent = buildXContentNoFields(filepath);

			List<String> suggestedCategories = noFieldsClassify(xContent, type);

			float precision = calcPrecision(realCategories, suggestedCategories);
			float recall = calcRecall(realCategories, suggestedCategories);

			sumPrecicison = sumPrecicison + precision;
			sumRecall = sumRecall + recall;

			System.out.println(study.id() + " " + study.titleDE() + " | " + "Real: " + realCategories
					+ " Suggested: " + suggestedCategories + " Precision: " + precision + " Recall: " + recall);
		}

		float avgPrecision = sumPrecicison / fileList.size();
		float avgRecall = sumRecall / fileList.size();

		float fbalance = 2;
		float fmeasure = (float) ((float) ((1 + Math.pow(fbalance, 2)) * avgPrecision * avgRecall) / (Math.pow(fbalance, 2) * avgPrecision + avgRecall));
		
		List<Float> results = new ArrayList<Float>();
		results.add(avgPrecision);
		results.add(avgRecall);
		results.add(fmeasure);
		return results;
		
	}
	
	private XContentBuilder buildXContentNoFields(String filepath) {
		String doccontent = "";
		XContentBuilder newDoc = null;
		try {
			doccontent = new String(Files.readAllBytes(Paths.get(filepath)));
			newDoc = XContentFactory.jsonBuilder()
					.startObject()
						.field("category", "")
						.field("doccontent", doccontent)
					.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newDoc;
	}


	public void makeIndexNoFields() {
		boolean exists = client.admin().indices()
			    .prepareExists("nofields")
			    .execute().actionGet().isExists();
		
		if (exists == true) {
			DeleteIndexRequest request = new DeleteIndexRequest("nofields");
		    try {
		        DeleteIndexResponse response = client.admin().indices().delete(request).actionGet();
		        if (!response.isAcknowledged()) {
		            throw new Exception("Failed to delete index " + "nofields");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		client.admin().indices().prepareCreate("nofields")
		.setSettings(Settings.builder()             
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0))
		.get();

		makeIndexPartNoFields("cessda");
		makeIndexPartNoFields("za");
	}

	public void makeIndexPartNoFields(String part) {
		try {

			XContentBuilder indexMappings = XContentFactory.jsonBuilder().
					startObject().
					startObject(part).
					startObject("properties").
					startObject("category").
					field("type", "keyword").
					endObject().
					startObject("doccontent").
					field("type", "text").field("term_vector", "yes").
					endObject().
					endObject().
					endObject().
					endObject();


			client.admin().indices().preparePutMapping("nofields").setType(part).setSource(indexMappings).get();


			File[] fileList = new File(path + part + "/meganofields").listFiles();

			for(int i=0; i < fileList.length; i++) {
				String filepath = fileList[i].getPath();
				String filename = fileList[i].getName();
				System.out.println(filename);
				filename = filename.substring( 0, filename.indexOf( ".xml" ) );
				Map<String, Object> json = parseMegaNoFields(filepath, filename);

				IndexResponse response = client.prepareIndex("nofields", part).setSource(json).setId(Integer.toString(i+1))
						.get();

				String _index = response.getIndex();
				String _type = response.getType();
				String _id = response.getId();
				long _version = response.getVersion();
				RestStatus status = response.status();

				System.out.println(_index + " " + _type + " " + _id + " " + _version + " " + status);

			}


		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static Map<String, Object> parseMegaNoFields(String inFile, String filename){
		Map<String, Object> jsonDocument = new HashMap<String, Object>();

		org.jdom2.Document doc = new org.jdom2.Document();
		try {
			doc = new SAXBuilder().build(inFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		String doccontent = root.getChildText("doccontent");

		jsonDocument.put("category", filename);
		jsonDocument.put("doccontent", doccontent);

		return jsonDocument;
	}
	
	
	public List<String> noFieldsClassify(XContentBuilder xContent, String type) {
		
		XContentBuilder newDoc = xContent;

		Item item = new Item("nofields", type, newDoc);
		Item[] items = {item};

		SearchResponse response = client.prepareSearch("nofields")
				.setQuery(QueryBuilders
						.moreLikeThisQuery(items)
						.minTermFreq(1)
						.maxQueryTerms(10)
						.minDocFreq(1))
				.setTypes(type)
				.get();

		List<String> suggestedCategories = new ArrayList<String>();
		
		if (response.getHits().totalHits() != 0) {
			double topscore1 = response.getHits().getAt(0).getScore();


			for (int i=0; i<response.getHits().getTotalHits(); i++) {
				double score = response.getHits().getAt(i).getScore();
				if (score*1.5 < topscore1) break;
				Map<String, Object> field = response.getHits().getAt(i).sourceAsMap();
				String category = (String) field.get("category");
				suggestedCategories.add(category);
				if (i==9) break;
			}
		}

		return suggestedCategories;
		
	}


}
