package momu.jms;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResponseVerifierMessageListener implements MessageListener {
    private final Iterator<ResponseVerifier> it;
    private final ResponseVerifier.Results<String> results;

    public ResponseVerifierMessageListener(List<ResponseVerifier> responseVerifiers, ResponseVerifier.Results<String> results) {
        it = new ArrayList<ResponseVerifier>(responseVerifiers).iterator();
        this.results = results;
    }

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String messageText = textMessage.getText();
            System.out.println("Message received: " + messageText);
            it.next().verify(messageText);
            results.success(messageText);
        } catch (AssertionError a) {
            results.fail(a);
        } catch (Exception e) {
            results.error(e);
        }
    }
}
