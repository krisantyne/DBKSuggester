package de.unidue.DBKSuggester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;

/**
 * Megadocument classifier implementation using MoreLikeThis function from Elasticsearch
 */
public class MLTClassifier extends Classifier {


	/**
	 * @param client
	 */
	public MLTClassifier(TransportClient client) {
		super(client);
		// TODO Auto-generated constructor stub
	}



	/** 
	 * Sends a MoreLikeThisQuery with the uploaded study to Elasticsearch (one for CESSDA and one
	 * for ZA) and selects the top hits to use as suggestions
	 */
	@Override
	public List<List<Map<String, String>>> classify(String inFile){

		XContentBuilder newDoc = buildXContent(parse(inFile));

		if (newDoc != null) {

			Item item1 = new Item("megadoc", "cessda", newDoc);
			Item[] items1 = {item1};

			SearchResponse response1 = client.prepareSearch("megadoc")
					.setQuery(QueryBuilders
							.moreLikeThisQuery(items1)
							.minTermFreq(1)
							.maxQueryTerms(10) // <---- adjust max query terms here
							.minDocFreq(1))
					.setTypes("cessda")
					.get();


			List<Map<String, String>> suggestionsCESSDA = new ArrayList<Map<String, String>>();
			List<Map<String, String>> suggestionsZA = new ArrayList<Map<String, String>>();

			if (response1.getHits().totalHits() != 0) {
				double topscore1 = response1.getHits().getAt(0).getScore();


				for (int i=0; i<response1.getHits().getTotalHits(); i++) {
					double score = response1.getHits().getAt(i).getScore();
					if (score*1.3 < topscore1) break; // <---- cutoff for score can be adjusted here
					Map<String, Object> field = response1.getHits().getAt(i).sourceAsMap();
					String category = (String) field.get("category");
					String ids = (String) field.get("ids");
					Map<String, String> suggestion = new HashMap<String, String>();
					suggestion.put(category, ids);
					suggestionsCESSDA.add(suggestion);
					if (i==9) break;
				}
			}


			Item item2 = new Item("megadoc", "za", newDoc);
			Item[] items2 = {item2};

			SearchResponse response2 = client.prepareSearch("megadoc")
					.setQuery(QueryBuilders
							.moreLikeThisQuery(items2)
							.minTermFreq(1)
							.maxQueryTerms(10) // <---- adjust max query terms here
							.minDocFreq(1))
					.setTypes("za")
					.get();

			if (response2.getHits().totalHits() != 0) {
				double topscore2 = response2.getHits().getAt(0).getScore();

				for (int i=0; i<response2.getHits().getTotalHits(); i++) {
					double score = response2.getHits().getAt(i).getScore();
					if (score*1.3 < topscore2) break; // <---- cutoff for score can be adjusted here
					Map<String, Object> field = response2.getHits().getAt(i).sourceAsMap();
					String category = (String) field.get("category");
					String ids = (String) field.get("ids");
					Map<String, String> suggestion = new HashMap<String, String>();
					suggestion.put(category, ids);
					suggestionsZA.add(suggestion);
					if (i==9) break;
				}
			}


			List<List<Map<String, String>>> suggestions = new ArrayList<List<Map<String, String>>>();
			suggestions.add(suggestionsCESSDA);
			suggestions.add(suggestionsZA);
			return suggestions;
		}
		else {
			return null;
		}

	}

}
