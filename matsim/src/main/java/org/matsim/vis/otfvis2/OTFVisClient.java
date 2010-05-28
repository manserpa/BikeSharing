package org.matsim.vis.otfvis2;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D.Double;
import java.rmi.RemoteException;
import java.util.List;

import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.NetJComponent;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.QueryEntry;
import org.matsim.vis.otfvis.gui.OTFQueryControl.IdResolver;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public final class OTFVisClient extends OTFClient {

	private boolean swing = false;
	
	private OTFConnectionManager connect = new OTFConnectionManager();

	public OTFVisClient() {
		super();
	}

	private void prepareConnectionManager() {
		this.connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		this.connect.connectWriterToReader(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		if (swing) {
			this.connect.connectReaderToReceiver(LinkHandler.class, NetJComponent.SimpleQuadDrawer.class);
			this.connect.connectReaderToReceiver(OTFAgentsListHandler.class,  NetJComponent.AgentDrawer.class);
		} else {
			this.connect.connectReaderToReceiver(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
			this.connect.connectReaderToReceiver(LinkHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
			this.connect.connectReceiverToLayer(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);		
			this.connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		}
	}

	protected OTFClientQuad getRightDrawerComponent() throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		OTFClientQuad clientQ2 = createNewView(null, connectR, this.hostControlBar.getOTFHostControl());
		return clientQ2;
	}

	@Override
	protected OTFDrawer createDrawer(){
		prepareConnectionManager();
		try {
			frame.getContentPane().add(new OTFTimeLine("time", hostControlBar), BorderLayout.SOUTH);
			OTFDrawer mainDrawer;
			if (swing) {
				mainDrawer = new NetJComponent(this.getRightDrawerComponent());
			} else {
				mainDrawer = new OTFOGLDrawer(this.getRightDrawerComponent());
			}
			if (hostControlBar.getOTFHostControl().isLiveHost()) {
				final OTFQueryControl queryControl = new OTFQueryControl(hostControlBar, OTFClientControl.getInstance().getOTFVisConfig());
				queryControl.getQueries().clear();
				queryControl.getQueries().add(new QueryEntry("agentPlan", "show the current plan of an agent", QueryAgentPlan.class));
				queryControl.setAgentIdResolver(new IdResolver() {

					@Override
					public List<String> resolveId(Double origRect) {
						QueryAgentId.Result agentIdQuery = (QueryAgentId.Result) queryControl.createQuery(new QueryAgentId(origRect));
						return agentIdQuery.agentIds;
					}


				});
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, OTFClientControl.getInstance().getOTFVisConfig());
				queryControl.setQueryTextField(queryControlBar.getTextField());
				frame.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
			}
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfigGroup createOTFVisConfig() {
		try {
			saver = new SettingsSaver(this.masterHostControl.getAddress());
			return this.masterHostControl.getOTFServer().getOTFVisConfig();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	public void setSwing(boolean swing) {
		this.swing = swing;
	}

}
