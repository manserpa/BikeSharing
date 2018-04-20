/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.minibus.hook;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * 
 * @author aneumann
 *
 */
class PTransitRouterFactory implements Provider<TransitRouter>, StartupListener, IterationStartsListener {
	
	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);

	private TransitRouterConfig transitRouterConfig;

	private boolean needToUpdateRouter = true;
	private TransitRouterNetwork routerNetwork = null;
	private Provider<TransitRouter> routerFactory = null;

	@Inject private TransitSchedule schedule;
	
	public PTransitRouterFactory(Config config){
		this.createTransitRouterConfig(config);
	}

	private void createTransitRouterConfig(Config config) {
		this.transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
	}
	
	private void updateTransitSchedule() {
		this.needToUpdateRouter = true;
	}

	@Override
	public TransitRouter get() {
		if(needToUpdateRouter) {
			// okay update all routers
			// TODO (PM) use Raptor as speedy router
			if(this.routerFactory == null) {
				Gbl.assertNotNull(this.transitRouterConfig);
				this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.transitRouterConfig.getBeelineWalkConnectionDistance());
			}
			needToUpdateRouter = false;
		}
		
		if (this.routerFactory == null) {
			PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
			TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterConfig, preparedTransitSchedule);
			return new TransitRouterImpl(this.transitRouterConfig, preparedTransitSchedule, routerNetwork, ttCalculator, ttCalculator);
		} else {
			return this.routerFactory.get();
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.updateTransitSchedule();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.updateTransitSchedule();
	}
}