# open-rmbt-map

This repository contains the map server for the RTR-NetTest.

*This repo is in beta testing, please do not use it for production.*

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
