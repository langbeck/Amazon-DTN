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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import br.ufpa.adtn.util.Properties;

public class ConvergenceLayerConfiguration {
	private final Collection<AdapterConfiguration> adapters;
	private final Properties properties;
	private String className;
	
	public ConvergenceLayerConfiguration(String className, Properties properties) {
		this.adapters = new ArrayList<AdapterConfiguration>();
		this.properties = properties;
		this.className = className;
	}
	
	public void addAdapter(AdapterConfiguration adapter) {
		adapters.add(adapter);
	}
	
	public Collection<AdapterConfiguration> getAdapters() {
		return Collections.unmodifiableCollection(adapters);
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public String getClassName() {
		return className;
	}
}
