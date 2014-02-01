package it.thepund.timeliner.bur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.sf.json.JSONObject;

import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

public class LettersTimeliner {

	private static String OUTPUT;

	public static void main(String[] args) throws RepositoryException, QueryEvaluationException, MalformedQueryException, IOException {


		if (args.length == 1) {
			OUTPUT = args[0];
		}
		
//		TODO: We are currently skipping the following time-consuming query to the remote repository:
//				select distinct ?letter ?entity
//				
//						where {
//							?letter rdf:type bibo:Letter. 
//							?f dcterms:isPartOf ?letter. 
//							?f ?p ?entity. 
//							?entity rdfs:label ?l. 
//									FILTER regex(?l,"Wilhelm von Bode","i") 
//						}
//				
//		we also skip this other query
//				
//				select ?letter 
//				where {
//					?letter <http://www.netseven.it/ontology/letter/sender> ?sender. 
//				FILTER regex(?sender,"Bode","i")  }
//		
// 		We are hard-coding the results obtained from the query in two arrays

		String[] lettersTalkingAboutBode = new String[] {"http://burckhardtsource.org/letter/29","http://burckhardtsource.org/letter/44","http://burckhardtsource.org/letter/53","http://burckhardtsource.org/letter/57","http://burckhardtsource.org/letter/59","http://burckhardtsource.org/letter/58","http://burckhardtsource.org/letter/89","http://burckhardtsource.org/letter/116","http://burckhardtsource.org/letter/365"};
		String[] lettersFromBode = new String[] {"http://burckhardtsource.org/letter/3","http://burckhardtsource.org/letter/44","http://burckhardtsource.org/letter/53","http://burckhardtsource.org/letter/57","http://burckhardtsource.org/letter/59","http://burckhardtsource.org/letter/58","http://burckhardtsource.org/letter/89"};

		
		
		
		//The following properties are collected from the triples inside annotations bodies
		String[] MappedPropertiesFromAnnotations = new String[] {"http://purl.org/spar/cito/cites","http://purl.org/pundit/ont/oa#identifies","http://purl.org/net7/korbo/item/73666","http://purl.org/net7/korbo/item/73667","http://purl.org/net7/korbo/item/76396","http://purl.org/net7/korbo/item/76397","http://purl.org/net7/korbo/item/76398","http://purl.org/net7/korbo/item/76399","http://www.netseven.it/ontology/letter/sendingDate","http://www.netseven.it/ontology/letter/receivingDate","http://www.netseven.it/ontology/letter/sendingPlace","http://www.netseven.it/ontology/letter/receivingPlace","http://www.netseven.it/ontology/letter/sender","http://www.w3.org/2000/01/rdf-schema#comment","http://purl.org/dc/elements/1.1/"};
		//The following properties are collected from triples attached to the letters themselves
		String[] MappedPropertiesFromMetadata = new String[] {"http://www.netseven.it/ontology/letter/sendingDate","http://www.netseven.it/ontology/letter/receivingDate","http://www.netseven.it/ontology/letter/sendingPlace","http://www.netseven.it/ontology/letter/receivingPlace","http://www.netseven.it/ontology/letter/sender","http://www.w3.org/2000/01/rdf-schema#comment","http://purl.org/dc/elements/1.1/title"};

		HashSet<String> letters = new HashSet<String>();

		for (String letter : lettersFromBode) {
			letters.add(letter);
		}
		for (String letter : lettersTalkingAboutBode) {
			letters.add(letter);
		}

		System.out.println(letters.toString());

		//Init the connection to the remote Sesmae repository
		Repository rep = new HTTPRepository("http://as.thepund.it:8080/openrdf-sesame/", "pundit");
		RepositoryConnection conn = rep.getConnection();

		
		/**
		 * This is the JSON-like structure where all the data is stored
		 * to look at this itermediate format use: 
		 * JSONObject jobj = JSONObject.fromObject(doTimelineJsJson(data));
		 */
		HashMap<String,HashMap<String,HashMap<String,Object>>> data = new HashMap<String, HashMap<String,HashMap<String,Object>>>();

		for (String letterUrl : letters) {

			System.out.println("Getting attributes for letter: " + letterUrl);
			HashMap<String,HashMap<String,Object>> attributes = getLetterAttributes(conn, letterUrl, MappedPropertiesFromAnnotations, MappedPropertiesFromMetadata);
			data.put(letterUrl,attributes);
		}

		System.out.println(data);

		JSONObject jobj = JSONObject.fromObject(doTimelineJsJson(data));

		Reader in = new StringReader(jobj.toString(3));

		Writer out = new BufferedWriter(new FileWriter(new File(OUTPUT)));
		//out.write("storyjs_jsonp_data = " + jobj.toString(3));
		out.write(jobj.toString(3));
		out.flush();
		out.close();


	}

