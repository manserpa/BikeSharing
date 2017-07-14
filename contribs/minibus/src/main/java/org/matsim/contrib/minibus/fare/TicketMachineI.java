package org.matsim.contrib.minibus.fare;

import java.util.HashSet;

public interface TicketMachineI {

	double getFare(StageContainer stageContainer);
	
	boolean isSubsidized(StageContainer stageContainer);
	
	int getAmountOfSubsidies(StageContainer stageContainer);
	
	void setSubsidizedStops100(HashSet<String> subsidizedStops);
	
	void setSubsidizedStops150(HashSet<String> subsidizedStops);
	
	void setSubsidizedStops225(HashSet<String> subsidizedStops);
	
	void setSubsidizedStops300(HashSet<String> subsidizedStops);
	
	double getPassengerDistanceKilometer(StageContainer stageContainer);

}