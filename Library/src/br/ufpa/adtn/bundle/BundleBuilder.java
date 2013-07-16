package br.ufpa.adtn.bundle;

import java.nio.ByteBuffer;

import br.ufpa.adtn.core.EID;

public class BundleBuilder {
	private EID destination;
	private EID custodian;
	private EID reportTo;
	private EID source;
	
	private ByteBuffer payload;
	private long lifetime;
	
	public BundleBuilder(EID source, EID destination, ByteBuffer payload) {
		this.destination = destination;
		this.payload = payload;
		this.source = source;
	}
	
	public EID getDestination() {
		return destination;
	}

	public void setDestination(EID destination) {
		this.destination = destination;
	}

	public EID getCustodian() {
		return custodian;
	}

	public void setCustodian(EID custodian) {
		this.custodian = custodian;
	}

	public EID getReportTo() {
		return reportTo;
	}

	public void setReportTo(EID reportTo) {
		this.reportTo = reportTo;
	}

	public EID getSource() {
		return source;
	}

	public void setSource(EID source) {
		this.source = source;
	}

	public ByteBuffer getPayload() {
		return payload;
	}

	public void setPayload(ByteBuffer payload) {
		this.payload = payload;
	}

	public long getLifetime() {
		return lifetime;
	}

	public void setLifetime(long lifetime) {
		this.lifetime = lifetime;
	}

	public Bundle build() {
		return null;
	}
}
