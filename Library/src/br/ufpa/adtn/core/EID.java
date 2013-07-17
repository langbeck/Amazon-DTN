package br.ufpa.adtn.core;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import br.ufpa.adtn.util.SDNV;



public final class EID implements Serializable {
	private static final long serialVersionUID = 3774973384300603807L;

	private static final Map<String, WeakReference<String>> G_STRINGS;
	private static final Map<String, SoftReference<EID>> INTERNAL;
	public static final EID NULL;
	
	static {
		G_STRINGS = new WeakHashMap<String, WeakReference<String>>();
		INTERNAL = new HashMap<String, SoftReference<EID>>();
		NULL = EID.get("dtn:none");
	}
	
	private static String getIntern(String s) {
		final Reference<String> ref = G_STRINGS.get(s);
		if (ref != null) {
			final String data = ref.get();
			if (data != null)
				return data;
		}
		
		G_STRINGS.put(s, new WeakReference<String>(s));
		return s;
	}
	
	public static EID forHost(String hostname) {
		if (!isValidHostname(hostname))
			throw new ParsingException("Invalid hostname: " + hostname);
		
		return EID.get("dtn://" + hostname);
	}
	
	public static EID get(String scheme, String ssp) {
		return get(String.format("%s:%s", scheme, ssp));
	}
	
	public static EID get(String eid) throws ParsingException {
		if (eid == null)
			return NULL;
		
		synchronized (INTERNAL) {
			final SoftReference<EID> ref = INTERNAL.get(eid);
			if (ref != null) {
				EID ie = ref.get();
				if (ie == null) {
					ie = parse(eid);
					INTERNAL.put(eid, new SoftReference<EID>(ie));
				}
				
				return ie;
			} else {
				final EID ie = parse(eid);
				INTERNAL.put(eid, new SoftReference<EID>(ie));
				return ie;
			}
		}
	}
	
	private static EID parse(String eid) throws ParsingException {
		final String[] parts = eid.split(":", 2);
		if (parts.length != 2)
			throw new ParsingException();
		
		return new EID(parts[0], parts[1]);
	}
	
	public static EID decode(ByteBuffer buffer) {
		/*
		 * This is not a bug. 16-bits are enough to hold
		 * a EID length.
		 */
		final int len = SDNV.decodeShort(buffer);
		final byte[] bbuf = new byte[len];
		buffer.get(bbuf);
		
		return get(new String(bbuf));
	}
	
	public static boolean isValidHostname(String hostname) {
		return hostname.matches("^([a-z0-9\\-]+\\.)*[a-z0-9\\-]+$");
	}
	
	public static boolean isValidScheme(String scheme) {
		/*
		 * FIXME In RFC 1738 ".", "-", ... are valid scheme characters.
		 * For now, I will let pass just alphanumerics. 
		 */
		return scheme.matches("^[a-z0-9]+$");
	}
	
	
	private final String scheme;
	private final String ssp;
	private final int len;
	
	private final byte[] rawData;
	private final int dLength;
	
	private EID(String scheme, String ssp) throws IllegalArgumentException {
		if (scheme == null || ssp == null)
			throw new IllegalArgumentException();
		
		final byte[] schemeData = scheme.getBytes();
		final byte[] sspData = ssp.getBytes();
		final int scheme_len = schemeData.length;
		final int ssp_len = sspData.length;
		
		if (ssp_len > 1023 || scheme_len > 1023)
			throw new IllegalArgumentException();
		
		
		this.dLength = scheme_len + ssp_len + 1;
		this.rawData = new byte[dLength];
		System.arraycopy(sspData, 0, rawData, scheme_len + 1, ssp_len);
		System.arraycopy(schemeData, 0, rawData, 0, scheme_len);
		rawData[scheme_len] = (byte) ':';
		
		this.len = scheme.length() + ssp.length() + 1;
		
		
		synchronized (G_STRINGS) {
			this.scheme = getIntern(scheme);
			this.ssp = getIntern(ssp);
		}
	}
	
	public String getScheme() {
		return scheme;
	}
	
	public String getSSP() {
		return ssp;
	}
	
	public Link getLink() {
		return Link.get(this);
	}
	
	public int getDataLength() {
		return rawData.length;
	}
	
	public int getLength() {
		return len;
	}
	
	public boolean isBase() {
		return scheme.equals("dtn") && ssp.matches("^//([a-z0-9\\-]+\\.)*[a-z0-9\\-]+$");
	}
	
	@Override
	public String toString() {
		return String.format("%s:%s", scheme, ssp);
	}
	
	public int encode(ByteBuffer buffer) {
		final int total = SDNV.writeInt(buffer, dLength) + dLength;
		buffer.put(rawData);
		return total;
	}
	
	public EID withScheme(String scheme) {
		return scheme.equals(this.scheme) ? this : EID.get(scheme, ssp);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
		result = prime * result + ((ssp == null) ? 0 : ssp.hashCode());
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
		EID other = (EID) obj;
		if (scheme == null) {
			if (other.scheme != null)
				return false;
		} else if (!scheme.equals(other.scheme))
			return false;
		if (ssp == null) {
			if (other.ssp != null)
				return false;
		} else if (!ssp.equals(other.ssp))
			return false;
		return true;
	}
}
