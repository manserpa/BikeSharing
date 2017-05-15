package playground.sebhoerl.recharging_avs.tracker;

public interface ConsumptionTracker {
    void addDistanceBasedConsumption(double start, double end, double consumption);
    void addTimeBasedConsumption(double start, double end, double consumption);
}
