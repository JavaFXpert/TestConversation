package watson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse;
import com.ibm.watson.developer_cloud.service.exception.UnauthorizedException;
import com.ibm.watson.developer_cloud.util.GsonSingleton;

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
    private static String API_VERSION = "v1-experimental";
    private static String PASSWORD = "Y8gUTfOHTB2b";
    private static String URL = "https://gateway.watsonplatform.net/conversation-experimental/api";
    private static String USERNAME = "371ffd54-0cab-49bd-b555-7b9320234ada";

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
      if (response.getContext().containsKey("callRetrieveAndRank")) {
        String query = response.getInputText();
        System.out.println("query: " + query);
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


}
