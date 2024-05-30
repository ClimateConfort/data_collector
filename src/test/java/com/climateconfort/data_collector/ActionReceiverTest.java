package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.climateconfort.common.Constants;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;

class ActionReceiverTest {
    private static final long roomId = 1;
    private static final long buildingId = 1;
    private static final String QUEUE_NAME = "queue-string";
    private static final String TAG = "TAG";

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Mock
    private DeclareOk declareOk;

    ActionReceiver actionReceiver;

    boolean waiting = false;

    @BeforeEach
    void setUp() throws IOException, TimeoutException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        when(channel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn("queue-string");
        when(channel.basicConsume(QUEUE_NAME, true, mock(ActionReceiver.ActionConsumer.class))).thenReturn(TAG);
        actionReceiver = new ActionReceiver(getProperties());
        setField(actionReceiver, "connectionFactory", connectionFactory);
    }

    @Test
    void subscribeTest() throws InterruptedException, IOException, TimeoutException {
        Thread subscriberThread = new Thread(() -> {
            try {
                actionReceiver.subscribe();
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException("Runtime error");
            }
        });
        subscriberThread.start();

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> subscriberThread.getState().equals(Thread.State.WAITING));

        actionReceiver.stop();
        verify(connectionFactory).newConnection();
        verify(connection).createChannel();
        verify(channel).exchangeDeclare(Constants.SENSOR_ACTION_EXCHANGE, "topic");
        verify(channel).queueBind(QUEUE_NAME, Constants.SENSOR_ACTION_EXCHANGE,
                String.format("%d.%d", buildingId, roomId));
    //    verify(channel).basicCancel(TAG);
    }

    @Test
    void handleDeliveryTest() throws IOException {
        PrintStream originalOut = System.out;
        try {
            String action = "Action";
            ActionReceiver.ActionConsumer consumer = actionReceiver.new ActionConsumer(channel);
            ByteArrayOutputStream outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));
            consumer.handleDelivery("consumerTag", mock(Envelope.class), mock(BasicProperties.class),
                    action.getBytes());
            assertEquals("The action should be: " + action + "\n", outContent.toString());
        } finally {
            System.setOut(originalOut);
        }
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("room_id", String.valueOf(roomId));
        properties.setProperty("building_id", String.valueOf(buildingId));
        return properties;
    }

    private <T, E> void setField(T target, String fieldName, E newValue)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }
}
