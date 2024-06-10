package com.climateconfort.data_collector;

import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.climateconfort.common.SensorData;

public class CsvDataReader implements DataReader {
    private static final Logger LOGGER = LogManager.getLogger(CsvDataReader.class);
    private final long buildingId;
    private final long roomId;
    private final Iterator<CSVRecord> recordIterator;
    private final int readInterval;

    public CsvDataReader(Properties properties, CSVParser parser) {
        this.roomId = Integer.parseInt(properties.getProperty("climateconfort.room_id", "NaN"));
        this.buildingId = Integer.parseInt(properties.getProperty("climateconfort.building_id", "NaN"));
        this.recordIterator = parser.iterator();
        this.readInterval = Integer.parseInt(properties.getProperty("climateconfort.read_interval", "0"));
    }

    @Override
    public Optional<SensorData> read() throws NumberFormatException {
        if (!recordIterator.hasNext()) {
            return Optional.empty();
        }
        CSVRecord csvRecord = recordIterator.next();
        long unixTime = Long.parseLong(csvRecord.get("Time"));
        float temperature = Float.parseFloat(csvRecord.get("Temperature"));
        float lightLvl = 0;
        float airQuality = 0;
        float soundLvl = Float.parseFloat(csvRecord.get("SoundLvL"));
        float humidity = Float.parseFloat(csvRecord.get("Humidity"));
        float pressure = Float.parseFloat(csvRecord.get("Pressure"));
        try {
            Thread.sleep(readInterval);
        } catch (InterruptedException e) {
            LOGGER.error("Not valid interval number", e);
            Thread.currentThread().interrupt();
        }
        return Optional.of(new SensorData(unixTime, buildingId, roomId, -1, temperature, lightLvl, airQuality, soundLvl,
                humidity, pressure));
    }
}
