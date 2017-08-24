package playground.manserpa.results;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import playground.manserpa.spatialData.CSVUtils;

public class EvolutionOfScores {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 10;
		
		HashMap<Integer, String> ops = new HashMap<Integer, String>();
	
		List<List<Double>> opsMean = new ArrayList<List<Double>>();
	    
		String csvFileOps = "EvolutionOfScores.csv";
	    FileWriter writerOps = new FileWriter(csvFileOps);
	    
	    CSVUtils.writeLine(writerOps, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFileOpsMean = "EvolutionOfScoresMean.csv";
	    FileWriter writerOpsMean = new FileWriter(csvFileOpsMean);
	    
	    CSVUtils.writeLine(writerOpsMean, Arrays.asList("Iteration", "Mean"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
        	List<Double> opsRun = new ArrayList<>();
	        
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
		        	
		        	int k = 0;
		            while ((line = br.readLine()) != null) {
		            	String[] stats = line.split("\t");
		            	if (k != 0)	{
		            		if (Integer.parseInt(stats[0]) % 50 == 0 && Integer.parseInt(stats[0]) != 0)	{
		            			ops.put(Integer.parseInt(stats[0]), stats[5]);
		            		}
		            		
		            		if (Integer.parseInt(stats[0]) % 2 == 0)	{
		            			opsRun.add(Double.parseDouble(stats[5]));
		            		}
		            	}
		            	k++;
		            }
		            
		            opsMean.add(opsRun);

					CSVUtils.writeLine(writerOps, Arrays.asList(ops.get(50), ops.get(100), ops.get(150), ops.get(200), ops.get(250),
							ops.get(300), ops.get(350), ops.get(400), ops.get(450), ops.get(500), ops.get(550), ops.get(600)), ';');
	
				
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ops.clear();
	        }
        }
        
        for (int p = 0; p <= 300; p++)	{
        	List<Double> getMeanOps = new ArrayList<Double>();
        	
        	for(int i = 0; i < numberOfRuns; i++)	{
        		getMeanOps.add(opsMean.get(i).get(p));
        	}
        	double totalNumberOps = 0;
        	
        	for (double op : getMeanOps)	{
        		totalNumberOps += op;
        	}
        	
        	double totalMeanOps = totalNumberOps / getMeanOps.size();
        	
        	try {
	        	CSVUtils.writeLine(writerOpsMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanOps)), ';');
			
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        }
		
        writerOpsMean.flush();
        writerOpsMean.close();
		
		writerOps.flush();
		writerOps.close();
	}
}
