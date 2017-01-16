package org.matsim.core.events;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public class BikeshareDebug extends Event{

	public static final String EVENT_TYPE = "bss_leg";
	
//	private final Id<Link> linkId;
	
	private final String fromStationId;
	private final String toStationId;
	private final double bssTime;
	private final double bssDistance;
	
	public BikeshareDebug(double time, String fromStationId, String toStationId, double bssTime, double bssDistance) {
		super(time);
//		this.linkId = linkId;
		this.fromStationId = fromStationId;
		this.toStationId = toStationId;
		this.bssDistance = bssDistance;
		this.bssTime = bssTime;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
/*	
	public Id<Link> getLinkId(){
		return this.linkId;
	}
	*/
	public String getFromStationId() {
		return this.fromStationId;
	}
	
	public String getToStationId() {
		return this.toStationId;
	}
	
	public double getBSSTime() {
		return this.bssTime;
	}
	
	public double getBSSDistance() {
		return this.bssDistance;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put(ATTRIBUTE_TIME, Double.toString(super.getTime()));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		attr.put("fromStation",  this.fromStationId);
		attr.put("toStation",  this.toStationId);
		attr.put("bssDistance",  Double.toString(this.bssDistance));
		attr.put("bssTime",  Double.toString(this.bssTime));
		return attr;
	}

	
}
