package playground.sebhoerl.recharging_avs;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeCalculatorConfig;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeCalculatorModule;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculatorConfig;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculatorModule;

import java.net.MalformedURLException;

public class RunTestSimulation {
    public static void main(String[] args) throws MalformedURLException {
        String configFile = "/home/sebastian/belser/stage2/config_opt_v1_8000.xml";
        String baselinePlansPath = "/home/sebastian/belser/testing/output_plans.xml.gz";
        String outputPath = "/home/sebastian/belser/testing/output";

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(
                configFile,
                new AVConfigGroup(), dvrpConfigGroup, new RechargingConfig(),
                new StaticChargeCalculatorConfig(), new BinnedChargeCalculatorConfig());

        config.controler().setOutputDirectory(outputPath);
        config.plans().setInputFile(baselinePlansPath);
        config.controler().setLastIteration(10);
        config.controler().setWriteEventsInterval(10);

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(AVRoute.class, new AVRouteFactory());
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new RechargingModule());
        controler.addOverridingModule(new StaticChargeCalculatorModule());
        controler.addOverridingModule(new BinnedChargeCalculatorModule());
        controler.addOverridingModule(new AVTravelTimeModule());

        controler.run();
    }
}