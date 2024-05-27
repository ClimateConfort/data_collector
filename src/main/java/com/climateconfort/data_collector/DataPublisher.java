package com.climateconfort.data_collector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.climateconfort.common.Constants;
import com.climateconfort.common.SensorData;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class DataPublisher {
    
    private final long roomId;
    private final long buildingId;
    private final ConnectionFactory connectionFactory;

    public DataPublisher(Properties properties) throws NumberFormatException {
        this.connectionFactory = new ConnectionFactory();
        this.connectionFactory.setHost(properties.getProperty("rabbitmq_server_ip", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(properties.getProperty("rabbitmq_server_port", "5672")));
        this.connectionFactory.setUsername(properties.getProperty("rabbitmq_server_user", "guest"));
        this.connectionFactory.setPassword(properties.getProperty("rabbitmq_server_password", "guest"));
        this.roomId = Integer.parseInt(properties.getProperty("room_id", "NaN"));
        this.buildingId = Integer.parseInt(properties.getProperty("building_id", "NaN"));
    }

    public void publish(SensorData sensorData) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(Constants.SENSOR_EXCHANGE_NAME, "direct");
            channel.basicPublish(Constants.SENSOR_EXCHANGE_NAME, String.format(buildingId + "-" + roomId), null, prepareToSend(sensorData));
        }
    }

    private byte[] prepareToSend(SensorData sensorData) throws IOException {
        sensorData.setRoomId(roomId);
        sensorData.setBuildingId(buildingId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(sensorData);
        return byteArrayOutputStream.toByteArray();
    }
}
