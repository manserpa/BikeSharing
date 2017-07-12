package playground.manserpa.minibus;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;


/**
 * Entry point, registers all necessary hooks
 * 
 * @author aneumann
 */
public final class RunMinibusTest {

	private final static Logger log = Logger.getLogger(RunMinibusTest.class);

	public static void main(final String[] args) {
		
		String configFile = args[0];
		
		Config config = ConfigUtils.loadConfig( configFile, new PConfigGroup() ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setCreateGraphs(false);

		controler.addOverridingModule(new PModule()) ;
		
		//ConfigUtils.addOrGetModule(scenario.getConfig(), PConfigGroup.class ).setUseAVContrib(true);
	
		
		controler.run();
		
	}		
}