	/**
	 * 1) Builds the connections between letters and Entities mentioned in Annotations of the letters.
	 * Entities are retrieved from each triple contained in Annotations that have the form:
	 * 
	 * Fragment_of_letter_A is_part_of letter_A
	 * Fragment_of_letter_A PROPERTY ENTITY
	 * 
	 * where PROPERTY is one of the properties in @param propsFromAnn
	 * 
	 * 2) Add data from the properties in @param propsFromMeta
	 * 
	 * @param conn
	 * @param letterUrl
	 * @param propsFromAnn
	 * @param propsFromMeta
	 * @return
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 */
	private static HashMap<String, HashMap<String,Object>> getLetterAttributes(RepositoryConnection conn ,String letterUrl, String[] propsFromAnn, String[] propsFromMeta) throws QueryEvaluationException, RepositoryException, MalformedQueryException {
		HashMap<String,HashMap<String,Object>> attributes = new HashMap<String, HashMap<String,Object>>();

		for (String prop : propsFromAnn) {

			String query = "select distinct ?entity ?propertyLabel ?entityLabel ?occurrence where { " +
					"?fragment <http://purl.org/dc/terms/isPartOf> <" + letterUrl + ">. " +
					"?fragment <" + prop + "> ?entity . " +
					"?fragment <http://purl.org/dc/elements/1.1/description> ?occurrence . " +
					"<" + prop + "> <http://www.w3.org/2000/01/rdf-schema#label> ?propertyLabel . " +
					"?entity <http://www.w3.org/2000/01/rdf-schema#label> ?entityLabel." +
					"}";


			TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();

			while (result.hasNext()) {

				BindingSet set = result.next();
				Resource entity = (Resource)set.getBinding("entity").getValue();
				Value propertyLabel = set.getBinding("propertyLabel").getValue();
				Value entityLabel = set.getBinding("entityLabel").getValue();
				Value occ = set.getBinding("occurrence").getValue();
				
				HashMap<String,Object> values;
				if (attributes.containsKey(propertyLabel.stringValue())) {
					values = attributes.get(propertyLabel.stringValue());
				} else {
					values = new HashMap<String,Object>();
					attributes.put(propertyLabel.stringValue(), values);
				}
				values.put(entity.stringValue(),entityLabel.stringValue());
				
				HashMap<String,Object> occurrences;
				if (attributes.containsKey("occurrences")) {
					occurrences = (HashMap<String,Object>)attributes.get("occurrences");
				} else {
					occurrences = new HashMap<String, Object>();
					attributes.put("occurrences",occurrences);
				}
				occurrences.put(entity.stringValue(), occ.stringValue());

			}

		}
		
		for (String prop : propsFromMeta) {
			
			String query = "select distinct ?value ?valueLabel ?propertyLabel where { " +
					"<" + letterUrl + "> <" + prop + "> ?value. " +
					"OPTIONAL {<" + prop + "> <http://www.w3.org/2000/01/rdf-schema#label> ?propertyLabel. }" +
					"OPTIONAL {?value <http://www.w3.org/2000/01/rdf-schema#label> ?valueLabel.}" +
					"}";
			
			TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate();

			while (result.hasNext()) {

				BindingSet set = result.next();
				Value value = set.getBinding("value").getValue();
				
				String propertyLabel;
				if (set.getBinding("propertyLabel") != null) {
					propertyLabel = set.getBinding("propertyLabel").getValue().stringValue();	
				} else {
					propertyLabel = Utils.getLabelFromURL(prop);
				}
				
				
				String valueLabel;
				if (set.getBinding("valueLabel") != null) {
					valueLabel = set.getBinding("valueLabel").getValue().stringValue();
				} else {
					valueLabel = Utils.getLabelFromURL(value.stringValue());
				}
				

				HashMap<String,Object> values;
				if (attributes.containsKey(propertyLabel)) {
					values = attributes.get(propertyLabel);
				} else {
					values = new HashMap<String,Object>();
					attributes.put(propertyLabel, values);
				}
				values.put(value.stringValue(),valueLabel);


			}
		}
		
		

		return attributes;
	}
	
	
	/**
	 * Builds a JSON compliant with TimelinJS from the internal Java structure.
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static HashMap<String,Object> doTimelineJsJson(HashMap<String,HashMap<String,HashMap<String,Object>>> data) throws IOException {
		
		HashMap<String,Object> timelinenode = new HashMap<String,Object>();
		
		HashMap<String, Object> datesnode = new HashMap<String, Object>();
		
		ArrayList<HashMap<String, Object>> slidenode = new ArrayList<HashMap<String, Object>>();
		
		datesnode.put("date", slidenode);
		datesnode.put("headline", "Wilhelm von Bode and Burckhardt");
		datesnode.put("type", "default");
		datesnode.put("text", "An experiment with timelines...");
		datesnode.put("startDate", "1883");
		datesnode.put("endDate", "1897");
		
		timelinenode.put("timeline", datesnode);
		
		for (String letterUrl : data.keySet()) {
		
			HashMap<String, Object> slideAttributes = new HashMap<String, Object>();
			
			slideAttributes.put("uri", letterUrl);
			
			HashMap<String,HashMap<String,Object>> letterAttributes = data.get(letterUrl);
			
			HashMap<String,Object> endingDate = letterAttributes.get("sendingDate");
			String date = endingDate.keySet().iterator().next();
			slideAttributes.put("startDate", date.replaceAll("-", ","));
			
			HashMap<String,Object> comment = letterAttributes.get("rdf-schema#comment");
			String title = comment.keySet().iterator().next();
			slideAttributes.put("headline", title);
			
			URL call = new URL(letterUrl + ".annotate");
	        BufferedReader in = new BufferedReader(new InputStreamReader(call.openStream()));
	        String text = "";
	        String line;
	        while ((line = in.readLine()) != null)
	            text += line;
	        in.close();
			
	        
	        text = getHTMLRichContent(text, letterAttributes);
	        
	        slideAttributes.put("text", text);
	        
			slidenode.add(slideAttributes);
			
		}
		
		return timelinenode;
	}

	/**
	 * Creates an HTML representation of the letter, including entities boxes and retrieving the content of the letters and formatting
	 * @param text
	 * @param attributes
	 * @return
	 */
	
