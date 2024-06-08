package com.climateconfort.data_collector;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.climateconfort.common.Constants;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class HearbeatReceiver {

    private boolean isStop;
    private final AtomicBoolean isAlive;
    private final ConnectionFactory connectionFactory;
    private final Semaphore semaphore;
    
    public HearbeatReceiver(Properties properties) {
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(properties.getProperty("rabbitmq.server.ip", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(properties.getProperty("rabbitmq.server.port", "5672")));
        this.connectionFactory.setUsername(properties.getProperty("rabbitmq.server.user", "guest"));
        this.connectionFactory.setPassword(properties.getProperty("rabbitmq.server.password", "guest"));
        this.isStop = false;
        this.isAlive = new AtomicBoolean(true);
        this.semaphore = new Semaphore(1);
    }

    public void subscribe() throws IOException, TimeoutException, InterruptedException {
        try (Connection connection = connectionFactory.newConnection();
                Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(Constants.HEARTBEAT_EXCHANGE, "fanout");
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, Constants.HEARTBEAT_EXCHANGE, null);
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

    public void setToFalse() {
        isAlive.set(false);
    }

    public void waitForHeartbeat() throws InterruptedException {
        semaphore.acquire();
    }

    public synchronized void stop() {
        isStop = true;
        this.notifyAll();
    }

    class ActionConsumer extends DefaultConsumer {
    
        public ActionConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
                throws IOException {
            System.out.println("Server's heartbeat reached");

            semaphore.release();
        }
    }

}
