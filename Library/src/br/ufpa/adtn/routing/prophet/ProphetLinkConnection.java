package br.ufpa.adtn.routing.prophet;

import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.routing.Message.TLVParser;
import br.ufpa.adtn.routing.MessageLinkConnection;
import br.ufpa.adtn.routing.prophet.ProphetDataRouting.NeighborPredict;
import br.ufpa.adtn.routing.prophet.ProphetUtil.BundleSpec;
import br.ufpa.adtn.util.Logger;

public class ProphetLinkConnection extends MessageLinkConnection<ProphetLinkConnection, ProphetBundleRouter, ProphetMessageConnection, ProphetTLV> {
	private static final Logger LOGGER = new Logger("ProphetLinkConnection");
	
	private final ProphetDataRouting dataRout;
	
	public ProphetLinkConnection(ProphetDataRouting dataRout) {
		super(ProphetTLV.PARSER);
		this.dataRout = dataRout;
	}
	
	@Override
	protected void onParked() {
		LOGGER.v("onParked event");	
		
		if (getRouter().updatePresence(this))
			getProvider().create();
	}

	@Override
	public ProphetMessageConnection createMessageConnection() {
		return new ProphetMessageConnection();
	}
	
	public ProphetDataRouting getProphetDataRouting() {
		return dataRout;
	}
	
	public BundleSpec[] update(NeighborPredict[] preds) {
		/*
		 * Considering that I'm node A, my neighbor is node B
		 * and others are C.
		 */
		final EID remote_eid = getRegistrationEndpointID();
		for (int i = 0, len = preds.length; i < len; i++) {
			final NeighborPredict np = preds[i];
			final EID c_eid = np.getEID();
			final float p_ac = dataRout.getPredict(c_eid);
			final float p_bc = np.getPredict();
			final float p_ab = dataRout.getPredict(remote_eid);
			
			//Updating transitivity
			dataRout.updateTransitivity(c_eid, p_ab, p_bc);
			
			//Check better candidate to delivering
			if (p_ac < p_bc){
				BPAgent.routeLink(c_eid, remote_eid);
				LOGGER.d("RouteLink to " + remote_eid);
			}
			else {
				BPAgent.routeUnlink(c_eid, remote_eid);
				LOGGER.d("RouteUnlink to " + remote_eid);
			}				
		}
		
		// Decide which bundles will be offered and return it.
		return ProphetUtil.getSpec(BPAgent.getBundlesFor(remote_eid));
	}
}
