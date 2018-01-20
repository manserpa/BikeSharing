package playground.manserpa.minibus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.minibus.genericUtils.GridNode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.manserpa.spatialData.CSVUtils;

public final class GridCellActivities {
	
	private FileWriter writer;
	
	private Scenario scenario;
	
	private HashMap<String, Integer> gridNodeId2ActsCountMap = new HashMap<>();
	
	private Geometry include;
	private Geometry exclude;
	private final GeometryFactory factory;
	
	private GridCellActivities()	{
	
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new PopulationReader(scenario).readFile("population.xml.gz");
		
		String line = "";
        String agentsToDeleteFile = "AgentsToDelete.csv";
        
        if(agentsToDeleteFile != null)	{
	        try (BufferedReader br = new BufferedReader(new FileReader(agentsToDeleteFile))) {
	
	            while ((line = br.readLine()) != null) {
	            	scenario.getPopulation().removePerson(Id.create(line, Person.class));
	            }
	
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
        }
        
        factory = new GeometryFactory();
		
		readShapeFile("ScenarioBoundaries.shp");
	
	}
	
	
	public void readShapeFile(String shpFile) {
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(shpFile);
		Collection<Geometry> include = new ArrayList<>();
		Collection<Geometry> exclude = new ArrayList<>();
		
		for(SimpleFeature f: features){
			boolean incl = true;
			Geometry g = null;
			for(Object o: f.getAttributes()){
				if(o instanceof Polygon){
					g = (Geometry) o;
				}else if (o instanceof MultiPolygon){
					g = (Geometry) o;
				}
				else if (o instanceof String){
					incl = Boolean.parseBoolean((String) o);
				}
			}
			if(! (g == null)){
				if(incl){
					include.add(g);
				}else{
					exclude.add(g);
				}
			}
		}
		
		this.include = this.factory.createGeometryCollection(include.toArray(new Geometry[include.size()])).buffer(0);
		this.exclude = this.factory.createGeometryCollection(exclude.toArray(new Geometry[exclude.size()])).buffer(0);
	}
	
	private boolean nodeInServiceArea(double x, double y) {
		Coordinate coord = new Coordinate(x, y);
		Point p = factory.createPoint(coord);
		if(this.include.contains(p)){
			if(exclude.contains(p)){
				return false;
			}
			return true;
		}
		return false;
	}

	
	private void run()	{
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					if(!act.getType().equals("pt interaction") && nodeInServiceArea(act.getCoord().getX(), act.getCoord().getY()))	{
						String gridNodeId = GridNode.getGridNodeIdForCoord(act.getCoord(), 300);
						if (gridNodeId2ActsCountMap.get(gridNodeId) == null) {
							gridNodeId2ActsCountMap.put(gridNodeId, 0);
						}
						gridNodeId2ActsCountMap.put(gridNodeId, gridNodeId2ActsCountMap.get(gridNodeId) + 1);
					}
				}
			}
		}
	}
	
	
	private void write()	{
		try {
			writer = new FileWriter("activities.csv");
	
	    CSVUtils.writeLine(writer , Arrays.asList("GridCell", "Activities"), ';');
	    for(String id: gridNodeId2ActsCountMap.keySet())	{
	    	CSVUtils.writeLine(writer, Arrays.asList(id, Integer.toString(gridNodeId2ActsCountMap.get(id))), ';');
	    }
	    writer.flush();
	    writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	
	public static void main(String[] args)	{
		
		GridCellActivities gca = new GridCellActivities();
		gca.run();
		gca.write();
		
	}
}
