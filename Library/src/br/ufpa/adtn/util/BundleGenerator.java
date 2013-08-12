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
package br.ufpa.adtn.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.bundle.BundleInfo;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SystemClock;

public class BundleGenerator extends PeriodicEvent {
	private final static Logger LOGGER = new Logger("BundleGenerator");
	private final Collection<EID> destinations;
	private final int bSize;
	private int gSequence;
	
	public BundleGenerator(int bSize, long interval, TimeUnit unit) {
		super(interval, unit);
		this.destinations = new HashSet<EID>();
		this.bSize = bSize;
		this.gSequence = 0;
	}
	
	protected void onEvent() {
		final long now = SystemClock.millis();
		
		synchronized (destinations) {
			for (EID destination : destinations) {
				final byte[] filler = String.format(
						"[Bundle #%d for \"%s\" at %d ms] ",
						gSequence++, destination.toString(), now
				).getBytes();
				final byte[] payload = new byte[bSize];
				for (int i = 0, len = payload.length, flen = filler.length; i < len; i++)
					payload[i] = filler[i % flen];
				
				final Bundle bundle = new Bundle(
						BundleInfo.create(
								destination,
								BPAgent.getHostEID()
						),
						DataBlock.wrap(payload)
				);
				
				LOGGER.v(String.format(
						"Created to \"%s\" [ID:%016x]",
						destination, bundle.getUniqueID() 
				));
				BPAgent.addBundle(bundle);
			}
		}
	}
}
