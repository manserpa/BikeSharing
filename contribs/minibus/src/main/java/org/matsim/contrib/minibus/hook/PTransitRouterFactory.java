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
import org.matsim.contrib.minibus.raptor.RaptorUtils;
import org.matsim.contrib.minibus.raptor.SwissRailRaptor;
import org.matsim.contrib.minibus.raptor.SwissRailRaptorData;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author aneumann
 *
 */
class PTransitRouterFactory implements Provider<TransitRouter> {

	private final static Logger log = Logger.getLogger(PTransitRouterFactory.class);

	private final Config config;

	private boolean needToUpdateRouter = true;
	private boolean initializeRaptor = true;
	private Provider<TransitRouter> routerFactory = null;

	@Inject private TransitSchedule schedule;

	public PTransitRouterFactory(Config config){
		this.config = config;
	}

	void updateTransitSchedule() {
		this.needToUpdateRouter = true;
	}

	@Override
	public TransitRouter get() {
		if(initializeRaptor) {
			this.routerFactory = createSpeedyRouter();
			initializeRaptor = false;
		}
		if (needToUpdateRouter) {
			log.info("Using raptor routing");
			return this.createRaptorRouter();
		} else {
			return this.routerFactory.get();
		}
	}

	synchronized private TransitRouter createRaptorRouter() {
		SwissRailRaptorData raptorData = SwissRailRaptorData.create(this.schedule, RaptorUtils.createRaptorConfig(this.config));
		return new SwissRailRaptor(raptorData);
	}

	private Provider<TransitRouter> createSpeedyRouter() {
		try {
			Class<?> cls = Class.forName("org.matsim.contrib.minibus.raptor.SwissRailRaptorFactory");
			Constructor<?> ct = cls.getConstructor(new Class[] {TransitSchedule.class, Config.class});
			return (Provider<TransitRouter>) ct.newInstance(this.schedule, this.config);
		} catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			log.info(e.toString() );
		}
		return null;
	}
}

