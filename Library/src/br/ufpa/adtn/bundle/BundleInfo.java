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

import br.ufpa.adtn.core.EID;


public final class BundleInfo {
	private final EID destination;
	private final EID custodian;
	private final EID reportTo;
	private final EID source;

	private final int creation_time;
	private final int creation_seq;

	private final int fragment_offset;
	private final int total_data_len;
	
	private final int lifetime;
	private final int flags;
	
	public BundleInfo(EID destination, EID custodian, EID reportTo, EID source,
			int creation_time, int creation_seq, int fragment_offset,
			int total_data_len, int lifetime, int flags) {
		
		this.destination = destination;
		this.custodian = custodian;
		this.reportTo = reportTo;
		this.source = source;
		this.creation_time = creation_time;
		this.creation_seq = creation_seq;
		this.fragment_offset = fragment_offset;
		this.total_data_len = total_data_len;
		this.lifetime = lifetime;
		this.flags = flags;
	}

	public EID getDestination() {
		return destination;
	}

	public EID getCustodian() {
		return custodian;
	}

	public EID getReportTo() {
		return reportTo;
	}

	public EID getSource() {
		return source;
	}

	public int getCreationTime() {
		return creation_time;
	}

	public int getCreationSequence() {
		return creation_seq;
	}

	public int getFragmentOffset() {
		return fragment_offset;
	}

	public int getTotalDataUnitLength() {
		return total_data_len;
	}

	public int getLifetime() {
		return lifetime;
	}

	public int getFlags() {
		return flags;
	}
}
