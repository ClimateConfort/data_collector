package com.climateconfort.data_collector;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.climateconfort.common.SensorData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MainTest {

    private static final int roomId = 1;
    private static final int buildingId = 1;

    @TempDir
    Path tempDir;

    @Mock
    private CsvDataReader csvDataReader;

    @Mock
    private DataPublisher dataPublisher;

    @Mock
    private ActionReceiver actionReceiver;

    private Main main;

    private Path datasetPath;
    private Path propertiesPath;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        // Create temporary test properties file
        propertiesPath = tempDir.resolve("test.properties");
        Properties properties = getProperties();
        try (var writer = Files.newBufferedWriter(propertiesPath)) {
            properties.store(writer, "Test properties");
        }

        // Create temporary dataset CSV file
        datasetPath = tempDir.resolve("dataset.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(datasetPath)) {
            writer.write("Time,Temperature,LightLvl,AirQuality,SoundLvl,Humidity,Pressure\n");
            writer.write("100,448,203.16,311.05,686.11,798.65,575.02\n");
        }

        main = new Main(datasetPath, propertiesPath);
        setField(main, "csvDataReader", csvDataReader);
        setField(main, "dataPublisher", dataPublisher);
        setField(main, "actionReceiver", actionReceiver);
    }

    @Test
    void testConstructor() throws IOException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        assertNotNull(setFieldPublic(Main.class, "csvDataReader").get(main));
        assertNotNull(setFieldPublic(Main.class, "dataPublisher").get(main));
        assertNotNull(setFieldPublic(Main.class, "actionReceiver").get(main));
    }

    @Test
    void testSetupCorrect() {
        Scanner scanner = mock(Scanner.class);
        main.setup(scanner);
        verify(actionReceiver, times(0)).stop();
    }

    @Test
    void testSetupIncorrect() throws Exception {
        Scanner scanner = mock(Scanner.class);
        doThrow(new IOException()).when(actionReceiver).subscribe();
        doNothing().when(actionReceiver).stop();
        assertDoesNotThrow(() -> main.setup(scanner));
    }

    @Test
    void testStart() throws IOException, TimeoutException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        SensorData sensorData = new SensorData(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        when(csvDataReader.read()).thenReturn(Optional.of(sensorData));
        doAnswer(invocation -> {
            setField(main, "isStop", true);
            return null;
        }).when(dataPublisher).publish(sensorData);
        setField(main, "isStop", false);
        main.start();
        verify(dataPublisher, atLeastOnce()).publish(any(SensorData.class));
    }

    @Test
    void testStartException() throws IOException, TimeoutException {
        SensorData sensorData = new SensorData(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
        when(csvDataReader.read()).thenReturn(Optional.of(sensorData));
        doThrow(new IOException()).when(dataPublisher).publish(sensorData);
        assertThrows(RuntimeException.class, () -> main.start());
    }

    @Test
    void testGenerateArgumentOptions() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method generateArgumentOptionsMethod = setMethodPublic(Main.class, "generateArgumentOptions");
        assertNotNull(generateArgumentOptionsMethod.invoke(null));
    }

    @Test
    void testParseArguments() throws Exception {
        String[] args = { "-d", datasetPath.toString(), "-p", propertiesPath.toString() };

        Method parseArgumentsMethod = setMethodPublic(Main.class, "parseArguments", String[].class);

        CommandLine cmd = (CommandLine) parseArgumentsMethod.invoke(null, (Object) args);

        assertTrue(cmd.hasOption("d"));
        assertTrue(cmd.hasOption("p"));
        assertEquals(datasetPath.toString(), cmd.getOptionValue("d"));
        assertEquals(propertiesPath.toString(), cmd.getOptionValue("p"));
    }

    @Test
    void testMain() {
        String[] args1 = { "-h" };
        String[] args2 = { "-v" };
        assertDoesNotThrow(() -> Main.main(args1));
        assertDoesNotThrow(() -> Main.main(args2));
    }

    private <T, E> void setField(T target, String fieldName, E newValue)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }

    private <T> Field setFieldPublic(Class<T> target, String fieldName)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = target.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private <T> Method setMethodPublic(Class<T> target, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        Method method = target.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("room_id", String.valueOf(roomId));
        properties.setProperty("building_id", String.valueOf(buildingId));
        return properties;
    }
}
