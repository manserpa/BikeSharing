package playground.manserpa.minibus;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import contrib.baseline.IVTBaselineScoringFunctionFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.PtConstants;

import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorStrategyModule;
import playground.manserpa.minibus.zurich_replanning.ZurichPlanStrategyProvider;


import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.Collection;



/**
 * Reference Scenario -> no minibus, no AVs, just a normal Zurich Scenario
 * 
 */

public final class RunZurichReference {

	private final static Logger log = Logger.getLogger(RunMinibusTest.class);

	public static void main(final String[] args) throws MalformedURLException, FileNotFoundException {
		
		if(args.length == 0){
			log.info("Arg 1: config.xml is missing.");
			log.info("Check http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/ for an example.");
			System.exit(1);
		}
		
		String configFile = args[0];
		
		
		// 1. Configuration

        Config config = ConfigUtils.loadConfig(configFile, new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        // 2. Basic controller setup

        Controler controler = new Controler(scenario);
        
        controler.getConfig().controler().setCreateGraphs(false);


		// 3. IVT-specifics

        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {}
            
            @Provides @Singleton
            ScoringFunctionFactory provideScoringFunctionFactory(Scenario scenario) {
                return new IVTBaselineScoringFunctionFactory(scenario, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE));
            }
        });
        
        final Network network = scenario.getNetwork();
        

        FileInputStream stream = new FileInputStream(ConfigGroup.getInputFileURL(config.getContext(), "nodes.list").getPath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        final Set<Node> permissibleNodes = new HashSet<>();
        final Set<Link> permissibleLinks = new HashSet<>();


        reader.lines().forEach((String nodeId) -> permissibleNodes.add(network.getNodes().get(Id.createNodeId(nodeId))) );
        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getOutLinks().values()));
        permissibleNodes.forEach((Node node) -> permissibleLinks.addAll(node.getInLinks().values()));
        final Set<Link> filteredPermissibleLinks = permissibleLinks.stream().filter((l) -> l.getAllowedModes().contains("car")).collect(Collectors.toSet());
        log.info("Loaded " + permissibleLinks.size() + " permissible links.");
        
        
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(new TypeLiteral<Collection<Link>>() {}).annotatedWith(Names.named("zurich")).toInstance(filteredPermissibleLinks);
                
                addPlanStrategyBinding("zurichModeChoice").toProvider(ZurichPlanStrategyProvider.class);
            }
        });
        
		controler.run();
		
	}		
}