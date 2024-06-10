package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import com.climateconfort.common.Constants;
import com.climateconfort.common.SensorData;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

class DataPublisherTest {

    private static final int roomId = 1;
    private static final int buildingId = 1;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Channel channel;

    @Captor
    private ArgumentCaptor<byte[]> byteArrayCaptor;

    private DataPublisher dataPublisher;
    private SensorData sensorData;

    @BeforeEach
    void setUp() throws IOException, TimeoutException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        MockitoAnnotations.openMocks(this);
        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        sensorData = new SensorData(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        try (MockedConstruction<TlsManager> mockedConstruction = mockConstruction(TlsManager.class, (mock, context) -> when(mock.getSslContext()).thenReturn(mock(SSLContext.class)))) {
            dataPublisher = new DataPublisher(getProperties());
        }
        setField(dataPublisher, "connectionFactory", connectionFactory);
    }

    private <T, E> void setField(T target, String fieldName, E newValue)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("climateconfort.room_id", String.valueOf(roomId));
        properties.setProperty("climateconfort.building_id", String.valueOf(buildingId));
        return properties;
    }

    @Test
    void testPublish() throws IOException, TimeoutException, ClassNotFoundException {
        dataPublisher.publish(sensorData);

        verify(connectionFactory).newConnection();
        verify(connection).createChannel();
        verify(channel).exchangeDeclare(Constants.SENSOR_EXCHANGE_NAME, "direct");
        verify(channel).basicPublish(eq(Constants.SENSOR_EXCHANGE_NAME), eq(String.format(buildingId + "-" + roomId)),
                isNull(),
                byteArrayCaptor.capture());

        byte[] capturedBytes = byteArrayCaptor.getValue();
        SensorData capturedSensorData = deserialize(capturedBytes);

        assertEquals(1, capturedSensorData.getRoomId());
        assertEquals(1, capturedSensorData.getBuildingId());
    }

    private SensorData deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length)) {
            baos.write(bytes);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                return (SensorData) ois.readObject();
            }
        }
    }
}