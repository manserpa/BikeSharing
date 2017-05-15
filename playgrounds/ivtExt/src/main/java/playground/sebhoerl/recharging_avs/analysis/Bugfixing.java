package playground.sebhoerl.recharging_avs.analysis;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.vehicles.Vehicle;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.*;
import java.util.*;

public class Bugfixing {
    static private String time2String(double time) {
        double hours = Math.floor(time / 3600.0);
        time -= hours * 3600.0;

        double minutes = Math.floor(time / 60.0);
        time -= minutes * 60.0;

        double seconds = time;

        return String.format("%02d:%02d:%02d", (int)hours, (int)minutes, (int)seconds);
    }

    static public void main(String[] args) throws IOException {
        BinCalculator binCalculator = BinCalculator.createByInterval(6.0 * 3600.0, 22.0 * 3600.0, 900.0);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);

        DistanceHandler distanceHandler = new DistanceHandler(network, binCalculator);
        RechargingHandler rechargingHandler = new RechargingHandler(binCalculator);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(distanceHandler);
        eventsManager.addHandler(rechargingHandler);

        new MatsimEventsReader(eventsManager).readFile(args[1]);

        OutputStreamWriter writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(args[2].replace(".csv", ".waitingTime.csv"))));

        writer.write("BIN;MEAN;MEDIAN;Q90\n");

        for (int i = 0; i < binCalculator.getBins(); i++) {
            double[] binDistances = distanceHandler.pickupDistances.get(i).stream().mapToDouble(Double::doubleValue).toArray();
            DescriptiveStatistics statistics = new DescriptiveStatistics(binDistances);

            writer.write(String.format("%s;%f;%f;%f\n", time2String(binCalculator.getStart(i)), statistics.getMean(), statistics.getPercentile(50), statistics.getPercentile(90)));
        }

        writer.flush();
        writer.close();

        writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(args[2].replace(".csv", ".recharging.csv"))));

        writer.write("BIN;RECHARGING\n");

        for (int i = 0; i < binCalculator.getBins(); i++) {
            writer.write(String.format("%s;%f\n", time2String(binCalculator.getStart(i)), rechargingHandler.recharging.get(i)));
        }

        writer.flush();
        writer.close();
    }

    static public class RechargingHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
        final private BinCalculator binCalculator;
        final public List<Double> recharging;

        final private Map<Id<Person>, Double> startTimes = new HashMap<>();

        public RechargingHandler(BinCalculator binCalculator) {
            this.binCalculator = binCalculator;
            this.recharging = new ArrayList<>(Collections.nCopies(binCalculator.getBins(), 0.0));
        }


        @Override
        public void handleEvent(ActivityEndEvent event) {
            if (event.getActType().equals("AVRecharge")) {
                Double start = startTimes.remove(event.getPersonId());

                for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(start, event.getTime())) {
                    recharging.set(entry.getIndex(), recharging.get(entry.getIndex()) + entry.getWeight());
                }
            }
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (event.getActType().equals("AVRecharge")) {
                startTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void reset(int iteration) {

        }
    }

    static public class DistanceHandler implements ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
        final private Network network;
        final private BinCalculator binCalculator;

        final private Map<Id<Person>, ActivityEndEvent> stayEndEvents = new HashMap<>();
        final private Map<Id<Person>, Double> distances = new HashMap<>();
        final private Map<Id<Vehicle>, LinkEnterEvent> enterEvents = new HashMap<>();

        final public List<List<Double>> pickupDistances;

        private Id<Person> id(Id<Vehicle> vehicleId) {
            return Id.createPersonId(vehicleId);
        }

        public DistanceHandler(Network network, BinCalculator binCalculator) {
            this.network = network;
            this.binCalculator = binCalculator;

            this.pickupDistances = new ArrayList<>();
            for (int i = 0; i < binCalculator.getBins(); i++) this.pickupDistances.add(new LinkedList<>());
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            if (event.getActType().equals("AVStay")) {
                stayEndEvents.put(event.getPersonId(), event);
                distances.put(event.getPersonId(), 0.0);
            }
        }

        @Override
        public void handleEvent(LinkEnterEvent event) {
            if (stayEndEvents.containsKey(id(event.getVehicleId()))) {
                enterEvents.put(event.getVehicleId(), event);
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            if (stayEndEvents.containsKey(id(event.getVehicleId()))) {
                LinkEnterEvent enterEvent = enterEvents.remove(event.getVehicleId());

                if (enterEvent != null && enterEvent.getLinkId().equals(event.getLinkId())) {
                    distances.put(id(event.getVehicleId()), distances.get(id(event.getVehicleId())) + network.getLinks().get(event.getLinkId()).getLength());
                }
            }
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            ActivityEndEvent stayEndEvent = stayEndEvents.remove(event.getPersonId());

            if (stayEndEvent != null && event.getActType().equals("AVPickup")) {
                Double distance = distances.remove(event.getPersonId());

                if (distance != null) {
                    if (binCalculator.isCoveredValue(event.getTime())) {
                        pickupDistances.get(binCalculator.getIndex(event.getTime())).add(distance);
                    }
                }
            }
        }

        @Override
        public void reset(int iteration) {}
    }
}
