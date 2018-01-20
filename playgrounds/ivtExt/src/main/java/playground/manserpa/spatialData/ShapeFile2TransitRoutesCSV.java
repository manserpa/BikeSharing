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
 * Purpose: Returns a csv with all linkIds served by public transport
 * 
 * Input: shape file, network.xml and transitschedule.xml
 * Output: .csv containing all links served by public transport
 * 
 * @author manserpa
 * 
 */

public final class ShapeFile2TransitRoutesCSV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ShapeFile2TransitRoutesCSV cs = new ShapeFile2TransitRoutesCSV(args[0]);
		
		//for(int simulationRun = 1; simulationRun <= 6; simulationRun++)	{
		//	cs.run(args[1], "Run" + simulationRun + args[2], simulationRun);
		//}
		cs.run(args[1], args[2], 1);
		
	}
	
	private ShapeFile2TransitRoutesCSV(String shpFile)	{
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
			for(Object o: f.getAttributes())	{
				if(o instanceof Polygon)	{
					g = (Geometry) o;
				}else if (o instanceof MultiPolygon)	{
					g = (Geometry) o;
				}
				else if (o instanceof String)	{
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
	
	private void run(String networkFile, String transitScheduleFile, int simulationRun) throws IOException	{
	
		LinkedHashSet<String> transitSet = new LinkedHashSet<String>();
	
		String csvFileNodes = "NodesZH" + simulationRun + ".csv";
	    FileWriter writerNodes = new FileWriter(csvFileNodes);
	    
	    CSVUtils.writeLine(writerNodes, Arrays.asList("id", "x", "y"), ';');
	    
	    String csvFileLinks = "LinksZH" + simulationRun + ".csv";
	    FileWriter writerLinks = new FileWriter(csvFileLinks );
	    
	    CSVUtils.writeLine(writerLinks , Arrays.asList("id", "from","to","length","modes","freespeed"), ';');
	    
	    String csvFileTransitLinks = "TransitLinksSubsidized" + simulationRun + ".csv";
	    FileWriter writerTransitLinks = new FileWriter(csvFileTransitLinks );
	    
	    CSVUtils.writeLine(writerTransitLinks , Arrays.asList("TransitLine", "TransitMode","LinkId"), ';');
	    
		try {
			List<String> nodeList = new ArrayList<>(); 
			List<String> linkList = new ArrayList<>(); 
			
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
							
							linkList.add(attributes.getValue("id"));
						
						}
					}
									
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				String transitLine;
				String transitMode;
				String transitRoute;
				boolean isTransitRoute = true;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitLine"))	{
						transitLine = attributes.getValue("id");
					}
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
						transitRoute = attributes.getValue("id");
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						isTransitRoute = true; 
					}	
					
					if(qName.equalsIgnoreCase("link"))	{
						//if(linkList.contains(attributes.getValue("refId")) && transitLine.contains("para") && 
						//		transitRoute.equals("para_292_220-417_42"))	{
						if(linkList.contains(attributes.getValue("refId")) && transitLine.contains("para"))	{
						//if(linkList.contains(attributes.getValue("refId")) && !transitMode.equals("pt"))	{
							
							transitSet.add(transitLine + "===" + transitMode + "===" + attributes.getValue("refId"));
							
						}
							
					}				
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transportMode")) {
			        	isTransitRoute = false;
			        }
			        
			    }
				
				 public void characters(char[] ch, int start, int length) throws SAXException {
				        if (isTransitRoute) {
				            transitMode = new String(ch, start, length);
				        }
				 }
				 
			};
			
			saxParser.parse(networkFile, handler);
			
			saxParser.parse(transitScheduleFile, handler2);
			
			for(String k: transitSet)	{
				
				String[] parts = k.split("===");
				
				try {
					CSVUtils.writeLine(writerTransitLinks, Arrays.asList(parts[0], parts[1], parts[2]), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			writerNodes.flush();
	        writerNodes.close();
	        
	        writerLinks.flush();
	        writerLinks.close();
	        
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