/* *********************************************************************** *
 * project: org.matsim.*
 * OTFAgentHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.interfaces;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * @author dstrippgen
 *
 * @param <SrcAgent>
 */
@Deprecated
public interface OTFAgentHandler<SrcAgent>  extends Serializable{
	public void writeAgent(SrcAgent agent, DataOutputStream out) throws IOException;
	public void readAgent(DataInputStream in) throws IOException;
}
