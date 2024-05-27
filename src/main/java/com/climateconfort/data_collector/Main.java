package com.climateconfort.data_collector;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public class Main {
    public static void main(String[] args) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(Files.newBufferedReader(Path.of("proba.csv")));
        CsvDataReader dataReader = new CsvDataReader(1, 1, parser);
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/main/resources/application.properties"));
        DataPublisher dataPublisher = new DataPublisher(properties);
        while (true) {
            dataReader.read().ifPresentOrElse(sensorData -> {
                try {
                    dataPublisher.publish(sensorData);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }, () -> System.out.println("No more data....."));
        }
    }
}