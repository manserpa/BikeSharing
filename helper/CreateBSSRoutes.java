import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.InvalidGridGeometryException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.DirectPosition2D;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import org.opengis.referencing.operation.TransformException;

/*
 * purpose: create bikeshare routes based on a list of stations
 * input file = xml file with stations (copy-paste from transit schedule possible)
 * output file = xml file with routes (has to be pasted in the transit line section in the transit schedule file)
 * 
 * different settings can be used to consider different types of bicycles (adjust speed and effect on speed of slopes) as well as different types of systems (access and egress time)
 * 
 * possible improvements:
 * - base "slope effects" on real world observations 
 * - use actual real world elevation
 * - base distances on a bike routing instead of beeline distance (benefits: better distance, consider actual slopes instead of difference between start and stop)
 */

public class CreateBSSRoutes {
/*
	// parameter set regular 	
	static final String setName = "regular"; // e. g. regular, e-bike (used for file name)
	
	static final double speed = 14.4; // unit: km/h
	static final double deviationFactor = 1.3; // beeline distance gets multiplied by that. should probably better be regressive (i. e. nearly one at high distances, high at low distances)
	static final double accessTime = 40; // access time in seconds
	static final double egressTime = 20; // egress time in seconds
	static final double ascendingSlopeEffect = 5.0; // controls the effect of ascending slope sections (10 = 1 height metre adds 10 metres) (not used, because elevation is not implemented)
	static final double descendingSlopeEffect = 2.9; // controls the effect of descending slope sections (2 = 1 height metre lowers distance by 2 metres) (not used, because elevation is not implemented)
*/
	
	// parameter set ebike
	static final String setName = "ebike"; // e. g. regular, e-bike (used for file name)
	
	static final double speed = 19.1; // unit: km/h
	static final double deviationFactor = 1.3; // beeline distance gets multiplied by that. should probably better be regressive (i. e. nearly one at high distances, high at low distances)
	static final double accessTime = 40; // access time in seconds
	static final double egressTime = 20; // egress time in seconds
	static final double ascendingSlopeEffect = 3.2; // controls the effect of ascending slope sections (10 = 1 height metre adds 10 metres) (not used, because elevation is not implemented)
	static final double descendingSlopeEffect = 2.6; // controls the effect of descending slope sections (2 = 1 height metre lowers distance by 2 metres) (not used, because elevation is not implemented)


