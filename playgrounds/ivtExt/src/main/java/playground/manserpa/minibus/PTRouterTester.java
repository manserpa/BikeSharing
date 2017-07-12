package playground.manserpa.minibus;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
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
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;

import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;


/**
 * Entry point, registers all necessary hooks
 * 
 * @author aneumann
 */
public final class PTRouterTester {

	private final static Logger log = Logger.getLogger(PTRouterTester.class);

	public static void main(final String[] args) {
		
		String configFile = args[0];
		
		Config config = ConfigUtils.loadConfig( configFile, new PConfigGroup() ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.getConfig().controler().setCreateGraphs(false);

		controler.addOverridingModule(new PModule()) ;
		
		//ConfigUtils.addOrGetModule(scenario.getConfig(), PConfigGroup.class ).setUseAVContrib(true);
		
		TransitRouterImpl router = new TransitRouterImpl( new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental() ), scenario.getTransitSchedule() ) ;
		
		
		List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(, (double) 0)), new FakeFacility(new Coord(x, (double) 0)), 5.5*3600, null);
		
	}		
}