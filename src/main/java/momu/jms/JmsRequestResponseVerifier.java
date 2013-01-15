package momu.jms;

import javax.jms.*;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This class is not thread-safe. If you need to use it across multiple threads, prefer instead to have an instance per
 * thread.
 */
public class JmsRequestResponseVerifier {
    private Connection connection;
    private Session session;

    public JmsRequestResponseVerifier(Connection connection) {
        this.connection = connection;
    }

    public void init() throws JMSException {
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    public void cleanup() {
        if (connection != null) {
            try {
                connection.stop();
            } catch (JMSException ignore) {}
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException ignore) {}
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException ignore) {}
        }
    }

    public Message buildTextMessage(String message) throws JMSException {
        TextMessage textMessage = session.createTextMessage();
        textMessage.setText(message);
        return textMessage;
    }

    public ResponseVerifier.Results sendAndVerifyResponse(Message message, Destination sendTo, List<ResponseVerifier> responseVerifiers) throws JMSException, InterruptedException, TimeoutException {
        TemporaryQueue replyTo = session.createTemporaryQueue();
        message.setJMSReplyTo(replyTo);
        try {
            return sendAndVerifyResponse(message, sendTo, replyTo, responseVerifiers);
        } finally {
            replyTo.delete();
        }
    }

    private ResponseVerifier.Results sendAndVerifyResponse(Message message, Destination sendTo, Destination repliesOn, List<ResponseVerifier> responseVerifiers) throws JMSException, InterruptedException, TimeoutException {
        MessageConsumer messageConsumer = session.createConsumer(repliesOn);
        try {
            final ResponseVerifier.Results results = prepareForVerification(messageConsumer, responseVerifiers);
            send(message, sendTo);
            results.await();
            return results;
        } finally {
            messageConsumer.close();
        }
    }

    private ResponseVerifier.Results prepareForVerification(MessageConsumer messageConsumer, List<ResponseVerifier> responseVerifiers) throws JMSException {
        final ResponseVerifier.Results<String> results = new ResponseVerifier.Results<String>(responseVerifiers.size());
        messageConsumer.setMessageListener(new ResponseVerifierMessageListener(responseVerifiers, results));
        return results;
    }

    private void send(Message message, Destination destination) throws JMSException {
        MessageProducer messageProducer = session.createProducer(destination);
        try {
            messageProducer.send(message);
            System.out.println("sent: " + ((TextMessage)message).getText());
        } finally {
            messageProducer.close();
        }
    }

}
