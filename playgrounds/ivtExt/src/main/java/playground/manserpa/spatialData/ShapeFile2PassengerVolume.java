package playground.manserpa.spatialData;

import com.vividsolutions.jts.geom.*;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 
 * ShapeFile2PassengerVolume: Reads the event file and returns the total number of passengers served by public transport on each link within a certain area.
 * 
 * Input: shape file, network.xml, transitschedule.xml, events.xml
 * Output: .csv with the linkIds and the associated passengers volumes.
 * 
 * @author manserpa
 * 
 */

public final class ShapeFile2PassengerVolume {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		ShapeFile2PassengerVolume cs = new ShapeFile2PassengerVolume(args[0]);
		
		for(int simulationRun = 1; simulationRun <= 10; simulationRun++)	{
			cs.run(args[1], "Run" + simulationRun + args[2], "Run" + simulationRun + args[3], simulationRun);
		}
		//cs.run(args[1], args[2], args[3]);
		
	}
	
	private ShapeFile2PassengerVolume(String shpFile)	{
		this.factory = new GeometryFactory();
		
		readShapeFile(shpFile);
	}
	
	public void readShapeFile(String shpFile) {
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(shpFile);
		Collection<Geometry> include = new ArrayList<>();
		Collection<Geometry> exclude = new ArrayList<>();
		
		for(SimpleFeature f: features){
			boolean incl = true;
			Geometry g = null;
			for(Object o: f.getAttributes()){
				if(o instanceof Polygon){
					g = (Geometry) o;
				}else if (o instanceof MultiPolygon){
					g = (Geometry) o;
				}
				else if (o instanceof String){
					incl = Boolean.parseBoolean((String) o);
				}
			}
			if(! (g == null)){
				if(incl){
					include.add(g);
				}else{
					exclude.add(g);
				}
			}
		}
		
		this.include = this.factory.createGeometryCollection(include.toArray(new Geometry[include.size()])).buffer(0);
		this.exclude = this.factory.createGeometryCollection(exclude.toArray(new Geometry[exclude.size()])).buffer(0);
	}
	
	private void run(String networkFile, String transitScheduleFile, String eventFile, int simulationRun) throws IOException	{
	
		List<String> transitRoutesInScenario = new ArrayList<>();
		
		HashMap<String, String> driver2vehicle = new HashMap<String, String>();
		HashMap<String, Integer> paxVolumeOnLink = new HashMap<String, Integer>();
		HashMap<String, Double> linkLength = new HashMap<String, Double>();
		HashMap<String, Integer> person2vehicle = new HashMap<String, Integer>();
	    
	    String csvFileTransitLinks = "LinkPassengerVolume" + simulationRun + ".csv";
	    FileWriter writerTransitLinks = new FileWriter(csvFileTransitLinks );
	    
	    CSVUtils.writeLine(writerTransitLinks , Arrays.asList("LinkId", "PAX"), ';');
	    
		try {
			List<String> nodeList = new ArrayList<>(); 
			List<String> linkList = new ArrayList<>(); 
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("node"))	{
						
						if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
							
							nodeList.add(attributes.getValue("id"));
							
						}
					}
					
					if(qName.equalsIgnoreCase("link"))	{
						
						if(nodeList.contains(attributes.getValue("from")) && nodeList.contains(attributes.getValue("to")))	{
							
							linkList.add(attributes.getValue("id"));
							linkLength.put(attributes.getValue("id"), Double.parseDouble(attributes.getValue("length")));
						
						}
					}				
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				String transitLine;
				String transitRoute;
				boolean isInScenario = true;
				boolean getMode = false;
				String transitMode;
				boolean isParatransit = true;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitLine"))	{
						transitLine = attributes.getValue("id");
					}
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
						transitRoute = attributes.getValue("id");
						isInScenario = true;
						if (!transitRoute.contains("para"))
							isParatransit = false;
						else
							isParatransit = true;
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						getMode = true; 
					}	
					
					if(qName.equalsIgnoreCase("link"))	{
						
						if(linkList.contains(attributes.getValue("refId")) && isInScenario)	{
							isInScenario = true;
						}
						else	{
							isInScenario = false;
						}	
					}				
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transportMode")) {
			        	getMode = false;
			        }
					
			        if(qName.equals("transitRoute")) {
			        	if(isParatransit)	{
			        	//if(!transitMode.equals("pt"))	{
			        		// ArrayList containing all the IDs
			        		transitRoutesInScenario.add(transitRoute);
			        		System.out.println("Line: " + transitLine + "; Route: " + transitRoute + "; Mode: " + transitMode);	
			        	}
			        }
			    }
				
				 public void characters(char[] ch, int start, int length) throws SAXException {
				        if (getMode) {
				            transitMode = new String(ch, start, length);
				        }
				 }
			};
			
			DefaultHandler handler3 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("TransitDriverStarts"))	{
							
							if(transitRoutesInScenario.contains(attributes.getValue("transitRouteId")))	{
								// put the vehicle in the map
								driver2vehicle.put(attributes.getValue("driverId"),attributes.getValue("vehicleId"));
								
								// first, no person is in the bus
								person2vehicle.put(attributes.getValue("vehicleId"), 0);
							}
						}
						
						if(attributes.getValue("type").equals("PersonEntersVehicle"))	{
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{
								
								// number of agents in a vehicle
								if (!driver2vehicle.containsKey(attributes.getValue("person")))	{
									person2vehicle.put(attributes.getValue("vehicle"), person2vehicle.get(attributes.getValue("vehicle")) + 1);
								}
							}
						}
						
						if(attributes.getValue("type").equals("PersonLeavesVehicle"))	{
							// an agent (not the driver) is leaving the bus
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{					
								if (!driver2vehicle.containsKey(attributes.getValue("person")))	{
									person2vehicle.put(attributes.getValue("vehicle"), person2vehicle.get(attributes.getValue("vehicle")) - 1);
								}
							}
							
							// the driver leaves the bus
							if (driver2vehicle.containsKey(attributes.getValue("person")))	{
								// driver can be removed from the map
								driver2vehicle.remove(attributes.getValue("person"));
								
								person2vehicle.remove(attributes.getValue("vehicle"));
							}
						}
						
						if(attributes.getValue("type").equals("left link"))	{
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{
								
								if(linkLength.get(attributes.getValue("link")) != null)	{
									
									int agentsInVehicle = person2vehicle.get(attributes.getValue("vehicle"));
									paxVolumeOnLink.put(attributes.getValue("link"), paxVolumeOnLink.getOrDefault(attributes.getValue("link"), 0) + 
											agentsInVehicle);
								}
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
				
			    }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);
			
			saxParser.parse(eventFile, handler3);
			System.out.println("Handled Run 1 ");
			
			
			List<Map.Entry<String, Integer>> paxVolumeOnLinkSorted =
	                new LinkedList<Map.Entry<String, Integer>>(paxVolumeOnLink.entrySet());
			
			Collections.sort(paxVolumeOnLinkSorted, new Comparator<Map.Entry<String, Integer>>() {
	            public int compare(Map.Entry<String, Integer> o1,
	                               Map.Entry<String, Integer> o2) {
	                return (o1.getValue()).compareTo(o2.getValue());
	            }
	        });
			
			for(Entry<String, Integer> linkId: paxVolumeOnLinkSorted)	{
				if (linkId.getValue() > 0)	{
					try {
						CSVUtils.writeLine(writerTransitLinks, Arrays.asList(linkId.getKey(), Integer.toString(linkId.getValue())), ';');
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
	        writerTransitLinks.flush();
	        writerTransitLinks.close();
			
		} catch (Exception e)	{
			e.printStackTrace();
		}
	
	}
	
	private boolean nodeInServiceArea(double x, double y) {
		Coordinate coord = new Coordinate(x, y);
		Point p = factory.createPoint(coord);
		if(this.include.contains(p)){
			if(exclude.contains(p)){
				return false;
			}
			return true;
		}
		return false;
	}
}