package br.ufpa.adtn.routing.prophet;

import java.util.Collection;

import java.util.HashSet;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BundleRouter;
import br.ufpa.adtn.core.Link;
import br.ufpa.adtn.routing.prophet.ProphetUtil.BundleSpec;
import br.ufpa.adtn.util.ContactRaceConditionHelper;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;

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