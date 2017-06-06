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

public final class VehKmCarAndAV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		VehKmCarAndAV cs = new VehKmCarAndAV(args[0]);
		
		cs.run(args[1], args[2], args[3]);
		
	}
	
	private VehKmCarAndAV(String shpFile)	{
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
		
		List<String> transitVehicles = new ArrayList<>();
		List<String> taxiVehicles = new ArrayList<>();
		List<String> privateCars = new ArrayList<>();
		
		HashSet<String> personList = new HashSet<String>();
		
		HashMap<String, Double> linkLength = new HashMap<String, Double>();
		
		try {
			List<String> nodeList = new ArrayList<>(); 
			
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
							linkLength.put(attributes.getValue("id"), Double.parseDouble(attributes.getValue("length")));
						}
					}				
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					if(qName.equalsIgnoreCase("departure"))	{
						transitVehicles.add(attributes.getValue("vehicleRefId"));
					}			
				}
			};
			
			DefaultHandler handler3 = new DefaultHandler()	{
				
				double privateCarKm = 0.0;
				double taxiAVKm = 0.0;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("AVVehicleAssignment"))	{
							taxiVehicles.add(attributes.getValue("vehicle"));
						}
						
						if(attributes.getValue("type").equals("departure"))	{
							if(attributes.getValue("legMode").equals("car"))	{
								personList.add(attributes.getValue("person"));
							}
						}
						
						if(attributes.getValue("type").equals("PersonEntersVehicle"))	{
							if(personList.contains(attributes.getValue("person")) && !transitVehicles.contains(attributes.getValue("vehicle"))
									&& !taxiVehicles.contains(attributes.getValue("vehicle")))	{
								// must be a private car -> put in the private car list and remove the person from the list
								privateCars.add(attributes.getValue("vehicle"));
								personList.remove(attributes.getValue("person"));
							}
						}
						
						if(attributes.getValue("type").equals("PersonLeavesVehicle"))	{
							if(privateCars.contains(attributes.getValue("vehicle")))	{
								privateCars.remove(attributes.getValue("vehicle"));
							}
						}
						
						if(attributes.getValue("type").equals("left link"))	{
							// differentiate between private car and AV taxi			
							if(privateCars.contains(attributes.getValue("vehicle")) && linkLength.containsKey(attributes.getValue("link")))	{
								privateCarKm += linkLength.get(attributes.getValue("link")) / 1000;
							}
							
							if(taxiVehicles.contains(attributes.getValue("vehicle")) && linkLength.containsKey(attributes.getValue("link")))	{
								taxiAVKm += linkLength.get(attributes.getValue("link"))  / 1000;
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("events")) {

			        	System.out.println(privateCarKm);
			        	System.out.println(taxiAVKm);
			        }
			    }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);
			saxParser.parse(eventFile, handler3);
			
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