/* *********************************************************************** *
 * project: org.matsim.*
 * PtRate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.yu.mautZH;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;

/**
 * An implementing of ContorlerListener, in order to output some information
 * about use of public transit through .txt-file and .png-picture
 * 
 * @author yu
 * 
 */
public class PtRate implements IterationEndsListener, ShutdownListener {
	// -----------------------------MEMBER VARIABLES-----------------------
	private final Plans population;
	private final PtCheck check;
	private final int maxIters;
	private final String BetaTraveling;
	private final String BetaTravelingPt;
	private double[] yPtRate = null;// an array, in which the fraction of
	// persons, who use public transit, will be
	// saved.
	private double[] yPtUser = null;// an array, in which the amount of persons,
	// who use public transit, will be saved.
	private double[] yPersons = null;// an array, in which the amount of all

	// persons in the simulation will be
	// saved.
	// -------------------------------CONSTRUCTOR---------------------------
	/**
	 * @param population -
	 *            the object of Plans in the simulation
	 * @param filename -
	 *            filename of .txt-file
	 * @param maxIters -
	 *            maximum number of iterations
	 * @param BetaTraveling -
	 *            parameter of marginal Utility of Traveling
	 * @param BetaTravelingPt -
	 *            parameter of marginal Utility of Traveling with public transit
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public PtRate(final Plans population, final String filename, int maxIters,
			String BetaTraveling, String BetaTravelingPt)
			throws FileNotFoundException, IOException {
		this.population = population;
		this.maxIters = maxIters;
		this.BetaTraveling = BetaTraveling;
		this.BetaTravelingPt = BetaTravelingPt;
		check = new PtCheck(filename);
		yPtRate = new double[maxIters / 10 + 1];
		yPtUser = new double[maxIters / 10 + 1];
		yPersons = new double[maxIters / 10 + 1];
	}

	/**
	 * writes .txt-file and paints 2 .png-picture
	 */
	public void notifyIterationEnds(IterationEndsEvent event) {
		int idx = event.getIteration();
		if (idx % 10 == 0) {
			Config cf = event.getControler().getConfig();
			check.resetCnt();
			check.run(population);
			yPtRate[idx / 10] = check.getPtRate();
			yPtUser[idx / 10] = check.getPtUserCnt();
			yPersons[idx / 10] = check.getPersonCnt();
			try {
				check.write(idx);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (idx == maxIters) {
				double[] x = new double[maxIters + 1];
				for (int i = 0; i < maxIters / 10 + 1; i++) {
					x[i] = i * 10;
				}
				XYLineChart ptRateChart = new XYLineChart("Schweiz: PtRate, "
						+ maxIters + "ITERs, BetaTraveling=" + BetaTraveling
						+ ", BetaTravelingPt=" + BetaTravelingPt
						+ ", BetaPerforming="
						+ cf.getParam("planCalcScore", "performing")
						+ ", flowCapacityFactor="
						+ cf.getParam("simulation", "flowCapacityFactor")
						+ ", storageCapacityFactor="
						+ cf.getParam("simulation", "storageCapacityFactor")
						+ ", " + cf.getParam("strategy", "ModuleProbability_2")
						+ "-ReRoute_Landmarks, "
						+ cf.getParam("strategy", "ModuleProbability_3")
						+ "-TimeAllocationMutator, "
						+ cf.getParam("strategy", "ModuleProbability_1")
						+ "-SelectExpBeta", "Iterations", "Pt-Rate");
				ptRateChart.addSeries("PtRate", x, yPtRate);
				ptRateChart.saveAsPng(
						Controler.getOutputFilename("PtRate.png"), 800, 600);
				XYLineChart personsChart = new XYLineChart(
						"Schweiz: PtUser/Persons, "
								+ maxIters
								+ "ITERs, BetaTraveling="
								+ BetaTraveling
								+ ", BetaTravelingPt="
								+ BetaTravelingPt
								+ ", BetaPerforming="
								+ cf.getParam("planCalcScore", "performing")
								+ ", flowCapacityFactor="
								+ cf.getParam("simulation",
										"flowCapacityFactor")
								+ ", storageCapacityFactor="
								+ cf.getParam("simulation",
										"storageCapacityFactor")
								+ ", "
								+ cf
										.getParam("strategy",
												"ModuleProbability_2")
								+ "-"
								+ cf.getParam("strategy", "Module_2")
								+ ", "
								+ cf
										.getParam("strategy",
												"ModuleProbability_3")
								+ "-"
								+ cf.getParam("strategy", "Module_3")
								+ ", "
								+ cf
										.getParam("strategy",
												"ModuleProbability_1") + "-"
								+ cf.getParam("strategy", "Module_1"),
						"Iterations", "PtUser/Persons");
				personsChart.addSeries("PtUser", x, yPtUser);
				personsChart.addSeries("Persons", x, yPersons);
				personsChart.saveAsPng(Controler
						.getOutputFilename("Persons.png"), 800, 600);

			}
		}
	}

	public void notifyShutdown(ShutdownEvent event) {
		try {
			check.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
