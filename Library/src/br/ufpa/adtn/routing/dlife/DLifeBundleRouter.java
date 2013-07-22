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
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BundleRouter;
import br.ufpa.adtn.core.Link;
import br.ufpa.adtn.core.SystemClock;
import br.ufpa.adtn.routing.dlife.DLifeUtil.BundleSpec;
import br.ufpa.adtn.util.ContactRaceConditionHelper;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;
import br.ufpa.adtn.util.TimeoutHelper;

public final class DLifeBundleRouter extends BundleRouter<DLifeBundleRouter, DLifeLinkConnection> {
	private static final Logger LOGGER = new Logger("DLifeRouter");
	private static final int MAX_AUSENCE_TIME = 20000;
	
	private TimeoutHelper<DLifeLinkConnection> toHelper;
	private ContactRaceConditionHelper crcHelper;
	private Collection<BundleSpec> carried;
	private Collection<BundleSpec> acked;
	private SocialInformation sInfo;

	@Override
	protected void onCreate(Properties config) {
		if (sInfo != null || toHelper != null || crcHelper != null)
			throw new IllegalStateException();
		
		this.sInfo = new SocialInformation(
				config.getInteger("dslen", 24),
				config.getInteger("dsdur", 3600),
				config.getFloat("dumping_factor", 0.8f)
		);

		this.carried = new HashSet<BundleSpec>();
		this.acked = new HashSet<BundleSpec>();
		
		final EventQueue eQueue = getEventQueue();
		this.toHelper = TimeoutHelper.create(eQueue, new TimeoutHelper.AbstractHandler<DLifeLinkConnection>() {
			@Override
			public void onTimeout(DLifeLinkConnection conn, long permanence) {
				sInfo.updateTCT(conn.getRegistrationEndpointID(), permanence / 1e3f);
			}
		}, MAX_AUSENCE_TIME);
		
		this.crcHelper = new ContactRaceConditionHelper(
				getLocalEID().withScheme("dtn").toString(),
				eQueue,
				MAX_AUSENCE_TIME
		);
		
		scheduleEndOfSample();
	}

	@Override
	public boolean onLinkNear(Link link) {
		LOGGER.i(String.format("onLinkNear: %s", link.getEndpointID()));
		final DLifeLinkConnection conn = link.getConnection(this);
		toHelper.refresh(conn);

		LOGGER.d("Parked: " + conn.isParked());
		return !conn.isParked();
	}
	
	boolean updatePresence(DLifeLinkConnection conn) {
		return crcHelper.update(conn.getEndpointID().toString());
	}
	
	@Override
	protected DLifeLinkConnection createConnection(Link link) {
		return new DLifeLinkConnection(sInfo);
	}

	private void scheduleEndOfSample() {
		scheduleEndOfSample(SystemClock.millis(), false);
	}
	
	private void scheduleEndOfSample(long now) {
		scheduleEndOfSample(now, true);
	}

	private void scheduleEndOfSample(long now, boolean toSync) {
		final long dur = sInfo.getSampleDuration() * 1000;
		final long wait = Math.max(dur - (now % dur), 500);
		
		if (!toSync)
			LOGGER.d(String.format(
					"Scheduling the end of the sample for %.3f seconds ahead",
					wait / 1e3
			));

		getEventQueue().schedule(
				new EndSample(),
				wait,
				TimeUnit.MILLISECONDS
		);
	}
	
	public void notifyCarriedBundle(Bundle bundle) {
		carried.add(new BundleSpec(bundle));
		// dirtyAll();
	}
	
	public void notifyAckedBundle(Bundle bundle) {
		acked.add(new BundleSpec(bundle));
		// dirtyAll();
	}
	
	public BundleSpec[] getCarried() {
//		return DLifeUtil.getSpec(BPAgent.getBundles());
		return carried.toArray(new BundleSpec[0]);
	}
	
	public BundleSpec[] getAcked() {
		return acked.toArray(new BundleSpec[0]);
	}
	
	public void updateAcked(BundleSpec spec) {
		acked.add(spec);
	}
	
	
	private class EndSample implements Runnable {
		
		@Override
		public void run() {
			final long now = SystemClock.millis();
			if (sInfo.getInternalDS() == sInfo.getDS(now)) {
				LOGGER.e("Sample not finished yet. Re-scheduling.");
				scheduleEndOfSample(now);
			} else {
				LOGGER.v("End of Sample");
				sInfo.update();
				scheduleEndOfSample();
			}
		}
	}
}
