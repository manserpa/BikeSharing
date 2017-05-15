package playground.sebhoerl.ant.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AVStateHandler extends AbstractHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
    final private Map<Id<Person>, Tuple<String, Double>> ongoing = new HashMap<>();

    public AVStateHandler(DataFrame data) {
        super(data);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (data.avStates.contains(event.getActType())) {
            ongoing.put(event.getPersonId(), new Tuple<>(event.getActType(), event.getTime()));
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        Tuple<String, Double> item = ongoing.remove(event.getPersonId());

        if (item != null) {
            if (!item.getFirst().equals(event.getActType())) {
                throw new IllegalStateException();
            }

            List<Double> bins = data.avStateCount.get(event.getActType());

            for (BinCalculator.BinEntry entry : data.binCalculator.getBinEntriesNormalized(item.getSecond(), event.getTime())) {
                bins.set(entry.getIndex(), bins.get(entry.getIndex()) + entry.getWeight());
            }
        }
    }

    @Override
    public void reset(int iteration) {}

    @Override
    protected void finish() {}
}
