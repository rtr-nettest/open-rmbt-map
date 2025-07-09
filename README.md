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
 <Parameter name="MAP_DB_PASSWORD" value="putyourpasswordhere" override="false"/>
 <Parameter name="MAP_DB_HOST" value="db.example.com" override="false"/>
 <Parameter name="MAP_DB_PORT" value="5432" override="false"/>
 <Parameter name="MAP_DB_NAME" value="rmbt" override="false"/>

 <!-- Map: redis connection -->
 <Parameter name="MAP_REDIS_HOST" value="localhost" override="false"/>
 <Parameter name="MAP_REDIS_PORT" value="6379" override="false"/>

 <!-- Statistic - logback configuration -->
 <Parameter name="LOGGING_CONFIG_FILE_MAP" value="/etc/tomcat9/logback-map.xml" override="false"/>
</Context>
```

Put the following into `logback-map.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>

<configuration scan="true">

     <include resource="org/springframework/boot/logging/logback/defaults.xml" />

 <!-- with console log: include resource="org/springframework/boot/logging/logback/base.xml"/  -->

    <appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <param name="Encoding" value="UTF-8"/>
<!-- define remote logging  host here -->
        <remoteHost>elk-host.example.com</remoteHost>
        <port>5000</port>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
<!-- add custom fields to identify server and host -->
            <customFields>{"app_name":"map-service", "host":"my-map-server"}</customFields>
        </encoder>
    </appender>
<!-- log levels: TRACE, DEBUG, INFO, WARN, ERROR -->
    <root level="INFO">
        <appender-ref ref="logstash"/>
    </root>
</configuration>
```
Replace the values according to your setup, e.g. if the database is on the same machine, host will be 127.0.0.1.
