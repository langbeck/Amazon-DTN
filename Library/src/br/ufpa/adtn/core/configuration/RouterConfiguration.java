package br.ufpa.adtn.core.configuration;

import br.ufpa.adtn.util.Properties;

public class RouterConfiguration {
	private final Properties properties;
	private final String className;
	private final String registration;
	
	public RouterConfiguration(String className) {
		this(className, new Properties());
	}
	
	public RouterConfiguration(String className, Properties properties) {
		if (properties == null)
			throw new NullPointerException();

		this.registration = properties.getString("registration");
		this.properties = properties;
		this.className = className;
	}
	
	public String getRegistration() {
		return registration;
	}
	
	public Properties getProperties() {
		return properties;
	}

	public String getClassName() {
		return className;
	}
}
