package playground.manserpa.results;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import playground.manserpa.spatialData.CSVUtils;


/**
 * 
 * Purpose: prepare a csv for a Matlab script (see this folder). It shows all the mutation of the operators and the mutation type at a given time
 * during the simulation
 * 
 * Input: pOperatorlogger.txt
 * Output: csv
 * 
 * @author manserpa
 * 
 */


public class OperatorMutations {
	
	public static void main(String[] args)  {
		
		try{

	        //Create object of FileReader
	        FileReader inputFile = new FileReader(args[0]);

	        //Instantiate the BufferedReader Class
	        BufferedReader bufferReader = new BufferedReader(inputFile);

	        //Variable to hold the one line data
	        String line;
	          
	        List<IterationStats> itStats = new ArrayList<>();
	          
	        int i = 0;

	        // Read file line by line and print on the console
	        while ((line = bufferReader.readLine()) != null)   {
	        	if(i != 0)	{
	        		List<String> psList = new ArrayList <>();
	        		  
	        		for (String part : line.split("\\s+")) {
	        	      	psList.add(part);
	        	    }
	        		  
	        		String[] itOp = psList.get(1).split("_");
	        		  
	        		int iteration = Integer.parseInt(itOp[1]);
	        		int operatorId = Integer.parseInt(itOp[2]);
	        		String routeIdNew = psList.get(3);
	        		String routeIdOld = psList.get(5);
	        		String mutation = psList.get(4);
	        		int lastIteration = Integer.parseInt(psList.get(0));
	        		  
	        		if(!mutation.equals("=====")) {
	        			IterationStats thisIteration = new IterationStats(lastIteration, iteration, operatorId, routeIdNew, routeIdOld, mutation);
	        			itStats.add(thisIteration);	
	        		}
	        	  
	        	}
	        	i ++;
	      }
	      
	        String csvFile2 = "MutationTrackerUnsuccessfull.csv";
		    FileWriter writer2 = new FileWriter(csvFile2);
		          
		    CSVUtils.writeLine(writer2, Arrays.asList("Iteration", "Operator", "IterationOfMutation", "Mutation"), ';');
		          
			for(IterationStats l: itStats)	{
				String[] mutationId = l.routeIdNew.split("_");
				try {
					CSVUtils.writeLine(writer2, Arrays.asList(Integer.toString(l.iteration), Integer.toString(l.operator), 
							mutationId[0],l.mutation), ';');
				} catch (IOException e) {
					e.printStackTrace();
				}   
			}	
			
	      List<OperatorStats> opStats = new ArrayList<>();
	        
	      // man muss dies für alle Linien machen, welche in der letzten Iteration noch überleben.
	      int it = 1;
	        
	      while(itStats.get(itStats.size() - 1).lastiteration == itStats.get(itStats.size() - it).lastiteration)	{
	  		
		  	int iterator = itStats.size() - it;
		  	int ancor = itStats.size() - it;
		  		
		  	String[] routeMutatedIt = itStats.get(ancor).routeIdNew.split("_");
		  		
		  	// das ist die letzte erfolgreiche Mutation
		  	OperatorStats thisOperation = new OperatorStats(itStats.get(ancor).iteration, itStats.get(ancor).operator, 
		  			Integer.parseInt(routeMutatedIt[0]), itStats.get(ancor).mutation, itStats.get(ancor).lastiteration);
			opStats.add(thisOperation);	
		  			
		  	// dann die vorgängige Mutation suchen, die zur erfolgreichen Mutation geführt haben
		  	while(iterator >= 0)	{
		  		if (itStats.get(iterator).iteration == itStats.get(ancor).iteration && itStats.get(iterator).operator == itStats.get(ancor).operator
		  				&& itStats.get(iterator).routeIdNew.equals(itStats.get(ancor).routeIdOld))	{
		  				
		  			routeMutatedIt = itStats.get(iterator).routeIdNew.split("_");
		  				
		  			thisOperation = new OperatorStats(itStats.get(iterator).iteration, itStats.get(iterator).operator, 
		  					Integer.parseInt(routeMutatedIt[0]), itStats.get(iterator).mutation, itStats.get(iterator).lastiteration);
		  			opStats.add(thisOperation);	
		  			ancor = iterator;
		  		}
		  				
		  		iterator--;
		  	}
		  	it ++;
		}
	  		
	    //Close the buffer reader
	    bufferReader.close();
	          
	    String csvFile = "MutationTracker.csv";
	    FileWriter writer = new FileWriter(csvFile);
	          
	    CSVUtils.writeLine(writer, Arrays.asList("Iteration", "Operator", "IterationOfMutation", "Mutation","SimulationIt"), ';');
	          
		for(OperatorStats l: opStats)	{
			try {
				CSVUtils.writeLine(writer, Arrays.asList(Integer.toString(l.iteration), Integer.toString(l.operator), 
					Integer.toString(l.routeIteration), l.mutation, Integer.toString(l.lastiteration)), ';');
			} catch (IOException e) {
				e.printStackTrace();
			}   
		}	
				
		writer.flush();
		writer.close();
		
		writer2.flush();
		writer2.close();
	          
	    }catch(Exception e){
	    	System.out.println("Error while reading file line by line:" + e.getMessage());                      
	    }	
	}
}

class IterationStats	{
	int lastiteration;
	int iteration;
	int operator;
	String routeIdNew;
	String routeIdOld;
	String mutation;
	
	public IterationStats(int lastiteration, int iteration, int operator, String routeIdNew, String routeIdOld, String mutation)	{
		this.lastiteration = lastiteration;
		this.iteration = iteration;
		this.operator = operator;
		this.routeIdNew = routeIdNew;
		this.routeIdOld = routeIdOld;
		this.mutation = mutation;
	}
}

class OperatorStats	{
	int iteration;
	int operator;
	int routeIteration;
	String mutation;
	int lastiteration;
	
	public OperatorStats(int iteration, int operator, int routeIteration, String mutation, int lastiteration)	{
		this.iteration = iteration;
		this.operator = operator;
		this.routeIteration = routeIteration;
		this.mutation = mutation;
		this.lastiteration = lastiteration;
	}
}