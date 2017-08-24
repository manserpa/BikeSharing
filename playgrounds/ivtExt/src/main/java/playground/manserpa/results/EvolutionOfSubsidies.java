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

public class EvolutionOfSubsidies {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 10;
		
		List<List<Double>> subsidiesMean = new ArrayList<List<Double>>();
	    
		String csvFileSubsidies = "EvolutionOfSubsidies.csv";
	    FileWriter writerSubsidies = new FileWriter(csvFileSubsidies);
	    
	    CSVUtils.writeLine(writerSubsidies, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFileSubsidiesMean = "EvolutionOfSubsidiesMean.csv";
	    FileWriter writerSubsidiesMean = new FileWriter(csvFileSubsidiesMean);
	    
	    CSVUtils.writeLine(writerSubsidiesMean, Arrays.asList("Iteration", "Mean"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
        	HashMap<Integer, Double> subsidies = new HashMap<Integer, Double>();
        	List<Double> subsidiesRun = new ArrayList<>();
	        
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
		        	int iteration = 0;
		        	int lastIteration = 0;
		        	double subsidiesIteration = 0.0;
		        	int k = 0;
		        	while ((line = br.readLine()) != null) {
		            	String[] stats = line.split("\t");
		            	if (k != 0)	{
		            		iteration = Integer.parseInt(stats[0]);
		            		if (lastIteration == iteration) {
		            			if (stats[2].equals("INBUSINESS") && !stats[3].equals("=====") && iteration % 2 == 0) {
		            				subsidiesIteration += Double.parseDouble(stats[13]);
		            			}
		           			} else {
		           				if (lastIteration % 50 == 0 && lastIteration != 0) {
			           				subsidies.put(lastIteration, subsidiesIteration);
		           				}
		           				if (lastIteration % 2 == 0) {
		           					subsidiesRun.add(subsidiesIteration);
		           					subsidiesIteration = 0.0;
		           				}
		            		}
		            	}
		            	lastIteration = iteration;
		            	k++;
		            }
		        	if (lastIteration == 600) {
						// Processes the last iteration after loop ended
						subsidies.put(lastIteration, subsidiesIteration);
						subsidiesRun.add(subsidiesIteration);
					}
		        	
		        	subsidiesMean.add(subsidiesRun);

					CSVUtils.writeLine(writerSubsidies, Arrays.asList(subsidies.get(50).toString(), subsidies.get(100).toString(), subsidies.get(150).toString(), subsidies.get(200).toString(), subsidies.get(250).toString(),
							subsidies.get(300).toString(), subsidies.get(350).toString(), subsidies.get(400).toString(), subsidies.get(450).toString(), subsidies.get(500).toString(), subsidies.get(550).toString(), subsidies.get(600).toString()), ';');
	
				
				} catch (IOException e) {
					
					e.printStackTrace();
				}
		        
		        subsidies.clear();
	        }
        }
        
        for (int p = 0; p <= 300; p++)	{
        	List<Double> getMeanSubsidies = new ArrayList<Double>();
        	
        	for(int i = 0; i < numberOfRuns; i++)	{
        		getMeanSubsidies.add(subsidiesMean.get(i).get(p));
        	}
        	double totalNumberSubsidies = 0;
        	
        	for (double subs : getMeanSubsidies)	{
        		totalNumberSubsidies += subs;
        	}
        	
        	double totalMeanSubsidies = totalNumberSubsidies / getMeanSubsidies.size();
        	
        	try {
	        	CSVUtils.writeLine(writerSubsidiesMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanSubsidies)), ';');
			
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        }
		
        writerSubsidiesMean.flush();
        writerSubsidiesMean.close();
		
		writerSubsidies.flush();
		writerSubsidies.close();
	}
}
