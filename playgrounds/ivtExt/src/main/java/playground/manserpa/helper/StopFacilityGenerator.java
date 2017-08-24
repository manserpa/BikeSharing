package playground.manserpa.helper;

import com.vividsolutions.jts.geom.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public final class StopFacilityGenerator {
	
	/**
	 * 
	 * StopFacilityGenerator: The station generator introduces a stop facility on every link in a certain area (shape file). There are certain limitations:
	 * The free-flow speed, the link type. This could be adjusted within this script.
	 * 
	 * Input: Shape file, network file
	 * Output: A .xml containing all desired stop facilities
	 * 
	 * @author manserpa
	 * 
	 */
	
	
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		StopFacilityGenerator cs = new StopFacilityGenerator(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private StopFacilityGenerator(String shpFile)	{
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
	
		List<LinkList> linkList = new ArrayList<>();
		List<NodeList> nodeList = new ArrayList<>();
		
		try {
			List<String> nodeListe = new ArrayList<>(); 
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
				
				if(qName.equalsIgnoreCase("node"))	{
					
					if(nodeInServiceArea(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y"))))	{
						
						NodeList thisNode = new NodeList(attributes.getValue("id"),Double.parseDouble(attributes.getValue("x")),
								Double.parseDouble(attributes.getValue("y")));
						nodeList.add(thisNode);	
						
						nodeListe.add(attributes.getValue("id"));
						
					}
				}
				
				if(qName.equalsIgnoreCase("link"))	{
					
					if(nodeListe.contains(attributes.getValue("from")) && nodeListe.contains(attributes.getValue("to")))	{
						
						LinkList thisLink = new LinkList(attributes.getValue("id"),attributes.getValue("from"),
								attributes.getValue("to"),Double.parseDouble(attributes.getValue("freespeed")));
						linkList.add(thisLink);	

					}
				}
				
			}
			};
			
			saxParser.parse(networkFile, handler);
			
			Element transitStop = new Element("transitStop");
			
			int k = 1;
			
			for (LinkList link: linkList)	{
					
				String fromNode = link.from;
				String toNode = link.to;
				String linkRef = link.linkId;
					
				double fromXCoord = 0;
				double fromYCoord = 0;
				double toXCoord = 0;
				double toYCoord = 0;
				
				// max link speed to avoid stops on highways
				if(link.freespeed < 22)	{
					
					for(NodeList node: nodeList)	{
						if (fromNode.equals( node.nodeId ))	{
							fromXCoord = node.x;
							fromYCoord = node.y;
							break;
						}
					}
						
					for(NodeList node: nodeList)	{
						if (toNode.equals( node.nodeId ))	{
							toXCoord = node.x;
							toYCoord = node.y;
							break;
						}
					}
						
					double xCoord = ( fromXCoord + toXCoord ) / 2;
					double yCoord = ( fromYCoord + toYCoord ) / 2;
					
					Element stopFacility = new Element("stopFacility");
					stopFacility.setAttribute("linkRefId", linkRef);
					stopFacility.setAttribute("x", String.valueOf(xCoord));
					stopFacility.setAttribute("id", fromNode + toNode + "_" + k + "_para");
					stopFacility.setAttribute("y", String.valueOf(yCoord));
					stopFacility.setAttribute("name", fromNode + toNode + "_" + k + "_para");
						
					transitStop.addContent(stopFacility);
					k++;
				}
					
			}
				
			PrintWriter output = new PrintWriter ("StopFacilities.xml") ;
			Document doc = new Document(transitStop);
		
			XMLOutputter serializer = new XMLOutputter();
		    serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
		    serializer.output(doc, output);
		    
		    output.close() ;
			
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
	
	class LinkList	{
		String linkId;
		String from;
		String to;
		double freespeed;
		
		public LinkList(String linkId, String from, String to, double freespeed)	{
			this.linkId = linkId;
			this.from = from;
			this.to = to;
			this.freespeed = freespeed;
		}
	}

	class NodeList	{
		String nodeId;
		double x;
		double y;
		
		public NodeList(String nodeId, double x, double y)	{
			this.nodeId = nodeId;
			this.x = x;
			this.y = y;
		}
	}

}