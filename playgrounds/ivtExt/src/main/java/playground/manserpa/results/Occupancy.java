package playground.manserpa.results;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import playground.manserpa.spatialData.CSVUtils;

@Deprecated
public class Occupancy {
	public static void main(String[] args) throws IOException {
		
		HashMap<String, Double> capacity = new HashMap<String, Double>();
		HashMap<String, Double> volume = new HashMap<String, Double>();
		HashMap<String, Double> occupancy = new HashMap<String, Double>();
	    
		
	    String csvFileOccupancy = "Occupancy.csv";
	    FileWriter writerOccupancy = new FileWriter(csvFileOccupancy);
	    
	    CSVUtils.writeLine(writerOccupancy, Arrays.asList("LinkId", "MeanCapacity"), ';');
	    
        String line = "";
        
	    if(args[0] != null)	{
	        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
	        	int k = 0;
	        	while ((line = br.readLine()) != null) {
		           	String[] stats = line.split(";");
		           	if (k != 0)	{
		           		capacity.put(stats[0], capacity.getOrDefault(stats[0], 0.0) + Double.parseDouble(stats[1]));
		           	}
		       	k++;
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
        	
	    if(args[1] != null)	{
	        try (BufferedReader br = new BufferedReader(new FileReader(args[1]))) {
	        	int k = 0;
	            while ((line = br.readLine()) != null) {
	            	String[] stats = line.split(";");
	            	if (k != 0)	{
	            		volume.put(stats[0], volume.getOrDefault(stats[0], 0.0) + Double.parseDouble(stats[1]));
	            	}
	            	k++;
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
        
        for( String e: capacity.keySet())	{
        	if(volume.get(e) != null)	{
        		occupancy.put(e, volume.get(e) / capacity.get(e));
        	}
        }
        
        List<Map.Entry<String, Double>> occupancySorted =
                new LinkedList<Map.Entry<String, Double>>(occupancy.entrySet());
		
		Collections.sort(occupancySorted, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
		
		for(Entry<String, Double> linkId: occupancySorted)	{
			if (linkId.getValue() > 0)	{
				try {
					CSVUtils.writeLine(writerOccupancy, Arrays.asList(linkId.getKey(), Double.toString(linkId.getValue())), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		writerOccupancy.flush();
		writerOccupancy.close();
	}
}
