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

public final class AgentsOnRouteCar {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		AgentsOnRouteCar cs = new AgentsOnRouteCar(args[0]);
		
		cs.run(args[1], args[2]);
		
	}
	
	private AgentsOnRouteCar(String shpFile)	{
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
	
	private void run(String networkFile, String eventFile) throws IOException	{
	
		// <event time="9156.0" type="departure" person="10894369" link="422340" legMode="car"  />
		//	<event time="16934.0" type="arrival" person="10448004" link="400522" legMode="car"  />

		HashSet<String> agent2OnRoute = new HashSet<String>();
		HashMap<Integer,Integer> agentsOnRoute = new HashMap<Integer,Integer>();
	    
	    String csvFilewriter = "AgentsOnRouteCar.csv";
	    FileWriter writer = new FileWriter(csvFilewriter );
	    
	    CSVUtils.writeLine(writer , Arrays.asList("TimeSlice","AgentsOnRoute"), ';');
	    
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
						
						}
					}				
				}
			};
			
			DefaultHandler handler3 = new DefaultHandler()	{
				
				int sliceSizeMin = 10; // aggregation level (minutes)
		        int sliceSize = sliceSizeMin * 60;
		        int agentsInAVehicle = 0;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("departure"))	{
							
							if(attributes.getValue("legMode").equals("car"))	{
								// agent is on Route if the trip started in the service area
								if(linkList.contains(attributes.getValue("link")))	{
									agent2OnRoute.add(attributes.getValue("person"));
									int thisSlice = (int) (Double.parseDouble(attributes.getValue("time"))) / sliceSize;
									agentsInAVehicle ++;
									agentsOnRoute.put(thisSlice, agentsInAVehicle);
								}
							}
						}
						
						
						if(attributes.getValue("type").equals("arrival"))	{
							// a person leaves a transit vehicle but it's not the driver
							if (agent2OnRoute.contains(attributes.getValue("person")))	{		
									int thisSlice =  (int) (Double.parseDouble(attributes.getValue("time"))) / sliceSize;
									agentsInAVehicle --;
									agentsOnRoute.put(thisSlice, agentsInAVehicle);
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			    }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(eventFile, handler3);
			
			int sliceSize = 600;
			for ( int i = 0; i < 30 * ( 3600 / sliceSize  ); i++){
	        	// i = 0 ... no. of slices during one day
	            CSVUtils.writeLine(writer, Arrays.asList(String.valueOf(i), String.valueOf(agentsOnRoute.getOrDefault(i,0))), ';');
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