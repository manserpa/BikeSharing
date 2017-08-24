package playground.manserpa.helper;

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

public final class ReducePopulationSize {
	
	/**
	 * 
	 * ReducesPopulationSize: Trims the total population to a certain shape file (I took an area with a 2km buffer around my actual area of interest).
	 * 	it cuts of all agents that do not have any activity within the shape file nor cross the area during the day.
	 * 
	 * Input: Shape file, network file and population file
	 * Output: A .csv containing all IDs of the remaining agents
	 * 
	 * @author manserpa
	 * 
	 */
	
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		ReducePopulationSize cs = new ReducePopulationSize(args[0]);
		
		cs.run(args[1], args[2]);
		
	}
	
	private ReducePopulationSize(String shpFile)	{
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
	
	private void run(String networkFile, String populationFile) throws IOException	{
		
	    String csvFile = "AgentsToDelete.csv";
	    FileWriter writer = new FileWriter(csvFile);
	    
	    CSVUtils.writeLine(writer , Arrays.asList("Id"), ';');
	    
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			List<String> nodeList = new ArrayList<>(); 
			List<String> linkList = new ArrayList<>(); 
			
			HashSet<ActCoords> activityCoords = new HashSet<>();
			HashSet<String> routeList = new HashSet<>();
			 
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
			
			 
			DefaultHandler handler2 = new DefaultHandler()	{
				
				String personId;
				boolean hasActivityInZurich = false;
				boolean crossesZurich = false;
				boolean isRoute = false;
				int numberOfAgents = 0;
				int numberOfAgentsInZurich = 0;
				int numberOfAgentsCrossingZurich = 0;
				String route;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("person"))	{
						
						hasActivityInZurich = false;
						crossesZurich = false;
						personId = attributes.getValue("id");
						numberOfAgents++;
						System.out.println(numberOfAgents);
						
					}
					
					if(qName.equalsIgnoreCase("route"))	{
						isRoute = true;
					}	
					
					if(qName.equalsIgnoreCase("act"))	{	
							ActCoords thisActivity = new ActCoords(Double.parseDouble(attributes.getValue("x")),Double.parseDouble(attributes.getValue("y")));
							activityCoords.add(thisActivity);
					}	
					
					
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("route")) {
			        	routeList.add(route);
			        	isRoute = false;
			        }
					
			        if(qName.equals("person")) {
			        	
			        	for(ActCoords e: activityCoords)	{
							if(nodeInServiceArea(e.x,e.y))	{
								hasActivityInZurich = true;
								break;
							}
			        	}
			        	
			        	activityCoords.clear();
			        	
			        	if(!hasActivityInZurich)	{
				        	for(String i: routeList)	{
				        		
				        		String[] linkSequence = i.split(" ");
				        		
					        	for(String link: linkSequence)	{
					        		if (linkList.contains(link))	{
					        			crossesZurich = true;
					        			break;
					        		}
					        	}
					        	
					        	if(crossesZurich)
					        		break;
				        	}
			        	}

			        	routeList.clear();		        	
			        	
			        	if(hasActivityInZurich)
			        		numberOfAgentsInZurich++;
			        	
			        	if(crossesZurich)
			        		numberOfAgentsCrossingZurich++;
			        
			        	// wenn ein Agent sowohl keine Aktivität in Zürich hat und nicht durch Zürich reist, wird er nicht mehr berücksichtigt in der Simulation
			        	if(!hasActivityInZurich && !crossesZurich)	{
			        		try {
								CSVUtils.writeLine(writer, Arrays.asList(personId), ';');
							} catch (IOException e) {
								e.printStackTrace();
							}
			        	}
			        }
			        
			        if(qName.equals("population")) {
						System.out.println("Total number of Agents: " + numberOfAgents);
						System.out.println("Total number of Agents with Activities in the Shape: " + numberOfAgentsInZurich);
						System.out.println("Total number of Agents crossing the Shape: " + numberOfAgentsCrossingZurich);
			        }
			    }
				
				 public void characters(char[] ch, int start, int length) throws SAXException {
				        if (isRoute) {
				            route = new String(ch, start, length);
				        }
				 }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(populationFile, handler2);
	        
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
	
	class ActCoords	{
		double x;
		double y;
		
		public ActCoords(double x, double y)	{
			this.x = x;
			this.y = y;
		}
	}
}