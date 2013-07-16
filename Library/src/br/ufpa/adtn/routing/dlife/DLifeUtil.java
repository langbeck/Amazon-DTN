package br.ufpa.adtn.routing.dlife;

import java.util.Collection;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.EID;

public final class DLifeUtil {

	public static int computeBundleID(Bundle bundle) {
		// TODO Implement
		return 0;
	}

	public static BundleSpec[] getSpec(Collection<Bundle> bundles) {
		final BundleSpec[] specs = new BundleSpec[bundles.size()];
		int i = 0;
		for (Bundle bundle : bundles)
			specs[i++] = new BundleSpec(bundle);
		
		return specs;
	}
	
	public static class BundleSpec {
		private final EID dst;
		private final int id;
		
		public BundleSpec(Bundle bundle) {
			this(bundle.getDestination(), computeBundleID(bundle));
		}
		
		public BundleSpec(EID dst, int id) {
			this.dst = dst;
			this.id = id;
		}
		
		public EID getDestination() {
			return dst;
		}
		
		public int getId() {
			return id;
		}
		
		@Override
		public String toString() {
			return String.format("BundleSpec[id=0x%08X, dst=%s]", id, dst);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dst == null) ? 0 : dst.hashCode());
			result = prime * result + id;
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
			if (dst == null) {
				if (other.dst != null)
					return false;
			} else if (!dst.equals(other.dst))
				return false;
			if (id != other.id)
				return false;
			return true;
		}
	}

	private DLifeUtil() { }
}
