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

public final class TransitSchedule2CapacityPerLink {
	
	// TODO: move this to another folder
	
	/**
	 * 
	 * This file calculates the total seat capacity provided for each link during the whole day. It can do that for a number of simulation runs, but
	 * the folder hierarchy has to be correct
	 * 
	 * Input: Shape file, network file, transitSchedule
	 * Output: A .csv containing all linkIDs and the corresponding capacity per day in a sorted list (due to nicer plots)
	 * 
	 * @author manserpa
	 * 
	 */
	
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	public static void main(String[] args) throws IOException	{
		TransitSchedule2CapacityPerLink cs = new TransitSchedule2CapacityPerLink(args[0]);
		
		//for(int simulationRun = 1; simulationRun <= 5; simulationRun++)	{
		//	cs.run(args[1], "0AVs/Run" + simulationRun + args[2], simulationRun);
		//}
		
		
		cs.run(args[1], args[2], 1);
		
	}
	
	private TransitSchedule2CapacityPerLink(String shpFile)	{
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
		
		List<String> transitLinkList = new ArrayList<>();
		
		List<CapacityTot> totCapacity = new ArrayList<>();
		    
	    String csvFileTransitLinks = "CapacityPerLinkPara" + simulationRun + ".csv";
	    FileWriter writerTransitLinks = new FileWriter(csvFileTransitLinks );
	    
	    CSVUtils.writeLine(writerTransitLinks , Arrays.asList("LinkRefId", "TotCap"), ';');
	    
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
				
				String transitLine;
				int capacity = 0;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{	
					
					if(qName.equalsIgnoreCase("transitLine"))	{
						
						transitLine = attributes.getValue("id");
						
					}

					if(qName.equalsIgnoreCase("link"))	{
						//if(linkList.contains(attributes.getValue("refId")) && transitLine.contains("para"))	{
						if(linkList.contains(attributes.getValue("refId")))	{
							
							transitLinkList.add(attributes.getValue("refId"));
							
						}
					}	
					
					if(qName.equalsIgnoreCase("departure") && transitLine.contains("para"))	{
						
						/*
						// Case of VBZ analysisBUS
						String[] vehicleRef = attributes.getValue("vehicleRefId").split("_");
						String transitMode = vehicleRef[0];
						*/
						
						String transitMode = attributes.getValue("vehicleRefId");
						
						capacity = 0;
						
						
						// Case of Minibus Analysis
						if (transitMode.contains("Minibus")) {
							transitMode = "Minibus";
							capacity = 20;
						}
						else if (transitMode.contains("Standardbus")){
							transitMode = "Standardbus";
							capacity = 60;
						}
						else if (transitMode.contains("Gelenkbus")){
							transitMode = "Gelenkbus";
							capacity = 100;
						}
						else	{
							transitMode = "something else";
						}
						
						
						/*
						// Case of VBZ analysisBUS
						
						if (transitMode.equals("NFB")) {
							transitMode = "NFB";
							capacity = 70;
						}
						else if (transitMode.equals("BUS")){
							transitMode = "BUS";
							capacity = 70;
						}
						else if (transitMode.equals("KB")){
							transitMode = "KB";
							capacity = 30;
						}
						else if (transitMode.equals("NFT")){
							transitMode = "NFT";
							capacity = 180;
						}
						else if (transitMode.equals("T")){
							transitMode = "T";
							capacity = 140;
						}
						else	{
							transitMode = "something else";
						}
						*/
						
						System.out.println(transitLine + " " + transitMode);
						
						for(String linkRefId : transitLinkList)	{
							
							boolean hasLink = false;
							
							for(CapacityTot s: totCapacity)	{
								if(linkRefId.equals(s.linkId))	{
									s.totCap += capacity;
									hasLink = true;
								}
							}
							
							if(!hasLink)	{
								CapacityTot thisLink = new CapacityTot(linkRefId, capacity);
								totCapacity.add(thisLink);
							}
							
						}	
					}
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transitRoute")) {
			        	
			        	transitLinkList.clear();
			        
			        }
			    }
			};
			
			saxParser.parse(networkFile, handler);
			
			saxParser.parse(transitScheduleFile, handler2);
			/*
			saxParser.parse(transitScheduleFile2, handler2);
			saxParser.parse(transitScheduleFile3, handler2);
			saxParser.parse(transitScheduleFile4, handler2);
			saxParser.parse(transitScheduleFile5, handler2);
			saxParser.parse(transitScheduleFile6, handler2);
			saxParser.parse(transitScheduleFile7, handler2);
			saxParser.parse(transitScheduleFile8, handler2);
			saxParser.parse(transitScheduleFile9, handler2);
			saxParser.parse(transitScheduleFile10, handler2);
			*/
			
			
			Collections.sort(totCapacity, new Comparator<CapacityTot>() {
			    @Override public int compare(CapacityTot p1, CapacityTot p2) {
			        return p1.totCap - p2.totCap; // descending
			    }
			});
			
			
			for(CapacityTot s: totCapacity)	{	
				if(s.totCap > 0)	
					CSVUtils.writeLine(writerTransitLinks, Arrays.asList(s.linkId, Integer.toString(s.totCap)), ';');
			} 
	        
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
	
	class CapacityTot	{
		String linkId;
		int totCap;
		
		public CapacityTot(String linkId, int totCap)	{
			this.linkId = linkId;
			this.totCap = totCap;
		}
	}
}