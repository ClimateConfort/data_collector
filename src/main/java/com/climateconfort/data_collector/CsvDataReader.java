package com.climateconfort.data_collector;

import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.climateconfort.common.SensorData;

public class CsvDataReader implements DataReader {

    private final long buildingId;
    private final long roomId;
    private final Iterator<CSVRecord> recordIterator;

    // CSVFormat.DEFAULT.builder()
    // .setHeader()
    // .setSkipHeaderRecord(true)
    // .build()
    // .parse(Files.newBufferedReader(datasetPath)
    public CsvDataReader(long buildingId, long roomId, CSVParser parser) {
        this.buildingId = buildingId;
        this.roomId = roomId;
        this.recordIterator = parser.iterator();
    }

    @Override
    public Optional<SensorData> read() throws NumberFormatException {
        if (!recordIterator.hasNext()) {
            return Optional.empty();
        }
        CSVRecord csvRecord = recordIterator.next();
        long unixTime = Long.parseLong(csvRecord.get("Time"));
        float temperature = Float.parseFloat(csvRecord.get("Temperature"));
        float lightLvl = Float.parseFloat(csvRecord.get("LightLvl"));
        float airQuality = Float.parseFloat(csvRecord.get("AirQuality"));
        float soundLvl = Float.parseFloat(csvRecord.get("SoundLvl"));
        float humidity = Float.parseFloat(csvRecord.get("Humidity"));
        float pressure = Float.parseFloat(csvRecord.get("Pressure"));
        return Optional.of(new SensorData(unixTime, buildingId, roomId, -1, temperature, lightLvl, airQuality, soundLvl,
                humidity, pressure));
    }
}