	private static String getHTMLRichContent(String text, HashMap<String, HashMap<String,Object>> attributes) {
		String result = text.split("<body>")[1].split("</body>")[0].replaceAll("\t", "");
		result ="<table>" +
						"<tr><td style=\"width: 70%;\">" +
							result +
						"</td><td style=\"vertical-align:top; width: 30%; text-align:right;\">" +
							getHTMLEntities(attributes) + 
						"</td></tr>" +
					"</table>";
		//result = result.replaceAll("<div class=\"pundit-content\"", "<div class=\"pundit-content\"");
		return result;
	}

	private static String getHTMLEntities(HashMap<String, HashMap<String, Object>> attributes) {
		
		String result = "";
		
		result += getHTMLEntitiesFromProperty("identifies place","Places in this letter", attributes);
		result += getHTMLEntitiesFromProperty("identifies person","Persons in this letter", attributes);
		result += getHTMLEntitiesFromProperty("identifies","Other entities in this letter", attributes);
		
		return result;
		
	}

	private static String getHTMLEntitiesFromProperty(String prop, String title, HashMap<String, HashMap<String, Object>> attributes) {
		
		String result ="";
		
		if (attributes.containsKey(prop)) {
			result += "<strong>" + title + "</strong><br/>";
			Iterator<String> iter = attributes.get(prop).keySet().iterator();
			while (iter.hasNext()) {
				String place = (String) iter.next();
				String placeLabel = (String)attributes.get(prop).get(place);
				result += "<a  target=\"_blank\" href=\"" + place + "\">" + placeLabel + "</a><br/>";
			} 
			
		}
		
		return result;
	}


}
