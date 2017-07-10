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

public final class Frequencies {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		Frequencies cs = new Frequencies(args[0]);
		
		for(int simulationRun = 1; simulationRun <= 1; simulationRun++)	{
			cs.run(args[1], args[2], simulationRun);
		}
		
	}
	
	private Frequencies(String shpFile)	{
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
	
	private void run(String networkFile, String transitScheduleFile, int simulationRun) throws IOException	{
		
		HashMap<String, List<Double>> transitRoute2Departures = new HashMap<String, List<Double>>();
		HashMap<String, List<String>> transitRoute2Links = new HashMap<String, List<String>>();
		
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
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				String transitRoute;
				boolean getMode = false;
				String transitMode;
				boolean crossesScenario = false;
				boolean isParatransit = false;
				boolean isInScenario;
				
				List<Double> departures = new ArrayList<>();
				List<String> links = new ArrayList<>();
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
					
			        	departures = new ArrayList<>();
			        	links = new ArrayList<>();
						
						transitRoute = attributes.getValue("id");
						crossesScenario = false;
						isInScenario = true;
						if (!transitRoute.contains("para"))
							isParatransit = false;
						else
							isParatransit = true;
					}
					
					if(qName.equalsIgnoreCase("transportMode"))	{
						if(isParatransit)	{
							getMode = false;
							transitMode = "bus";
						}
						else	{
							getMode = true; 
						}
					}	
					
					if(qName.equalsIgnoreCase("link"))	{
						if(linkList.contains(attributes.getValue("refId")))	{
							crossesScenario = true;
						}
						
						if(linkList.contains(attributes.getValue("refId")) && isInScenario)	{
							isInScenario = true;
						}
						else	{
							isInScenario = false;
						}
						
						links.add(attributes.getValue("refId"));
					}
					
					if(qName.equalsIgnoreCase("departure"))	{
						String[] departureTime = attributes.getValue("departureTime").split(":");
						double departureTimeInSeconds = Double.parseDouble(departureTime[0]) * 3600 + Double.parseDouble(departureTime[1]) * 60 +
								Double.parseDouble(departureTime[2]);
						
						departures.add(departureTimeInSeconds);
					}
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transportMode")) {
			        	getMode = false;
			        }
			        
			        if(qName.equals("departures") && crossesScenario && !transitMode.equals("pt")) {
			        	Collections.sort(departures);
			        	transitRoute2Departures.put(transitRoute, departures);
			        }
			        
			        if(qName.equals("route") && crossesScenario && !transitMode.equals("pt")) {
			        	transitRoute2Links.put(transitRoute, links);
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
			
			List<Double> departures2Interval = new ArrayList<>();
			List<Double> frequencies = new ArrayList<>();
			
			for(int i = 1800; i <= 108000; i += 1800)	{
				frequencies = new ArrayList<>();
				
				for(String route : transitRoute2Departures.keySet())	{
					int numberOfDepartures = 0;
					for(double k : transitRoute2Departures.get(route))	{
						if((int) k <= i && (int) k > i - 1800)
							numberOfDepartures++;
					}
					if(numberOfDepartures > 0)
						frequencies.add(numberOfDepartures / 0.5);
				}
				
				double totfrequ = 0.0;
				double maxfrequ = 0.0;
				for(double frequ: frequencies)	{
					if(maxfrequ < frequ)
						maxfrequ = frequ;
				}
				double averagefrequ = totfrequ / frequencies.size();
				
				departures2Interval.add(maxfrequ);
				
			}
			
			String csvFile = "FrequencyAnalysisMaxReference.csv";
		    FileWriter writer = new FileWriter(csvFile);
		    
		    CSVUtils.writeLine(writer, Arrays.asList("TimeInterval", "NumberOfDepartures"), ';');
			
		    
		    int interval = 1800;
		    for(int i = 0; i < departures2Interval.size(); i++)	{
				try {
					CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(interval),
							Double.toString(departures2Interval.get(i))), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
				interval += 1800;
		    }
		
	        writer.flush();
	        writer.close();
	        
	        /*
	         * Spatial Frequency-Analysis
	         */
	        
			String csvFile2 = "FrequencyAnalysisSpatial"+simulationRun+".csv";
		    FileWriter writer2 = new FileWriter(csvFile2);
		    
		    CSVUtils.writeLine(writer2, Arrays.asList("LinkId", "AvgFrequency"), ';');
	        
			HashMap<String, List<Integer>> link2Frequency = new HashMap<>();
			
			for(String route : transitRoute2Departures.keySet())	{
				int numberOfDepartures = 0;
				for(double k : transitRoute2Departures.get(route))	{
					//if((int) k <= 64800 && (int) k > 61200)
					if((int) k <= 32400 && (int) k > 28800)
						numberOfDepartures++;
				}
				
				if(numberOfDepartures > 0)	{
					for(String linkId: transitRoute2Links.get(route))	{
						if(linkList.contains(linkId))	{
							List<Integer> linkListe = new ArrayList<>();
							List<Integer> currentLinkFrequencies = link2Frequency.getOrDefault(linkId, linkListe);
							
							currentLinkFrequencies.add(numberOfDepartures);
							link2Frequency.put(linkId, currentLinkFrequencies);
						}
					}
				}
			}
				
			
			for(String linkId: link2Frequency.keySet())	{
				double totfrequ = 0.0;
				for(int frequ : link2Frequency.get(linkId))	{
					totfrequ += (double) frequ;
				}

				double averagefrequ = totfrequ / link2Frequency.get(linkId).size();
				
				try {
					CSVUtils.writeLine(writer2, Arrays.asList(linkId,
							Double.toString(averagefrequ)), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
	        writer2.flush();
	        writer2.close();
			
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