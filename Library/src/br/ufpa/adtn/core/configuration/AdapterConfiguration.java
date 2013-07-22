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
