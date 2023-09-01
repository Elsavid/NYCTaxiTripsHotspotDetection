package nyc_taxi_trips_analysis;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.util.Random;


public class NYCTaxiTripsAnalysisHotSpotDetectionJob {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ParameterTool params = ParameterTool.fromArgs(args);
        env.getConfig().setGlobalJobParameters(params);
        env.setParallelism(8);
//        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);


        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("host:9092")
                .setTopics("nyc_trips")
                .setGroupId("my-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("host:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("hotspots")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();


        SingleOutputStreamOperator<String> kafkaInput = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "NYC Trips (Kakfa Input)")
                .name("Kakfa Source");

        SingleOutputStreamOperator<Trip> trips = kafkaInput.map(new DeserializeToTrip()).name("Trips");
        SingleOutputStreamOperator<Tuple3<Long, Location, Integer>> timestampedGridLocationIndices = trips.map(new ExtractGridLocation()).name("Timestamped Grid Locations");

        KeyedStream<Tuple3<Long, Location, Integer>, Location> keyedGridLocations = timestampedGridLocationIndices.keyBy(new LocationKeySelector());

        WindowedStream<Tuple3<Long, Location, Integer>, Location, TimeWindow> windowedGridStream = keyedGridLocations.window(TumblingProcessingTimeWindows.of(Time.seconds(20)));
        SingleOutputStreamOperator<Tuple3<Long, Location, Integer>> locationCounts = windowedGridStream.reduce(new ReduceFunction<Tuple3<Long, Location, Integer>>() {
            @Override
            public Tuple3<Long, Location, Integer> reduce(Tuple3<Long, Location, Integer> current, Tuple3<Long, Location, Integer> previous) throws Exception {
                return new Tuple3<>(current.f0, current.f1, current.f2 + previous.f2);
            }
        }).name("Grid Counts");
        SingleOutputStreamOperator<String> serializedLocationCounts = locationCounts.map(new MapFunction<Tuple3<Long, Location, Integer>, String>() {
            @Override
            public String map(Tuple3<Long, Location, Integer> value) throws Exception {
                return value.f1 + ":" + value.f2;
            }
        }).name("Serialized Location Counts");

//        serializedLocationCounts.writeAsText("file:///home/shubham/test.out", FileSystem.WriteMode.OVERWRITE);
        serializedLocationCounts.sinkTo(sink).name("Kafka Sink");
        env.execute("NYC Taxi Trips Analysis");
    }


    public static class DeserializeToTrip implements MapFunction<String, Trip> {
        @Override
        public Trip map(String value) throws Exception {
            String[] ts_lat_long = value.split(",");
            long ts = Long.parseLong(ts_lat_long[0]);
            Location location = new Location(Double.parseDouble(ts_lat_long[2]), Double.parseDouble(ts_lat_long[1]));
            return new Trip(ts, location);
        }
    }

    public static class LocationKeySelector implements KeySelector<Tuple3<Long, Location, Integer>, Location> {
        @Override
        public Location getKey(Tuple3<Long, Location, Integer> value) throws Exception {
            return value.f1;
        }
    }

    public static class ExtractGridLocation implements MapFunction<Trip, Tuple3<Long, Location, Integer>> {
        @Override
        public Tuple3<Long, Location, Integer> map(Trip trip) throws Exception {
            // Do a MxN grid,for this use case, a 4x4
            int M = 8;
            int N = 8;
            double LONGITUDE_MIN = -82.4320353716;
            double LONGITUDE_MAX = -82.2651053948;
            double LATITUDE_MIN = 49.6819204221;
            double LATITUDE_MAX = 49.8288620438;

            double long_diff = Math.abs(LONGITUDE_MAX - LONGITUDE_MIN) / M;
            double lat_diff = Math.abs(LATITUDE_MAX - LATITUDE_MIN) / N;

            double LONG = 0, LAT = 0;
            while (LONG < M) {
                if (trip.getLocation().getPos_x() >= (LONGITUDE_MIN + (LONG * long_diff))
                        && trip.getLocation().getPos_x() < (LONGITUDE_MIN + ((LONG + 1) * long_diff)))
                    break;
                else
                    LONG += 1;
            }
            while (LAT < N) {
                if (trip.getLocation().getPos_y() >= (LATITUDE_MIN + (LAT * lat_diff))
                        && trip.getLocation().getPos_y() < (LATITUDE_MIN + ((LAT + 1) * lat_diff)))
                    break;
                else LAT += 1;
            }


            return new Tuple3<Long, Location, Integer>(trip.getTs(), new Location(LONGITUDE_MIN + long_diff * LONG, LATITUDE_MIN + lat_diff * LAT), 1);

        }
    }

    public static int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

}
