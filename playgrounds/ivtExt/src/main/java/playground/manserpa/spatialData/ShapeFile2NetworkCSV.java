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
 * Purpose: Gives a .csv with all linkIds within a certain area (shape file)
 * 
 * Input: shape file, network.xml
 * Output: .csv with linkIDs
 * 
 * @author manserpa
 * 
 */

public final class ShapeFile2NetworkCSV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ShapeFile2NetworkCSV cs = new ShapeFile2NetworkCSV(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private ShapeFile2NetworkCSV(String shpFile)	{
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
	
	private void run(String networkFile) throws IOException	{
	
		String csvFileNodes = "NodesZH.csv";
	    FileWriter writerNodes = new FileWriter(csvFileNodes);
	    
	    CSVUtils.writeLine(writerNodes, Arrays.asList("id", "x", "y"), ';');
	    
	    String csvFileLinks = "LinksZH.csv";
	    FileWriter writerLinks = new FileWriter(csvFileLinks );
	    
	    CSVUtils.writeLine(writerLinks , Arrays.asList("id", "from","to","length","modes","freespeed"), ';');
	    
		try {
			List<String> nodeList = new ArrayList<>(); 
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
				
				if(qName.equalsIgnoreCase("node"))	{
					
					if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
						
						try {
							CSVUtils.writeLine(writerNodes, Arrays.asList(attributes.getValue("id"), attributes.getValue("x"), attributes.getValue("y")), ';');
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						nodeList.add(attributes.getValue("id"));
						
					}
				}
				
				if(qName.equalsIgnoreCase("link"))	{
					
					if(nodeList.contains(attributes.getValue("from")) && nodeList.contains(attributes.getValue("to")))	{
						
						try {
							CSVUtils.writeLine(writerLinks, Arrays.asList(attributes.getValue("id"), attributes.getValue("from"), attributes.getValue("to"),
									attributes.getValue("length"), attributes.getValue("modes"), attributes.getValue("freespeed")), ';');
						} catch (IOException e) {
							e.printStackTrace();
						}
					
					}
				}
				
			}
			};
			
			saxParser.parse(networkFile, handler);
			
			writerNodes.flush();
	        writerNodes.close();
	        
	        writerLinks.flush();
	        writerLinks.close();
			
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