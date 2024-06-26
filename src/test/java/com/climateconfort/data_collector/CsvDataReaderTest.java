package com.climateconfort.data_collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.climateconfort.common.SensorData;

class CsvDataReaderTest {

    private static final long BUILDING_ID = 1L;
    private static final long ROOM_ID = 1L;

    @Mock
    private CSVParser csvParser;

    @Mock
    private Iterator<CSVRecord> recordIterator;

    @Mock
    private CSVRecord csvRecord;

    private CsvDataReader csvDataReader;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(csvParser.iterator()).thenReturn(recordIterator);
        csvDataReader = new CsvDataReader(getProperties(), csvParser);
    }

    @Test
    void readDataTest() throws IOException {
        when(recordIterator.hasNext()).thenReturn(true).thenReturn(false);
        when(recordIterator.next()).thenReturn(csvRecord);
        when(csvRecord.get("Time")).thenReturn("1627884000");
        when(csvRecord.get("Temperature")).thenReturn("22.5");
        when(csvRecord.get("LightLvl")).thenReturn("300");
        when(csvRecord.get("AirQuality")).thenReturn("50");
        when(csvRecord.get("SoundLvL")).thenReturn("30");
        when(csvRecord.get("Humidity")).thenReturn("45");
        when(csvRecord.get("Pressure")).thenReturn("1012");

        Optional<SensorData> result = csvDataReader.read();
        assertTrue(result.isPresent());
        SensorData sensorData = result.get();
        assertEquals(1627884000L, sensorData.getUnixTime());
        assertEquals(22.5f, sensorData.getTemperature());
        assertEquals(30f, sensorData.getSoundLevel());
        assertEquals(45f, sensorData.getHumidity());
        assertEquals(1012f, sensorData.getPressure());
    }

    @Test
    void readNoDataTest() throws IOException {
        when(recordIterator.hasNext()).thenReturn(false);
        Optional<SensorData> result = csvDataReader.read();
        assertFalse(result.isPresent());
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("climateconfort.room_id", String.valueOf(ROOM_ID));
        properties.setProperty("climateconfort.building_id", String.valueOf(BUILDING_ID));
        return properties;
    }
}