package com.climateconfort.data_collector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class CsvDataReader implements DataReader {

    private final Iterator<CSVRecord> recordIterator;

    // CSVFormat.DEFAULT.builder()
    //             .setHeader()
    //             .setSkipHeaderRecord(true)
    //             .build()
    //             .parse(Files.newBufferedReader(datasetPath)
    public CsvDataReader(CSVParser parser) {
        this.recordIterator = parser.iterator();
    }

    @Override
    public Optional<SensorData> read() {
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
        return Optional.of(new SensorData(unixTime, temperature, lightLvl, airQuality, soundLvl, humidity, pressure));
    }

}
