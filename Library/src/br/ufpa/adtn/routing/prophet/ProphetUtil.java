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

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.EID;

/**
 * Stores useful classes and mechanisms to operation of the protocol.
 * 
 * @author Douglas Cirqueira
 */
public class ProphetUtil {
	public static BundleSpec[] getSpec(Collection<Bundle> bundles) {
		final BundleSpec[] specs = new BundleSpec[bundles.size()];
		int i = 0;
		for (Bundle bundle : bundles)
			specs[i++] = new BundleSpec(bundle);
		
		return specs;
	}
	
	public static class BundleSpec {
		private final EID dst;
		private final EID src;
		private final int cts;
		private final int cts_seqn;
		private final byte b_flag;
		
		public BundleSpec(Bundle bundle) {
			this(bundle.getDestination(), bundle.getSource());
		}
		
		public BundleSpec(EID src, EID dst) {
			this.src = src;
			this.dst = dst;
			this.cts = 0;
			this.cts_seqn = 0;
			this.b_flag = 0x00;
		}
		
		public BundleSpec(EID src, EID dst, byte b_flag) {
			this.src = src;
			this.dst = dst;
			this.cts = 0;
			this.cts_seqn = 0;
			this.b_flag = b_flag;
		}
		
		public EID getSource() {
			return src;
		}
		
		public EID getDestination() {
			return dst;
		}
		
		public int timeStamp() {
			// TODO Implement
			return cts;
		}
		
		public int timeStampSeqNumber() {
			// TODO Implement
			return cts_seqn;
		}
		
		public byte getBFlag() {
			return b_flag;
		}
		
		@Override
		public String toString() {
			return String.format("BundleSpec[dst=%s]", dst);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + cts;
			result = prime * result + cts_seqn;
			result = prime * result + ((src == null) ? 0 : src.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BundleSpec other = (BundleSpec) obj;
			if (cts != other.cts)
				return false;
			if (cts_seqn != other.cts_seqn)
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (!src.equals(other.src))
				return false;
			return true;
		}

		
	}

}
