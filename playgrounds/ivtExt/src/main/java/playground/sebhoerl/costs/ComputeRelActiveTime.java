package playground.sebhoerl.costs;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.collections.Tuple;

import java.io.*;
import java.util.*;

public class ComputeRelActiveTime {
    public static void main(String[] args) throws IOException {
        String outputPath = args[0];

        String inputPath = args[1];
        String version = args[2];
        String quantile = args[3];
        int fleetSize = Integer.parseInt(args[4]);

        boolean outputExists = new File(outputPath).exists();

        FileOutputStream stream = new FileOutputStream(outputPath, true);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));

        if (!outputExists) {
            String headers[] = {
                    "VERSION",
                    "QUANTILE",
                    "FLEET_SIZE",
                    "UTILZATION_MORNING",
                    "UTILZATION_AFTERNOON",
                    "UTILZATION_OFFPEAK",
                    "WAITING_MEDIAN_MORNING",
                    "WAITING_MEDIAN_AFTERNOON",
                    "WAITING_MEDIAN_OFFPEAK",
                    "WAITING_90P_MORNING",
                    "WAITING_90P_AFTERNOON",
                    "WAITING_90P_OFFPEAK",
                    "WAITING_MEAN_MORNING",
                    "WAITING_MEAN_AFTERNOON",
                    "WAITING_MEAN_OFFPEAK",
                    "MODE_SHARE_MORNING",
                    "MODE_SHARE_AFTERNOON",
                    "MODE_SHARE_OFFPEAK",
                    "UNDERSUPPLY_MORNING",
                    "UNDERSUPPLY_AFTERNOON",
                    "UNDERSUPPLY_OFFPEAK"
            };

            writer.write(String.join(";", headers) + "\n");
            writer.flush();
        }

        LinkedList<String> elements = new LinkedList<>();
        elements.add(version);
        elements.add(quantile);
        elements.add(String.valueOf(fleetSize));

        for (double value : runUtilizationAnalysis(inputPath, fleetSize)) {
            elements.add(String.valueOf(value));
        }

        writer.write(String.join(";", elements) + "\n");
        writer.flush();

        stream.close();
    }

    final static double MORNING_PEAK_START = 8 * 3600.0;
    final static double MORNING_PEAK_END = 9 * 3600.0;

    final static double AFTERNOON_PEAK_START = 17 * 3600.0;
    final static double AFTERNOON_PEAK_END = 18 * 3600.0;

    final static double OFFPEAK_START = 13 * 3600.0;
    final static double OFFPEAK_END = 14 * 3600.0;

    static public double[] runUtilizationAnalysis(String eventsSource, double numberOfVehicles) {
        RelActiveTimeHandler morningPeakUtilizationHandler = new RelActiveTimeHandler(MORNING_PEAK_START, MORNING_PEAK_END, numberOfVehicles);
        RelActiveTimeHandler afternoonPeakUtilizationHandler = new RelActiveTimeHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END, numberOfVehicles);
        RelActiveTimeHandler offpeakUtilizationHandler = new RelActiveTimeHandler(OFFPEAK_START, OFFPEAK_END, numberOfVehicles);

        WaitingTimeHandler morningPeakWaitingTimeHandler = new WaitingTimeHandler(MORNING_PEAK_START, MORNING_PEAK_END);
        WaitingTimeHandler afternoonPeakWaitingTimeHandler = new WaitingTimeHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END);
        WaitingTimeHandler offpeakWaitingTimeHandler = new WaitingTimeHandler(OFFPEAK_START, OFFPEAK_END);

        ModeShareHandler morningPeakModeShareHandler = new ModeShareHandler(MORNING_PEAK_START, MORNING_PEAK_END);
        ModeShareHandler afternoonPeakModeShareHandler = new ModeShareHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END);
        ModeShareHandler offpeakModeShareHandler = new ModeShareHandler(OFFPEAK_START, OFFPEAK_END);

        DispatchModeHandler morningPeakDispatchModeHandler = new DispatchModeHandler(MORNING_PEAK_START, MORNING_PEAK_END);
        DispatchModeHandler afternoonPeakDispatchModeHandler = new DispatchModeHandler(AFTERNOON_PEAK_START, AFTERNOON_PEAK_END);
        DispatchModeHandler offpeakDispatchModeHandler = new DispatchModeHandler(OFFPEAK_START, OFFPEAK_END);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(morningPeakUtilizationHandler);
        eventsManager.addHandler(afternoonPeakUtilizationHandler);
        eventsManager.addHandler(offpeakUtilizationHandler);
        eventsManager.addHandler(morningPeakWaitingTimeHandler);
        eventsManager.addHandler(afternoonPeakWaitingTimeHandler);
        eventsManager.addHandler(offpeakWaitingTimeHandler);
        eventsManager.addHandler(morningPeakModeShareHandler);
        eventsManager.addHandler(afternoonPeakModeShareHandler);
        eventsManager.addHandler(offpeakModeShareHandler);
        eventsManager.addHandler(morningPeakDispatchModeHandler);
        eventsManager.addHandler(afternoonPeakDispatchModeHandler);
        eventsManager.addHandler(offpeakDispatchModeHandler);

        new MatsimEventsReader(eventsManager).readFile(eventsSource);

        morningPeakUtilizationHandler.finish();
        afternoonPeakUtilizationHandler.finish();
        offpeakUtilizationHandler.finish();

        return new double[] {
                morningPeakUtilizationHandler.getUtilization(),
                afternoonPeakUtilizationHandler.getUtilization(),
                offpeakUtilizationHandler.getUtilization(),
                morningPeakWaitingTimeHandler.getStatistics().getPercentile(50),
                afternoonPeakWaitingTimeHandler.getStatistics().getPercentile(50),
                offpeakWaitingTimeHandler.getStatistics().getPercentile(50),
                morningPeakWaitingTimeHandler.getStatistics().getPercentile(90),
                afternoonPeakWaitingTimeHandler.getStatistics().getPercentile(90),
                offpeakWaitingTimeHandler.getStatistics().getPercentile(90),
                morningPeakWaitingTimeHandler.getStatistics().getMean(),
                afternoonPeakWaitingTimeHandler.getStatistics().getMean(),
                offpeakWaitingTimeHandler.getStatistics().getMean(),
                morningPeakModeShareHandler.getModeShare(),
                afternoonPeakModeShareHandler.getModeShare(),
                offpeakModeShareHandler.getModeShare(),
                morningPeakDispatchModeHandler.getUndersupplyShare(),
                afternoonPeakDispatchModeHandler.getUndersupplyShare(),
                offpeakDispatchModeHandler.getUndersupplyShare()
        };
    }

    static public class RelActiveTimeHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
        final private Map<Id<Person>, Double> customerStartTimes = new HashMap<>();

        final private double measurementStartTime;
        final private double measurementEndTime;
        final private double numberOfVehicles;

        private double totalIdleTime = 0.0;

        public double getTotalIdleTime() {
            return totalIdleTime;
        }

        public double getTotalTime() {
            return (measurementEndTime - measurementStartTime) * numberOfVehicles;
        }

        public double getUtilization() {
            return getTotalIdleTime() / getTotalTime();
        }

        public RelActiveTimeHandler(double measurementStartTime, double measurementEndTime, double numberOfVehicles) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
            this.numberOfVehicles = numberOfVehicles;
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            if (event.getActType().equals("AVDropoff")) {
                Double startTime = customerStartTimes.remove(event.getPersonId());
                Double endTime = event.getTime();

                if (startTime != null) {
                    boolean startTimeCovered = measurementStartTime <= startTime && startTime <= measurementEndTime;
                    boolean endTimeCovered = measurementStartTime <= endTime && endTime <= measurementEndTime;

                    if (startTimeCovered && endTimeCovered) {
                        totalIdleTime += endTime - startTime;
                    } else if (startTimeCovered && !endTimeCovered) {
                        totalIdleTime += measurementEndTime - startTime;
                    } else if (!startTimeCovered && endTimeCovered) {
                        totalIdleTime += endTime - measurementStartTime;
                    }
                }
            }
        }

        public void finish() {
            for (Id<Person> personId : new HashSet<>(customerStartTimes.keySet())) {
                handleEvent(new ActivityStartEvent(measurementEndTime, personId, null, null, "AVDropoff"));
            }
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            if (event.getActType().equals("AVPickup")) {
                customerStartTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void reset(int iteration) {}
    }

    static public class WaitingTimeHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
        final private Map<Id<Person>, Double> waitingStartTimes = new HashMap<>();

        final private double measurementStartTime;
        final private double measurementEndTime;

        private DescriptiveStatistics measuredWaitingTimes = new DescriptiveStatistics();

        public DescriptiveStatistics getStatistics() {
            return measuredWaitingTimes;
        }

        public WaitingTimeHandler(double measurementStartTime, double measurementEndTime) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals("av")) {
                waitingStartTimes.put(event.getPersonId(), event.getTime());
            }
        }

        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            Double startTime = waitingStartTimes.remove(event.getPersonId());
            Double endTime = event.getTime();

            if (startTime != null && measurementStartTime <= startTime && startTime <= measurementEndTime && measurementStartTime <= endTime && endTime <= measurementEndTime) {
                Double waitingTime = endTime - startTime;
                measuredWaitingTimes.addValue(waitingTime);
            }
        }

        @Override
        public void reset(int iteration) {}
    }

    static class ModeShareHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler {
        final private Map<Id<Person>, Tuple<Double,String>> ongoing = new HashMap<>();

        final private double measurementStartTime;
        final private double measurementEndTime;

        private long totalNumberOfTrips = 0;
        private long avNumberOfTrips = 0;

        public double getModeShare() {
            return (double)avNumberOfTrips / (double)totalNumberOfTrips;
        }

        public ModeShareHandler(double measurementStartTime, double measurementEndTime) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
        }

        @Override
        public void handleEvent(ActivityEndEvent event) {
            if (event.getPersonId().toString().startsWith("av") || event.getPersonId().toString().startsWith("pt")) {
                return;
            }

            ongoing.put(event.getPersonId(), new Tuple<>(event.getTime(), null));
        }

        @Override
        public void handleEvent(ActivityStartEvent event) {
            Tuple<Double, String> trip = ongoing.remove(event.getPersonId());

            if (trip != null && trip.getSecond() != null) {
                if (measurementStartTime <= trip.getFirst() && trip.getFirst() <= measurementEndTime) {
                    if (trip.getSecond().equals("av")) {
                        avNumberOfTrips++;
                    }

                    totalNumberOfTrips++;
                }
            }
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals("transit_walk")) {
                return;
            }

            Tuple<Double, String> trip = ongoing.get(event.getPersonId());

            if (trip != null) {
                ongoing.put(event.getPersonId(), new Tuple<>(trip.getFirst(), event.getLegMode()));
            }
        }

        @Override
        public void reset(int iteration) {}
    }

    static class DispatchModeHandler implements BasicEventHandler {
        final private double measurementStartTime;
        final private double measurementEndTime;

        private double undersupplyDuration = 0.0;
        private Double undersupplyStart = null;

        public DispatchModeHandler(double measurementStartTime, double measurementEndTime) {
            this.measurementStartTime = measurementStartTime;
            this.measurementEndTime = measurementEndTime;
        }

        public double getUndersupplyShare() {
            return undersupplyDuration / (measurementEndTime - measurementStartTime);
        }

        @Override
        public void handleEvent(Event event) {
            if (event.getEventType().equals("AVHeuristicModeChange")) {
                if (event.getAttributes().get("mode").equals("UNDERSUPPLY")) {
                    undersupplyStart = event.getTime();
                } else if (event.getAttributes().get("mode").equals("OVERSUPPLY")) {
                    double undersupplyEnd = event.getTime();

                    if (measurementStartTime <= undersupplyStart && undersupplyEnd <= measurementEndTime) {
                        undersupplyDuration += undersupplyEnd - undersupplyStart;
                    } else if (measurementStartTime <= undersupplyStart && undersupplyStart <= measurementEndTime && undersupplyEnd > measurementEndTime) {
                        undersupplyDuration += measurementEndTime - undersupplyStart;
                    } else if (measurementStartTime > undersupplyStart && undersupplyEnd <= measurementEndTime && measurementStartTime <= undersupplyEnd) {
                        undersupplyDuration += undersupplyEnd - measurementStartTime;
                    }
                }
            }
        }

        @Override
        public void reset(int iteration) {}
    }
}
