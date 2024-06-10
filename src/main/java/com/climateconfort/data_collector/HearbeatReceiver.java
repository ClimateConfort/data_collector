package com.climateconfort.data_collector;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.climateconfort.common.Constants;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class HearbeatReceiver {
    private static final Logger LOGGER = LogManager.getLogger(HearbeatReceiver.class);
    private boolean isStop;
    private final ConnectionFactory connectionFactory;
    private final Semaphore semaphore;

    public HearbeatReceiver(Properties properties) throws UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        TlsManager tlsManager = new TlsManager(properties);
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(properties.getProperty("rabbitmq.server.ip", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(properties.getProperty("rabbitmq.server.port", "5672")));
        this.connectionFactory.setUsername(properties.getProperty("rabbitmq.server.user", "guest"));
        this.connectionFactory.setPassword(properties.getProperty("rabbitmq.server.password", "guest"));
        this.connectionFactory.useSslProtocol(tlsManager.getSslContext());
        this.isStop = false;
        this.semaphore = new Semaphore(1);
    }

    public void subscribe() throws IOException, TimeoutException, InterruptedException {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Constants.HEARTBEAT_EXCHANGE, "fanout");
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, Constants.HEARTBEAT_EXCHANGE, "");
            HeartbeatConsumer consumer = new HeartbeatConsumer(channel);
            String tag = channel.basicConsume(queueName, true, consumer);
            synchronized (this) {
                while (!isStop) {
                    this.wait();
                }
            }
            channel.basicCancel(tag);
        }
    }

    public void waitForHeartbeat() throws InterruptedException {
        LOGGER.info("Waiting for heartbeat...");
        semaphore.acquire();
        LOGGER.info("Waiting for heartbeat... - done");
    }

    public synchronized void stop() {
        isStop = true;
        this.notifyAll();
    }

    class HeartbeatConsumer extends DefaultConsumer {

        public HeartbeatConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            LOGGER.info("Server's heartbeat reached");
            if (semaphore.availablePermits() < 1) {
                semaphore.release();
            }
        }
    }

}
