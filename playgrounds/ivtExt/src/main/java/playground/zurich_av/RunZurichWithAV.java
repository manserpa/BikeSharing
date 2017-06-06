package playground.zurich_av;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.hook.PModule;
import org.matsim.contrib.zone.io.ZoneShpReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.PtConstants;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;
import playground.zurich_av.replanning.ZurichPlanStrategyProvider;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RunZurichWithAV {
    public static void main(String[] args) throws MalformedURLException, FileNotFoundException {
        String configFile = args[0];

        // 1. Configuration

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile, new PConfigGroup(), new AVConfigGroup(), dvrpConfigGroup, new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // 2. Basic controller setup

        Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        ConfigUtils.addOrGetModule(controler.getConfig(), PConfigGroup.class ).setUseAVContrib(true);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        
        controler.addOverridingModule(new PModule()) ;

        // 3. IVT-specifics

        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {}

            @Provides @Singleton
            ScoringFunctionFactory provideScoringFunctionFactory(Scenario scenario, AVConfig config) {
                return new AVScoringFunctionFactory(
                        new IVTBaselineScoringFunctionFactory(scenario, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)),
                        scenario, config);
            }
        });
        
        String line = "";
        String agentsToDeleteFile = "AgentsToDelete.csv";
        
        if(agentsToDeleteFile != null)	{
	        try (BufferedReader br = new BufferedReader(new FileReader(agentsToDeleteFile))) {
	
	            while ((line = br.readLine()) != null) {
	            	scenario.getPopulation().removePerson(Id.create(line, Person.class));
	            }
	
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }

        // 4. Set up permissible AV links

        final Network network = scenario.getNetwork();

        FileInputStream stream = new FileInputStream(ConfigGroup.getInputFileURL(config.getContext(), "nodes.list").getPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        final Set<Node> permissibleNodes = new HashSet<>();
        final Set<Link> permissibleLinks = new HashSet<>();

        reader.lines().forEach((String nodeId) -> permissibleNodes.add(network.getNodes().get(Id.createNodeId(nodeId))) );
        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getOutLinks().values()));
        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getInLinks().values()));
        final Set<Link> filteredPermissibleLinks = permissibleLinks.stream().filter((l) -> l.getAllowedModes().contains("car")).collect(Collectors.toSet());

        Logger.getLogger(RunZurichWithAV.class).info("Loaded " + filteredPermissibleLinks.size() + " permissible links (from " + permissibleLinks.size() + " in the area).");

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(new TypeLiteral<Collection<Link>>() {}).annotatedWith(Names.named("zurich")).toInstance(filteredPermissibleLinks);
                AVUtils.registerDispatcherFactory(binder(), "ZurichDispatcher", ZurichDispatcher.ZurichDispatcherFactory.class);
                AVUtils.registerGeneratorFactory(binder(), "ZurichGenerator", ZurichGenerator.ZurichGeneratorFactory.class);

                addPlanStrategyBinding("ZurichModeChoice").toProvider(ZurichPlanStrategyProvider.class);
            }
        });

        // 5. Run

      //note a single Random object is reused here
        Random randomGenerator = new Random();
        int randomInt = randomGenerator.nextInt(1500) + 1000;
        
        long randomSeed = (long) (randomInt);
        	
	    config.global().setRandomSeed(randomSeed);
	    String outputdir = config.controler().getOutputDirectory();
	    config.controler().setOutputDirectory(outputdir + "Seed" + randomSeed);
	        
	    config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
	    
	    controler.run();
    }
}
