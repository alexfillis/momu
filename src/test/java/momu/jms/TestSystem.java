package momu.jms;

import momu.system.Startable;
import momu.system.Stoppable;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQConnectionFactory;

import javax.jms.*;
import java.util.concurrent.CountDownLatch;

public class TestSystem implements Startable {

    private HornetQConnectionFactory cf;
    private Destination orderQueue;
    private Connection c;
    private Session session;
    private MessageConsumer consumer;

    public static void main(String[] args) {
        final Stoppable system = new TestSystem().start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                system.stop();
            }
        });
        try {
            system.awaitShutdown();
        } catch (java.lang.InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public Stoppable start() {
        System.out.println("Starting...");

        final CountDownLatch countDownLatch = new CountDownLatch(1);

        TransportConfiguration transportConfiguration = new TransportConfiguration(NettyConnectorFactory.class.getName());
        cf = HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
        orderQueue = HornetQJMSClient.createQueue("OrderQueue");
        try {
            c = cf.createConnection();
            session = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(orderQueue);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        Session session = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        try {
                            MessageProducer producer = session.createProducer(message.getJMSReplyTo());
                            try {
                                TextMessage textMessage = session.createTextMessage();
                                String text = ((TextMessage) message).getText();
                                System.out.println("Message received: " + text);
                                textMessage.setText(text + " and thanks");
                                producer.send(message.getJMSReplyTo(), textMessage);
                            } finally {
                                producer.close();
                            }
                        } finally {
                            session.close();
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            });
            c.start();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Started");

        return new Stoppable() {
            @Override
            public void stop() {
                System.out.println("Shutting down...");
                cleanup();
                countDownLatch.countDown();
                System.out.println("Shutdown");
            }

            @Override
            public void awaitShutdown() throws InterruptedException {
                countDownLatch.await();
            }
        };
    }

    private void cleanup() {
        if (c != null) {
            try {
                c.stop();
            } catch (JMSException ignore) {}
        }
        if (consumer != null) {
            try {
                consumer.close();
            } catch (JMSException ignore) {}
        }
        if (session != null) {
            try {
                session.close();
            } catch (JMSException ignore) {}
        }
        if (c != null) {
            try {
                c.close();
            } catch (JMSException ignore) {}
        }
        if (cf != null) {
            cf.close();
        }
    }
}
