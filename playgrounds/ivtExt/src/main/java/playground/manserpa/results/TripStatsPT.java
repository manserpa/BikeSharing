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

public final class TripStatsPT {
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	
	public static void main(String[] args) throws IOException	{
		TripStatsPT cs = new TripStatsPT(args[0]);
		
		cs.run(args[1], args[2], args[3]);
		
	}
	
	private TripStatsPT(String shpFile)	{
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
	
	private void run(String networkFile, String transitScheduleFile, String eventFile) throws IOException	{
		
		List<String> transitRoutesInScenario = new ArrayList<>();
		HashMap<String, Double> linkLength = new HashMap<String, Double>();
		HashSet<String> transitVehicles	= new HashSet<String>();
		
		HashMap<String, String> person2trip = new HashMap<String, String>();
		HashSet<String> person2firstAct = new HashSet<String>();
		HashSet<String> person2PT = new HashSet<String>();
		ArrayList<String> person2PTTrip = new ArrayList<String>();
	    
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
							linkLength.put(attributes.getValue("id"), Double.parseDouble(attributes.getValue("length")));
						
						}
					}				
				}
			};
			
			DefaultHandler handler2 = new DefaultHandler()	{
				
				String transitRoute;
				boolean isInScenario = true;
				boolean getMode = false;
				String transitMode;
				boolean crossesScenario = false;
				boolean isParatransit = false;
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("transitRoute"))	{
						transitRoute = attributes.getValue("id");
						isInScenario = true;
						crossesScenario = false;
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
					}			
					
					if(qName.equals("departure") && crossesScenario && !transitMode.equals("pt")) {
			        	transitVehicles.add(attributes.getValue("vehicleRefId"));
			        }
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
					
			        if(qName.equals("transportMode")) {
			        	getMode = false;
			        }
					
			        if(qName.equals("transitRoute")) {
			        	//if(crossesScenario && !transitMode.equals("pt"))	{
			        	if(isParatransit)	{
			        		// ArrayList containing all the IDs
			        		transitRoutesInScenario.add(transitRoute);
			        		//System.out.println("Line: " + transitLine + "; Route: " + transitRoute + "; Mode: " + transitMode);	
			        	}
			        }
			    }
				
				 public void characters(char[] ch, int start, int length) throws SAXException {
				        if (getMode) {
				            transitMode = new String(ch, start, length);
				        }
				 }
			};
			
			DefaultHandler handler3 = new DefaultHandler()	{
				
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("event"))	{
						
						if(attributes.getValue("type").equals("actstart"))	{
							if(!attributes.getValue("actType").equals("pt interaction") && person2trip.containsKey(attributes.getValue("person")))	{
								// this must be the second activity -> trip finished!
								
								//if (person2PT.contains(attributes.getValue("person")) && linkList.contains(attributes.getValue("link")))	{
								if (person2PT.contains(attributes.getValue("person")))	{
									person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person")) + 
											"===PersonArrives" + attributes.getValue("actType") + "===" + attributes.getValue("time"));
									System.out.println(person2trip.get(attributes.getValue("person")));
									person2PTTrip.add(person2trip.get(attributes.getValue("person")));
									// process the trip
								}
								
								person2PT.remove(attributes.getValue("person"));
								person2trip.remove(attributes.getValue("person"));
							}
						}
						
						
						if(attributes.getValue("type").equals("actend"))	{
							
							// this must be an agent in the set not using pt -> remove it!
							if(person2trip.containsKey(attributes.getValue("person")))	{
								/*
								if(!attributes.getValue("actType").equals("pt interaction"))	{
									person2trip.remove(attributes.getValue("person"));
								}
								*/
								// the person is on a PT trip
								//if(attributes.getValue("actType").equals("pt interaction"))	{
								//	person2PT.add(attributes.getValue("person"));
								//}
								
								//if(linkList.contains(attributes.getValue("link")) && attributes.getValue("actType").equals("pt interaction"))	{
								if(attributes.getValue("actType").equals("pt interaction"))	{
									person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person")) + 
											"===PT_InteractionEnds===" + attributes.getValue("time"));
									person2PT.add(attributes.getValue("person"));
								}
								else	{
									person2trip.remove(attributes.getValue("person"));
								}
								
								// pt interaction is not in the service area
								//if(!linkList.contains(attributes.getValue("link")) && attributes.getValue("actType").equals("pt interaction"))	{
								//	person2trip.remove(attributes.getValue("person"));
								//}
							}	
							
							
							// this must be the first activity -> trip started!
							//if(linkList.contains(attributes.getValue("link")) && !attributes.getValue("actType").equals("pt interaction")
							//		&& !person2firstAct.contains(attributes.getValue("person")))	{
							if(!attributes.getValue("actType").equals("pt interaction") && !person2trip.containsKey(attributes.getValue("person")))	{	
								person2trip.put(attributes.getValue("person"), "PersonLeaves" + attributes.getValue("actType") + "===" + attributes.getValue("time"));
								person2PT.remove(attributes.getValue("person"));
							}
						}
						
						if(attributes.getValue("type").equals("PersonEntersVehicle") && person2trip.containsKey(attributes.getValue("person")))	{
							if (transitVehicles.contains(attributes.getValue("vehicle")))	{
								// prüfen, ob das Vehicle in der Liste ist -> sonst Person removen
								person2trip.put(attributes.getValue("person"), person2trip.get(attributes.getValue("person")) + 
										"===PersonEntersVehicle" + attributes.getValue("vehicle") + "===" + attributes.getValue("time"));
							}
							else	{
								person2trip.remove(attributes.getValue("person"));
							}
						}
					}		
				}
				
				public void endElement(String uri, String localName, String qName)
			            throws SAXException {
				
			    }
			};
			
			saxParser.parse(networkFile, handler);
			saxParser.parse(transitScheduleFile, handler2);
			saxParser.parse(eventFile, handler3);
			
			List<Double> accessTime = new ArrayList<>();
			List<Double> egressTime = new ArrayList<>();
			List<Double> firstWaitingTime = new ArrayList<>();
			List<Double> transferWaitingTime = new ArrayList<>();
			List<Integer> numberOfTransfers = new ArrayList<>();
			List<Double> totalTripTime = new ArrayList<>();
			List<Double> inVehicleTime = new ArrayList<>();
			
			for(String e : person2PTTrip)	{
				
				String[] tripsequence = e.split("===");
				accessTime.add(Double.parseDouble(tripsequence[3]) - Double.parseDouble(tripsequence[1]));
				egressTime.add(Double.parseDouble(tripsequence[tripsequence.length-1]) - Double.parseDouble(tripsequence[tripsequence.length-3]));
				firstWaitingTime.add(Double.parseDouble(tripsequence[5]) - Double.parseDouble(tripsequence[3]));
				totalTripTime.add(Double.parseDouble(tripsequence[tripsequence.length-1]) - Double.parseDouble(tripsequence[1]));
				
				int numberOfTrans = (tripsequence.length - 10) / 6;
				numberOfTransfers.add((tripsequence.length - 10) / 6);
				
				double inVehicleTimeLeg = 0.0;
				
				for (int i = 0; i < numberOfTrans + 1; i++)	{
					if (i != 0)	{
						transferWaitingTime.add(Double.parseDouble(tripsequence[(i * 6) + 5]) - Double.parseDouble(tripsequence[(i * 6) + 3]));
					}
					inVehicleTimeLeg += Double.parseDouble(tripsequence[(i * 6) + 7]) - Double.parseDouble(tripsequence[(i * 6) + 5]);
				}
				
				inVehicleTime.add(inVehicleTimeLeg);
				
			}
			
			double totAmount = 0.0;
			for(double e : accessTime)	{
				totAmount += e;
			}
			double meanAccessTime = totAmount / accessTime.size();
			Collections.sort(accessTime);
			meanAccessTime = accessTime.get((int)(accessTime.size()*0.5));			
		    
			totAmount = 0.0;
			for(double i : egressTime)	{
				totAmount += i;
			}
			double meanEgressTime = totAmount / egressTime.size();
			Collections.sort(egressTime);
			meanEgressTime = egressTime.get((int)(egressTime.size()*0.5));
			
			totAmount = 0.0;
			for(double e : inVehicleTime)	{
				totAmount += e;
			}
			double meanInVehicleTime = totAmount / inVehicleTime.size();
			
			totAmount = 0.0;
			for(int e : numberOfTransfers)	{
				totAmount += (double) e;
			}
			double meanNumberOfTransfers = totAmount / numberOfTransfers.size();
			
			totAmount = 0.0;
			for(double e : firstWaitingTime)	{
				totAmount += e;
			}
			double meanFirstWaitingTime = totAmount / firstWaitingTime.size();
			
			totAmount = 0.0;
			for(double e : transferWaitingTime)	{
				totAmount += e;
			}
			double meanTransferWaitingTime = totAmount / transferWaitingTime.size();
			
			totAmount = 0.0;
			for(double e : totalTripTime)	{
				totAmount += e;
			}
			double meanTotalTripTime = totAmount / totalTripTime.size();
			
			String csvFile = "TripStats.csv";
		    FileWriter writer = new FileWriter(csvFile);
		    
		    CSVUtils.writeLine(writer, Arrays.asList("NumberOfTrips", "AccessWalk [m]", "EgressWalk [m]", "InVehicleTime", "Transfers", "FirstWaitingTime",
		    		"TransferWaitingTime", "TripTime"), ';');
			
			try {
				CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(person2PTTrip.size()), Double.toString(meanAccessTime * 1.3111111111111111), 
						Double.toString(meanEgressTime * 1.3111111111111111), Double.toString(meanInVehicleTime / 60), Double.toString(meanNumberOfTransfers),
						Double.toString(meanFirstWaitingTime / 60), Double.toString(meanTransferWaitingTime / 60), Double.toString(meanTotalTripTime / 60)), ';');
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