package com.climateconfort.data_collector;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

public class Main {

    private static final String PROGRAM_NAME = "data_collector";
    private static final String PROGRAM_VERSION = "1.0.0";

    private final CsvDataReader csvDataReader;
    private final DataPublisher dataPublisher;
    private final ActionReceiver actionReceiver;
    private boolean isStop;

    public Main(Path datasetPath, Path propertiesPath) throws IOException {
        Properties properties = new Properties();
        try (BufferedReader bufferedReader = Files.newBufferedReader(propertiesPath)) {
            properties.load(bufferedReader);
        }
        CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(Files.newBufferedReader(datasetPath));
        this.csvDataReader = new CsvDataReader(properties, parser);
        this.dataPublisher = new DataPublisher(properties);
        this.actionReceiver = new ActionReceiver(properties);
        this.isStop = false;
    }

    public void setup(Scanner scanner) {
        Thread subscriberThread = new Thread(() -> {
            try {
                actionReceiver.subscribe();
            } catch (IOException | TimeoutException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        Thread waitThread = new Thread(() -> {
            scanner.nextLine();
            actionReceiver.stop();
            isStop = false;
        });
        subscriberThread.start();
        waitThread.start();
    }

    public void start() {
        while (!isStop) {
            csvDataReader
                    .read()
                    .ifPresentOrElse(sensorData -> {
                        try {
                            dataPublisher.publish(sensorData);
                        } catch (IOException | TimeoutException e) {
                            throw new IllegalStateException(e);
                        }
                    }, () -> System.out.println("No data..."));
        }
    }

    private static Options generateArgumentOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "Show help");
        options.addOption("v", "version", false, "Show version");
        options.addOption("d", "dataset", true, "Dataset path");
        options.addOption("p", "properties", true, "Properties file path");
        return options;
    }

    private static CommandLine parseArguments(String[] args) throws ParseException {
        Options argOptions = generateArgumentOptions();
        CommandLineParser parser = new DefaultParser();
        return parser.parse(argOptions, args);
    }

    public static void main(String[] args) {

        String propertiesPath = "";
        String datasetPath = "";

        try {

            CommandLine cmd = parseArguments(args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(PROGRAM_NAME, generateArgumentOptions());
                return;
            }

            if (cmd.hasOption("v")) {
                System.out.println("Version: " + PROGRAM_VERSION);
                return;
            }

            if (cmd.hasOption("d")) {
                datasetPath = cmd.getOptionValue("d");
            }

            if (cmd.hasOption("p")) {
                propertiesPath = cmd.getOptionValue("p");
            }

            if (!cmd.hasOption("d") && !cmd.hasOption("p")) {
                System.out.println("No valid options provided. Use -h for help.");
                return;
            }

            Main main = new Main(Paths.get(datasetPath), Paths.get(propertiesPath));
            main.setup(new Scanner(System.in));
            main.start();
        } catch (ParseException e) {
            System.out.println("Error parsing command-line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(PROGRAM_NAME, generateArgumentOptions());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}