package watson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.ibm.watson.developer_cloud.conversation.v1_experimental.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse;
import com.ibm.watson.developer_cloud.util.GsonSingleton;
import watson.model.DevoxxDocument;

/**
 * Created by jamesweaver on 8/11/16.
 */
/**
 * Copyright IBM Corp. 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


  public class CallCorpus {
    private static final String SEARCH_REST_ENDPOINT = "http://sherlock.devoxx.com/api/search/";
    private static String API_VERSION = "v1-experimental";
    private static String PASSWORD = "Y8gUTfOHTB2b";
    private static String URL = "https://gateway.watsonplatform.net/conversation-experimental/api";
    private static String USERNAME = "371ffd54-0cab-49bd-b555-7b9320234ada";

    private static final int TIMEOUT_IN_MILLIS = 15000;
    private static final String OUTPUT_MODE = "outputMode";
    private static final String JSON = "json";

    private static final String TEXT_GET_RANKED_KEYWORDS = "https://gateway-a.watsonplatform.net/calls/text/TextGetRankedKeywords";
    private static final String TEXT = "text";
    private static final String APIKEY = "apikey";
    private static final String apikey = "3f8ada5e94db2aa57f5b7804be2d1017c3ba8ace";
    private static final String KEYWORDS = "keywords";

  /*
  "credentials": {
    "url": "https://gateway.watsonplatform.net/concept-insights/api",
    "password": "FPhRNXvLnJ9m",
    "username": "a425694b-f06a-4956-886d-e2e9e66d7c65"
   */

    private static final String INSIGHTS_USERNAME = "a425694b-f06a-4956-886d-e2e9e66d7c65";
    private static final String INSIGHTS_PASSWORD = "FPhRNXvLnJ9m";

    private static final Logger LOGGER = Logger.getLogger(CallCorpus.class.getName());
    private static boolean LOGGING_ENABLED = Boolean.parseBoolean(System.getenv("LOGGING_ENABLED"));

    private MessageRequest buildMessageFromPayload(InputStream body) {
      StringBuilder sbuilder = null;
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(body, "UTF-8"));
        sbuilder = new StringBuilder();
        String str = reader.readLine();
        while (str != null) {
          sbuilder.append(str);
          str = reader.readLine();
          if (str != null) {
            sbuilder.append("\n");
          }
        }
        return GsonSingleton.getGson().fromJson(sbuilder.toString(), MessageRequest.class);
      } catch (IOException e) {
        System.out.println("ProxyResource.JSON_READ:" +  e);
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          System.out.println("CallCorpus.STREAM_CLOSE: " + e);
        }
      }
      return null;
    }

    /**
     * This method is responsible for sending the query the user types into the UI to the Watson
     * services. The code demonstrates how the conversation service is called, how the response is
     * evaluated, and how the response is then sent to the retrieve and rank service if necessary.
     *
     * @param request The full query the user asked of Watson
     * @param id The ID of the conversational workspace
     * @return The response from Watson. The response will always contain the conversation service's
     *         response. If the intent confidence is high or the intent is out_of_scope, the response
     *         will also contain information from the retrieve and rank service
     */
    private MessageResponse getWatsonResponse(MessageRequest request, String id) throws Exception {

      List<DevoxxDocument> payload = null;

      // Configure the Watson Developer Cloud SDK to make a call to the appropriate conversation
      // service. Specific information is obtained from the VCAP_SERVICES environment variable
      ConversationService service =
          new ConversationService(API_VERSION != null ? API_VERSION : ConversationService.VERSION_DATE_2016_05_19);
      if (USERNAME != null || PASSWORD != null) {
        service.setUsernameAndPassword(USERNAME, PASSWORD);
      }
      if (URL != null) {
        service.setEndPoint(URL);
      }

      // Use the previously configured service object to make a call to the conversational service
      MessageResponse response = service.message(id, request).execute();

      // Determine if conversation's response is sufficient to answer the user's question or if we
      // should call the retrieve and rank service to obtain better answers
      if (response.getOutput().toString().contains("callRetrieveAndRank")) {
        String query = response.getInputText();
        System.out.println("query: " + query);

        List<String> searchWords = getKeywordsFromText(query);
        String spaceDelimited = String.join("%20", searchWords);

        System.out.println("spaceDelimited: \"" + spaceDelimited + "\"");

        payload = retrieveDocument(spaceDelimited);

        // For this app, both the original conversation response and the retrieve and rank response
        // are sent to the UI. Extract and add the conversational response to the ultimate response
        // we will send to the user. The UI will process this response and show the top 5 retrieve
        // and rank answers to the user in the main UI. The JSON response section of the UI will
        // show information from the calls to both services.
        Map<String, Object> output = response.getOutput();
        if (output == null) {
          output = new HashMap<String, Object>();
          response.setOutput(output);
        }
        // Send the user's question to the retrieve and rank service
        //List<DocumentPayload> docs = retrieveAndRankClient.getDocuments(query);

        // Append the retrieve and rank answers to the output object that will be sent to the UI
        output.put("DevoxxPayload", payload); //$NON-NLS-1$

      }
      else {
        String query = response.getInputText();
        System.out.println("query else: " + query);
      }

      return response;
    }

  public Response postMessage(String id, InputStream body) {

    HashMap<String, Object> errorsOutput = new HashMap<String, Object>();
    MessageRequest request = buildMessageFromPayload(body);

    if (request == null) {
      System.out.println("ProxyResource.NO_REQUEST");
    }

    MessageResponse response = null;

    try {
      response = getWatsonResponse(request, id);

    } catch (Exception e) {
      System.out.println("Something is terribly wrong");
    }
    Response retVal = Response.ok(new Gson().toJson(response, MessageResponse.class)).type(MediaType.APPLICATION_JSON).build();
    //System.out.println("retval.toString(): " + retVal.toString());
    return retVal;
  }

  private List<DevoxxDocument> retrieveDocument(String delimitedText) throws IOException {
    List<DevoxxDocument> devoxxPayload = new ArrayList<>();

    final Document doc = Jsoup.connect(SEARCH_REST_ENDPOINT + delimitedText)
        .timeout(TIMEOUT_IN_MILLIS)
        .method(Connection.Method.GET)
        .data("username", INSIGHTS_USERNAME)
        .data("password", INSIGHTS_PASSWORD)
        .data(OUTPUT_MODE, JSON)
        .ignoreContentType(true)
        .execute()
        .parse();

    LOGGER.info(doc.text());

    final JsonElement jsonElement = new JsonParser().parse(doc.text());
    final JsonArray jsonArray = jsonElement.getAsJsonArray();
    Iterator iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      JsonObject jsonObject = (JsonObject)iterator.next();

      DevoxxDocument devoxxDocument = new DevoxxDocument();

      // Get the id
      if (jsonObject.has("id")) {
        devoxxDocument.setId(jsonObject.get("id").toString());
        System.out.println("id: " + devoxxDocument.getId());
      }

      // Get the label
      if (jsonObject.has("label")) {
        devoxxDocument.setLabel(jsonObject.get("label").toString());
        System.out.println("label: " + devoxxDocument.getLabel());
      }

      // Get the score
      if (jsonObject.has("score")) {
        devoxxDocument.setScore(jsonObject.get("score").toString());
        System.out.println("score: " + devoxxDocument.getScore());
      }

      // Get the elements in userFields
      if (jsonObject.has("userFields")) {
        JsonObject userFields = (JsonObject) jsonObject.get("userFields");

        // Get the authors
        if (userFields.has("authors")) {
          devoxxDocument.setAuthors(userFields.get("authors").toString());
          System.out.println("authors: " + devoxxDocument.getAuthors());
        }

        // Get the emotions
        if (userFields.has("emotions")) {
          devoxxDocument.setEmotions(userFields.get("emotions").toString());
          System.out.println("emotions: " + devoxxDocument.getEmotions());
        }

        // Get the language
        if (userFields.has("language")) {
          devoxxDocument.setLanguage(userFields.get("language").toString());
          System.out.println("language: " + devoxxDocument.getLanguage());
        }

        // Get the link
        if (userFields.has("link")) {
          devoxxDocument.setLink(userFields.get("link").toString());
          System.out.println("link: " + devoxxDocument.getLink());
        }

        // Get the publicationDate
        if (userFields.has("publicationDate")) {
          devoxxDocument.setPublicationDate(userFields.get("publicationDate").toString());
          System.out.println("publicationDate: " + devoxxDocument.getPublicationDate());
        }

        // Get the sentiment
        if (userFields.has("sentiment")) {
          devoxxDocument.setSentiment(userFields.get("sentiment").toString());
          System.out.println("sentiment: " + devoxxDocument.getSentiment());
        }

        // Get the thumbnail
        if (userFields.has("thumbnail")) {
          devoxxDocument.setThumbnail(userFields.get("thumbnail").toString());
          System.out.println("thumbnail: " + devoxxDocument.getThumbnail());
        }

        // Get the thumbnailKeywords
        if (userFields.has("thumbnailKeywords")) {
          devoxxDocument.setThumbnailKeywords(userFields.get("thumbnailKeywords").toString());
          System.out.println("thumbnailKeywords: " + devoxxDocument.getThumbnailKeywords());
        }

      }

      devoxxPayload.add(devoxxDocument);
    }
    return devoxxPayload;

  }

  /**
   * Given the text of an abstract, identify keywords useful for recognizing
   *
   * @param text text of an abstract
   *
   * @return sorted list of unique keywords
   *
   *     curl -X POST \
   *          -d "apikey={API-KEY}" \
   *          -d "outputMode=json" \
   *          -d "text=this is some abstract text" \
   *          "https://gateway-a.watsonplatform.net/calls/text/TextGetRankedKeywords"
   */
  List<String> getKeywordsFromText(final String text) throws IOException {

    String queryText = (text == null || text.length() == 0) ? "keyword" : text;
    final List<String> keywords = new ArrayList<>();
    final Document doc =
        Jsoup.connect(TEXT_GET_RANKED_KEYWORDS)
            .timeout(TIMEOUT_IN_MILLIS)
            .method(Connection.Method.POST)
            .data(APIKEY, apikey)
            .data(OUTPUT_MODE, JSON)
            .data(TEXT, queryText)
            .ignoreContentType(true)
            .execute()
            .parse();

    final JsonElement element = new JsonParser().parse(doc.text());

    JsonArray array = element.getAsJsonObject().get(KEYWORDS).getAsJsonArray();

    for (final JsonElement keywordElement : array) {
      String label = keywordElement.getAsJsonObject().get("text").getAsString();
      String[] tokens = label.split(" ");
      for (String token : tokens) {
        if (!keywords.contains(token)) {
          keywords.add(token);
        }
      }
    }
    //Collections.sort(keywords);
    return keywords;
  }


}
