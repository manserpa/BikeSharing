package playground.manserpa.helper;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public final class DeleteUnusedStopsFromSchedule {
	
	/**
	 * 
	 * Purpose: Many transit lines (buses and trams) in the Zurich Scenario were removed. Because of that, the stop are not needed anymore. This code
	 * creates a new .xml with the stops we actually need
	 * 
	 * Input: transit schedule
	 * Output: A .xml containing stops that are actually needed
	 * 
	 * @author manserpa
	 * 
	 */
	
	// TODO: read in the transitschedule and generate a HashSet containing all stops served
	// then parse the transit stops and write only the ones that we actually need
	
	
	public static void main(String[] args) throws IOException	{
	
		List<TransitStopFacility> formalPTStops = new ArrayList<>();
		HashSet<String> stopsServed = new HashSet<>();
		
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
				
				if(qName.equalsIgnoreCase("stopFacility"))	{
					
					TransitStopFacility thisStop = new TransitStopFacility(attributes.getValue("id"), Double.parseDouble(attributes.getValue("x")),
							Double.parseDouble(attributes.getValue("y")),
							attributes.getValue("linkRefId"), attributes.getValue("name"),  Boolean.parseBoolean(attributes.getValue("isBlocking")));
					formalPTStops.add(thisStop);
					
				}
				
				if(qName.equalsIgnoreCase("stop"))	{
					
					stopsServed.add(attributes.getValue("refId"));
					
				}
				
			}
			};
			
			saxParser.parse(args[0], handler);
			
			Element transitStop = new Element("transitStop");
			
			for(TransitStopFacility formalStop : formalPTStops)	{
					
				if(stopsServed.contains(formalStop.id))	{
					Element stopFacility = new Element("stopFacility");
					stopFacility.setAttribute("id", formalStop.id);
					stopFacility.setAttribute("x", String.valueOf(formalStop.x));
					stopFacility.setAttribute("y", String.valueOf(formalStop.y));
					stopFacility.setAttribute("linkRefId", formalStop.linkRefId);
					stopFacility.setAttribute("name", formalStop.name);
					stopFacility.setAttribute("isBlocking", String.valueOf(formalStop.isBlocking));
							
					transitStop.addContent(stopFacility);
				}
				else	{
					System.out.println(formalStop.name);
				}
				
			}
				
			PrintWriter output = new PrintWriter ("StopFacilities.xml") ;
			Document doc = new Document(transitStop);
		
			XMLOutputter serializer = new XMLOutputter();
		    serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
		    serializer.output(doc, output);
		    
		    output.close() ;
			
		} catch (Exception e)	{
			e.printStackTrace();
		}
	
	}
}
	

class TransitStopFacility	{
	String id;
	double x;
	double y;
	String linkRefId;
	String name;
	boolean isBlocking;
		
	public TransitStopFacility(String id, double x, double y, String linkRefId, String name, boolean isBlocking)	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.linkRefId = linkRefId;
		this.name = name;
		this.isBlocking = isBlocking;
	}
}