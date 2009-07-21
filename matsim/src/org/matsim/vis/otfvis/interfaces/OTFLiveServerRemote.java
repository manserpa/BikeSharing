/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLiveServerRemote.java
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

import java.rmi.RemoteException;

/**
 * If a OTFServer reports to be alive (by returning true in the method isLive()) it can savely be casted
 * to an OTFLiveServerRemote instance. This offers additional options only useful with
 * the actual running simulation. Most importantly the answering of queries.
 *  
 * @author dstrippgen
 *
 */
public interface OTFLiveServerRemote extends OTFServerRemote {
	public void pause() throws RemoteException;
	public void play() throws RemoteException;
	
	public boolean replace(String id, double x, double y, int index, Class clazz) throws RemoteException;
	
	public OTFQuery answerQuery(OTFQuery query) throws RemoteException;
	public int getControllerStatus() throws RemoteException;
	public boolean requestControllerStatus(int status) throws RemoteException;
}
