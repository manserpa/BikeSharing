package bikeshare;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

// import org.matsim.contrib.matsim4urbansim.utils.network;
// utils for calculating distances http://www.matsim.org/apidocs/matsim4urbansim/0.7.0/org/matsim/contrib/matsim4urbansim/utils/network/NetworkUtil.html


public class CreateBSSRoutes {
	
	static final String setName = "regular"; // e. g. regular, e-bike (used for file name)
	static final double speed = 15; // unit: km/h, could be differentiated by distance (large distance - assume high speed tracks - higher average speed)
	static final double ascendingSlopeEffect = 10; // controls the effect of ascending slope sections (10 = 1 height metre adds 10 metres)
	static final double descendingSlopeEffect = 2; // controls the effect of descending slope sections (2 = 1 height metre lowers distance by 2 metres)
	static final double deviationFactor = 1.3; // beeline distance gets multiplied by that. should probably better be regressive (i. e. nearly one at high distances, high at low distances)
	static final double accessTime = 40; // access time in seconds
	static final double egressTime = 20; // egress time in seconds

	static final String coordType = "metres"; // use the coord system as stated in config file? then set "coord". other possibility: use coords directly as metres. then set "metres"

	
	public static void main(String[] args)  throws FileNotFoundException {
		
		List<Element> stations = new ArrayList<>(); 
		List<BSSRoute> routes = new ArrayList<>();

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(args[0]);
		
		try {
			// build list of all stations
			Document document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			List<Element> list = rootNode.getChildren("stopFacility");

			for (int i = 0; i < list.size(); i++) {
				Element node = (Element) list.get(i);
				stations.add(node);
			}

			
			// create routes for all connections between all stations, taking into account the settings from above (create XML structure)
			Iterator<Element> it = list.iterator();
			while(it.hasNext()){
				Element i = (Element) it.next();
				
				Iterator<Element> it2 = list.iterator();
				while(it2.hasNext()){
					Element j = (Element) it2.next();
					
					if ( !i.getAttribute("id").equals(j.getAttribute("id"))) // only do the following if we do not have two times the same station 
					{
						
						// convert x/y-coords to ??? scheme in order to obtain z (elevation) values
						// obviously, it would be much better to get actual routes, so street network topology, number of intersections etc. could be integrated in the distance

						// TODO

						
						

						// calculate distance
						double dis = 0; // distance in metres
						if ( coordType.equals("metres")){
							
							dis = Math.sqrt(Math.pow(i.getAttribute("x").getDoubleValue() - j.getAttribute("x").getDoubleValue(), 2) + Math.pow(i.getAttribute("y").getDoubleValue() - j.getAttribute("y").getDoubleValue(), 2) ); 
									
						} else {
							// TODO 
						}
						dis = dis * deviationFactor;
						
						
						
						// calculate height difference
						double deltaZ = 0; // TODO calculate height difference here
						if ( deltaZ > 0 ) dis = dis + (deltaZ * ascendingSlopeEffect);
						else dis = dis - (deltaZ * descendingSlopeEffect);
						
					
						// add extra travel time for intersections here
						
						
						// sum up travel time (incl. access/egress) 
						double tt = dis / (speed / 3.6) + accessTime + egressTime; // tt in seconds
						
					
					
						// add to routes list (required: start and end stop id, travel time)
						BSSRoute thisRoute = new BSSRoute(i.getAttributeValue("id"), j.getAttributeValue("id"), tt);
						routes.add(thisRoute);
						
					
					}
					
				}
				
				
			}
			
			// write file
			Element rootNode2 = new Element("transitLine");

			Iterator<BSSRoute> itRoutes = routes.iterator();
			while(itRoutes.hasNext())
			{
				BSSRoute thisRoute = itRoutes.next();
				
				Element transitRoute = new Element("transitRoute");
				transitRoute.setAttribute("id", "bikeshare_"+thisRoute.startid+"_"+thisRoute.endid);
				
				Element transportMode = new Element("transportMode");
				transportMode.addContent("bikeshare");
				
				Element routeProfile = new Element("routeProfile");
				Element startStop = new Element("stop");
				startStop.setAttribute("refId", thisRoute.startid);
				startStop.setAttribute("departureOffset", "00:00:00");

				Element endStop = new Element("stop");
				endStop.setAttribute("refId", thisRoute.endid);
				int hr = ((Double)(thisRoute.tt/3600)).intValue();
				int min = ((Double)((thisRoute.tt%3600)/60)).intValue();
				int s = ((Double)(thisRoute.tt%60)).intValue();
				endStop.setAttribute("arrivalOffset", hr + ":" + min + ":" + s);
				
				routeProfile.addContent(startStop);
				routeProfile.addContent(endStop);
				
				
				Element departures = new Element("departures");
				
				transitRoute.addContent(transportMode);
				transitRoute.addContent(routeProfile);
				transitRoute.addContent(departures);

				rootNode2.addContent(transitRoute);
			}

			PrintWriter output = new PrintWriter ("bssroutes-"+setName+".xml") ;
			Document doc = new Document(rootNode2);
			
			try {
				XMLOutputter serializer = new XMLOutputter();
		        serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
				serializer.output(doc, output);
			}
			catch (IOException e) {
				System.err.println(e);
			}

			output.close() ;
			
			
			
			
		
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
			
		

	}

}


/*

	
	public static void buildCSMembershipFile(){
		

		try {
			

			// get all persons
			Document document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			List list = rootNode.getChildren("person");

			for (int i = 0; i < list.size(); i++) {
				Element node = (Element) list.get(i);
				persons.add(node.getAttributeValue("id"));
			}
			
			
			// create new document for cs membership of these persons
			Element rootNodeNew = new Element("memberships");
			
			Iterator<String> it = persons.iterator();
			while(it.hasNext()){
				String personid = it.next();
				
				Element person = new Element("person");
				person.setAttribute("id", personid);
				
				Element company = new Element("company");
				company.setAttribute("id", "Mobility");
				
				Element mTw = new Element("carsharing");
				mTw.setAttribute("name", "twoway");
				company.addContent(mTw);
				
				Element mOw = new Element("carsharing");
				mOw.setAttribute("name", "oneway");
				company.addContent(mOw);

				Element mFf = new Element("carsharing");
				mFf.setAttribute("name", "freefloating");
				company.addContent(mFf);
				
				person.addContent(company);
				
				rootNodeNew.addContent(person);

			}
			
			PrintWriter output = new PrintWriter ("C:\\Users\\Gabriel\\git\\matsimproject\\contribs\\carsharing\\test\\input\\org\\matsim\\contrib\\carsharing\\casebikesharing\\siouxfalls\\csmembership_new.xml") ;
			
			
			Document doc = new Document(rootNodeNew);
			try {
				XMLOutputter serializer = new XMLOutputter();
		        serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
				serializer.output(doc, output);
			}
			catch (IOException e) {
				System.err.println(e);
			}

			output.close() ;

		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		
	}
	
	public static void getDimensions(){
		List<Float> xCoords = new ArrayList<>(); 
		List<Float> yCoords = new ArrayList<>(); 
		
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("C:\\Users\\Gabriel\\git\\matsimproject\\contribs\\carsharing\\test\\input\\org\\matsim\\contrib\\carsharing\\casebikesharing\\siouxfalls\\network.xml");

		try {
			
			Document document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			List list = rootNode.getChild("nodes").getChildren("node");

			for (int i = 0; i < list.size(); i++) {
				Element node = (Element) list.get(i);
				xCoords.add(Float.parseFloat(node.getAttributeValue("x")));
				yCoords.add(Float.parseFloat(node.getAttributeValue("y")));
			}
			
			float minX=0;
			float maxX=0;
			float minY=0;
			float maxY=0;
			Iterator<Float> it = xCoords.iterator();
			while ( it.hasNext()){
				Float thisNode = it.next();
				if ( thisNode > maxX) maxX = thisNode;
				if ( thisNode < minX || minX == 0.0) minX = thisNode;
			}
			Iterator<Float> it2 = yCoords.iterator();
			while ( it2.hasNext()){
				Float thisNode = it2.next();
				if ( thisNode > maxY) maxY = thisNode;
				if ( thisNode < minY || minY == 0.0) minY = thisNode;
			}
			
			System.out.println("maxX: " + maxX);
			System.out.println("minX: " + minX);
			System.out.println("maxY: " + maxY);
			System.out.println("minY: " + minY);
			
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
		
		
	}

	public static void generateFfCars() throws FileNotFoundException{
		
		int numberOfCars = 200;
		String idPrefix = "FF_";
		int boundaries[] = {678126, 687482, 4818750, 4832294}; // min X, max X, min Y, max Y
		HashMap<String, HashMap> cars = new HashMap<>();
		
		
		// generate cars
		
		for ( int i = 0; i < numberOfCars; i++){
			HashMap<String, Integer> coords = new HashMap<>();
			coords.put("x", ThreadLocalRandom.current().nextInt(boundaries[0], boundaries[1] + 1));
			coords.put("y", ThreadLocalRandom.current().nextInt(boundaries[2], boundaries[3] + 1));
			cars.put(idPrefix + i, coords);
		}
		
		
		// generate XML
		
		
		
		
	}
*/



