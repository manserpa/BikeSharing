package playground.sebhoerl.recharging_avs.tracker;

import com.google.inject.Singleton;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.utils.io.IOUtils;
import playground.sebhoerl.av_paper.BinCalculator;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CSVConsumptionTracker implements ConsumptionTracker, IterationStartsListener, IterationEndsListener {
    final private BinCalculator binCalculator;

    final private List<Double> timeBasedConsumption;
    final private List<Double> distanceBasedConsumption;

    public CSVConsumptionTracker(BinCalculator binCalculator) {
        this.binCalculator = binCalculator;

        timeBasedConsumption = new ArrayList<>(binCalculator.getBins());
        distanceBasedConsumption = new ArrayList<>(binCalculator.getBins());

        for (int i = 0; i < binCalculator.getBins(); i++) {
            timeBasedConsumption.add(0.0);
            distanceBasedConsumption.add(0.0);
        }
    }

    private void addConsumption(List<Double> consumptionData, double start, double end, double consumption) {
        double consumptionPerSecond = consumption / (end - start);

        for (BinCalculator.BinEntry entry : binCalculator.getBinEntriesNormalized(start, end)) {
            consumptionData.set(entry.getIndex(), consumptionData.get(entry.getIndex()) + entry.getWeight() * binCalculator.getInterval() * consumptionPerSecond);
        }
    }

    @Override
    public void addDistanceBasedConsumption(double start, double end, double consumption) {
        addConsumption(distanceBasedConsumption, start, end, consumption);
    }

    @Override
    public void addTimeBasedConsumption(double start, double end, double consumption) {
        addConsumption(timeBasedConsumption, start, end, consumption);
    }

    private void clearConsumption(List<Double> consumption) {
        for (int i = 0; i < binCalculator.getBins(); i++) {
            consumption.set(i, 0.0);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        clearConsumption(distanceBasedConsumption);
        clearConsumption(timeBasedConsumption);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        try {
            OutputStream stream = IOUtils.getOutputStream(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "consumption.csv"));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(stream)));

            writer.write("TIME;DISTANCE_BASED;TIME_BASED\n");

            for (int i = 0; i < binCalculator.getBins(); i++) {
                writer.write(String.format("%d;%f;%f\n", (int) binCalculator.getStart(i), distanceBasedConsumption.get(i), timeBasedConsumption.get(i)));
            }

            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
