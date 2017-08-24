package playground.manserpa.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class GetAverageScore {
	
	/**
	 * 
	 * GetAverageScore: Just a helper file for a short analysis of the distribution of the agents' scores
	 * 
	 * Input: Plan file containing scored plans
	 * Output: The distribution of the scores -> e.g. median and quartiles
	 * 
	 * @author manserpa
	 * 
	 */
	
	
	public static void main(String[] args)  throws IOException {
		
		List<Double> scoreList = new ArrayList<>();
		
		try {
			 
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			 
			DefaultHandler handler = new DefaultHandler()	{
				
				public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException	{
					
					if(qName.equalsIgnoreCase("plan"))	{
						if(attributes.getValue("selected").equals("yes"))	{
							scoreList.add(Double.parseDouble(attributes.getValue("score")));
						}
					}
					
				}
			};
			
			saxParser.parse(args[0], handler);
			
			
			Collections.sort(scoreList);
			int median = (int) (0.5 * scoreList.size());
			int lowerQuartile = (int) (0.25 * scoreList.size());
			int upperQuartile = (int) (0.75 * scoreList.size());
			
			
			System.out.println("lower Quartile: " + scoreList.get(lowerQuartile));
			System.out.println("Median: " + scoreList.get(median));
			System.out.println("upper Quartile: " + scoreList.get(upperQuartile));
			
			
		} catch (Exception e)	{
			e.printStackTrace();
		}
	}
}
