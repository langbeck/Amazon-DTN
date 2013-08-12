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
package br.ufpa.adtn.bundle;

import java.nio.ByteBuffer;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.util.DataBlock;

@SuppressWarnings("unused")
public class BundleBuilder {
	private EID destination;
	private EID source;
	
	private ByteBuffer payload;
	private long lifetime;
	
	public BundleBuilder() {
		this.destination = EID.NULL;
		this.source = EID.NULL;
		this.payload = null;
		this.lifetime = -1;
	}

	public BundleBuilder setDestination(EID destination) {
		this.destination = destination;
		return this;
	}

	public BundleBuilder setSource(EID source) {
		this.source = source;
		return this;
	}

	public BundleBuilder setPayload(ByteBuffer payload) {
		this.payload = payload;
		return this;
	}

	public BundleBuilder setLifetime(long lifetime) {
		this.lifetime = lifetime;
		return this;
	}

	public Bundle build() {
		return new Bundle(
				BundleInfo.create(
						destination,
						source,
						payload.limit()
				),
				DataBlock.wrap(payload)
		);
	}
}
