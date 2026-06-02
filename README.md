# open-rmbt-map

This repository contains the map server for the RTR-NetTest.


## License
*Open-RMBT* is released under the [Apache License, Version 2.0](LICENSE). It was developed
by the [Austrian Regulatory Authority for Broadcasting and Telecommunications (RTR-GmbH)](https://www.rtr.at/).

## Required Tomcat settings
Add the following variables to `context.xml`:

```xml
<Context>
 <!-- Map: database connection -->
 <Parameter name="MAP_DB_USER" value="rmbt" override="false"/>
 <Parameter name="MAP_DB_PASSWORD" value="<change-me>" override="false"/>
 <Parameter name="MAP_DB_HOST" value="db.example.com" override="false"/>
 <Parameter name="MAP_DB_PORT" value="5432" override="false"/>
 <Parameter name="MAP_DB_NAME" value="rmbt" override="false"/>

 <!-- Map: redis connection -->
 <Parameter name="MAP_REDIS_HOST" value="localhost" override="false"/>
 <Parameter name="MAP_REDIS_PORT" value="6379" override="false"/>

</Context>
```

##### Configure Logging - Console or Logstash

The default configuration is to send log to `console`. In current Debian installations systemd
redirects console output to systemd's journal.
Older systems logged to `/var/log/tomcat10/catalina.out`.

The following `context.xml` configuration sends log to Logstash at `elk.example.com`:

```xml
<!-- Logging  -->
<Parameter name="LOG_HOST"     value="elk.example.com"       override="false"/>
<Parameter name="LOG_PORT"     value="5000"                  override="false"/>
<Parameter name="LOGGING_HOST" value="dev"                   override="false"/>
```

Alternatively, one might want to define a custom logging configuration.
First, the alternative configuration file need to be specified in `context.xml`:
```xml
 <Parameter name="LOGGING_CONFIG_FILE_STATISTIC" value="/etc/tomcat10/logback.xml" override="false"/>
```
Again, make sure that the file `/etc/tomcat10/logback.xml` is owned by `tomcat`.

This example logs to both Logstash and console:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %5p [%t] %-40.40logger{39} : %m%n</pattern>
        </encoder>
    </appender>

    <appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>elk.example.com:5000</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"map-service","host":"dev"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="logstash"/>
    </root>

</configuration>
```
