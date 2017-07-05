package playground.manserpa.results;

import com.vividsolutions.jts.geom.*;

import playground.manserpa.spatialData.CSVUtils;

import org.apache.commons.math.stat.descriptive.rank.Max;
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

public final class AccessibilityServedStops {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		AccessibilityServedStops cs = new AccessibilityServedStops(args[0]);
		
		ArrayList<String> transitSchedules = new ArrayList<String>();
		
		for(int i = 1; i < 11; i++){
			transitSchedules.add("Run"+i+"/it.600/transitSchedule.xml");
		}
		cs.run(transitSchedules, args[2]);
		
	}
	
	private AccessibilityServedStops(String shpFile)	{
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
	
	private void run(ArrayList<String> transitSchedule, String facilities) throws IOException	{
		
		HashSet<String> servedStops = new HashSet<>(); 
		HashMap<String, Coord> stopList = new HashMap<>();
		HashMap<String, Integer> householdClassification = new HashMap<>();
		
		householdClassification.put("classA", 0);
		householdClassification.put("classB", 0);
		householdClassification.put("classC", 0);
		householdClassification.put("classD", 0);
		householdClassification.put("classE", 0);
	
		String csvFile = "StopsAccessibility.csv";
	    FileWriter writer = new FileWriter(csvFile);
	    
	    CSVUtils.writeLine(writer, Arrays.asList("0-300", "300-500","500-750","750-1000",">1000"), ';');
	    
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			DefaultHandler handler = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("stopFacility"))	{
						
						if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{

							Coord thisStop = new Coord(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y")));
							stopList.put(attributes.getValue("id"), thisStop);
							
						}
					}
					if(qName.equalsIgnoreCase("stop"))	{
						if (stopList.containsKey(attributes.getValue("refId")))	{
							servedStops.add(attributes.getValue("refId"));
						}
					}
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("facility"))	{
						
						if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))) && 
								attributes.getValue("desc") != null)	{
							
							Coord householdCoord = new Coord(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y")));
							
							String[] facilityType = attributes.getValue("desc").split(",");
							
							if(attributes.getValue("desc").contains("Home"))	{
								int numberOfHouseholds = facilityType.length;
								
								double minDistance = Double.MAX_VALUE;
								
								for(String stop: servedStops)	{
									double euclideanDistance = CoordUtils.calcEuclideanDistance(householdCoord, stopList.get(stop));
									if (euclideanDistance < 300)	{
										minDistance = euclideanDistance;
										break;
									}
									if (euclideanDistance < minDistance)	{
										minDistance = euclideanDistance;
									}
								}
								
								if(minDistance < 300)	{
									householdClassification.put("classA", householdClassification.get("classA") + numberOfHouseholds);
								}
								else if (minDistance >= 300 && minDistance < 500)	{
									householdClassification.put("classB", householdClassification.get("classB") + numberOfHouseholds);
								}
								else if (minDistance >= 500 && minDistance < 750)	{
									householdClassification.put("classC", householdClassification.get("classC") + numberOfHouseholds);
								}
								else if (minDistance >= 750 && minDistance < 1000)	{
									householdClassification.put("classD", householdClassification.get("classD") + numberOfHouseholds);
								}
								else if (minDistance >= 1000)	{
									householdClassification.put("classE", householdClassification.get("classE") + numberOfHouseholds);
								}
								
								
							}
						}
					}
				}
			};
			
			for(String i : transitSchedule)	{
				saxParser.parse(i, handler);
			}
			saxParser.parse(facilities, handler2);
			
			double totalHouseholds = householdClassification.get("classA") + householdClassification.get("classB") + 
					householdClassification.get("classC") + householdClassification.get("classD") + householdClassification.get("classE");
			
			double percentageA = householdClassification.get("classA") / totalHouseholds;
			double percentageB = ( householdClassification.get("classA") + householdClassification.get("classB") ) / totalHouseholds;
			double percentageC = ( householdClassification.get("classA") + householdClassification.get("classB") + householdClassification.get("classC") ) / totalHouseholds;
			double percentageD = ( householdClassification.get("classA") + householdClassification.get("classB") + householdClassification.get("classC") +
					householdClassification.get("classD") ) / totalHouseholds;
			double percentageE = ( householdClassification.get("classA") + householdClassification.get("classB") + householdClassification.get("classC") +
					householdClassification.get("classD") + householdClassification.get("classE")) / totalHouseholds;
			
			System.out.println(percentageA);
			System.out.println(percentageB);
			System.out.println(percentageC);
			System.out.println(percentageD);
			System.out.println(percentageE);
			
			try {
				CSVUtils.writeLine(writer, Arrays.asList(Double.toString(percentageA), Double.toString(percentageB),
						Double.toString(percentageC),Double.toString(percentageD),Double.toString(percentageE)), ';');
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			writer.flush();
	        writer.close();
			
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