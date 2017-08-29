package org.matsim.contrib.minibus.routeProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.PConfigGroup.PVehicleSettings;
import org.matsim.contrib.minibus.genericUtils.GridNode;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Back-And-Forth route provider. Purpose: creates to routes for each plan -> one for the route forth and one
 * for the route back
 *  
 * @author manserpa
 *
 */

final class BackAndForthScheduleProvider implements PRouteProvider{

	private final static Logger log = Logger.getLogger(BackAndForthScheduleProvider.class);
	public final static String NAME = "BackAndForthScheduleProvider";
	
	private final Network net;
	private final LeastCostPathCalculator routingAlgo;
	private final TransitSchedule scheduleWithStopsOnly;
	private final RandomStopProvider randomStopProvider;
	private final RandomPVehicleProvider randomPVehicleProvider;
	private final LinkedHashMap<Id<Link>, TransitStopFacility> linkId2StopFacilityMap;
	private final double vehicleMaximumVelocity;
	private final double planningSpeedFactor;
	private final double driverRestTime;
	
	private final TimeAwareComplexCircleScheduleProviderHandler handler;
	private final String transportMode;
	private final Collection<PVehicleSettings> pVehicleSettings;
	private PPlan pOperatorPlan;
	
	public BackAndForthScheduleProvider(TransitSchedule scheduleWithStopsOnly, Network network, RandomStopProvider randomStopProvider, RandomPVehicleProvider randomPVehicleProvider, double vehicleMaximumVelocity, double planningSpeedFactor, double driverRestTime, String pIdentifier, EventsManager eventsManager, final String transportMode, Collection<PVehicleSettings> pVehicleSettings) {
		
		Network pTransitNetwork = network;
		for (Link l : pTransitNetwork.getLinks().values())	{
			if(!l.getAllowedModes().contains("car"))
				pTransitNetwork.removeLink(l.getId());
			if(l.getFreespeed() > 23)
				l.setFreespeed(80/3.6);
		}
		
		this.net = pTransitNetwork;
		
		this.pVehicleSettings = pVehicleSettings;
		this.randomPVehicleProvider = randomPVehicleProvider;
		this.scheduleWithStopsOnly = scheduleWithStopsOnly;
		
		
		// ----------------------------------------------------------
		//TODO (manserpa) adapt the disutilities according to the config
		// ----------------------------------------------------------
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0); // Here, it may make sense to use the variable cost parameters given in the config. Ihab/Daniel may'14
		this.routingAlgo = new Dijkstra(this.net, tC, tC);
		@SuppressWarnings("serial")
		Set<String> modes =  new HashSet<String>(){{
			// this is the networkmode and explicitly not the transportmode
			add(TransportMode.car);
//			add(TransportMode.pt);
			}};
		((Dijkstra)this.routingAlgo).setModeRestriction(modes);
		
		// register all stops by their corresponding link id
		this.linkId2StopFacilityMap = new LinkedHashMap<>();
		for (TransitStopFacility stop : this.scheduleWithStopsOnly.getFacilities().values()) {
			if (stop.getLinkId() == null) {
				log.warn("There is a potential paratransit stop without a corresponding link id. Shouldn't be possible. Check stop " + stop.getId());
			} else {
				this.linkId2StopFacilityMap.put(stop.getLinkId(), stop);
			}
		}
		
