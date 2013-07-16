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
