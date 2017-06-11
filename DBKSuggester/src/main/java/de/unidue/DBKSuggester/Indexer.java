package de.unidue.DBKSuggester;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Builds Elasticsearch index and adds new studies both to the index and the megadoc files
 */
public class Indexer {

	private TransportClient client;
	private Megadoc megadoc;
	String path = "";

	/**
	 * @param client
	 * @param megadoc
	 */
	public Indexer(TransportClient client, Megadoc megadoc) {
		this.client = client;
		this.megadoc = megadoc;
		
		path = megadoc.path;
	}

	/**
	 * Updates the megadoc files and index for each of the user selected categories with the new study
	 * @param inFile The uploaded file
	 * @param cessda CESSDA categories selected by user
	 * @param za ZA categories selected by user
	 */
	public void indexStudy(File inFile, List<String> cessda, List<String> za){

		for (String c : cessda) {
			String cleanc = c.replace("/ ", "");
			String filepath = path + "cessda" + "/mega/" + cleanc + ".xml";
			Map<String, Object> json = parseMegaDocument(filepath, cleanc);
			Study study = megadoc.getStudy(inFile);
			writeXML(json, study, filepath);
			updateDocInIndex(cleanc, "cessda");
		}
		
		for (String z : za) {
			String cleanc = z.replace("/ ", "");
			String filepath = path + "za" + "/mega/" + cleanc + ".xml";
			Map<String, Object> json = parseMegaDocument(filepath, cleanc);
			Study study = megadoc.getStudy(inFile);
			writeXML(json, study, filepath);
			updateDocInIndex(cleanc, "za");
		}
		
	}
	
	/**
	 * Adds the new study to the contents of a megadoc file and writes the updated file
	 * @param json Megadoc file converted to JSON
	 * @param study New study as a Study object
	 * @param filepath File path of the megadoc file
	 */
	private void writeXML(Map<String, Object> json, Study study, String filepath) {
		
		String eol = System.getProperty("line.separator");
		
		String ids = json.get("ids") + " " + study.id();
		String titlesDE = json.get("titlesDE") + eol + study.titleDE();
		String titlesEN = json.get("titlesEN") + eol + study.titleEN();
		String creators = json.get("creators") + eol + study.creators();
		String contentsDE = json.get("contentsDE") + eol + study.contentDE();
		String contentsEN = json.get("contentsEN") + eol + study.contentEN();
		
		
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
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(document, new FileWriter(filepath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
	
	
	/**
	 * Finds a megadoc in the index and updates it with the new file content
	 * @param categoryName The megadoc's name
	 * @param type CESSDA or ZA collection
	 */
	private void updateDocInIndex(String categoryName, String type) {
		
		SearchResponse response = client.prepareSearch("megadoc")
				.setQuery(QueryBuilders
						.termQuery("category", categoryName))
				.get();
		String docid = response.getHits().getAt(0).getId();
		
		UpdateRequest updateRequest = new UpdateRequest();
		updateRequest.index("megadoc");
		updateRequest.type(type);
		updateRequest.id(docid);
		String filepath = path + type + "/mega/" + categoryName + ".xml";
		updateRequest.doc(parseMegaDocument(filepath, categoryName));
		try {
			client.update(updateRequest).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Creates megadoc index in Elasticsearch or resets it if it already exists
	 */
	public void makeIndex(){

		boolean exists = client.admin().indices()
			    .prepareExists("megadoc")
			    .execute().actionGet().isExists();
		
		if (exists == true) {
			DeleteIndexRequest request = new DeleteIndexRequest("megadoc");
		    try {
		        DeleteIndexResponse response = client.admin().indices().delete(request).actionGet();
		        if (!response.isAcknowledged()) {
		            throw new Exception("Failed to delete index " + "megadoc");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		client.admin().indices().prepareCreate("megadoc")
		.setSettings(Settings.builder()             
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0))
		.get();
		
		makeIndexPart("cessda");
		makeIndexPart("za");
		

	}

	/**
	 * Creates the index mappings and fills the index with data from the megadoc files
	 * @param part CESSDA or ZA
	 */
	private void makeIndexPart(String part) {

		try {

			XContentBuilder indexMappings = XContentFactory.jsonBuilder().
					startObject().
					startObject(part).
					startObject("properties").
					startObject("category").
					field("type", "keyword").
					endObject().
					startObject("ids").
					field("type", "text").field("index", "false").
					endObject().
					startObject("titlesDE").
					field("type", "text").field("analyzer", "german").field("term_vector", "yes").
					endObject().
					startObject("titlesEN").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					startObject("creators").
					field("type", "text").field("term_vector", "yes").
					endObject().
					startObject("contentsDE").
					field("type", "text").field("analyzer", "german").field("term_vector", "yes").
					endObject().
					startObject("contentsEN").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					endObject().
					endObject().
					endObject();


			client.admin().indices().preparePutMapping("megadoc").setType(part).setSource(indexMappings).get();


			File[] fileList = new File(path + part + "/mega").listFiles();

			for(int i=0; i < fileList.length; i++) {
				String filepath = fileList[i].getPath();
				String filename = fileList[i].getName();
				System.out.println(filename);
				filename = filename.substring( 0, filename.indexOf( ".xml" ) );
				Map<String, Object> json = parseMegaDocument(filepath, filename);

				IndexResponse response = client.prepareIndex("megadoc", part).setSource(json).setId(Integer.toString(i+1))
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


	/**
	 * Makes JSON from a megadocument file
	 * @param inFile The megadocument file
	 * @param filename The (file)name that gets put in the category field
	 * @return JSON formatted content
	 */
	public static Map<String, Object> parseMegaDocument(String inFile, String filename){
		Map<String, Object> jsonDocument = new HashMap<String, Object>();

		org.jdom2.Document doc = new org.jdom2.Document();
		try {
			doc = new SAXBuilder().build(inFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		String ids = root.getChildText("ids");
		String titlesDE = root.getChildText("titlesDE");
		String titlesEN = root.getChildText("titlesEN");
		String creators = root.getChildText("creators");
		String contentsDE = root.getChildText("contentsDE");
		String contentsEN = root.getChildText("contentsEN");

		jsonDocument.put("category", filename);
		jsonDocument.put("ids", ids);
		jsonDocument.put("titlesDE", titlesDE);
		jsonDocument.put("titlesEN", titlesEN);
		jsonDocument.put("creators", creators);
		jsonDocument.put("contentsDE", contentsDE);
		jsonDocument.put("contentsEN", contentsEN);

		return jsonDocument;
	}

}
