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


/**
 * 
 * Purpose: analysis of the A-Taxi trips (waiting times, trip times, etc)
 * 
 * Input: shape file and events.xml
 * Output: a .csv with the statistics
 * 
 * @author manserpa
 * 
 */


public final class TripStatisticsAV {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		TripStatisticsAV cs = new TripStatisticsAV(args[0]);
		
		cs.run(args[1]);
		
	}
	
	private TripStatisticsAV(String shpFile)	{
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
	
	private void run(String eventFile) throws IOException	{
		
		HashMap<String, String> person2trip = new HashMap<String, String>();
		ArrayList<String> avTrips = new ArrayList<String>();
		ArrayList<String> avTripDistance = new ArrayList<String>();
	    
		try {
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			DefaultHandler handler = new DefaultHandler()	{
				
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("departure") && attributes.getValue("legMode").equals("av"))	{
							// wenn leg mode av -> person speichern plus zeit
							person2trip.put(attributes.getValue("person"), "Request===" + attributes.getValue("time"));
						}
						
						
						if(attributes.getValue("type").equals("PersonEntersVehicle"))	{
							if(person2trip.containsKey(attributes.getValue("person")))	{
								person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person")) + 
										"===EntersVehicle===" + attributes.getValue("time"));
							}
						}
						
						if(attributes.getValue("type").equals("AVTransit"))	{
							if(person2trip.containsKey(attributes.getValue("person")))	{
								person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person")) + 
										"===LeavesVehicle===" + attributes.getValue("time"));
								avTrips.add(person2trip.get(attributes.getValue("person")));
								avTripDistance.add(attributes.getValue("distance"));
								
								person2trip.remove(attributes.getValue("person"));
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
				
			    }
			};
			
			saxParser.parse(eventFile, handler);
			
			double paxDistance = 0.0;
			
			for(String e: avTripDistance)	{
				paxDistance += Double.parseDouble(e);
			}
			double meanTripDistance = paxDistance / avTripDistance.size();
			System.out.println(avTrips.size());
			System.out.println(paxDistance);
			
			//AV Trip: request time, invehicle time, total trip time
			//pax km kann direkt ausgelesen werden "Request Time [min]", "InVehicleTime [min]", "InVehicleDistance [km]", "TripTime [min]"
			
			List<Double> requestTime = new ArrayList<>();
			List<Double> inVehicleTime = new ArrayList<>();
			List<Double> tripTime = new ArrayList<>();
			
			for(String e : avTrips)	{		
				String[] tripsequence = e.split("===");
				requestTime.add(Double.parseDouble(tripsequence[3]) - Double.parseDouble(tripsequence[1]));
				inVehicleTime.add(Double.parseDouble(tripsequence[5]) - Double.parseDouble(tripsequence[3]));
				tripTime.add(Double.parseDouble(tripsequence[5]) - Double.parseDouble(tripsequence[1]));
			}
			
			double totAmount = 0.0;
			for(double e : requestTime)	{
				totAmount += e;
			}
			double meanRequestTime = totAmount / requestTime.size();
			
			totAmount = 0.0;
			for(double e : inVehicleTime)	{
				totAmount += e;
			}
			double meanInVehicleTime = totAmount / inVehicleTime.size();
			
			totAmount = 0.0;
			for(double e : tripTime)	{
				totAmount += e;
			}
			double meanTripTime = totAmount / tripTime.size();
			
			String csvFile = "AVTripStats.csv";
		    FileWriter writer = new FileWriter(csvFile);
		    
		    CSVUtils.writeLine(writer, Arrays.asList("NumberOfTrips", "Request Time [min]", "InVehicleTime [min]", "InVehicleDistance [km]", "TripTime [min]"), ';');
			
			try {
				CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(avTrips.size()), Double.toString(meanRequestTime / 60),
						Double.toString(meanInVehicleTime / 60), Double.toString(meanTripDistance / 1000),
						Double.toString(meanTripTime / 60)), ';');
			} catch (IOException e) {
				e.printStackTrace();
			}
		
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