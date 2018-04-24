/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package org.matsim.contrib.minibus.raptor;

import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author mrieser / SBB
 */
@Singleton
public class SwissRailRaptorFactory implements Provider<TransitRouter> {

    private SwissRailRaptorData data = null;
    private final TransitSchedule schedule;
    private final RaptorConfig raptorConfig;

    @Inject
    public SwissRailRaptorFactory(final TransitSchedule schedule, final Config config) {
        this.schedule = schedule;
        this.raptorConfig = RaptorUtils.createRaptorConfig(config);
    }

    @Override
    public TransitRouter get() {
        SwissRailRaptorData data = getData();
        return new SwissRailRaptor(data);
    }

    private SwissRailRaptorData getData() {
        if (this.data == null) {
            this.data = prepareData();
        }
        return this.data;
    }

    synchronized private SwissRailRaptorData prepareData() {
        if (this.data != null) {
            // due to multithreading / race conditions, this could still happen.
            // prevent doing the work twice.
            return this.data;
        }
        this.data = SwissRailRaptorData.create(this.schedule, this.raptorConfig);
        return this.data;
    }

}
