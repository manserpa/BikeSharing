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

public final class ServedStopsInShapeFile {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ServedStopsInShapeFile cs = new ServedStopsInShapeFile(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private ServedStopsInShapeFile(String shpFile)	{
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
	
	private void run(String transitSchedule) throws IOException	{
		
		HashSet<String> servedStops = new HashSet<>(); 
		HashMap<String, Coordinates> stopList = new HashMap<>();
	
		String csvFile = "ServedStops.csv";
	    FileWriter writer = new FileWriter(csvFile);
	    
	    CSVUtils.writeLine(writer, Arrays.asList("id", "x", "y"), ';');
	    
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			DefaultHandler handler = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("stopFacility"))	{
						
						if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
							/*
							try {
								CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(counter), attributes.getValue("id"), attributes.getValue("x"), 
										attributes.getValue("y"), attributes.getValue("name"), attributes.getValue("linkRefId")), ';');
							} catch (IOException e) {
								e.printStackTrace();
							}
							*/
							Coordinates thisStop = new Coordinates(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y")));
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
			
			saxParser.parse(transitSchedule, handler);
			
			for(String i: servedStops)	{
				Coordinates thisStop = stopList.get(i);
				System.out.println(i + "; x: " + thisStop.x + "; y: "+ thisStop.y);
				
				
				try {
					CSVUtils.writeLine(writer, Arrays.asList(i, Double.toString(thisStop.x), Double.toString(thisStop.y)), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			System.out.println(servedStops.size());
			
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

class Coordinates	{
	double x;
	double y;
	
	public Coordinates(double x, double y)	{
		this.x = x;
		this.y = y;
	}
}