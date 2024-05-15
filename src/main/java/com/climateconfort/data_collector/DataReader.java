package com.climateconfort.data_collector;

import java.util.Optional;

public interface DataReader {
    Optional<SensorData> read();
}
