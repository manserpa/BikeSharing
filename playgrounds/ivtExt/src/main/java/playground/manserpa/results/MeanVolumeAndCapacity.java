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

public class MeanVolumeAndCapacity {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 10;
		
		HashMap<String, Double> capacity = new HashMap<String, Double>();
		HashMap<String, Double> volume = new HashMap<String, Double>();
		HashMap<String, Double> occupancy = new HashMap<String, Double>();
	    
		String csvFileMeanCapacity = "MeanCapacity.csv";
	    FileWriter writerMeanCapacity = new FileWriter(csvFileMeanCapacity);
	    
	    CSVUtils.writeLine(writerMeanCapacity, Arrays.asList("LinkId", "MeanCapacity"), ';');
	    
	    String csvFileMeanVolume = "MeanVolume.csv";
	    FileWriter writerMeanVolume = new FileWriter(csvFileMeanVolume);
	    
	    CSVUtils.writeLine(writerMeanVolume, Arrays.asList("LinkId", "MeanCapacity"), ';');
	    
	    
	    // mean Occupancy not needed here
	    String csvFileMeanOccupancy = "MeanOccupancy.csv";
	    FileWriter writerMeanOccupancy = new FileWriter(csvFileMeanOccupancy);
	    
	    CSVUtils.writeLine(writerMeanOccupancy, Arrays.asList("LinkId", "MeanCapacity"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
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
        }
        
        for(int i = 10; i < numberOfRuns + 10; i++)	{
        	
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
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
        }
        
        for( String e: capacity.keySet())	{
        	if(volume.get(e) != null)	{
        		occupancy.put(e, volume.get(e) / capacity.get(e));
        		volume.put(e, volume.get(e) / 10);
        	}
        	capacity.put(e, capacity.get(e) / 10);
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
					CSVUtils.writeLine(writerMeanOccupancy, Arrays.asList(linkId.getKey(), Double.toString(linkId.getValue())), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		
		
		writerMeanOccupancy.flush();
		writerMeanOccupancy.close();
		
		List<Map.Entry<String, Double>> capacitySorted =
                new LinkedList<Map.Entry<String, Double>>(capacity.entrySet());
		
		Collections.sort(capacitySorted, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
		
		for(Entry<String, Double> linkId: capacitySorted)	{
			if (linkId.getValue() > 0)	{
				try {
					CSVUtils.writeLine(writerMeanCapacity, Arrays.asList(linkId.getKey(), Double.toString(linkId.getValue())), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		writerMeanCapacity.flush();
		writerMeanCapacity.close();
		
		
		
		List<Map.Entry<String, Double>> volumeSorted =
                new LinkedList<Map.Entry<String, Double>>(volume.entrySet());
		
		Collections.sort(volumeSorted, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
		
		for(Entry<String, Double> linkId: volumeSorted)	{
			if (linkId.getValue() > 0)	{
				try {
					CSVUtils.writeLine(writerMeanVolume, Arrays.asList(linkId.getKey(), Double.toString(linkId.getValue())), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		writerMeanVolume.flush();
		writerMeanVolume.close();
		
	}
}
