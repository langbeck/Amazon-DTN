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
package br.ufpa.adtn.routing.prophet;

import java.util.Collection;
import java.util.HashSet;

import br.ufpa.adtn.core.BundleRouter;
import br.ufpa.adtn.core.Link;
import br.ufpa.adtn.routing.prophet.ProphetUtil.BundleSpec;
import br.ufpa.adtn.util.ContactRaceConditionHelper;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;

/**
 * A. Lindgren, A. Doria,  E. Davies, S. Grasicm, Probabilistic Routing
 * Protocol for Intermittently Connected Networks, RFC 6693, August 2012.
 * 
 * This is the main PROPHET routing class. Responsible for storing the engine
 * race condition to start communication with another node. Moreover, it has to
 * interface with the BPA to consult Bundles available. Stores the instance of
 * ProphetDataRouting and distributes to all ProphetLinkConnections created. 
 * Implements the notification method of near communication link. 
 * 
 * @author Douglas Cirqueira
 */
public class ProphetBundleRouter extends BundleRouter<ProphetBundleRouter, ProphetLinkConnection> {
	private static final Logger LOGGER = new Logger("ProphetRouter");
	private static final int MAX_AUSENCE_TIME = 20000;
	
	private ContactRaceConditionHelper crcHelper;
	private Collection<BundleSpec> bundles;
	private ProphetDataRouting dataR;

	@Override
	protected void onCreate(Properties config) {
		if (crcHelper != null || dataR != null)
			throw new IllegalStateException();
		
		this.dataR = new ProphetDataRouting();
		this.bundles = new HashSet<BundleSpec>();
		final EventQueue eQueue = getEventQueue();
		this.crcHelper = new ContactRaceConditionHelper(
				getLocalEID().withScheme("dtn").toString(),
				eQueue,
				MAX_AUSENCE_TIME
		);
	}
	
	@Override
	protected boolean onLinkNear(Link link) {
		LOGGER.i(String.format("onLinkNear: %s", link.getEndpointID()));
		ProphetLinkConnection conn = link.getConnection(this);
		dataR.updatePredict(conn.getRegistrationEndpointID());
		LOGGER.d("Parked: " + conn.isParked());
		return !conn.isParked();
	}
	
	boolean updatePresence(ProphetLinkConnection conn) {
		return crcHelper.update(conn.getEndpointID().toString());
	}

	@Override
	protected ProphetLinkConnection createConnection(Link link) {
		return new ProphetLinkConnection(dataR);
	}
	
	public BundleSpec[] getSpecBundles() {
		return bundles.toArray(new BundleSpec[0]);
	}
	
	public Collection<BundleSpec> getCollectionBundles() {
		return bundles;
	}
	
}