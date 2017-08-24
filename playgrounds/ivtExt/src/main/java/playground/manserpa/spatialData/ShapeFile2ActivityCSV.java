package playground.manserpa.spatialData;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * purpose: search in the plan file for activities that the agents perform during the day (selected plans only)
 * input: shape file and plan file
 * output: a .csv containing all activities and the coordinates
 * 
 */


public class ShapeFile2ActivityCSV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ShapeFile2ActivityCSV cs = new ShapeFile2ActivityCSV(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private ShapeFile2ActivityCSV(String shpFile)	{
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
		
	
	public void run(String planFile)  throws IOException {
		
		List<ActivityList> activityList = new ArrayList<>();
		
		
		String csvFile = "ActivitiesZH.csv";
        FileWriter writer = new FileWriter(csvFile);

        CSVUtils.writeLine(writer, Arrays.asList("type", "x", "y"), ';');
        
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
				boolean selected = false;
				String person;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("person"))	{
						person = attributes.getValue("id");
					}
					
					if(qName.equalsIgnoreCase("plan"))	{
						if (attributes.getValue("selected").equals("yes")) {
							selected = true;
						}
						else	{
							selected = false;
						}
					}
					
					else if(qName.equalsIgnoreCase("act") && selected)	{
						
						if(!attributes.getValue("type").equals("pt interaction"))	{
							
							ActivityList thisActivity = new ActivityList(attributes.getValue("type"),Double.parseDouble(attributes.getValue("x")),
									Double.parseDouble(attributes.getValue("y")));
							if(activityInServiceArea(thisActivity.x, thisActivity.y))
								activityList.add(thisActivity);
							
						}
					}
				}
				
				public void endElement(String uri, String localName, String qName) throws SAXException {
					
			        if(qName.equals("plan") && selected) {
				        	
				       	for(ActivityList s: activityList)	{
					       	try {	
								CSVUtils.writeLine(writer, Arrays.asList(s.type, Double.toString(s.x), Double.toString(s.y)), ';');
							} catch (IOException e) {
								e.printStackTrace();
							}
				        }
			        	
				        activityList.clear();
			        
			        }
			    }
			};
			
			saxParser.parse(planFile, handler);
			
			writer.flush();
	        writer.close();
			
		} catch (Exception e)	{
			e.printStackTrace();
		}
			
	}
	
	private boolean activityInServiceArea(double x, double y) {
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

class ActivityList	{
	String type;
	double x;
	double y;
	
	public ActivityList(String type, double x, double y)	{
		this.type = type;
		this.x = x;
		this.y = y;
	}
}
