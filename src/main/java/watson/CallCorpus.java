package watson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.ibm.watson.developer_cloud.conversation.v1_experimental.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse;
import com.ibm.watson.developer_cloud.service.exception.UnauthorizedException;
import com.ibm.watson.developer_cloud.util.GsonSingleton;
import watson.model.DevoxxPayload;

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

        retrieveDocument();
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

  private void retrieveDocument() throws IOException {
    final Document doc = Jsoup.connect(SEARCH_REST_ENDPOINT + "lambda")
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

      DevoxxPayload devoxxPayload = new DevoxxPayload();

      // Get the id
      if (jsonObject.has("id")) {
        devoxxPayload.setId(jsonObject.get("id").toString());
        System.out.println("id: " + devoxxPayload.getId());
      }

      // Get the label
      if (jsonObject.has("label")) {
        devoxxPayload.setLabel(jsonObject.get("label").toString());
        System.out.println("label: " + devoxxPayload.getLabel());
      }

      // Get the score
      if (jsonObject.has("score")) {
        devoxxPayload.setScore(jsonObject.get("score").toString());
        System.out.println("score: " + devoxxPayload.getScore());
      }

      // Get the elements in userFields
      if (jsonObject.has("userFields")) {
        JsonObject userFields = (JsonObject) jsonObject.get("userFields");

        // Get the authors
        if (userFields.has("authors")) {
          devoxxPayload.setAuthors(userFields.get("authors").toString());
          System.out.println("authors: " + devoxxPayload.getAuthors());
        }

        // Get the emotions
        if (userFields.has("emotions")) {
          devoxxPayload.setEmotions(userFields.get("emotions").toString());
          System.out.println("emotions: " + devoxxPayload.getEmotions());
        }

        // Get the language
        if (userFields.has("language")) {
          devoxxPayload.setLanguage(userFields.get("language").toString());
          System.out.println("language: " + devoxxPayload.getLanguage());
        }

        // Get the link
        if (userFields.has("link")) {
          devoxxPayload.setLink(userFields.get("link").toString());
          System.out.println("link: " + devoxxPayload.getLink());
        }

        // Get the publicationDate
        if (userFields.has("publicationDate")) {
          devoxxPayload.setPublicationDate(userFields.get("publicationDate").toString());
          System.out.println("publicationDate: " + devoxxPayload.getPublicationDate());
        }

        // Get the sentiment
        if (userFields.has("sentiment")) {
          devoxxPayload.setSentiment(userFields.get("sentiment").toString());
          System.out.println("sentiment: " + devoxxPayload.getSentiment());
        }

        // Get the thumbnail
        if (userFields.has("thumbnail")) {
          devoxxPayload.setThumbnail(userFields.get("thumbnail").toString());
          System.out.println("thumbnail: " + devoxxPayload.getThumbnail());
        }

        // Get the thumbnailKeywords
        if (userFields.has("thumbnailKeywords")) {
          devoxxPayload.setThumbnailKeywords(userFields.get("thumbnailKeywords").toString());
          System.out.println("thumbnailKeywords: " + devoxxPayload.getThumbnailKeywords());
        }

        //JsonElement authors = userFields.get("authors");
        //System.out.println("authors: " + devoxxPayload.getAuthors());
      }


    }

    /*
    if (json.has(IMAGE_KEYWORDS)) {
      final JsonArray imageKeywords = json.get(IMAGE_KEYWORDS).getAsJsonArray();

      if (imageKeywords.size() > 0) {
        return imageKeywords.get(0).getAsJsonObject().get("text").getAsString();
      }
    }
    */

    //return "no results";

  }
}
