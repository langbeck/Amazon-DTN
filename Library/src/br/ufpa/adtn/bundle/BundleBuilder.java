package br.ufpa.adtn.bundle;

import java.nio.ByteBuffer;

import br.ufpa.adtn.core.EID;

@SuppressWarnings("unused")
public class BundleBuilder {
	public static final long DEFAULT_LIFETIME = 3600;
	
	private EID destination;
	private EID custodian;
	private EID reportTo;
	private EID source;
	
	private ByteBuffer payload;
	private long lifetime;
	
	public BundleBuilder() {
		this.lifetime = DEFAULT_LIFETIME;
		this.destination = EID.NULL;
		this.custodian = EID.NULL;
		this.reportTo = EID.NULL;
		this.source = EID.NULL;
		this.payload = null;
	}

	public BundleBuilder setDestination(EID destination) {
		this.destination = destination;
		return this;
	}

	public BundleBuilder setCustodian(EID custodian) {
		this.custodian = custodian;
		return this;
	}

	public BundleBuilder setReportTo(EID reportTo) {
		this.reportTo = reportTo;
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
		return new Bundle(source, destination, payload);
	}
}
