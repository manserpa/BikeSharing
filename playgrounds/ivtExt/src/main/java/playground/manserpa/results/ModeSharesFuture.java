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

public final class ModeSharesFuture {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		ModeSharesFuture cs = new ModeSharesFuture(args[0]);
		
		String csvFileTransitLinks = "ModeSharesFuture.csv";
	    FileWriter writerTransitLinks = new FileWriter(csvFileTransitLinks );
	    
	    CSVUtils.writeLine(writerTransitLinks , Arrays.asList("PT", "Car","Av","Walk","Bike"), ';');
		
	    int i = 0;
	    
	    while(i <= 600)	{
	    	cs.run("5000AVs/Run5/it."+i+"/run."+i+".plans.xml", writerTransitLinks);
	    	
	    	System.out.println("Handled Iteration " + i);
	    	
	    	i += 50;
	    }

        writerTransitLinks.flush();
        writerTransitLinks.close();
		
	}
	
	private ModeSharesFuture(String shpFile)	{
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
	
	private void run(String planFile, FileWriter writerTransitLinks) throws IOException	{
		
		HashSet<String> person2trip = new HashSet<String>();
		HashMap<String, Integer> modeCounter = new HashMap<String, Integer>();
	    
	    
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			DefaultHandler handler = new DefaultHandler()	{
				
				boolean selected;
				String person;
				String legMode;
				boolean isTransitTrip;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
				    if(qName.equalsIgnoreCase("person"))	{
						person = attributes.getValue("id");
					}
							
					if(qName.equalsIgnoreCase("plan"))	{
						if(attributes.getValue("selected").equals("yes"))	{
							selected = true;
						}
						else	{
							selected = false;
						}
					}
					
					if(qName.equalsIgnoreCase("activity") && selected)	{
						// and the person is not yet in the map
						if(!attributes.getValue("type").equals("pt interaction") && !person2trip.contains(person))	{
							person2trip.add(person);
							isTransitTrip = false;
							// put the person into a map --> trip started
						}
						// the person is already in the map
						else if(!attributes.getValue("type").equals("pt interaction") && person2trip.contains(person))	{
							// process the trip --> it has finished
							if(!isTransitTrip)
								modeCounter.put(legMode, modeCounter.getOrDefault(legMode, 0) + 1);
							else
								modeCounter.put("pt", modeCounter.getOrDefault("pt", 0) + 1);

							isTransitTrip = false;
							legMode = "something else";
						}
					}
					
					
					if(qName.equalsIgnoreCase("leg"))	{
						if(attributes.getValue("mode").equals("car"))	{
							legMode = "car";
						}
						else if(attributes.getValue("mode").equals("bike"))	{
							legMode = "bike";
						}
						else if(attributes.getValue("mode").equals("av"))	{
							legMode = "av";
						}
						else if(attributes.getValue("mode").equals("walk"))	{
							legMode = "walk";
						}
						else if(attributes.getValue("mode").equals("pt"))	{
							isTransitTrip = true;
						}
						else	{
							legMode = "something else";
						}
					}	
							
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
					if(qName.equalsIgnoreCase("plan"))	{
						person2trip.remove(person);
					}
				
			    }
			};
			
			saxParser.parse(planFile, handler);
			
			modeCounter.remove("something else");
			
			int totalTrips = 0;
			
			for(String k : modeCounter.keySet())	{
				totalTrips += modeCounter.get(k);
			}
			
			for(String k : modeCounter.keySet())	{
				System.out.println("Share of mode " + k + ": " + ((double) modeCounter.get(k)) / ((double) totalTrips));
			}
			
			double sharePT = ((double) modeCounter.get("pt")) / ((double) totalTrips);
			double shareCar = ((double) modeCounter.get("car")) / ((double) totalTrips);
			double shareWalk = ((double) modeCounter.get("walk")) / ((double) totalTrips);
			double shareBike = ((double) modeCounter.get("bike")) / ((double) totalTrips);
			double shareAV = 0;
			
			if( modeCounter.containsKey("av") )	{
				shareAV = ((double) modeCounter.get("av")) / ((double) totalTrips);
			}
			
			try {
				CSVUtils.writeLine(writerTransitLinks, Arrays.asList(Double.toString(sharePT),Double.toString(shareCar),Double.toString(shareAV),
						Double.toString(shareWalk),Double.toString(shareBike)), ';');
			} catch (IOException e) {
				e.printStackTrace();
			}
			
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