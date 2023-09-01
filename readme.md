# NYC Taxi Trips Analysis for Hotspot Detection 

This project is implemented for Distributed Systems Design Course. 
The goal of this project is to identify ”hotspots,” or areas of the city with a high density of
taxi pickups, utilising the taxi trip data from New York City. Finding hotspots can assist
in efficiently changing the dependent variables in the business environment, such as limiting
the rate fees according to the hotspot density and time of day, because using a taxi to get
from one location to another in the New City is very demanding. A model is created using
the corpus supplied by the NYC Taxi and Limousine Commission in order to acquire this
(TLC). In order to manage the data and the dispersed nature of the system, it uses Apache
Kafka and Flink.

The data set has first been modified to mimic data being live-fed to the system, guaranteeing
a fresh data stream each time. After that, a topic on Apache Kafka is fed with the input
of taxi trip data, and a pipeline of distributed nodes employing Kafka connectors to Apache
Flink reads and processes the topic. To facilitate simultaneous reading and processing, there
are many Flink nodes. The output from those nodes is eventually broadcast in Kafka on a
different topic. Based on the feedback received on the subject, hotspot spots that represent
the density of taxi drivers are plotted

## Compiling the code

Set up the project **nyc_taxi_trips_analysis** using maven to download the dependencies and plugins required for generating a fat jar. Compile the code using the following command or by using the IDE's integrated maven interface. This should generate a fat jar file in target directory.

```maven
mvn package
```

For the python data ingestion and visualization notebooks, set up a python environment by installing the dependencies using the following command:

```bash
pip install requirements.txt
```

## Setting up the cluster

1. Create a virtual machine or a docker container.

2. Create an ssh cluster by generating ssh keys and exchanging public keys with all the nodes using following two commands (note: user has to be the same for all nodes in the cluster, if required, set up a user for flink nodes)

   ```bash
   ssh-keygen -t rsa -b 4096
   ssh-copy-id -i ~/.ssh/id_rsa.pub <USER>@<HOST>
   ```

3. Create alias bindings for the IP addresses in the /etc/hosts files on all the nodes as follows:

   ```
   # For DSD
   <IP-for-VM>		vm
   <IP-for-host>	host
   ```

4. Copy the flink and kafka libraries under the 'libraries' directory in the repository.

5. Create the flink master and worker nodes by modifying the config files in conf directory.

   1. In the flink-conf.yaml file, update the following line:

      ```
      taskmanager.numberOfTaskSlots: 4
      ```

   2. In the masters file, add the following:

      ```
      host:8081
      ```

   3. In the workers file, add the following:

      ```
      host
      vm
      ```

## Running the project

1. Ensure that [nyc_taxi.csv dataset](http://s3.amazonaws.com/datashader-data/nyc_taxi.zip) is added under the "datasets" directory, from the link in the project documentation and "libraries" directory has [flink](https://flink.apache.org/downloads.html) and [kafka](https://kafka.apache.org/downloads) directories.
2. Update the root directory path in the env.sh and source it.
3. Start zookeper and kafka servers with the following commands.
    ```bash
    $KAFKA_BIN/zookeeper-server-start.sh $KAFKA_CONFIG/zookeeper.properties
    $KAFKA_BIN/kafka-server-start.sh $KAFKA_CONFIG/server.properties
    ```
4. Start the flink cluster.
    Ensure it is running with the second command and open the web interface at http://localhost:8081/
    ```bash
    $FLINK_BIN/start-cluster.sh
    ps aux | grep flink
    ```
5. Run the python scripts from the IngestNYCTrips jupyter notebook.
6. Run the python scripts from the VisualizeGrid jupyter notebook.
7. Submit the flink job to the flink server using the following command. Ensure the job is running on the web interface.
    ```
    flink run -c nyc_taxi_trips_analysis.NYCTaxiTripsAnalysisHotSpotDetectionJob nyc_taxi_trips_analysis_java-0.1.jar
    ```