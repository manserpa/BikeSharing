/* *********************************************************************** *
 * project: org.matsim.*
 * OTFGLDrawableImpl.java
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

package org.matsim.vis.otfvis.opengl.drawer;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;


/**
 * OTFGLDrawableImpl is a basic implementation of the OTFOGLDrawable interface with an 
 * additional simple invalidation mechanism.
 * 
 * @author dstrippgen
 *
 */
public abstract class OTFGLDrawableImpl implements OTFGLDrawable, OTFDataReceiver {
	boolean isValid = true;
	static public GL gl;

	public final void draw() {
		// Make sure onDraw is called only once per object
		onDraw(gl);
		isValid = true;
	}
	public void invalidate(SceneGraph graph) {
		isValid = false;
		graph.addItem(this);
	}

}