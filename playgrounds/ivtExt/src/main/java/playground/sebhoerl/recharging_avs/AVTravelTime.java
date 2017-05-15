package playground.sebhoerl.recharging_avs;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class AVTravelTime implements TravelTime {
    final private AVTravelTimeTracker travelTimeTracker;
    final private TravelTime delegate;

    final private double maximumInterpolationTime = 300.0;
    final private double exponent = 1.0;

    public AVTravelTime(AVTravelTimeTracker travelTimeTracker, TravelTime delegate) {
        this.delegate = delegate;
        this.travelTimeTracker = travelTimeTracker;
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        AVTravelTimeTracker.LinkTravelTime travelTime = travelTimeTracker.getLinkTravelTime(link.getId());
        double delegateTravelTime = delegate.getLinkTravelTime(link, Math.max(0.0, time), person, vehicle);

        if (!Double.isNaN(travelTime.travelTime) && !Double.isNaN(travelTime.updateTime) && travelTime.updateTime > time - maximumInterpolationTime) {
            return Math.max(1.0, interpolate(delegateTravelTime, travelTime.travelTime, Math.max(0, time - travelTime.updateTime) / maximumInterpolationTime));
        } else {
            return Math.max(1.0, delegateTravelTime);
        }
    }

    private double interpolate(double freespeedTravelTime, double measuredTravelTime, double relativeElapsedTime) {
        return Math.pow(1.0 - relativeElapsedTime, exponent) * measuredTravelTime + Math.pow(relativeElapsedTime, exponent) * freespeedTravelTime;
    }
}
