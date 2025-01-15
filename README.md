# open-rmbt-map

This repository contains the map server for the RTR-NetTest.

*This repo is in beta testing, please do not use it for production.*

## License
*Open-RMBT* is released under the [Apache License, Version 2.0](LICENSE). It was developed
by the [Austrian Regulatory Authority for Broadcasting and Telecommunications (RTR-GmbH)](https://www.rtr.at/).

## Required Tomcat settings
Add the following variables to `context.xml` within the `<Resource>` block:

     <!-- Map: database connection -->
     <Parameter name="MAP_DB_USER" value="rmbt" override="false"/>
     <Parameter name="MAP_DB_PASSWORD" value="putyourpasswordhere" override="false"/>
     <Parameter name="MAP_DB_HOST" value="db.example.com" override="false"/>
     <Parameter name="MAP_DB_PORT" value="5432" override="false"/>
     <Parameter name="MAP_DB_NAME" value="rmbt" override="false"/>

     <!-- Map: redis connection -->
     <Parameter name="MAP_REDIS_HOST" value="localhost" override="false"/>
     <Parameter name="MAP_REDIS_PORT" value="6379" override="false"/>

Replace the values according to your setup, e.g. if the database is on the same machine, host will be 127.0.0.1.
