package br.ufpa.adtn.routing.dlife;

import java.util.Collection;
import java.util.HashSet;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.routing.MessageLinkConnection;
import br.ufpa.adtn.routing.dlife.DLifeUtil.BundleSpec;
import br.ufpa.adtn.routing.dlife.SocialInformation.NeighborWeight;
import br.ufpa.adtn.util.Logger;

public class DLifeLinkConnection extends MessageLinkConnection<DLifeLinkConnection, DLifeBundleRouter, DLifeMessageConnection, DLifeTLV> {
	private static final Logger LOGGER = new Logger("DLifeLinkConnection");
	private final Collection<BundleSpec> carried;
	private final Collection<BundleSpec> acked;
	private final SocialInformation sInfo;
	
	public DLifeLinkConnection(SocialInformation sInfo) {
		super(DLifeTLV.PARSER);
		this.carried = new HashSet<BundleSpec>();
		this.acked = new HashSet<BundleSpec>();
		this.sInfo = sInfo;
	}

	@Override
	protected void onParked() {
		LOGGER.v("onParked event");
		
		if (getRouter().updatePresence(this))
			getProvider().create();
	}

	@Override
	public DLifeMessageConnection createMessageConnection() {
		return new DLifeMessageConnection();
	}

	public SocialInformation getSocialInformation() {
		return sInfo;
	}
	
	public void update(NeighborWeight[] weights, BundleSpec[] carried, BundleSpec[] acked, int storage, float importance) {
		for (int i = 0, len = carried.length; i < len; i++)
			this.carried.add(carried[i]);
		
		final DLifeBundleRouter router = getRouter();
		for (int i = 0, len = acked.length; i < len; i++) {
			final BundleSpec spec = acked[i];
			
			// Update router references
			router.updateAcked(spec);
			
			// Update neighbor references
			this.carried.remove(spec);
			this.acked.add(spec);
		}
		
		final EID local_eid = getEndpointID();
		final float tecdi = sInfo.getTECDi();
		for (int i = 0, len = weights.length; i < len; i++) {
			final NeighborWeight nw = weights[i];
			final EID eid = nw.getEID();
			final float lw = sInfo.getWeight(eid);
			
			if (lw != Float.NaN) {
				if (nw.getWeight() < lw)	BPAgent.routeUnlink(eid, local_eid);
				else						BPAgent.routeLink(eid, local_eid);
			} else if (tecdi >= importance)	BPAgent.routeUnlink(eid, local_eid);
			else							BPAgent.routeLink(eid, local_eid);
		}
		
		sInfo.setNeighborTECDi(local_eid, importance);
	}
	
	public boolean isCarrying(Bundle bundle) {
		return carried.contains(new BundleSpec(bundle));
	}
}
