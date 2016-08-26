package watson;

import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.util.GsonSingleton;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by jamesweaver on 8/11/16.
 */
public class TestConversation {
  private static String WORKSPACE_ID = "e1faa444-789c-40f6-bc6a-34fd765450a9";
  public static void main(String args[]) {
    String text = "How many legs does a dog have?";
    //String text = "Why is there air?";
    //String text = "what is a rock?";
    //String text = "What is a JavaFX application?";
    //String text = "What is an angular application?";
    //String text = "lambda expressions";
    //String text = "How to structure JavaFX?";

    CallCorpus callCorpus = new CallCorpus();

    MessageRequest request = new MessageRequest.Builder().inputText(text).build();
    String payload = GsonSingleton.getGsonWithoutPrettyPrinting().toJson(request, MessageRequest.class);
    InputStream inputStream = new ByteArrayInputStream(payload.getBytes());

    Response jaxResponse = callCorpus.postMessage(WORKSPACE_ID, inputStream);
    MessageResponse serviceResponse = GsonSingleton.getGsonWithoutPrettyPrinting()
        .fromJson(jaxResponse.getEntity().toString(), MessageResponse.class);

    List<String> serviceText = serviceResponse.getText();

    System.out.println("serviceText:");
    for (String str : serviceText) {
      System.out.println(str);
    }
  }
}
