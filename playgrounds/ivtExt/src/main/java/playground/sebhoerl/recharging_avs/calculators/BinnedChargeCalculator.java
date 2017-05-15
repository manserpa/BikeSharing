package playground.sebhoerl.recharging_avs.calculators;

import com.google.inject.Inject;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

public class BinnedChargeCalculator implements ChargeCalculator {
    final private BinnedChargeCalculatorConfig config;
    final private BinnedChargeCalculatorData data;
    final private double S_PER_H = 3600.0;

    @Inject
    public BinnedChargeCalculator(BinnedChargeCalculatorConfig config, BinnedChargeCalculatorData data) {
        this.config = config;
        this.data = data;
    }

    @Override
    public double calculateConsumption(double from, double until, double distance) {
        int fromBin = data.calculateBin(from);
        int untilBin = data.calculateBin(until);

        double speed = distance / (until - from);

        if (fromBin == untilBin) {
            return data.getDischargeRateByDistance(fromBin) * distance / 1e3;
        }

        double consumption = 0.0;

        consumption += ((data.getBinEndTime(fromBin) - from) * speed / 1e3) * data.getDischargeRateByDistance(fromBin);
        consumption += ((until - data.getBinStartTime(untilBin)) * speed / 1e3) * data.getDischargeRateByDistance(untilBin);

        for (int b = fromBin + 1; b < untilBin; b++) {
            consumption += (data.getBinDuration(b) * speed / 1e3) * data.getDischargeRateByTime(b);
        }

        return consumption;
    }

    @Override
    public double calculateConsumption(double from, double until) {
        int fromBin = data.calculateBin(from);
        int untilBin = data.calculateBin(until);

        if (fromBin == untilBin) {
            return (until - from) / S_PER_H * data.getDischargeRateByTime(fromBin);
        }

        double consumption = 0.0;

        consumption += (data.getBinEndTime(fromBin) - from) / S_PER_H * data.getDischargeRateByTime(fromBin);
        consumption += (until - data.getBinStartTime(untilBin)) / S_PER_H * data.getDischargeRateByTime(untilBin);

        for (int b = fromBin + 1; b < untilBin; b++) {
            consumption += data.getBinDuration(b) / S_PER_H * data.getDischargeRateByTime(b);
        }

        return consumption;
    }

    @Override
    public double getInitialCharge(double now) {
        return data.getMaximumCharge(data.calculateBin(now));
    }

    @Override
    public double getMaximumCharge(double now) {
        return data.getMaximumCharge(data.calculateBin(now));
    }

    @Override
    public boolean isCritical(double charge, double now) {
        return charge < data.getMinimumCharge(data.calculateBin(now));
    }

    @Override
    public double getRechargeTime(double now) {
        int currentBin = data.calculateBin(now);

        // TODO: maybe introduce another parameter here.
        // recharging ALWAYS starts at 0.0 now
        double charge = 0.0; // data.getMinimumCharge(currentBin);
        double time = 0.0;
        double duration;

        do {
            duration = data.getBinDuration(currentBin);

            charge += duration / S_PER_H * data.getRechgargeRate(currentBin);
            time += duration;

            currentBin += 1;
        } while (charge < data.getMaximumCharge(currentBin - 1) && currentBin < data.getNumberOfBins());

        return time - S_PER_H * (charge - data.getMaximumCharge(currentBin - 1)) / data.getRechgargeRate(currentBin - 1);
    }
}
