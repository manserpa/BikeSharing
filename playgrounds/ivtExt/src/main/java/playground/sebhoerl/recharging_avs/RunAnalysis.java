package playground.sebhoerl.recharging_avs;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import playground.sebhoerl.ant.AnalysisRunner;
import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunAnalysis {
    public static void main(String[] args) throws InterruptedException, IOException {
        Config config = ConfigUtils.loadConfig(args[0]);

        BinCalculator binCalculator = BinCalculator.createByInterval(0, 108000, 300);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(config.network().getInputFile());

        ExecutorService executor = Executors.newFixedThreadPool(8);

        final String OUTPUT_PATH = args[2];
        final String RELEVANT_OPERATOR = null;

        AnalysisRunner runner = new AnalysisRunner(binCalculator, network,
                args[1],
                args[2]);
        runner.run();

        DataFrame dataFrame = runner.dataFrame;

        new File(OUTPUT_PATH).mkdirs();

        String operatorPrefix = RELEVANT_OPERATOR == null ? "" : RELEVANT_OPERATOR + "_";

        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "departures.csv"), dataFrame, dataFrame.departureCount);
        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "arrivals.csv"), dataFrame, dataFrame.arrivalCount);
        writeCountsByModeAndBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "travelling.csv"), dataFrame, dataFrame.travellerCount);
        writeCountsByState(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "states.csv"), dataFrame, dataFrame.avStateCount);

        writeSingleCountByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "waiting_customers.csv"), "WAITING", dataFrame, dataFrame.waitingCount);
        writeSingleCountByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "idle_avs.csv"), "IDLE", dataFrame, dataFrame.idleAVs);

        writeTimesByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_waiting_times.csv"), dataFrame, dataFrame.waitingTimes);
        writeTimesByBin(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_travel_times.csv"), dataFrame, dataFrame.travelTimes);


        writeDistances(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "av_distances.txt"), dataFrame);
        writeInfo(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "info.txt"), dataFrame);

        writeOccupancy(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "occupancy.csv"), dataFrame);

        writeLegChains(IOUtils.getBufferedWriter(OUTPUT_PATH + "/" + operatorPrefix + "chains.csv"), dataFrame);
    }

    private static <T extends Number> void writeCountsByModeAndBin(BufferedWriter writer, DataFrame dataFrame, Map<String, List<T>> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.addAll(dataFrame.modes);
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (String mode : dataFrame.modes) {
                elements.add(data.get(mode).get(i).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeCountsByState(BufferedWriter writer, DataFrame dataFrame, Map<String, List<T>> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.addAll(dataFrame.avStates);
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (String mode : dataFrame.avStates) {
                elements.add(data.get(mode).get(i).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeSingleCountByBin(BufferedWriter writer, String topic, DataFrame dataFrame, List<T> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.add(topic);
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));
            elements.add(data.get(i).toString());

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeTimesByBin(BufferedWriter writer, DataFrame dataFrame, List<List<T>> data) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        elements.add("TIMES...");
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (int j = 0; j < data.get(i).size(); j++) {
                elements.add(data.get(i).get(j).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }

        writer.close();
    }

    private static <T extends Number> void writeDistances(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> elements = new LinkedList<>();

        for (Double distance : dataFrame.avDistances) {
            elements.add(distance.toString());
        }

        writer.write(String.join(";", elements));
        writer.close();
    }

    private static void writeOccupancy(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> elements = new LinkedList<>();

        elements.add("BIN");
        for (int pax = 0; pax < 5; pax++) {
            elements.add(String.valueOf(pax) + "PAX");
        }
        writer.write(String.join(";", elements) + "\n");
        elements.clear();

        for (int i = 0; i < dataFrame.binCalculator.getBins(); i++) {
            elements.add(String.valueOf(i));

            for (int pax = 0; pax < 5; pax++) {
                elements.add(dataFrame.occupancy.get(pax).get(i).toString());
            }

            writer.write(String.join(";", elements) + "\n");
            elements.clear();
        }
        writer.close();

    }

    private static void writeInfo(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        writer.write(String.format("vehicleDistance = %f\n", dataFrame.vehicleDistance));
        writer.write(String.format("passengerDistance = %f\n", dataFrame.passengerDistance));
        writer.write(String.format("avVehicleDistance = %f\n", dataFrame.avVehicleDistance));
        writer.write(String.format("avPassengerDistance = %f\n", dataFrame.avPassengerDistance));
        writer.write(String.format("avEmptyRideDistance = %f\n", dataFrame.avEmptyRideDistance));
        writer.close();
    }

    private static void writeLegChains(BufferedWriter writer, DataFrame dataFrame) throws IOException {
        List<String> chainKeys = new LinkedList<>();
        chainKeys.addAll(dataFrame.chainCounts.keySet());
        Collections.sort(chainKeys);

        for (String chain : chainKeys) {
            writer.write(String.format("%s;%d\n", chain, dataFrame.chainCounts.get(chain)));
        }

        writer.close();
    }
}