		this.randomStopProvider = randomStopProvider;
		this.vehicleMaximumVelocity = vehicleMaximumVelocity;
		this.planningSpeedFactor = planningSpeedFactor;
		this.driverRestTime = driverRestTime;
		this.handler = new TimeAwareComplexCircleScheduleProviderHandler(pIdentifier);
		eventsManager.addHandler(this.handler);
		this.transportMode = transportMode;
	}
	
	@Override
	public TransitLine createTransitLineFromOperatorPlan(Id<Operator> operatorId, PPlan plan){
		this.pOperatorPlan = plan;
		return this.createTransitLine(Id.create(operatorId, TransitLine.class), plan.getStartTime(), plan.getEndTime(), plan.getNVehicles(), plan.getStopsToBeServed(), plan.getPVehicleType(), Id.create(plan.getId(), TransitRoute.class), plan.getId());
	}
	
	private TransitLine createTransitLine(Id<TransitLine> pLineId, double startTime, double endTime, int numberOfVehicles, ArrayList<TransitStopFacility> stopsToBeServed, String pVehicleType, Id<TransitRoute> routeId, Id<PPlan> planId){
		
		// initialize
		TransitLine line = this.scheduleWithStopsOnly.getFactory().createTransitLine(pLineId);			
		routeId = Id.create(pLineId + "-" + routeId, TransitRoute.class);
		// create this route according to stopsToBeServed
		TransitRoute transitRouteBack = createRoute(routeId, stopsToBeServed, pVehicleType, planId);
		// create this route according to the reversed stopsToBeServed (if A -> B)
		TransitRoute transitRouteForth = createRoute(routeId, stopsToBeServedReversed, pVehicleType, planId);
		
		// register route
		line.addRoute(transitRouteBack);
		line.addRoute(transitRouteForth);
		
		
		// add departures
		// ----------------------------------------------------------
		//TODO (manserpa) adapt the disutilities according to the config
		// ----------------------------------------------------------
		
		int n = 0;
		
		int headway = (int) (this.driverRestTime + transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset()) / numberOfVehicles;
		for (int i = 0; i < numberOfVehicles; i++) {
			for (double j = startTime + i * headway; j <= endTime; ) {
				Departure departure = this.scheduleWithStopsOnly.getFactory().createDeparture(Id.create(n, Departure.class), j);
				
				departure.setVehicleId(Id.create(transitRoute.getId().toString() + "-" + i +"_" + pVehicleType, Vehicle.class));
				//departure.setVehicleId(Id.create(vehicleIdNew + "-" + i, Vehicle.class));
				transitRoute.addDeparture(departure);
				j += transitRoute.getStops().get(transitRoute.getStops().size() - 1).getDepartureOffset() + this.driverRestTime;
				n++;
			}
		}		
		
//		log.info("added " + n + " departures");		
		return line;
	}
	
	
	
	
	// ----------------------------------------------------------
	//TODO (manserpa) modify the router
	// ----------------------------------------------------------
	
	
	private TransitRoute createRoute(Id<TransitRoute> routeID, ArrayList<TransitStopFacility> stopsToBeServed, String pVehicleType, Id<PPlan> planId){
		
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<>();
		HashSet<String> gridStopHashSet = new HashSet<>();
		
		for (TransitStopFacility transitStopFacility : stopsToBeServed) {
			tempStopsToBeServed.add(transitStopFacility);
			
			String gridNodeId = GridNode.getGridNodeIdForCoord(transitStopFacility.getCoord(), 300);
			gridStopHashSet.add(gridNodeId);
		}
		tempStopsToBeServed.add(stopsToBeServed.get(0));
		
		// create links - network route		
		Id<Link> startLinkId = null;
		Id<Link> lastLinkId = null;

		
		List<Link> links = new LinkedList<>();
		
		// for each stop
		for (TransitStopFacility stop : tempStopsToBeServed) {
			if(startLinkId == null){
				startLinkId = stop.getLinkId();
			}
			
			if(lastLinkId != null){
				links.add(this.net.getLinks().get(lastLinkId));
				Path path = this.routingAlgo.calcLeastCostPath(this.net.getLinks().get(lastLinkId).getToNode(), this.net.getLinks().get(stop.getLinkId()).getFromNode(), 0.0, null, null);

				for (Link link : path.links) {
					links.add(link);
				}
			}
			
			lastLinkId = stop.getLinkId();
		}

		links.remove(0);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(startLinkId, lastLinkId);
		route.setLinkIds(startLinkId, NetworkUtils.getLinkIds(links), lastLinkId);

		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<>();
		double runningTime = 0.0;
		
		
		// get capacity of the vehicle
		double capacity = 0.0;
		
		for (PVehicleSettings pVS : this.pVehicleSettings) {
            if (pVehicleType.equals(pVS.getPVehicleName())) {
            	capacity = pVS.getCapacityPerVehicle() * 10;
            }
        }
		
		// first stop
		TransitRouteStop routeStop;
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(tempStopsToBeServed.get(0), runningTime, runningTime);
		
		routeStop.setAwaitDepartureTime(true);
		stops.add(routeStop);
		
		ArrayList<TransitStopFacility> tempStopsToBeServedNew = new ArrayList<>();
		tempStopsToBeServedNew.add(tempStopsToBeServed.get(0));
		
		int k = 1;
		
		// problem: with the new code -> the stop sequence is probably not the same for the same transitroute
		// thus, the operator can not rely on the times from the last iteration
		boolean isSameStopSequenceAsLastIteration = true;
		
		// additional stops
		for (Link link : links) {
			
			// there is no stop on this link
			runningTime += (link.getLength() / (Math.min(this.vehicleMaximumVelocity, link.getFreespeed()) * this.planningSpeedFactor));
			
			// is there any stop facility on that link?
			if(this.linkId2StopFacilityMap.get(link.getId()) == null){
				continue;
			}
			
			
			if (tempStopsToBeServed.get(k).getLinkId().equals(link.getId()))	{
				
				// different from {@link ComplexCircleScheduleProvider}		
				if(isSameStopSequenceAsLastIteration)	{
					if(tempStopsToBeServed.get(k).equals(this.handler.getServedStopsInLastIteration(routeID, stops.size())))	{
						double runningTimeMod = modifyRunningTimeAccordingToTheLastIterationIfPossible(runningTime, 
								this.handler.getOffsetForRouteAndStopNumber(routeID, stops.size()));
						if (runningTimeMod > runningTime)
							runningTime = runningTimeMod;
					}
					else	{
						isSameStopSequenceAsLastIteration = false;
					}
				}
				
				routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(tempStopsToBeServed.get(k), 
						runningTime, runningTime + getMinStopTime(capacity));
				runningTime += getMinStopTime(capacity);
				
				tempStopsToBeServedNew.add(tempStopsToBeServed.get(k));
				
				routeStop.setAwaitDepartureTime(true);
				stops.add(routeStop);
				
				k++;
				
			}
			else	{
				
				String gridNode = GridNode.getGridNodeIdForCoord(this.linkId2StopFacilityMap.get(link.getId()).getCoord(), 300);
				
				if(gridStopHashSet.contains(gridNode))	{
					continue;
				}
				
				// hier muss jetzt geprüft werden, ob die Anzahl Aktivitäten überdurchschnittlich hoch sind
				if(this.randomStopProvider.hasHighNumberOfActivitiesInGrid(gridNode))	{
					
					// different from {@link ComplexCircleScheduleProvider}
					if(isSameStopSequenceAsLastIteration)	{
						if(this.linkId2StopFacilityMap.get(link.getId()).equals(this.handler.getServedStopsInLastIteration(routeID, stops.size())))	{
							double runningTimeMod = modifyRunningTimeAccordingToTheLastIterationIfPossible(runningTime, 
									this.handler.getOffsetForRouteAndStopNumber(routeID, stops.size()));
							if (runningTimeMod > runningTime)
								runningTime = runningTimeMod;
						}
						else	{
							isSameStopSequenceAsLastIteration = false;
						}
					}
					
					routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(this.linkId2StopFacilityMap.get(link.getId()), 
							runningTime, runningTime + getMinStopTime(capacity));
					runningTime += getMinStopTime(capacity);
					
					tempStopsToBeServedNew.add(this.linkId2StopFacilityMap.get(link.getId()));
					
					routeStop.setAwaitDepartureTime(true);
					stops.add(routeStop);
					
					gridStopHashSet.add(gridNode);
				}
			}
		}
		
		// last stop
		runningTime += (this.net.getLinks().get(tempStopsToBeServed.get(0).getLinkId()).getLength() / (Math.min(this.vehicleMaximumVelocity, this.net.getLinks().get(tempStopsToBeServed.get(0).getLinkId()).getFreespeed()) * this.planningSpeedFactor));
		
		// different from {@link ComplexCircleScheduleProvider}
		if(isSameStopSequenceAsLastIteration)	{
			if(tempStopsToBeServed.get(0).equals(this.handler.getServedStopsInLastIteration(routeID, stops.size())))	{
				double runningTimeMod = modifyRunningTimeAccordingToTheLastIterationIfPossible(runningTime, this.handler.getOffsetForRouteAndStopNumber(routeID, stops.size()));
				if (runningTimeMod > runningTime)
					runningTime = runningTimeMod;
			}
		}
		
		routeStop = this.scheduleWithStopsOnly.getFactory().createTransitRouteStop(tempStopsToBeServed.get(0), runningTime, runningTime + getMinStopTime(capacity));
		routeStop.setAwaitDepartureTime(true);
		stops.add(routeStop);
		
		TransitRoute transitRoute = this.scheduleWithStopsOnly.getFactory().createTransitRoute(routeID, route, stops, this.transportMode);
		return transitRoute;
	}



	
	
	public int getMinStopTime(double capacity){
		int minStopTime = (int) (0.2 * capacity + 15);
		return minStopTime;
	}

	@Override
	public TransitStopFacility getRandomTransitStop(int currentIteration){
		return this.randomStopProvider.getRandomTransitStop(currentIteration);
	}
	
	@Override
	public TransitStopFacility drawRandomStopFromList(List<TransitStopFacility> choiceSet) {
		return this.randomStopProvider.drawRandomStopFromList(choiceSet);
	}

	@Override
	public TransitLine createEmptyLineFromOperator(Id<Operator> id) {
		return this.scheduleWithStopsOnly.getFactory().createTransitLine(Id.create(id, TransitLine.class));
	}

	@Override
	public Collection<TransitStopFacility> getAllPStops() {
		return this.scheduleWithStopsOnly.getFacilities().values();
	}
	
	private double modifyRunningTimeAccordingToTheLastIterationIfPossible(double runningTime, double offsetFromLastIteration){
		if (offsetFromLastIteration != -Double.MAX_VALUE) {
			runningTime = offsetFromLastIteration;
		}
		return runningTime;
	}

	@Override
	public String getRandomPVehicle() {
		return this.randomPVehicleProvider.getRandomPVehicle();
	}

	@Override
	public String getSmallestPVehicle() {
		return this.randomPVehicleProvider.getSmallestPVehicle();
	}	
}