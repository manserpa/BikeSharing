package playground.manserpa.results;

import com.vividsolutions.jts.geom.*;

import playground.manserpa.spatialData.CSVUtils;

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
 * Purpose: returns the shares of the modes within public transport (Bus, Tram, Train)
 * 
 * Input: shape file, transitSchedule.xml, events.xml
 * Output: the shares of these modes in a csv
 * 
 * @author manserpa
 * 
 */


public final class ModeSharesPT {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		ModeSharesPT cs = new ModeSharesPT(args[0]);
		
		cs.run(args[1], args[2], args[3]);
		
	}
	
	private ModeSharesPT(String shpFile)	{
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
	
	private void run(String networkFile, String transitScheduleFile, String eventFile) throws IOException	{
	
		HashMap<String, String> transitRoutes2Mode = new HashMap<String, String>();
		HashMap<String, String> vehicle2person = new HashMap<String, String>();
		HashMap<String, Integer> mode2legs = new HashMap<String, Integer>();
		
		HashMap<String, Double> linkLength = new HashMap<String, Double>();
	    
	    String csvFileTransitLinks = "Nothing.csv";
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
				boolean crossesScenario = false;
				boolean isParatransit = true;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
						transitRoute = attributes.getValue("id");
						crossesScenario = false;
						if (!transitRoute.contains("para"))
							isParatransit = false;
						else
							isParatransit = true;
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						if(isParatransit)	{
							getMode = false;
							transitMode = "bus";
						}
						else	{
							getMode = true; 
						}
					}		
					
					if(qName.equalsIgnoreCase("link"))	{
						if(linkList.contains(attributes.getValue("refId")))	{
							crossesScenario = true;
						}
					}		
					
			        if(qName.equals("departure") && crossesScenario) {
			        	transitRoutes2Mode.put(attributes.getValue("vehicleRefId"), transitMode);
			        }
					
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
			        if(qName.equals("transportMode")) {
			        	getMode = false;
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
						
						if(attributes.getValue("type").equals("PersonEntersVehicle"))	{
							if(!vehicle2person.containsValue(attributes.getValue("vehicle")) && transitRoutes2Mode.containsKey(attributes.getValue("vehicle")))	{
								vehicle2person.put("driver", attributes.getValue("vehicle"));
								// the first person entering is the driver
							}
							else if(vehicle2person.containsValue(attributes.getValue("vehicle")))	{
								// put the agent in a vehicle
								vehicle2person.put(attributes.getValue("person"), attributes.getValue("vehicle"));
							}
						}
						
						if(attributes.getValue("type").equals("PersonLeavesVehicle"))	{
							// an agent (not the driver) is leaving the bus
							if(vehicle2person.containsKey(attributes.getValue("person")))	{
								//process this agent
								String mode = transitRoutes2Mode.get(attributes.getValue("vehicle"));
								mode2legs.put(mode, mode2legs.getOrDefault(mode, 0) + 1);
								vehicle2person.remove(attributes.getValue("person"));
							}
							if(attributes.getValue("person").equals("driver"))	{
								vehicle2person.remove("driver");
								if( vehicle2person.containsValue(attributes.getValue("vehicle")))	{
									System.out.println("elo");
								}
										
							}
						}
					}		
				}
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);
			
			saxParser.parse(eventFile, handler3);
			int totalTrips = 0;
			for(String e : mode2legs.keySet())	{
				totalTrips += mode2legs.get(e);
			}
			
			for(String e : mode2legs.keySet())	{
				double share = ((double) mode2legs.get(e)) / ((double) totalTrips);
				System.out.println("Mode: " + e + "\t share: " + share);
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