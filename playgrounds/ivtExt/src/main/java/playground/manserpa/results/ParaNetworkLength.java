package playground.manserpa.results;

import com.vividsolutions.jts.geom.*;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public final class ParaNetworkLength {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		ParaNetworkLength cs = new ParaNetworkLength(args[0]);
		
		cs.run(args[1], args[2]);
		
	}
	
	private ParaNetworkLength(String shpFile)	{
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
	
	private void run(String networkFile, String transitScheduleFile) throws IOException	{
	
		HashSet<String> networkLinks = new HashSet<String>();
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
				
				boolean isParaLine = false;
				String transitLine;
				String transitMode;
				boolean getMode = false;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitLine"))	{
						transitLine = attributes.getValue("id");
						
						if(transitLine.contains("para"))
							isParaLine = true;
						else
							isParaLine = false;
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						getMode = true; 
					}	
					
					//if(qName.equalsIgnoreCase("link") && !transitMode.equals("pt") && !transitMode.equals("bus"))	{
					if(qName.equalsIgnoreCase("link") && isParaLine)	{
						if(linkLength.containsKey(attributes.getValue("refId")))	{
							networkLinks.add(attributes.getValue("refId"));
						}
					}			
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transportMode")) {
			        	getMode = false;
			        }
					
			    }
				
				 public void characters(char[] ch, int start, int length) throws SAXException {
				        if (getMode) {
				            transitMode = new String(ch, start, length);
				        }
				 }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);

			double totalLength = 0.0;
			for( String i : networkLinks)	{
				totalLength += linkLength.get(i);;
			}
			
			System.out.println(totalLength / 1000);
			
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