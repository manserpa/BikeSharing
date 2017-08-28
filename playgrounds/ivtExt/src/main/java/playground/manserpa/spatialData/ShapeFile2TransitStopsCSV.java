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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


/**
 * 
 * Returns all transit stops within a certain shape file
 * 
 * Input: shapefile, transitschedule.xml
 * Output: .csv with all transit stops and coordinates
 * 
 * @author manserpa
 * 
 */


public final class ShapeFile2TransitStopsCSV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ShapeFile2TransitStopsCSV cs = new ShapeFile2TransitStopsCSV(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private ShapeFile2TransitStopsCSV(String shpFile)	{
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
	
		String csvFile = "TransitStopsZH.csv";
	    FileWriter writer = new FileWriter(csvFile);
	    
	    CSVUtils.writeLine(writer, Arrays.asList("OID", "id", "x", "y","name","linkRefId"), ';');
	    
		try {
			List<String> StopList = new ArrayList<>(); 
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			DefaultHandler handler = new DefaultHandler()	{
	
				int counter = 1;
				boolean hasStopinZurich;
				String transitLineId;
				
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
				
				if(qName.equalsIgnoreCase("stopFacility"))	{
					
					if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
						
						
						try {
							CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(counter), attributes.getValue("id"), attributes.getValue("x"), 
									attributes.getValue("y"), attributes.getValue("name"), attributes.getValue("linkRefId")), ';');
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						counter ++;
						StopList.add(attributes.getValue("id"));
						
					}
				}
				
				
				if(qName.equalsIgnoreCase("transitLine"))	{
					
					transitLineId = attributes.getValue("id");
					hasStopinZurich = false;
					
				}
				
				if(qName.equalsIgnoreCase("stop"))	{
					
					if(StopList.contains(attributes.getValue("refId")))
						hasStopinZurich = true;
					
				}
				
			}
			
			public void endElement(String uri, String localName, String qName)
		            throws SAXException {
				
		        if(qName.equals("transitLine") && hasStopinZurich) {
		        	
		        	System.out.println(transitLineId);
		        	
		        }
		        
		    }
			
			};
			
			saxParser.parse(transitSchedule, handler);
			
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