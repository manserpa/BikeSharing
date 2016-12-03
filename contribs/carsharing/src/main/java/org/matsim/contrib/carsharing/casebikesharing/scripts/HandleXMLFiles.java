package org.matsim.contrib.carsharing.casebikesharing.scripts;
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

public class HandleXMLFiles {

	public static void main(String[] args) throws FileNotFoundException {
		generateFfCars();
	}
	
	public static void buildCSMembershipFile(){
		List<String> persons = new ArrayList<>(); 
		
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File("C:\\Users\\Gabriel\\git\\matsimproject\\contribs\\carsharing\\test\\input\\org\\matsim\\contrib\\carsharing\\casebikesharing\\siouxfalls\\population.xml\\population.xml");

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
		
		Element rootNode = new Element("companies");
		Element thisCompany = new Element("company");
		thisCompany.setAttribute("name", "Mobility");
			
		for ( Map.Entry<String, HashMap> entry : cars.entrySet()){
			String id = entry.getKey();
			HashMap<String,Integer> coords = entry.getValue();
			
			Element thisCarElement = new Element("freefloating");
			thisCarElement.setAttribute("id", id);
			thisCarElement.setAttribute("x", String.valueOf(coords.get("x")));
			thisCarElement.setAttribute("y", String.valueOf(coords.get("y")));
			thisCarElement.setAttribute("type", "car");
			
			thisCompany.addContent(thisCarElement);
		}

		rootNode.addContent(thisCompany);
		PrintWriter output = new PrintWriter ("C:\\Users\\Gabriel\\git\\matsimproject\\contribs\\carsharing\\test\\input\\org\\matsim\\contrib\\carsharing\\casebikesharing\\siouxfalls\\carsharingstations_new.xml") ;
		Document doc = new Document(rootNode);
		
		try {
			XMLOutputter serializer = new XMLOutputter();
	        serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
			serializer.output(doc, output);
		}
		catch (IOException e) {
			System.err.println(e);
		}

		output.close() ;
		
		
		
	}
	
}