	static final String geotiffPath = "C:\\matsim\\dem\\GDAL_ADF_2_GeoTIFF.tif";
	static final String trafficCRS = "EPSG:26914"; // = CRS of the stops 
	static final String geodataCRS = "EPSG:4269";  // = CRS of the geotiff

	
	public static void main(String[] args)  throws FileNotFoundException, InvalidGridGeometryException, TransformException {
		
		List<Element> stations = new ArrayList<Element>(); 
		List<BSSRoute> routes = new ArrayList<BSSRoute>();

		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(args[0]);
		
		try {
			// build list of all stations
			Document document = (Document) builder.build(xmlFile);
			Element rootNode = document.getRootElement();
			List<Element> list = rootNode.getChildren("stopFacility");

			for (int i = 0; i < list.size(); i++) {
				Element node = (Element) list.get(i);
				stations.add(node);
			}

			
			// init tiff
			File file = new File(geotiffPath);
			GeoTiffReader reader = new GeoTiffReader(file);
			
			GridCoverage2D grid = (GridCoverage2D) reader.read(null);
			Raster gridData = grid.getRenderedImage().getData();
//			Envelope env = coverage.getEnvelope();
//			RenderedImage image = coverage.getRenderedImage();
			
			
			// create routes for all connections between all stations, taking into account the settings from above (create XML structure)
			Iterator<Element> it = list.iterator();
			while(it.hasNext()){
				Element i = (Element) it.next();
				
				Iterator<Element> it2 = list.iterator();
				while(it2.hasNext()){
					Element j = (Element) it2.next();
					
					if ( !i.getAttribute("id").equals(j.getAttribute("id"))) // only do the following if we do not have two times the same station 
					{
						
						// obtain z values
						// partly from http://www.smartjava.org/content/access-information-geotiff-using-java
						Coord startCoord2D = CoordUtils.createCoord(i.getAttribute("x").getDoubleValue(), i.getAttribute("y").getDoubleValue());
						Coord endCoord2D = CoordUtils.createCoord(j.getAttribute("x").getDoubleValue(), j.getAttribute("y").getDoubleValue());
						CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(trafficCRS, geodataCRS);
						
						Coord startCoord2DNewCRS = ct.transform(startCoord2D);
						Coord endCoord2DNewCRS = ct.transform(endCoord2D);
						
						double[] pixel;
						double[] data;
						GridGeometry2D gg = grid.getGridGeometry();
						

						DirectPosition2D posWorldStart = new DirectPosition2D(startCoord2DNewCRS.getX(), startCoord2DNewCRS.getY());
				        GridCoordinates2D posGridStart = gg.worldToGrid(posWorldStart);
				 
				        pixel = new double[1];
				        data = gridData.getPixel(posGridStart.x, posGridStart.y, pixel);
						double zStart = data[0];
						
				        
						DirectPosition2D posWorldEnd = new DirectPosition2D(endCoord2DNewCRS.getX(), endCoord2DNewCRS.getY());
				        GridCoordinates2D posGridEnd = gg.worldToGrid(posWorldEnd);
				 
				        pixel = new double[1];
				        data = gridData.getPixel(posGridEnd.x, posGridEnd.y, pixel);
						double zEnd = data[0];
						
						
						Coord startCoord3D = CoordUtils.createCoord(startCoord2D.getX(), startCoord2D.getY(), zStart);
						Coord endCoord3D = CoordUtils.createCoord(endCoord2D.getX(), endCoord2D.getY(), zEnd);

						
						// calculate the distance
						// we could have calculated the distance based on the 2D situation, but we anyways calculate the z difference so we can also integrate that without much effort
						// obviously, it would be much better to get actual routes, so street network topology, number of intersections etc. could be integrated in the distance
						double dis = CoordUtils.calcEuclideanDistance(startCoord3D, endCoord3D);
						dis = dis * deviationFactor;
						double disImaginary = dis; // used for travel time calculation (takes into account factors that prolongate the travel time, implemented: height difference) 
						

						// effect of height difference
						double deltaZ = zEnd - zStart;
						if ( deltaZ > 0 ) disImaginary = disImaginary + (deltaZ * ascendingSlopeEffect);
						else disImaginary = disImaginary - (deltaZ * descendingSlopeEffect);
						
					
						// add extra travel time for intersections here (not implemented, could be if routing is added)

						
						
						// sum up travel time (incl. access/egress) 
						double tt = disImaginary / (speed / 3.6) + accessTime + egressTime; // tt in seconds
						
					
						// add to routes list (required: start and end stop id, travel time)
						BSSRoute thisRoute = new BSSRoute(i.getAttributeValue("id"), j.getAttributeValue("id"), tt, dis, disImaginary, deltaZ);
						routes.add(thisRoute);
						
					
					}
					
				}
				
			}
			
			
			// write the files

/*			
 			it seems we don't need to generate new stops	
 			
			// stops
			Element rootNodeOutputStops = new Element("transitStops");
			
			it = list.iterator();
			while(it.hasNext()){
				Element i = (Element) it.next();
				
				Iterator<Element> it2 = list.iterator();
				while(it2.hasNext()){
					Element j = (Element) it2.next();
					
					if ( !i.getAttribute("id").equals(j.getAttribute("id"))) // only do the following if we do not have two times the same station 
					{
						Element stopFacilityStart = new Element("stopFacility");
						stopFacilityStart.setAttribute("linkRefId", i.getAttribute("linkRefId").getValue());
						stopFacilityStart.setAttribute("x", i.getAttribute("x").getValue());
						stopFacilityStart.setAttribute("y", i.getAttribute("y").getValue());
						stopFacilityStart.setAttribute("id", i.getAttribute("id").getValue() + "___" + j.getAttribute("id").getValue() + "___0");
						stopFacilityStart.setAttribute("name", i.getAttribute("id").getValue() + "___" + j.getAttribute("id").getValue() + "___0");

						Element stopFacilityEnd = new Element("stopFacility");
						stopFacilityEnd.setAttribute("linkRefId", j.getAttribute("linkRefId").getValue());
						stopFacilityEnd.setAttribute("x", j.getAttribute("x").getValue());
						stopFacilityEnd.setAttribute("y", j.getAttribute("y").getValue());
						stopFacilityEnd.setAttribute("id", i.getAttribute("id").getValue() + "___" + j.getAttribute("id").getValue() + "___1");
						stopFacilityEnd.setAttribute("name", i.getAttribute("id").getValue() + "___" + j.getAttribute("id").getValue() + "___1");
						
						rootNodeOutputStops.addContent(stopFacilityStart);
						rootNodeOutputStops.addContent(stopFacilityEnd);
					
					}
				}
			}
			PrintWriter outputStops = new PrintWriter ("bssStops.xml") ;
			Document docStops = new Document(rootNodeOutputStops);
			
			try {
				XMLOutputter serializer = new XMLOutputter();
		        serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
				serializer.output(docStops, outputStops);
			}
			catch (IOException e) {
				System.err.println(e);
			}

			outputStops.close() ;
*/			
						
			
			
			// routes
			Element rootNodeOutputRoutes = new Element("transitLine");

			Iterator<BSSRoute> itRoutes = routes.iterator();
			while(itRoutes.hasNext())
			{
				BSSRoute thisRoute = itRoutes.next();
				
				Element transitRoute = new Element("transitRoute");
				transitRoute.setAttribute("id", "bikeshare___"+thisRoute.getStartid()+"___"+thisRoute.getEndid());
				
				Element transportMode = new Element("transportMode");
				transportMode.addContent("bikeshare");
				
				Element routeProfile = new Element("routeProfile");
				Element startStop = new Element("stop");
//				startStop.setAttribute("refId", thisRoute.getStartid() + "___" + thisRoute.getEndid() + "___0");
				startStop.setAttribute("refId", thisRoute.getStartid());
				startStop.setAttribute("departureOffset", "00:00:00");

				Element endStop = new Element("stop");
//				endStop.setAttribute("refId", thisRoute.getStartid() + "___" + thisRoute.getEndid() + "___1");
				endStop.setAttribute("refId", thisRoute.getEndid());
				int hr = ((Double)(thisRoute.getTt()/3600)).intValue();
				int min = ((Double)((thisRoute.getTt()%3600)/60)).intValue();
				int s = ((Double)(thisRoute.getTt()%60)).intValue();
				endStop.setAttribute("arrivalOffset", hr + ":" + min + ":" + s);
				
				routeProfile.addContent(startStop);
				routeProfile.addContent(endStop);
				
				
				Element departures = new Element("departures");
				
				transitRoute.addContent(transportMode);
				transitRoute.addContent(routeProfile);
				transitRoute.addContent(departures);

				rootNodeOutputRoutes.addContent(transitRoute);
			}

			PrintWriter output = new PrintWriter ("bssRoutes-"+setName+".xml") ;
			Document doc = new Document(rootNodeOutputRoutes);
			
			try {
				XMLOutputter serializer = new XMLOutputter();
		        serializer.setFormat( Format.getPrettyFormat().setIndent( "  " ) );
				serializer.output(doc, output);
				
				System.out.println("xml files written");
			}
			catch (IOException e) {
				System.err.println(e);
			}
			output.close() ;
			
			
			// write control files
			Map<String,HashMap> fromToStations = new HashMap<String, HashMap>();
			
			Iterator<BSSRoute> itR = routes.iterator();
			while (itR.hasNext()){
				BSSRoute thisRoute = (BSSRoute) itR.next();
				if ( fromToStations.containsKey(thisRoute.getStartid()) ){
					HashMap<String, BSSRoute> thisStart = fromToStations.get(thisRoute.getStartid());
					thisStart.put(thisRoute.getEndid(), thisRoute);
				}
				else
				{
					HashMap<String,BSSRoute> thisStart = new HashMap<String, BSSRoute>();
					thisStart.put(thisRoute.getEndid(), thisRoute);
					fromToStations.put(thisRoute.getStartid(), thisStart);
				}
			}
			
			
			File ttFile = new File("tt-"+setName+".csv");
			File disFile = new File ("dis-"+setName+".csv");
			File disImFile = new File ("disIm-"+setName+".csv");
			File heightDiffFile = new File ("heightDiff-"+setName+".csv");

			PrintWriter ttOutput = new PrintWriter ( ttFile ) ;
			PrintWriter disOutput = new PrintWriter ( disFile ) ;
			PrintWriter disImOutput = new PrintWriter ( disImFile ) ;
			PrintWriter heightDiffOutput = new PrintWriter ( heightDiffFile ) ;
			
			Set<String> setSt = fromToStations.keySet();
			boolean start = true;
			LinkedList<String> cols = new LinkedList<String>();
			
			for(String entry: setSt){
				Map<String,BSSRoute> thisStart = fromToStations.get(entry);
				Set<String> setStTo = thisStart.keySet();
				
				// adds the first row, contains all the "to" station ids
				if ( start ){
					

					// first cel is empty
					ttOutput.print("\t");
					disOutput.print("\t");
					disImOutput.print("\t");
					heightDiffOutput.print("\t");

					cols.add(entry);

					ttOutput.print(entry);
					disOutput.print(entry);
					disImOutput.print(entry);
					heightDiffOutput.print(entry);
					
					ttOutput.print("\t");
					disOutput.print("\t");
					disImOutput.print("\t");
					heightDiffOutput.print("\t");
					

					for(String entryTo: setStTo	){
						cols.add(entryTo);
						
						ttOutput.print(entryTo);
						disOutput.print(entryTo);
						disImOutput.print(entryTo);
						heightDiffOutput.print(entryTo);
						
						ttOutput.print("\t");
						disOutput.print("\t");
						disImOutput.print("\t");
						heightDiffOutput.print("\t");
					}

					ttOutput.println("");
					disOutput.println("");
					disImOutput.println("");
					heightDiffOutput.println("");

					start = false;
				}
				
				// prints the "from" station id
				ttOutput.print(entry);
				disOutput.print(entry);
				disImOutput.print(entry);
				heightDiffOutput.print(entry);
				
				ttOutput.print("\t");
				disOutput.print("\t");
				disImOutput.print("\t");
				heightDiffOutput.print("\t");
				
				
				// prints the actual values
				for(String entryTo: cols ){
					BSSRoute thisRoute = thisStart.get(entryTo);
					
					if ( thisRoute != null)
					{
						ttOutput.print(thisRoute.getTt());
						disOutput.print(thisRoute.getDis());
						disImOutput.print(thisRoute.getDisImaginary());
						heightDiffOutput.print(thisRoute.getHeightDiff());
					}
					
					ttOutput.print("\t");
					disOutput.print("\t");
					disImOutput.print("\t");
					heightDiffOutput.print("\t");
					
				}
				
				ttOutput.println("");
				disOutput.println("");
				disImOutput.println("");
				heightDiffOutput.println("");
				
			}
			
			ttOutput.close();
			disOutput.close();
			disImOutput.close();
			heightDiffOutput.close();
				
			System.out.println("csv files written");
		
		} catch (IOException io) {
			System.out.println(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
		}
			
		

	}

}



