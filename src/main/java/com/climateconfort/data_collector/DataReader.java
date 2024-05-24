package com.climateconfort.data_collector;

import java.util.Optional;

import com.climateconfort.common.SensorData;

public interface DataReader {
    Optional<SensorData> read();
}
