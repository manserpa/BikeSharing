package playground.zurich_av;

import org.junit.Test;
import org.junit.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import playground.zurich_av.replanning.ZurichSubtourModeChoiceAlgorithm;

import java.util.Arrays;
import java.util.Collection;

public class TestZurichReplanning {
    @Test
    public void testRelpanningAlgorithm() {
        Network network = NetworkUtils.createNetwork();

        Node node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0.0, 0.0));
        Node node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(1.0, 0.0));
        Node node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(2.0, 0.0));
        Node node4 = network.getFactory().createNode(Id.createNodeId("node4"), new Coord(3.0, 0.0));

        Link link12 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        Link link23 = network.getFactory().createLink(Id.createLinkId("link2"), node2, node3);
        Link link34 = network.getFactory().createLink(Id.createLinkId("link3"), node3, node4);

        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);

        network.addLink(link12);
        network.addLink(link23);
        network.addLink(link34);

        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl();

        PlanAlgorithm withAV = new PlanAlgorithm() {
            @Override
            public void run(Plan plan) {

            }
        };

        PlanAlgorithm withoutAV = new PlanAlgorithm() {
            @Override
            public void run(Plan plan) {

            }
        };
    }
}
