package com.climateconfort.data_collector;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.climateconfort.common.Constants;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class ActionReceiver {
    private static final Logger LOGGER = LogManager.getLogger(ActionReceiver.class);
    private final long roomId;
    private final long buildingId;
    private final ConnectionFactory connectionFactory;
    private boolean isStop;

    public ActionReceiver(Properties properties) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        TlsManager tlsManager = new TlsManager(properties);
        this.roomId = Integer.parseInt(properties.getProperty("climateconfort.room_id", "NaN"));
        this.buildingId = Integer.parseInt(properties.getProperty("climateconfort.building_id", "NaN"));
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(properties.getProperty("rabbitmq.server.ip", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(properties.getProperty("rabbitmq.server.port", "5672")));
        this.connectionFactory.setUsername(properties.getProperty("rabbitmq.server.user", "guest"));
        this.connectionFactory.setPassword(properties.getProperty("rabbitmq.server.password", "guest"));
        this.connectionFactory.useSslProtocol(tlsManager.getSslContext());
        this.isStop = false;
    }

    public void subscribe() throws IOException, TimeoutException, InterruptedException {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Constants.SENSOR_ACTION_EXCHANGE, "direct");
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, Constants.SENSOR_ACTION_EXCHANGE, String.format("%d.%d", buildingId, roomId));
            ActionConsumer consumer = new ActionConsumer(channel);
            String tag = channel.basicConsume(queueName, true, consumer);
            synchronized (this) {
                while (!isStop) {
                    this.wait();
                }
            }
            channel.basicCancel(tag);
        }
    }

    public synchronized void stop() {
        isStop = true;
        this.notifyAll();
    }

    public class ActionConsumer extends DefaultConsumer {

        public ActionConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            String actionMessage = new String(body);
            LOGGER.info("The action should be: {}", actionMessage);
        }
    }
}
