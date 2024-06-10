# data_collector
## Description
It publishes sensor data to a RabbitMQ server.

## Installation
First, install the [ClimateConfort/common](https://github.com/ClimateConfort/common) library:

```
$> git clone https://github.com/ClimateConfort/common.git
$> cd common
$> mvn clean install
```

After installing the library, clone this repo and create an executable jar:
```
$> git clone https://github.com/ClimateConfort/data_collector.git
$> cd data_collector
$> mvn clean package -DskipTests
```

The generated jar will be located in `target/data_collector-1.0.0-jar-with-dependencies.jar`:


## Usage
The usage can be seen by executing the following command:

```
$> java -jar target/data_collector-1.0.0-jar-with-dependencies.jar -h
usage: data_collector
 -d,--dataset <arg>      Dataset path
 -h,--help               Show help
 -p,--properties <arg>   Properties file path
 -v,--version            Show version
```

There is a [dataset](climate_data.csv) at the root of this directory, and the [properties file](config/conf.properties) is located under `config/conf.properties`.

For TLS, extra content will be provided to be able to run it. This content will be a Docker image with necessary client-side keys and certificates, that must be specified in the properties file.

Example execution:
```
$> java -jar data_collector-1.0.0-jar-with-dependencies.jar -d climate_data.csv -p config/conf.properties
09:18:59.137 [main] INFO  com.climateconfort.data_collector.Main - Sending data from Building ID: 1 - Room ID: 1
```

**DISCLAIMER: ONLY TESTED IN LINUX**

## Configuration File
These are the configuration file options:

- `climateconfort.room_id`: This is the Room ID where the sensor is located.
- `climateconfort.building_id`: This is the Building ID where the sensor is located.
- `climateconfort.read_interval`: This is the value in miliseconds of the sensor read rate.
- `rabbitmq.server.ip`: The RabbitMQ server IP.
- `rabbitmq.server.port`: The RabbitMQ server port.
- `rabbitmq.server.user`: The RabbitMQ server user name.
- `rabbitmq.server.password`: The RabbitMQ server password.
- `rabbitmq.tls.pkcs12_key_path`: The PKCS12 key path.
- `rabbitmq.tls.pcks12_password`: The PKCS12 key password.
- `rabbitmq.tls.java_key_store_path`: The Java Key Store path.
- `rabbitmq.tls.java_key_store_password`: The Java Key Store password.

For a working example look under `config/conf.properties`