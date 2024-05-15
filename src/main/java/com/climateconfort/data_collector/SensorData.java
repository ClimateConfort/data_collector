package com.climateconfort.data_collector;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SensorData {
    private long unixTime;
    private float temperature;
    private float lightLevel;
    private float airQuality;
    private float soundLevel;
    private float humidity;
    private float pressure;
}
