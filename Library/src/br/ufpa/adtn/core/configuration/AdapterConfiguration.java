package br.ufpa.adtn.core.configuration;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.util.Properties;

public class AdapterConfiguration {
	private final LoadConfiguration config;
	private final Properties properties;
	private final EID local_eid;
	
	public AdapterConfiguration(LoadConfiguration config, EID local_eid) {
		this(config, local_eid, new Properties());
	}
	
	public AdapterConfiguration(LoadConfiguration config, EID local_eid, Properties properties) {
		if (properties == null)
			throw new NullPointerException();
		
		this.properties = properties;
		this.local_eid = local_eid;
		this.config = config;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public EID getLocalEID() {
		if (local_eid != null)
			return local_eid;
		
		final String hostname = config.getHostname();
		if (hostname == null)
			return EID.NULL;
		
		return EID.get("dtn://" + hostname);
	}
}
