/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
		
//		if (getRouter().updatePresence(this))
			getMessageProvider().create();
	}

	@Override
	public DLifeMessageConnection createMessageConnection() {
		return new DLifeMessageConnection();
	}

	public SocialInformation getSocialInformation() {
		return sInfo;
	}
	
	public void update(NeighborWeight[] weights, BundleSpec[] carried, BundleSpec[] acked, long storage, float importance) {
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
		
		final EID local_eid = getEndpointID(); // FIXME Is variable name remote_eid?
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
