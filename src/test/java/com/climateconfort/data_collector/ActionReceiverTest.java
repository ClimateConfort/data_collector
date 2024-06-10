package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
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
            IllegalArgumentException, IllegalAccessException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        MockitoAnnotations.openMocks(this);
        when(connectionFactory.newConnection()).thenReturn(connection);
        doNothing().when(connectionFactory).useSslProtocol(any(SSLContext.class));
        when(connection.createChannel()).thenReturn(channel);
        when(channel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn(QUEUE_NAME);
        try (MockedConstruction<TlsManager> mockedConstruction = mockConstruction(TlsManager.class, (mock, context) -> when(mock.getSslContext()).thenReturn(mock(SSLContext.class)))) {
            actionReceiver = new ActionReceiver(getProperties());
        }
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

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> subscriberThread.getState().equals(Thread.State.WAITING));

        actionReceiver.stop();
        verify(connectionFactory).newConnection();
        verify(connection).createChannel();
        verify(channel).exchangeDeclare(Constants.SENSOR_ACTION_EXCHANGE, "direct");
        verify(channel).queueBind(QUEUE_NAME, Constants.SENSOR_ACTION_EXCHANGE,
                String.format("%d.%d", buildingId, roomId));
    }

    @Test
    void handleDeliveryTest() throws IOException {
        String action = "Action";
        ActionReceiver.ActionConsumer consumer = actionReceiver.new ActionConsumer(channel);
        assertDoesNotThrow(
                () -> consumer.handleDelivery("consumerTag", mock(Envelope.class), mock(BasicProperties.class),
                        action.getBytes()));

    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("climateconfort.room_id", String.valueOf(roomId));
        properties.setProperty("climateconfort.building_id", String.valueOf(buildingId));
        return properties;
    }

    private <T, E> void setField(T target, String fieldName, E newValue)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }
}
