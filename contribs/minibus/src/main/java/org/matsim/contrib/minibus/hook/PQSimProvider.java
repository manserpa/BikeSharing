/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEnginePlugin;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dynagent.run.DynActivityEnginePlugin;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * The MobsimFactory is only necessary so that I can add the {@link PTransitAgent}.
 *
 * @author aneumann
 *
 */

class PQSimProvider implements Provider<Mobsim> {

	private final static Logger log = Logger.getLogger(PQSimProvider.class);

	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Inject Collection<AbstractQSimPlugin> plugins;

	@Override
	public Netsim get() {
		List<AbstractQSimPlugin> plugins = new LinkedList<>();
		for (AbstractQSimPlugin plugin : this.plugins) {
			if (!(plugin instanceof TransitEnginePlugin) && !(plugin instanceof PopulationPlugin)) {
				plugins.add(plugin);
			}
		}
		if(scenario.getConfig().transit().isUseTransit())
			plugins.add(new SBBTransitEnginePlugin(scenario.getConfig()));

		QSim qSim = QSimUtils.createQSim(scenario, eventsManager, plugins);

		AgentFactory agentFactory;
		if (scenario.getConfig().transit().isUseTransit()) {
			agentFactory = new PTransitAgentFactory(qSim);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		return qSim;
	}

}
