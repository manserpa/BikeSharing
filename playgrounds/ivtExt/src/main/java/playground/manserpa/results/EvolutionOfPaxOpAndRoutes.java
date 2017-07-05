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

public class EvolutionOfPaxOpAndRoutes {
	public static void main(String[] args) throws IOException {
		
		int numberOfRuns = 10;
		
		HashMap<Integer, String> ops = new HashMap<Integer, String>();
		HashMap<Integer, String> routes = new HashMap<Integer, String>();
		HashMap<Integer, String> pax = new HashMap<Integer, String>();
		HashMap<Integer, String> vehicles = new HashMap<Integer, String>();
	
		List<List<Double>> opsMean = new ArrayList<List<Double>>();
		List<List<Double>> opsIBMean = new ArrayList<List<Double>>();
		List<List<Double>> routesMean = new ArrayList<List<Double>>();
		List<List<Double>> routesIBMean = new ArrayList<List<Double>>();
		List<List<Double>> paxMean = new ArrayList<List<Double>>();
		List<List<Double>> paxIBMean = new ArrayList<List<Double>>();
		List<List<Double>> vehiclesMean = new ArrayList<List<Double>>();
		List<List<Double>> vehiclesIBMean = new ArrayList<List<Double>>();
		
		List<List<Double>> avgBudgetPerOperator = new ArrayList<List<Double>>();
		List<List<Double>> avgScorePerRoute = new ArrayList<List<Double>>();
	    
		String csvFileOps = "EvolutionOfOperators.csv";
	    FileWriter writerOps = new FileWriter(csvFileOps);
	    
	    CSVUtils.writeLine(writerOps, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFileOpsMean = "EvolutionOfOperatorsMean.csv";
	    FileWriter writerOpsMean = new FileWriter(csvFileOpsMean);
	    
	    CSVUtils.writeLine(writerOpsMean, Arrays.asList("Iteration", "Mean", "IB"), ';');
	    
	    String csvFileRoutes = "EvolutionOfRoutes.csv";
	    FileWriter writerRoutes = new FileWriter(csvFileRoutes);
	    
	    CSVUtils.writeLine(writerRoutes, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFileRoutesMean = "EvolutionOfRoutesMean.csv";
	    FileWriter writerRoutesMean = new FileWriter(csvFileRoutesMean);
	    
	    CSVUtils.writeLine(writerRoutesMean, Arrays.asList("Iteration", "Mean", "IB"), ';');
	    
	    String csvFilePax = "EvolutionOfPax.csv";
	    FileWriter writerPax = new FileWriter(csvFilePax);
	    
	    CSVUtils.writeLine(writerPax, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFilePaxMean = "EvolutionOfPaxMean.csv";
	    FileWriter writerPaxMean = new FileWriter(csvFilePaxMean);
	    
	    CSVUtils.writeLine(writerPaxMean, Arrays.asList("Iteration", "Mean", "IB"), ';');
	    
	    String csvFileVehicles = "EvolutionOfVehicles.csv";
	    FileWriter writerVehicles = new FileWriter(csvFileVehicles);
	    
	    CSVUtils.writeLine(writerVehicles, Arrays.asList("50", "100", "150","200","250","300","350","400","450","500","550","600"), ';');
	    
	    String csvFileVehiclesMean = "EvolutionOfVehiclesMean.csv";
	    FileWriter writerVehiclesMean = new FileWriter(csvFileVehiclesMean);
	    
	    CSVUtils.writeLine(writerVehiclesMean, Arrays.asList("Iteration", "Mean", "IB"), ';');
	    
	    String csvFileScoreBudgetMean = "EvolutionOfScoreAndBudgetMean.csv";
	    FileWriter writerScoreBudgetMean = new FileWriter(csvFileScoreBudgetMean);
	    
	    CSVUtils.writeLine(writerScoreBudgetMean, Arrays.asList("Iteration", "MeanBudget", "MeanScore"), ';');
	    
        String line = "";
        
        for(int i = 0; i < numberOfRuns; i++)	{
        	
        	List<Double> opsRun = new ArrayList<>();
        	List<Double> paxRun = new ArrayList<>();
        	List<Double> routesRun = new ArrayList<>();
        	List<Double> vehiclesRun = new ArrayList<>();
        	
        	List<Double> opsIBRun = new ArrayList<>();
        	List<Double> paxIBRun = new ArrayList<>();
        	List<Double> routesIBRun = new ArrayList<>();
        	List<Double> vehiclesIBRun = new ArrayList<>();
        	
        	List<Double> budgetRun = new ArrayList<>();
        	List<Double> scoreRun = new ArrayList<>();
	        
	        if(args[i] != null)	{
		        try (BufferedReader br = new BufferedReader(new FileReader(args[i]))) {
		        	
		        	int k = 0;
		            while ((line = br.readLine()) != null) {
		            	String[] stats = line.split("\t");
		            	if (k != 0)	{
		            		if (Integer.parseInt(stats[0]) % 50 == 0 && Integer.parseInt(stats[0]) != 0)	{
		            			ops.put(Integer.parseInt(stats[0]), stats[2]);
		            			routes.put(Integer.parseInt(stats[0]), stats[4]);
		            			pax.put(Integer.parseInt(stats[0]), stats[6]);
		            			vehicles.put(Integer.parseInt(stats[0]), stats[8]);
		            		}
		            		
		            		if (Integer.parseInt(stats[0]) % 2 == 0)	{
		            			opsRun.add(Double.parseDouble(stats[1]));
		            			routesRun.add(Double.parseDouble(stats[3]));
		            			paxRun.add(Double.parseDouble(stats[5]));
		            			vehiclesRun.add(Double.parseDouble(stats[7]));
		            			
		            			opsIBRun.add(Double.parseDouble(stats[2]));
		            			routesIBRun.add(Double.parseDouble(stats[4]));
		            			paxIBRun.add(Double.parseDouble(stats[6]));
		            			vehiclesIBRun.add(Double.parseDouble(stats[8]));
		            			
		            			budgetRun.add(Double.parseDouble(stats[9]));
		            			scoreRun.add(Double.parseDouble(stats[11]));
		            		}
		            	}
		            	k++;
		            }
		            
		            opsMean.add(opsRun);
		            routesMean.add(routesRun);
		            paxMean.add(paxRun);
		            vehiclesMean.add(vehiclesRun);
		            
		            opsIBMean.add(opsIBRun);
		            routesIBMean.add(routesIBRun);
		            paxIBMean.add(paxIBRun);
		            vehiclesIBMean.add(vehiclesIBRun);
		            
		            avgBudgetPerOperator.add(budgetRun);
		    		avgScorePerRoute.add(scoreRun);

					CSVUtils.writeLine(writerOps, Arrays.asList(ops.get(50), ops.get(100), ops.get(150), ops.get(200), ops.get(250),
							ops.get(300), ops.get(350), ops.get(400), ops.get(450), ops.get(500), ops.get(550), ops.get(600)), ';');
	
					CSVUtils.writeLine(writerRoutes, Arrays.asList(routes.get(50), routes.get(100), routes.get(150), routes.get(200), routes.get(250),
							routes.get(300), routes.get(350), routes.get(400), routes.get(450), routes.get(500), routes.get(550), routes.get(600)), ';');
						
					CSVUtils.writeLine(writerPax, Arrays.asList(pax.get(50), pax.get(100), pax.get(150), pax.get(200), pax.get(250),
							pax.get(300), pax.get(350), pax.get(400), pax.get(450), pax.get(500), pax.get(550), pax.get(600)), ';');
					
					CSVUtils.writeLine(writerVehicles, Arrays.asList(vehicles.get(50), vehicles.get(100), vehicles.get(150), vehicles.get(200), vehicles.get(250),
							vehicles.get(300), vehicles.get(350), vehicles.get(400), vehicles.get(450), vehicles.get(500), vehicles.get(550), vehicles.get(600)), ';');
				
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				ops.clear();
				routes.clear();
				pax.clear();
				vehicles.clear();
	        }
        }
        
        for (int p = 0; p <= 300; p++)	{
        	List<Double> getMeanOps = new ArrayList<Double>();
            List<Double> getMeanRoutes = new ArrayList<Double>();
            List<Double> getMeanPax = new ArrayList<Double>();
            List<Double> getMeanVehicles = new ArrayList<Double>();
        	
            List<Double> getMeanIBOps = new ArrayList<Double>();
            List<Double> getMeanIBRoutes = new ArrayList<Double>();
            List<Double> getMeanIBPax = new ArrayList<Double>();
            List<Double> getMeanIBVehicles = new ArrayList<Double>();
            
            List<Double> getMeanBudget = new ArrayList<Double>();
            List<Double> getMeanScore = new ArrayList<Double>();
            
        	for(int i = 0; i < numberOfRuns; i++)	{
        		getMeanOps.add(opsMean.get(i).get(p));
        		getMeanRoutes.add(routesMean.get(i).get(p));
        		getMeanPax.add(paxMean.get(i).get(p));
        		getMeanVehicles.add(vehiclesMean.get(i).get(p));
        		
        		getMeanIBOps.add(opsIBMean.get(i).get(p));
        		getMeanIBRoutes.add(routesIBMean.get(i).get(p));
        		getMeanIBPax.add(paxIBMean.get(i).get(p));
        		getMeanIBVehicles.add(vehiclesIBMean.get(i).get(p));
        		
        		getMeanBudget.add(avgBudgetPerOperator.get(i).get(p));
        		getMeanScore.add(avgScorePerRoute.get(i).get(p));
        	}
        	
        	double totalNumberOps = 0;
        	double totalNumberRoutes = 0;
        	double totalNumberPax = 0;
        	double totalNumberVehicles = 0;
        	
        	double totalNumberIBOps = 0;
        	double totalNumberIBRoutes = 0;
        	double totalNumberIBPax = 0;
        	double totalNumberIBVehicles = 0;
        	
        	double totalBudget = 0;
        	double totalScore = 0;
        	
        	for (double op : getMeanOps)	{
        		totalNumberOps += op;
        	}
        	for (double op : getMeanRoutes)	{
        		totalNumberRoutes += op;
        	}
        	for (double op : getMeanPax)	{
        		totalNumberPax += op;
        	}
        	for (double op : getMeanVehicles)	{
        		totalNumberVehicles += op;
        	}
        	
        	for (double op : getMeanIBOps)	{
        		totalNumberIBOps += op;
        	}
        	for (double op : getMeanIBRoutes)	{
        		totalNumberIBRoutes += op;
        	}
        	for (double op : getMeanIBPax)	{
        		totalNumberIBPax += op;
        	}
        	for (double op : getMeanIBVehicles)	{
        		totalNumberIBVehicles += op;
        	}
        	
        	for (double op : getMeanBudget)	{
        		totalBudget += op;
        	}
        	for (double op : getMeanScore)	{
        		totalScore += op;
        	}
        	
        	double totalMeanOps = totalNumberOps / getMeanOps.size();
        	double totalMeanRoutes = totalNumberRoutes / getMeanRoutes.size();
        	double totalMeanPax = totalNumberPax / getMeanPax.size();
        	double totalMeanVehicles = totalNumberVehicles / getMeanVehicles.size();
        	
        	double totalMeanIBOps = totalNumberIBOps / getMeanIBOps.size();
        	double totalMeanIBRoutes = totalNumberIBRoutes / getMeanIBRoutes.size();
        	double totalMeanIBPax = totalNumberIBPax / getMeanIBPax.size();
        	double totalMeanIBVehicles = totalNumberIBVehicles / getMeanIBVehicles.size();
        	
        	double totalMeanBudget = totalBudget / getMeanBudget.size();
        	double totalMeanScore = totalScore / getMeanScore.size();
        	
        	try {
	        	CSVUtils.writeLine(writerOpsMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanOps),
	        			Double.toString(totalMeanIBOps)), ';');
	        	CSVUtils.writeLine(writerRoutesMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanRoutes),
	        			Double.toString(totalMeanIBRoutes)), ';');
	        	CSVUtils.writeLine(writerPaxMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanPax),
	        			Double.toString(totalMeanIBPax)), ';');
	        	CSVUtils.writeLine(writerVehiclesMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanVehicles),
	        			Double.toString(totalMeanIBVehicles)), ';');
	        	
	        	CSVUtils.writeLine(writerScoreBudgetMean, Arrays.asList(Integer.toString(p * 2), Double.toString(totalMeanBudget),
	        			Double.toString(totalMeanScore)), ';');			
			} catch (IOException e) {
				e.printStackTrace();
			}
        	
        }
		
        writerOpsMean.flush();
        writerOpsMean.close();
        
        writerRoutesMean.flush();
        writerRoutesMean.close();
        
        writerPaxMean.flush();
        writerPaxMean.close();
        
        writerVehiclesMean.flush();
        writerVehiclesMean.close();
		
		writerOps.flush();
		writerOps.close();
		
		writerRoutes.flush();
		writerRoutes.close();
		
		writerPax.flush();
		writerPax.close();
		
		writerVehicles.flush();
		writerVehicles.close();
		
		writerScoreBudgetMean.flush();
		writerScoreBudgetMean.close();
	}
}
