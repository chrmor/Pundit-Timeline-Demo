package it.thepund.timeliner;


/*
 * TODO:Possible improvements:
 * 
 * 1)remove the items without a date. 
 * This has to be done at the end of the process as triples are loaded iteratively into JSON items: we cannot know in advance if an item will have a valid date or not!
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Path ("/")
public class TimeLineReader {

	private int minDate = 10000;
	private int maxDate = -10000;
	private String annotationApi = "";

	private int count = 0;
	
	@GET
	@Path("/to.jsonp")
	@Produces(MediaType.TEXT_PLAIN)
	public String getTimelineJSON(@QueryParam("notebook-ids") String ids, @QueryParam("namespace") String notebooksNamespace, @QueryParam("api") String annotationApi) throws IOException {

		String[] notebookIds = ids.split(",");
		
		this.annotationApi = annotationApi;

		//Create the empty JSON to be filled later ...
		JSONObject root = new JSONObject();
		root.put("timeline", new JSONObject());
		JSONObject timeline = root.getJSONObject("timeline");
		timeline.put("date", new JSONArray());
		JSONArray datesArray = timeline.getJSONArray("date");


		for (int i = 0; i < notebookIds.length; i++) {

			//Make the call to get Notebook content ...
			JSONObject obj = getNotebookJSON(notebookIds[i]);
			JSONObject metadata = (JSONObject)obj.get("metadata");
			JSONArray annotations = (JSONArray)obj.get("annotations");


			ArrayList<String> names = getRDFPropertyValues(notebooksNamespace + notebookIds[i], Namespaces.RDFS_LABEL, metadata);

			//Creating the headline of the timeline...
			if (notebookIds.length==1) {
				timeline.put("headline", names.get(0));
				timeline.put("type", "default");
				timeline.put("text", "A timeline view of this public notebook.");
			} else {
				timeline.put("headline", "Collaborative timeline");
				timeline.put("type", "default");
				timeline.put("text", "A timeline view of different public notebooks.");
			}

			Iterator iter = annotations.iterator();

			while (iter.hasNext()) {

				JSONObject anns = (JSONObject)iter.next();
				JSONObject graph = (JSONObject)anns.get("graph");

				JSONObject itemsGraph = (JSONObject)anns.get("items");

				Iterator iter2 = graph.keySet().iterator();

				while(iter2.hasNext()) {

					String subject = (String)iter2.next();

					ArrayList<String> dates = getRDFPropertyValues(subject, Namespaces.PROPERTY_DATE, graph);
					ArrayList<String> startDates = getRDFPropertyValues(subject, Namespaces.PROPERTY_STARTDATE, graph);
					ArrayList<String> endDates = getRDFPropertyValues(subject, Namespaces.PROPERTY_ENDDATE, graph);
					ArrayList<String> creators = getRDFPropertyValues(subject, Namespaces.PROPERTY_CREATOR, graph);
					ArrayList<String> comments = getRDFPropertyValues(subject, Namespaces.PROPERTY_COMMENT, graph);
					ArrayList<String> depictions = getRDFPropertyValues(subject, Namespaces.PROPERTY_DEPICTS, graph);
					ArrayList<String> tags = getRDFPropertyValues(subject, Namespaces.PROPERTY_TAG, graph);
					ArrayList<String> titles = getRDFPropertyValues(subject, Namespaces.PROPERTY_TITLE, graph);
					ArrayList<String> similar = getRDFPropertyValues(subject, Namespaces.PROPERTY_SIMILAR, graph);
					ArrayList<String> describedIn = getRDFPropertyValues(subject, Namespaces.PROPERTY_DESCRIBED_IN, graph);
					ArrayList<String> citations = getRDFPropertyValues(subject, Namespaces.PROPERTY_CITES, graph);
					ArrayList<String> hasDepiction = getRDFPropertyValues(subject, Namespaces.PROPERTY_HAS_DEPICTION, graph);

					ArrayList<String> pages = getRDFPropertyValues(subject, Namespaces.PROPERTY_PAGE, itemsGraph);
					ArrayList<String> images = getRDFPropertyValues(subject, Namespaces.PROPERTY_IMAGE, itemsGraph);
					ArrayList<String> labels = getRDFPropertyValues(subject, Namespaces.RDFS_LABEL, itemsGraph);
					ArrayList<String> types = getRDFPropertyValues(subject, Namespaces.RDF_TYPE, itemsGraph);
					ArrayList<String> descriptions = getRDFPropertyValues(subject, Namespaces.PROPERTY_DESCRIPTION, itemsGraph);

					
					JSONObject node = new JSONObject();
					boolean isPresent = false;
					for (int k = 0; k < datesArray.size(); k++) {
						JSONObject entry = (JSONObject)datesArray.get(k);
						String entryUri = (String)entry.get("uri");
						if (entryUri.equals(subject)) {
							node = entry;
							isPresent = true;
						}
					}

					if (!isPresent) {

						node.put("uri", subject);


					}


					if (startDates != null && !startDates.isEmpty()) {
						if (node.get("startDate") == null) {
							String date = transformDateformat(startDates.get(0));
							updateMinMaxDates(date);
							node.put("startDate", date);	
						};
						
						
					} else if (dates != null && !dates.isEmpty()) {
						if (node.get("startDate") == null) {
							String date = transformDateformat(dates.get(0));
							updateMinMaxDates(date);
							node.put("startDate", date);	
						}
						
					}
					if (endDates != null && !endDates.isEmpty()) {
						if (node.get("endDate") == null) {
							String date = transformDateformat(endDates.get(0));
							updateMinMaxDates(date);
							node.put("endDate", date);	
						}
						
					}

					String text = (String)node.get("text");
					if (text == null) {
						text = "";
					}

					
					if (types.contains(Namespaces.CLASS_TEXT_FRAGMENT)) {						
						if (!isPresent) {							
							node.put("headline", stringToUTF8(labels.get(0)) + "...");
							if (descriptions != null && !descriptions.isEmpty()) {
								String descriptionsString ="";
								for (int o = 0; o < descriptions.size(); o++) {
									descriptionsString += "<br/>" + stringToUTF8(descriptions.get(o));	
								}
								text += "<p>" + descriptionsString + "</p>";

							}							
						}

					} else if (types.contains(Namespaces.CLASS_IMAGE)) {
						
						if (!isPresent) {
							node.put("headline", createLabelForImages(labels.get(0)));
						}
											
					} else if (types.contains(Namespaces.CLASS_WEB_PAGE)) {
						
						if (!isPresent) {
							node.put("headline", createLabelForWebPage(subject, labels.get(0)));
						}
	
					}

					
					if (titles != null && !titles.isEmpty()) {
						node.put("headline", titles.get(0));
					} 
					
					
					if (images != null && !images.isEmpty()) {
						
						addImageToAsset(node, images.get(0));

					}	
					
					if (hasDepiction != null && !hasDepiction.isEmpty()) {

						ArrayList<String> depictionItems = getRDFPropertyValues(subject, Namespaces.PROPERTY_HAS_DEPICTION, graph);
						for (String depictionItem : depictionItems) {
							ArrayList<String> depictionURLs = getRDFPropertyValues(depictionItem, Namespaces.PROPERTY_IMAGE, itemsGraph);
							for (String depictionURL : depictionURLs) {
								addImageToAsset(node, depictionURL);
							}
						}
						
					}
					
					//XXX: The following properties (if present in more annotations) are added iteratively to the text of the frame 


					if (comments != null && !comments.isEmpty()) {
						String commentsString = "";
						for (int o = 0; o < comments.size(); o++) {
							commentsString += "<br/>" + comments.get(o);	
						}
						text += "<p><i>Comments:" + commentsString + "</i></p>";

					}
					if (similar != null && !similar.isEmpty()) {
						String similarString ="";
						for (int f = 0; f < similar.size(); f++) {

							ArrayList<String> similarLabel = getRDFPropertyValues(similar.get(f), Namespaces.RDFS_LABEL, itemsGraph);

							similarString += "<br/>";
							
							ArrayList<String> thumbs = getRDFPropertyValues(similar.get(f), Namespaces.PROPERTY_IMAGE, itemsGraph);
							
							if (thumbs != null && !thumbs.isEmpty()) {
								similarString += "<img src='" + thumbs.get(0) + "' height='40'></img><br/>";
							}
							
							similarString += "&nbsp;<a href='" + similar.get(f).split("#")[0] + "' target='_blank'>" + similarLabel + "</a>";
							
							
						}
						text += "<p>Similar to:" + similarString + "</p>";

					}
					if (creators != null && !creators.isEmpty()) {
						String creatorsString ="";
						for (int f = 0; f < creators.size(); f++) {

							ArrayList<String> creatorLabel = getRDFPropertyValues(creators.get(f), Namespaces.RDFS_LABEL, itemsGraph);
							
							creatorsString += "<br/>";
							
							ArrayList<String> thumbs = getRDFPropertyValues(creators.get(f), Namespaces.PROPERTY_IMAGE, itemsGraph);
							
							if (thumbs != null && !thumbs.isEmpty()) {
								creatorsString += "<img src='" + thumbs.get(0) + "' height='40'></img><br/>";
							}

							creatorsString += "<a href='" + creators.get(f) + "' target='_blank'>" + creatorLabel + "</a>";	
						}
						text += "<p>Creator:" + creatorsString + "</p>";

					}
					if (citations != null && !citations.isEmpty()) {
						String citationsString ="";
						for (int f = 0; f < citations.size(); f++) {

							ArrayList<String> citationsLabel = getRDFPropertyValues(citations.get(f), Namespaces.RDFS_LABEL, itemsGraph);
							
							citationsString += "<br/>";
							
							ArrayList<String> thumbs = getRDFPropertyValues(citations.get(f), Namespaces.PROPERTY_IMAGE, itemsGraph);
							
							if (thumbs != null && !thumbs.isEmpty()) {
								citationsString += "<img src='" + thumbs.get(0) + "' height='40'></img><br/>";
							}

							citationsString += "<br/><a href='" + citations.get(f) + "' target='_blank'>" + citationsLabel + "</a>";	
						}
						text += "<p>Cites:" + citationsString + "</p>";

					}
					if (describedIn != null && !describedIn.isEmpty()) {
						String string ="";
						for (int f = 0; f < describedIn.size(); f++) {

							ArrayList<String> label = getRDFPropertyValues(describedIn.get(f), Namespaces.RDFS_LABEL, itemsGraph);

							string += "<br/><a href='" + describedIn.get(f) + "' target='_blank'>" + label + "</a>";	
						}
						text += "<p>Described in:" + string + "</p>";

					}
					if (depictions != null && !depictions.isEmpty()) {
						String depictionsString ="";
						for (int p = 0; p < depictions.size(); p++) {

							ArrayList<String> depictedLabel = getRDFPropertyValues(depictions.get(p), Namespaces.RDFS_LABEL, itemsGraph);
							
							depictionsString += "<br/>";
							
							ArrayList<String> thumbs = getRDFPropertyValues(depictions.get(p), Namespaces.PROPERTY_IMAGE, itemsGraph);
							
							if (thumbs != null && !thumbs.isEmpty()) {
								depictionsString += "<img src='" + thumbs.get(0) + "' height='40'></img><br/>";
							}

							node.put("tag",depictedLabel);
							
							depictionsString += "<a href='" + depictions.get(p) +"' target='_blank'>" + depictedLabel + "</a>";

						}
						text += "<p>Depicts:" + depictionsString + "</p>";



					}
					if (tags != null && !tags.isEmpty()) {
						String tagString ="";
						for (int e = 0; e < tags.size(); e++) {

							ArrayList<String> tagLabel =getRDFPropertyValues(tags.get(e), Namespaces.RDFS_LABEL, itemsGraph);
							
							tagString += "&nbsp;&nbsp;";
							
							ArrayList<String> thumbs = getRDFPropertyValues(tags.get(e), Namespaces.PROPERTY_IMAGE, itemsGraph);
							
							if (thumbs != null && !thumbs.isEmpty()) {
								tagString += "<img src='" + thumbs.get(0) + "' height='40'></img>";
							}

							tagString += "<a href='" + tags.get(e) +"' target='_blank'>" + tagLabel + "</a> ";	
						}
						
						if (text.contains("<p>Tags:<br/>")) {
							String[] pieces = text.split("<p>Tags:");
							text = pieces[0] + "<p>Tags:<br/>" + tagString  + pieces[1]; 
						} else {
							text += "<p>Tags:<br/>" + tagString + "</p>";	
						} 
						
						

					}
					
					text += printObjectPropertyValue(subject, "http://purl.org/pundit/ont/ao#hasDepiction", "has depiction", graph, itemsGraph);
					text += printObjectPropertyValue(subject, "http://purl.org/marinelives/vocab/mentionsPerson", "mentions person", graph, itemsGraph);
					text += printObjectPropertyValue(subject, "http://purl.org/marinelives/vocab/mentionsBoat", "mentions boat", graph, itemsGraph);
					
					
					if (!isPresent && pages != null && !pages.isEmpty()) {
							for (int y = 0; y < pages.size(); y++) {
								text = "<p><a href='" + pages.get(y) + "' target='_blank'>Go to annotated page >></a></p>" + text;	
							}	
					} else if (!isPresent && types.contains(Namespaces.CLASS_WEB_PAGE)) {	
						text = "<p><a href='" + subject + "' target='_blank'>Go to annotated page >></a></p>" + text;
	
					}
					node.put("text", text);

					if (!isPresent) {
						datesArray.add(node);	
					}
					
					//}

				}


			}


		}
		timeline.put("startDate", minDate + ",1,1");
		timeline.put("endDate", maxDate + ",1,1");

		return "storyjs_jsonp_data = " + root.toString();

	}


	private String stringToUTF8(String string) {
		return new String(string.getBytes(),Charset.forName("UTF-8"));
	}


	private void addImageToAsset(JSONObject node, String imageUrl) {
		JSONObject asset;
		if(node.get("asset") == null) {
			asset = new JSONObject();
		} else {
			asset = (JSONObject)node.get("asset");
			//remove the copy that will be replaced with the new one...
			node.remove("asset");
		}
		
		asset.put("media", imageUrl);
		
		//Update the asset by putting a new copy in
		node.put("asset", asset);
		
		
	}
	
	/**
	 * Retrieves from the RDF graph, represented in RDF/JSON, the values of the triples with the given subject and predicate. 
	 * @param rdfSubject. The subject of the triples to be matched
	 * @param rdfProperty. The predicate of the triples to be matched
	 * @param graph The RDF graph represented in RDF/JSON
	 * @return
	 */
	private ArrayList<String> getRDFPropertyValues(String rdfSubject, String rdfProperty, JSONObject graph) {

		ArrayList<String> values = new ArrayList<String>();
		// Get the JSOB object representing all the triples with the given subject ...
		JSONObject triples = (JSONObject)graph.get(rdfSubject);
		if (triples == null) return values;
		// Get the JSON array with all the values of the matched triples ...
		JSONArray objectValues = (JSONArray)triples.get(rdfProperty);
		if (objectValues == null) return values;
		// For each object, get his string value and add it to the results ... 
		for (Object objv : objectValues) {
			JSONObject obj = (JSONObject)objv;
			String objvString = (String)obj.get("value");
			if (objvString != null) {
				values.add(objvString);
			}

		}

		return values;
	}  

	private String transformDateformat(String date) {
		return date.replace("-",",");
	}

	private void updateMinMaxDates(String date) {

		Integer dateInt = Integer.parseInt(date.split(",")[0]);
		if (dateInt.intValue() < minDate) {
			minDate = dateInt.intValue();
		}
		if (dateInt.intValue() > maxDate) {
			maxDate = dateInt.intValue();
		}

	}

	private String createLabelForWebPage(String subject, String label) {
		
		return stringToUTF8(label);
	}
	
	private String createLabelForImages(String label) {
		String newLabel;
		if (label.length() > 50) {
			newLabel = stringToUTF8(label.substring(0, 50));
		} else {
			newLabel = stringToUTF8(label);
		}
		newLabel = newLabel.replace("[","").replace("]","");

		newLabel = newLabel.split("\\?")[0];

		newLabel = newLabel.split("/")[newLabel.split("/").length-1];

		return newLabel;
	}

	private JSONObject getNotebookJSON(String id) throws IOException {

		String parameters = "notebooks/" + id + "/";
		URL call = new URL(annotationApi + parameters);
		BufferedReader reader = new BufferedReader(new InputStreamReader(call.openStream()));
		String json = "";
		String line = null;
		while ((line = reader.readLine()) != null) {
			json += line;
			//System.out.println(line);
		}

		return JSONObject.fromObject(json);

	}



	public static void main(String[] args) throws IOException {

		TimeLineReader tlrd = new TimeLineReader();

		String root = tlrd.getTimelineJSON("c2484d04", "http://swickynotes.org/notebook/resource/", "http://as.thepund.it:8080/annotationserver/api/open/");

		System.out.println(root);



	}
	
	/**
	 * 
	 * Returns an HTML representation of the given RDF triple
	 * @param subject. the subject of the triple
	 * @param property. the predicate
	 * @param propertyLabel. the label of the predicate
	 * @param graph. the RDf annotation graph in RDF/JSON format 
	 * @param itemsGraph. the Items of the annotation as an RDF/JSON serialized graph
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String printObjectPropertyValue(String subject, String property, String propertyLabel, JSONObject graph, JSONObject itemsGraph) throws UnsupportedEncodingException {
		
		String html = "";
		
		// get the values of the matched triples
		ArrayList<String> values = getRDFPropertyValues(subject, property, graph);
		
		if (values != null && !values.isEmpty()) {
			String content ="";
			
			// For each value ...
			for (int f = 0; f < values.size(); f++) {
				
				// Get the rdf:label of the object value. NOTE: this information is stored in the annotation Items graph. 
				ArrayList<String> label = getRDFPropertyValues(values.get(f), Namespaces.RDFS_LABEL, itemsGraph);
				content += "<br/>";
				
				// Get the image associated to the value, if any ...
				ArrayList<String> thumbs = getRDFPropertyValues(values.get(f), Namespaces.PROPERTY_IMAGE, itemsGraph);
				// if there is a n image, create a simple HTML enclosing it ...
				if (thumbs != null && !thumbs.isEmpty()) {
					content += "<img src='" + thumbs.get(0) + "' height='40'></img><br/>";
				}
				String url = values.get(f).split("#")[0];
				
				// Create an hyperlink to the resource...
				content += "<a href=\"" + url + "\">" + label + "</a>";
				
			}
			html += "<p>" + propertyLabel + ":" + content + "</p>";

		}
		return html;
		
	}

}
