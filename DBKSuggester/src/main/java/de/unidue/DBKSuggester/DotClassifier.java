package de.unidue.DBKSuggester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class DotClassifier extends Classifier {


	public DotClassifier(TransportClient client) {
		super(client);
		// TODO Auto-generated constructor stub
	}

	@Override
	List<List<Map<String, String>>> classify(String inFile) {

		XContentBuilder newDoc = buildXContent(parse(inFile));
		Map<Integer, Float> cessdaScores = classifyPart(newDoc, "cessda");
		Map<Integer, Float> zaScores = classifyPart(newDoc, "za");

		List<Map<String, String>> suggestionsCESSDA = new ArrayList<Map<String, String>>();

		for (int c : cessdaScores.keySet()) {
			GetResponse response = client.prepareGet("megadoc", "cessda", Integer.toString(c)).get();
			Map<String, Object> fields = response.getSourceAsMap();
			String category = (String) fields.get("category");
			String ids = (String) fields.get("ids");
			Map<String, String> suggestion = new HashMap<String, String>();
			suggestion.put(category, ids);
			suggestionsCESSDA.add(suggestion);
		}

		List<Map<String, String>> suggestionsZA = new ArrayList<Map<String, String>>();

		for (int z : zaScores.keySet()) {
			GetResponse response = client.prepareGet("megadoc", "za", Integer.toString(z)).get();
			Map<String, Object> fields = response.getSourceAsMap();
			String category = (String) fields.get("category");
			String ids = (String) fields.get("ids");
			Map<String, String> suggestion = new HashMap<String, String>();
			suggestion.put(category, ids);
			suggestionsZA.add(suggestion);
		}

		List<List<Map<String, String>>> suggestions = new ArrayList<List<Map<String, String>>>();
		suggestions.add(suggestionsCESSDA);
		suggestions.add(suggestionsZA);
		return suggestions;

	}

	public Map<Integer, Float> classifyPart(XContentBuilder newDoc, String part) {

		Map<Integer, Float> bestScores = new HashMap<Integer, Float>();

		TermVectorsResponse termVectorsResponseForQuery = client.termVectors(new TermVectorsRequest()
				.doc(newDoc)
				.index("megadoc")
				.type(part)
				.termStatistics(true))
				.actionGet();

		try {
			Fields fieldsForQuery = termVectorsResponseForQuery.getFields();

			Map<String, Float> queryTermsTitlesDE = getQueryTerms(fieldsForQuery, "titlesDE");
			Map<String, Float> queryTermsTitlesEN = getQueryTerms(fieldsForQuery, "titlesEN");
			Map<String, Float> queryTermsCreators = getQueryTerms(fieldsForQuery, "creators");
			Map<String, Float> queryTermsContentsDE = getQueryTerms(fieldsForQuery, "contentsDE");
			Map<String, Float> queryTermsContentsEN = getQueryTerms(fieldsForQuery, "contentsEN");

			
			Map<Float, Integer> scores = new HashMap<Float, Integer>();
			
			int totalDocs = 1;
			if (part.equals("cessda")) totalDocs = 97;
			else if (part.equals("za")) totalDocs = 37;

			for (int i=1; i<=totalDocs; i++) {
				TermVectorsResponse termVectorsResponse = client.termVectors(new TermVectorsRequest()
						.index("megadoc")
						.type(part)
						.id(Integer.toString(i))
						.termStatistics(true))
						.actionGet();

				Fields fields = termVectorsResponse.getFields();

				Map<String, Float> docVectorTitlesDE = getDocVector(fields, "titlesDE", queryTermsTitlesDE.keySet());
				Map<String, Float> docVectorTitlesEN = getDocVector(fields, "titlesEN", queryTermsTitlesEN.keySet());
				Map<String, Float> docVectorCreators = getDocVector(fields, "creators", queryTermsCreators.keySet());
				Map<String, Float> docVectorContentsDE = getDocVector(fields, "contentsDE", queryTermsContentsDE.keySet());
				Map<String, Float> docVectorContentsEN = getDocVector(fields, "contentsEN", queryTermsContentsEN.keySet());


				float dotproductTitlesDE = calcDotProduct(queryTermsTitlesDE, docVectorTitlesDE);
				float dotproductTitlesEN = calcDotProduct(queryTermsTitlesEN, docVectorTitlesEN);
				float dotproductCreators = calcDotProduct(queryTermsCreators, docVectorCreators);
				float dotproductContentsDE = calcDotProduct(queryTermsContentsDE, docVectorContentsDE);
				float dotproductContentsEN = calcDotProduct(queryTermsContentsEN, docVectorContentsEN);

				float score = (dotproductContentsDE + dotproductContentsEN + dotproductCreators + dotproductTitlesDE + dotproductTitlesEN) / 5;

				scores.put(score, i);

			}

			List<Float> keys = new ArrayList<Float>(scores.keySet());
			Collections.sort(keys, Collections.reverseOrder());

			float topscore = keys.get(0);

			for (float k : keys) {
				if (k*1.5 < topscore) break;
				bestScores.put(scores.get(k), k);
			}



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bestScores;


	}

	public Map<String, Float> getQueryTerms(Fields fields, String field) {

		Map<Float, String> termsAndIDFs = new HashMap<Float, String>();
		Map<String, Float> termsAndTFIDFs = new HashMap<String, Float>();


		try {
			Terms terms = fields.terms(field);

			
			if (terms != null) {

				TermsEnum termsEnum = terms.iterator();

				int tmax = 0;


				while(termsEnum.next() != null) {
					int termFreq = termsEnum.postings(null, PostingsEnum.FREQS).freq();
					if (termFreq > tmax) {
						tmax = termFreq;
					}
				}


				termsEnum = terms.iterator();

				while(termsEnum.next() != null) {
					BytesRef term = termsEnum.term();
					int docFreq = termsEnum.docFreq();
					int termFreq = termsEnum.postings(null, PostingsEnum.FREQS).freq();
					float tf = 0;
					if (tmax != 0) {
						tf = (float) (0.5 * (1 + termFreq / (double) tmax));
					}
					float idf = 0;
					if (docFreq != 0) {
						idf = (float) Math.log((double) 134 / (double) docFreq);
					}
					float tfidf = tf * idf;
					if (term != null) {
						termsAndIDFs.put(idf, term.utf8ToString());
						termsAndTFIDFs.put(term.utf8ToString(), tfidf);
					}
				} 
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		List<Float> keys = new ArrayList<Float>(termsAndIDFs.keySet());
		Collections.sort(keys, Collections.reverseOrder());

		Map<String, Float> bestTermsAndTFIDFs = new HashMap<String, Float>();

		for (int i=0; i < keys.size() ; i++) {

			bestTermsAndTFIDFs.put(termsAndIDFs.get(keys.get(i)), termsAndTFIDFs.get(termsAndIDFs.get(keys.get(i))));
			if (i == 10) break;
		}

		return bestTermsAndTFIDFs;
	}


	public Map<String, Float> getDocVector(Fields fields, String field, Set<String> queryTerms) {

		Map<String, Float> docVector = new HashMap<String, Float>();

		try {
			Terms terms = fields.terms(field);

			if (terms != null) {


				TermsEnum termsEnum = terms.iterator();

				int tmax = 0;

				while(termsEnum.next() != null) {
					int termFreq = termsEnum.postings(null, PostingsEnum.FREQS).freq();
					if (termFreq > tmax) {
						tmax = termFreq;
					}
				}


				for (String qt : queryTerms) {
					termsEnum = terms.iterator();

					docVector.put(qt, 0f);

					while(termsEnum.next() != null) {
						BytesRef term = termsEnum.term();

						if (term.utf8ToString().equals(qt)) {
							int docFreq = termsEnum.docFreq();
							int termFreq = termsEnum.postings(null, PostingsEnum.FREQS).freq();
							float tf = (float) (0.5 * (1 + termFreq / (double) tmax));
							float idf = (float) Math.log((double) 134 / (double) docFreq);
							float tfidf = tf * idf;

							docVector.put(qt, tfidf);

						}
					} 
				}
			} else {
				for (String qt : queryTerms) {
					docVector.put(qt, 0f);
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return docVector;


	}

	public float calcDotProduct(Map<String, Float> query, Map<String, Float> doc) {
		float dotproduct = 0f;

		for (String t : query.keySet()) {
			float product = query.get(t) * doc.get(t);
			dotproduct = dotproduct + product;
		}

		return dotproduct;
	}



}
