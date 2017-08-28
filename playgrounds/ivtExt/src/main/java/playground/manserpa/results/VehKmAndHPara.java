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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * 
 * Purpose: analysis from the bus operator's perspective (vehicle kilometers etc.)
 * 
 * Input: shape file, network.xml, transitSchedule.xml, events.xml
 * Output: a .csv with the statistics
 * 
 * @author manserpa
 * 
 */

public final class VehKmAndHPara {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		VehKmAndHPara cs = new VehKmAndHPara(args[0]);
		
//		for(int simulationRun = 1; simulationRun <= 9; simulationRun++)	{
//			cs.run(args[1], "Run" + simulationRun + args[2], "Run" + simulationRun + args[3], simulationRun);
//		}
//		cs.run(args[1], args[2], args[3]);
		cs.run(args[1], "Run" + 10 + args[2], "Run" + 10 + args[3], 10);	
	}
	
	private VehKmAndHPara(String shpFile)	{
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
		HashMap<String, Double> vehicle2starttime = new HashMap<String, Double>();
		HashMap<String, Double> linkLength = new HashMap<String, Double>();
		HashMap<String, Double> vehicle2distance = new HashMap<String, Double>();
		HashMap<String, Integer> vehicle2numberOfPax = new HashMap<String, Integer>();
		HashMap<String, Integer> person2vehicle = new HashMap<String, Integer>();
		HashMap<String, Double> vehicle2personKm = new HashMap<String, Double>();
	    
	    String csvFileTransitLinks = "VehicleStats" + simulationRun + ".csv";
	    FileWriter writerTransitLinks = new FileWriter(csvFileTransitLinks );
	    
	    CSVUtils.writeLine(writerTransitLinks , Arrays.asList("Total Time [h]", "Total Distance [km]", "Total Number of Pax", "Total Number of PaxDistance [km]"), ';');
	    
	    String csvFilePaxKM = "PAXkmDistributionNotNeeded" + simulationRun + ".csv";
	    FileWriter writerPaxKM = new FileWriter(csvFilePaxKM);
	    
	    CSVUtils.writeLine(writerPaxKM , Arrays.asList("PaxKM"), ';');
	    
	    
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
						
						linkLength.put(attributes.getValue("id"), Double.parseDouble(attributes.getValue("length")));
						
						if(nodeList.contains(attributes.getValue("from")) && nodeList.contains(attributes.getValue("to")))	{
							
							linkList.add(attributes.getValue("id"));
						
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
				boolean crossesScenario = false;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitLine"))	{
						transitLine = attributes.getValue("id");
					}
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
						transitRoute = attributes.getValue("id");
						isInScenario = true;
						crossesScenario = false;
						if (!transitRoute.contains("para"))
							isParatransit = false;
						else
							isParatransit = true;
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						getMode = true; 
					}	
					
					if(qName.equalsIgnoreCase("link"))	{
						if(linkList.contains(attributes.getValue("refId"))) {
							crossesScenario = true;
						}
						
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
			        	//if(!transitMode.equals("pt") && crossesScenario)	{
			        	if(isParatransit)	{
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
				
				double totalDriveTime = 0.0;
				double totalDistance = 0.0;
				int totalNumberOfPax = 0;
				double totalNumberOfPaxKm = 0.0;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("TransitDriverStarts"))	{
							
							if(transitRoutesInScenario.contains(attributes.getValue("transitRouteId")))	{
								// put the vehicle in the map
								driver2vehicle.put(attributes.getValue("driverId"),attributes.getValue("vehicleId"));
								vehicle2starttime.put(attributes.getValue("vehicleId"), Double.parseDouble(attributes.getValue("time")));
								//System.out.println("Driver departs: " + attributes.getValue("driverId"));
								//System.out.println("Vehicle departs: " + attributes.getValue("vehicleId"));
								vehicle2distance.put(attributes.getValue("vehicleId"), 0.0);
								
								// first person is the driver
								vehicle2numberOfPax.put(attributes.getValue("vehicleId"), -1);
								
								// first, no person is in the bus
								person2vehicle.put(attributes.getValue("vehicleId"), 0);
								vehicle2personKm.put(attributes.getValue("vehicleId"), 0.0);
							}
						}
						
						if(attributes.getValue("type").equals("PersonEntersVehicle"))	{
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{
								vehicle2numberOfPax.put(attributes.getValue("vehicle"), vehicle2numberOfPax.get(attributes.getValue("vehicle")) + 1);
								
								if (!driver2vehicle.containsKey(attributes.getValue("person")))	{
									person2vehicle.put(attributes.getValue("vehicle"), person2vehicle.get(attributes.getValue("vehicle")) + 1);
								}
							}
						}
						
						if(attributes.getValue("type").equals("PersonLeavesVehicle"))	{
							// the driver is leaving the bus
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{					
								if (!driver2vehicle.containsKey(attributes.getValue("person")))	{
									person2vehicle.put(attributes.getValue("vehicle"), person2vehicle.get(attributes.getValue("vehicle")) - 1);
								}
							}
							
							if (driver2vehicle.containsKey(attributes.getValue("person")))	{
								double startTime = vehicle2starttime.get(driver2vehicle.get(attributes.getValue("person")));
								double driveTime = Double.parseDouble(attributes.getValue("time")) - startTime;
								totalDriveTime += driveTime;
								
								vehicle2starttime.remove(driver2vehicle.get(attributes.getValue("person")));
								driver2vehicle.remove(attributes.getValue("person"));
								
								double distance = vehicle2distance.get(attributes.getValue("vehicle"));
								totalDistance += vehicle2distance.get(attributes.getValue("vehicle"));
								vehicle2distance.remove(attributes.getValue("vehicle"));
								
								int numberOfPax = vehicle2numberOfPax.get(attributes.getValue("vehicle"));
								totalNumberOfPax += numberOfPax;
								vehicle2numberOfPax.remove(attributes.getValue("vehicle"));
								
								person2vehicle.remove(attributes.getValue("vehicle"));
								double paxDistance = vehicle2personKm.get(attributes.getValue("vehicle"));
								try {
									CSVUtils.writeLine(writerPaxKM, Arrays.asList(String.valueOf(paxDistance)), ';');
								} catch (IOException e) {
									e.printStackTrace();
								}
								totalNumberOfPaxKm += vehicle2personKm.get(attributes.getValue("vehicle"));
								vehicle2personKm.remove(attributes.getValue("vehicle"));
								
								/*
								System.out.println("Handled Departure; Vehicle: " + attributes.getValue("vehicle") + "; Distance Driven: " + distance / 1000);
								System.out.println("Time Driven: " + driveTime / 3600);
								System.out.println("Number of Pax: " + numberOfPax);
								System.out.println("Number of PaxDistance: " + paxDistance / 1000);
								*/
							}
						}
						
						if(attributes.getValue("type").equals("left link"))	{
							if (driver2vehicle.containsValue(attributes.getValue("vehicle")))	{
								
								//if(linkLength.get(attributes.getValue("link")) != null)	{
									double distance = linkLength.get(attributes.getValue("link"));
									
									vehicle2distance.put(attributes.getValue("vehicle"), vehicle2distance.get(attributes.getValue("vehicle")) + distance);	
									
									double paxKilometer = distance * person2vehicle.get(attributes.getValue("vehicle"));
									
									vehicle2personKm.put(attributes.getValue("vehicle"), vehicle2personKm.get(attributes.getValue("vehicle")) + paxKilometer);
								//}
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("events")) {
			        	try {
							CSVUtils.writeLine(writerTransitLinks, Arrays.asList(String.valueOf(totalDriveTime / 3600), String.valueOf(totalDistance / 1000),
									String.valueOf(totalNumberOfPax),String.valueOf(totalNumberOfPaxKm / 1000)), ';');
						} catch (IOException e) {
							e.printStackTrace();
						}
			        	System.out.println("Total Time [h]: " + totalDriveTime / 3600);
			        	System.out.println("Total Distance [km]: " + totalDistance / 1000);
			        	System.out.println("Total Number of Pax: " + totalNumberOfPax);
			        	System.out.println("Total Number of PaxDistance [km]: " + totalNumberOfPaxKm / 1000);
			        }
			    }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);
			saxParser.parse(eventFile, handler3);
			
	        writerTransitLinks.flush();
	        writerTransitLinks.close();
	        
	        
	        writerPaxKM.flush();
	        writerPaxKM.close();
			
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