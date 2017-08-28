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
public class MeanVolume {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 5;
		
		HashMap<String, Double> volume = new HashMap<String, Double>();
	    
	    String csvFileMeanVolume = "MeanCapacity.csv";
	    FileWriter writerMeanVolume = new FileWriter(csvFileMeanVolume);
	    
	    CSVUtils.writeLine(writerMeanVolume, Arrays.asList("LinkId", "MeanCapacity"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
		        	int k = 0;
		            while ((line = br.readLine()) != null) {
		            	String[] stats = line.split(";");
		            	if (k != 0)	{
		            		if( Double.parseDouble(stats[1]) > volume.getOrDefault(stats[0],0.0))	{
		            			volume.put(stats[0], Double.parseDouble(stats[1]));
		            		}
		            	}
		            	k++;
		            }
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
        }
        
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
