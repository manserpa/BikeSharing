package org.matsim.core.events;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public class BikeshareDebug extends Event{

	public static final String EVENT_TYPE = "bikeshare debug";
	
//	private final Id<Link> linkId;
	
	private final String message;
	
	public BikeshareDebug(double time, String message) {
		super(time);
//		this.linkId = linkId;
		this.message = message;
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
	public String getMessage() {
		return this.message;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put(ATTRIBUTE_TIME, Double.toString(super.getTime()));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		attr.put("message",  this.message);
		return attr;
	}

	
}
