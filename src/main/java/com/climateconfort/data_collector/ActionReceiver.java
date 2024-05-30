package com.climateconfort.data_collector;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.climateconfort.common.Constants;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP.BasicProperties;

public class ActionReceiver {
    private final long roomId;
    private final long buildingId;
    private final ConnectionFactory connectionFactory;
    private boolean isStop;

    public ActionReceiver(Properties properties) {
        this.roomId = Integer.parseInt(properties.getProperty("room_id", "NaN"));
        this.buildingId = Integer.parseInt(properties.getProperty("building_id", "NaN"));
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(properties.getProperty("rabbitmq_server_ip", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(properties.getProperty("rabbitmq_server_port", "5672")));
        this.connectionFactory.setUsername(properties.getProperty("rabbitmq_server_user", "guest"));
        this.connectionFactory.setPassword(properties.getProperty("rabbitmq_server_password", "guest"));
        this.isStop = false;
    }

    public void subscribe() throws IOException, TimeoutException, InterruptedException {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Constants.SENSOR_ACTION_EXCHANGE, "topic");
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, Constants.SENSOR_ACTION_EXCHANGE, String.format("%l.%l", buildingId, roomId));
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
            System.out.println("The action should be: " + new String(body));
        }
    }
}
