package playground.manserpa.results;

import com.vividsolutions.jts.geom.*;

import playground.manserpa.spatialData.CSVUtils;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
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

public final class TripLength {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		TripLength cs = new TripLength(args[0]);
		
		cs.run(args[1],args[2], args[3]);
		
	}
	
	private TripLength(String shpFile)	{
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
	
	private void run(String networkFile, String eventFileNoAVs, String eventFileAVs) throws IOException	{
		
		String csvFile = "TripTime.csv";
	    FileWriter writer = new FileWriter(csvFile);
	    
	    CSVUtils.writeLine(writer, Arrays.asList("StartLinkId", "TripTimePT", "TripTimeAV"), ';');
		
	    String csvFile2 = "TripSpatialTime.csv";
	    FileWriter writerSpatial = new FileWriter(csvFile2);
	    
	    CSVUtils.writeLine(writerSpatial, Arrays.asList("x", "y", "TimeReduction","ModeBefore","ModeAfter"), ';');
	    
	    ArrayList<Double> tripTimebefore = new ArrayList<Double>();
	    ArrayList<Double> tripTimeafter = new ArrayList<Double>();
	    
		HashMap<String, String> person2trip = new HashMap<String, String>();
		HashMap<String, String> person2PTTrip = new HashMap<String, String>();
		
		HashMap<String, String> person2AVtrip = new HashMap<String, String>();
	    
		List<String> nodeList = new ArrayList<>(); 
		List<String> linkList = new ArrayList<>(); 
		HashMap<String, Coord> link2Coords = new HashMap<String, Coord>();
		HashMap<String, Coordes> node2Coords = new HashMap<String, Coordes>();
		
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler1 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("node"))	{
						
						if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
							nodeList.add(attributes.getValue("id"));
							
							Coordes thisNode = new Coordes(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y")));
							node2Coords.put(attributes.getValue("id"), thisNode);
						}
					}
					
					if(qName.equalsIgnoreCase("link"))	{
						
						if(nodeList.contains(attributes.getValue("from")) && nodeList.contains(attributes.getValue("to")))	{
							linkList.add(attributes.getValue("id"));
							
							Coord thisLink = new Coord((node2Coords.get(attributes.getValue("from")).x
									+ node2Coords.get(attributes.getValue("to")).x)/2, (node2Coords.get(attributes.getValue("from")).y
											+ node2Coords.get(attributes.getValue("to")).y)/2);
							link2Coords.put(attributes.getValue("id"), thisLink);
						}
					}				
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("actend") && linkList.contains(attributes.getValue("link")))	{
							if(!attributes.getValue("actType").equals("pt interaction"))	{
								person2trip.put(attributes.getValue("person"), attributes.getValue("actType")+"==="+attributes.getValue("link")+
										"==="+attributes.getValue("time"));
							}
						}
						
						
						if(attributes.getValue("type").equals("departure") && person2trip.containsKey(attributes.getValue("person")))	{
							//if(!attributes.getValue("legMode").equals("car"))
							//if(!attributes.getValue("legMode").equals("pt") && !attributes.getValue("legMode").equals("transit_walk"))
								//person2trip.remove(attributes.getValue("person"));
							person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person"))+"==="+attributes.getValue("legMode"));
						}
						
						
						// schauen, dass Ende auch in Service Area
						if(attributes.getValue("type").equals("actstart") && linkList.contains(attributes.getValue("link")) && person2trip.containsKey(attributes.getValue("person")))	{
							//process the trip
							if(!attributes.getValue("actType").equals("pt interaction"))	{
								
								String[] personTrip = person2trip.get(attributes.getValue("person")).split("===");
								double tripTime = Double.parseDouble(attributes.getValue("time")) - Double.parseDouble(personTrip[2]);
								// double distance = distance aus person2trip zu attributes.getValue("link")
								// mit getEuclidianDistance
								double tripDistance = CoordUtils.calcEuclideanDistance(link2Coords.get(personTrip[1]),link2Coords.get(attributes.getValue("link")));
								person2PTTrip.put(attributes.getValue("person")+"==="+personTrip[0]+"==="+attributes.getValue("actType"),
										tripTime+"==="+personTrip[1]+"==="+personTrip[3]+"==="+tripDistance);
								
								person2trip.remove(attributes.getValue("person"));
							}
						}
						
					}		
				}
			};
			
			DefaultHandler handler3 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("actend") && linkList.contains(attributes.getValue("link")))	{
							if(!attributes.getValue("actType").equals("pt interaction"))	{
								person2AVtrip.put(attributes.getValue("person"), attributes.getValue("actType")+"==="+attributes.getValue("link")+
										"==="+attributes.getValue("time"));
							}
						}
						
						
						if(attributes.getValue("type").equals("departure") && person2AVtrip.containsKey(attributes.getValue("person")))	{
							person2AVtrip.put(attributes.getValue("person"), person2AVtrip.get(attributes.getValue("person"))+"==="+attributes.getValue("legMode"));
						}
						
						if(attributes.getValue("type").equals("actstart") && linkList.contains(attributes.getValue("link")) && person2AVtrip.containsKey(attributes.getValue("person")))	{
							//process the trip
							if(!attributes.getValue("actType").equals("pt interaction"))	{
								
								String[] personTrip = person2AVtrip.get(attributes.getValue("person")).split("===");
								double tripTime = Double.parseDouble(attributes.getValue("time")) - Double.parseDouble(personTrip[2]);
								
								if(person2PTTrip.containsKey(attributes.getValue("person")+"==="+personTrip[0]+"==="+attributes.getValue("actType")))	{
									// final trip = <StartlinkId, triplengthPT, triplengthAV>
									String mode = personTrip[3];
									
									String[] ptTrip = person2PTTrip.get(attributes.getValue("person")+"==="+personTrip[0]+"==="+attributes.getValue("actType")).split("===");
									
									double decreaseInTravelTime = 0.0;
									//avs decrease the travel time
									if (tripTime < Double.parseDouble(ptTrip[0]))	{
										decreaseInTravelTime = -1 * (Double.parseDouble(ptTrip[0]) - tripTime) / Double.parseDouble(ptTrip[0]);
									}
									else if (tripTime > Double.parseDouble(ptTrip[0])) {
										decreaseInTravelTime = (tripTime - Double.parseDouble(ptTrip[0])) / tripTime;
									}
									
									try {
										CSVUtils.writeLine(writer, Arrays.asList(ptTrip[1], ptTrip[0], Double.toString(tripTime)), ';');
									} catch (IOException e) {
										e.printStackTrace();
									}
									
									//trip distance change
									double tripDistance = CoordUtils.calcEuclideanDistance(link2Coords.get(personTrip[1]),link2Coords.get(attributes.getValue("link")));
									double tripDistanceDifference = (tripDistance - Double.parseDouble(ptTrip[3]));
									
									
									try {
										CSVUtils.writeLine(writerSpatial, Arrays.asList(Double.toString(link2Coords.get(ptTrip[1]).getX()), 
												Double.toString(link2Coords.get(ptTrip[1]).getY()), Double.toString(decreaseInTravelTime),
												ptTrip[2],mode,Double.toString(tripDistanceDifference)), ';');
									} catch (IOException e) {
										e.printStackTrace();
									}
									
									tripTimebefore.add(Double.parseDouble(ptTrip[0]));
									tripTimeafter.add(tripTime);
									
								}
								
								person2AVtrip.remove(attributes.getValue("person"));
							}
						}
						
					}		
				}
			};
			
			saxParser.parse(networkFile, handler1);
			saxParser.parse(eventFileNoAVs, handler2);
			saxParser.parse(eventFileAVs, handler3);
			
			double totAmount = 0.0;
			for(double number:tripTimebefore)	{
				totAmount += number;
			}
			
			System.out.println(totAmount / (tripTimebefore.size() * 60));
			
			totAmount = 0.0;
			for(double number:tripTimeafter)	{
				totAmount += number;
			}
			
			System.out.println(totAmount / (tripTimeafter.size() * 60));
		
	        writer.flush();
	        writer.close();
	        
	        writerSpatial.flush();
	        writerSpatial.close();
			
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