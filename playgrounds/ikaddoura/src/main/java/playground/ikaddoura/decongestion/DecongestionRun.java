/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.ikaddoura.decongestion;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisRun;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.IntervalBasedTollingAll;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;

/**
 * Starts an interval-based decongestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class DecongestionRun {

	private static final Logger log = Logger.getLogger(DecongestionRun.class);

	private static String configFile;
	private static String outputBaseDirectory;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputBaseDirectory = args[1];		
			log.info("output directory: "+ outputBaseDirectory);

		} else {
			configFile = "../../../runs-svn/vickrey-decongestion/input/config.xml";
			outputBaseDirectory = "../../../runs-svn/vickrey-decongestion/output2/";
		}
		
		DecongestionRun main = new DecongestionRun();
		main.run();
		
	}

	private void run() throws IOException {

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setMsa(true);
		decongestionSettings.setTOLL_BLEND_FACTOR(0.);
		decongestionSettings.setKd(0.);
		decongestionSettings.setKi(0.);
		decongestionSettings.setKp(0.05);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(0.8);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
		
		Config config = ConfigUtils.loadConfig(configFile);

		double weight = Double.NEGATIVE_INFINITY;
		for (StrategySettings settings : config.strategy().getStrategySettings()) {
			if (settings.getStrategyName().equals("TimeAllocationMutator")) {
				weight = settings.getWeight();
			}
		}
		
		String outputDirectory = outputBaseDirectory +
				"total" + config.controler().getLastIteration() + "it" +
				"_plans" + config.strategy().getMaxAgentPlanMemorySize() +
				"_ScoreMSA" + config.planCalcScore().getFractionOfIterationsToStartScoreMSA() +
				"_timeBinSize" + config.travelTimeCalculator().getTraveltimeBinSize() +
				"_BrainExpBeta" + config.planCalcScore().getBrainExpBeta() +
				"_timeMutationWeight" + weight +
				"_timeMutationRange" + config.timeAllocationMutator().getMutationRange();
		
		// General pricing settings
		outputDirectory = outputDirectory
					+ "_priceUpdate" + decongestionSettings.getUPDATE_PRICE_INTERVAL() + "_it"
					+ "_toleratedDelay" + decongestionSettings.getTOLERATED_AVERAGE_DELAY_SEC()
					+ "_start" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT()
					+ "_end" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT();
		
		// BangBang
//		outputDirectory = outputDirectory + 
//						"_init" + decongestionSettings.getINITIAL_TOLL() +
//						"_adj" + decongestionSettings.getTOLL_ADJUSTMENT();
			
		// PID
		outputDirectory = outputDirectory +
						"_Kp" + decongestionSettings.getKp() +
						"_Ki" + decongestionSettings.getKi() +
						"_Kd" + decongestionSettings.getKd() +
						"_tollMSA" + decongestionSettings.isMsa() + 
						"_blendFactor" + decongestionSettings.getTOLL_BLEND_FACTOR();
					
		log.info("Output directory: " + outputDirectory);
		
		config.controler().setOutputDirectory(outputDirectory + "/");
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		DecongestionInfo info = new DecongestionInfo(decongestionSettings);
		
		// #############################################################
		
		// congestion toll computation
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
				
				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		// #############################################################
	
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
        controler.run();
        
        PersonTripBasicAnalysisRun analysis = new PersonTripBasicAnalysisRun(outputDirectory);
		analysis.run();
	}
}

