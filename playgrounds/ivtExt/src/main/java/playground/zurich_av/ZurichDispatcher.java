package playground.zurich_av;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import javax.inject.Named;
import java.util.Collection;
import java.util.Map;

public class ZurichDispatcher implements AVDispatcher {
    final private AVDispatcher delegate;
    final private Collection<Link> permissibleLinks;

    public ZurichDispatcher(AVDispatcher delegate, Collection<Link> permissibleLinks) {
        this.delegate = delegate;
        this.permissibleLinks = permissibleLinks;
    }

    @Override
    public void onRequestSubmitted(AVRequest request) {
        if (permissibleLinks.contains(request.getFromLink()) && permissibleLinks.contains(request.getToLink())) {
            delegate.onRequestSubmitted(request);
        }
    }

    @Override
    public void onNextTaskStarted(AVVehicle vehicle) {
        delegate.onNextTaskStarted(vehicle);
    }

    @Override
    public void onNextTimestep(double now) {
        delegate.onNextTimestep(now);
    }

    @Override
    public void addVehicle(AVVehicle vehicle) {
        delegate.addVehicle(vehicle);
    }

    static public class ZurichDispatcherFactory implements AVDispatcherFactory {
        @Inject
        private Map<String, AVDispatcherFactory> dispatcherFactories;

        @Inject @Named("zurich")
        private Collection<Link> permissibleLinks;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            String delegateName = config.getParams().get("delegate");

            if (!dispatcherFactories.containsKey(delegateName)) {
                throw new IllegalArgumentException("Unknown dispatcher: " + delegateName);
            }

            return new ZurichDispatcher(dispatcherFactories.get(delegateName).createDispatcher(config), permissibleLinks);
        }
    }
}
