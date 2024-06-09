package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.climateconfort.common.Constants;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;

class HeartBeatTest {
    private static final String QUEUE_NAME = "queue-string";

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Mock
    private DeclareOk declareOk;

    @Mock
    private Semaphore semaphore;

    HearbeatReceiver hearbeatReceiver;

    @BeforeEach
    void setUp() throws IOException, TimeoutException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        MockitoAnnotations.openMocks(this);
        try (MockedConstruction<TlsManager> mockedConstruction = mockConstruction(TlsManager.class, (mock, context) -> when(mock.getSslContext()).thenReturn(mock(SSLContext.class)))) {
            hearbeatReceiver = new HearbeatReceiver(new Properties());
        }
        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        when(channel.queueDeclare()).thenReturn(declareOk);
        when(declareOk.getQueue()).thenReturn(QUEUE_NAME);
        setField(hearbeatReceiver, "connectionFactory", connectionFactory);
        setField(hearbeatReceiver, "semaphore", semaphore);
    }

    @Test
    void subscribeTest() throws InterruptedException, IOException, TimeoutException {
        Thread subscriberThread = new Thread(() -> {
            try {
                hearbeatReceiver.subscribe();
            } catch (IOException | TimeoutException | InterruptedException e) {
                throw new RuntimeException("Runtime error");
            }
        });
        subscriberThread.start();

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .until(() -> subscriberThread.getState().equals(Thread.State.WAITING));

        hearbeatReceiver.stop();
        verify(connectionFactory).newConnection();
        verify(connection).createChannel();
        verify(channel).exchangeDeclare(Constants.HEARTBEAT_EXCHANGE, "fanout");
        verify(channel).queueBind(QUEUE_NAME, Constants.HEARTBEAT_EXCHANGE, "");
    }

    @Test
    void waitForHeartbeatTest() throws InterruptedException {
        doNothing().when(semaphore).acquire();
        hearbeatReceiver.waitForHeartbeat();
        verify(semaphore).acquire();
    }

    @Test
    void handleDeliveryTest() throws IOException {
        String action = "Action";
        HearbeatReceiver.HeartbeatConsumer consumer = hearbeatReceiver.new HeartbeatConsumer(channel);
        assertDoesNotThrow(
                () -> consumer.handleDelivery("consumerTag", mock(Envelope.class), mock(BasicProperties.class),
                        action.getBytes()));
    }

    private <T, E> void setField(T target, String fieldName, E newValue)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }
}
