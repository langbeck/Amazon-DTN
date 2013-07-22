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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.ufpa.adtn.core.BundleStorage;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;

public class LoadConfiguration {
	private static final Logger LOGGER = new Logger("LoadConfiguration");
	
	private final Collection<ConvergenceLayerConfiguration> cLayers;
	private final Collection<RouterConfiguration> routers;
	private final Properties config;
	
	private String lockReason;
	private boolean locked;
	
	public LoadConfiguration() {
		this.cLayers = new ArrayList<ConvergenceLayerConfiguration>();
		this.routers = new ArrayList<RouterConfiguration>();
		this.config = new Properties();
		
		this.lockReason = "";
		this.locked = false;
	}
	
	public Properties getMainProperties() {
		return config.asReadOnly();
	}
	
	public String getHostname() {
		return config.getString("hostname");
	}
	
	public Collection<ConvergenceLayerConfiguration> getConvergenceLayers() {
		return Collections.unmodifiableCollection(cLayers);
	}
	
	public Collection<RouterConfiguration> getRouters() {
		return Collections.unmodifiableCollection(routers);
	}
	
	public void setStorageModel(String model) {
		if (!BundleStorage.hasModelRegistered(model)) {
			LOGGER.e(String.format(
					"Storage model \"%s\" is not a valid registered model.",
					model
			));
			return;
		}

		LOGGER.v(String.format(
				"Using \"%s\" as storage model",
				model
		));
		config.setString("storage", model);
	}
	
	public String getStorageModel() {
		return config.getString("storage");
	}
	
	public void setHostname(String hostname) {
		checkLock();
		
		if (hostname == null)
			return;
		
		if (!EID.isValidHostname(hostname)) {
			LOGGER.e("setHostname(): Invalid hostname [IGNORING]: " + hostname);
			return;
		}
		
		final String cHostname = config.getString("hostname");
		if (cHostname != null) {
			LOGGER.v(String.format(
					"Hostname already defined. Replacing \"%s\" for \"%s\"",
					cHostname,
					hostname
			));
		} else {
			LOGGER.v(String.format(
				"Defining hostname \"%s\"",
				hostname
			));
		}
		
		config.setString("hostname", hostname);
	}
	
	public void load(InputStream input) throws SAXException, IOException, ParserConfigurationException {
		checkLock();
		
		final Element root = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder().parse(input).getDocumentElement();
		
		// Make sure if the root node is "configuration"
		if (!root.getNodeName().equals("configuration"))
			throw new ParserConfigurationException("Wrong root node name");

		/*
		 * Read base configurations and process collisions of hostname with a
		 * previous defined one.
		 */
		final Properties lConfig = readNodeAttributes(root, null);
		final String lHostname = lConfig.remove("hostname");
		
		if (lHostname == null) {
			LOGGER.w("Hostname not defined in configuration file.");
		} else {
			final String cHostname = config.getString("hostname");
			if (cHostname != null) {
				if (!EID.isValidHostname(lHostname)) {
					LOGGER.w(String.format(
							"Hostname defined in configuration file isn't valid. Keeping previous hostname \"%s\"",
							cHostname
					));
				} else {
					LOGGER.w(String.format(
							"New hostname \"%s\" defined in configuration file. Overriding previous \"%s\" defined hostname.",
							lHostname, cHostname
					));
					
					config.setString("hostname", lHostname);
				}
			} else {
				if (!EID.isValidHostname(lHostname)) {
					LOGGER.w("Hostname defined in configuration file isn't valid. [IGNORING]");
				} else {
					LOGGER.v(String.format(
						"Hostname found in configuration file. Defining hostname \"%s\"",
						lHostname
					));

					config.setString("hostname", lHostname);
				}
			}
		}
		
		// Copy any other configuration remaining
		config.copy(lConfig);
		
		final NodeList nodes = root.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			final Node node = nodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			final String nname = node.getNodeName();
			if (nname.equals("convergence-layer")) {
				final ConvergenceLayerConfiguration clc = parseConvergenceLayer(node);
				if (clc == null)
					continue;
				
				cLayers.add(clc);
			} else if (nname.equals("router")) {
				final RouterConfiguration router = parseRouter(node);
				if (router == null)
					continue;
				
				routers.add(router);
			} else {
				throw new ParserConfigurationException("Unknow node: " + node.getNodeName());
			}
		}
	}
	
	private Properties readNodeAttributes(Node node, Properties parent) {
		final NamedNodeMap attributes = node.getAttributes();
		final Properties properties = new Properties(parent);
		for (int k = 0; k < attributes.getLength(); k++) {
			final Node item = attributes.item(k);
			properties.setString(
					item.getNodeName(),
					item.getNodeValue()
			);
		}
		
		return properties;
	}

	private ConvergenceLayerConfiguration parseConvergenceLayer(Node node) throws ParserConfigurationException {
		final Properties clConfig = readNodeAttributes(node, null);
		final String clName = clConfig.remove("class");
		if (clName == null) {
			LOGGER.e("The Convergence Layer does not have class attribute. [IGNORING]");
			return null;
		}
		
		LOGGER.v("Parsing Convergence Layer " + clName);
		final ConvergenceLayerConfiguration config = new ConvergenceLayerConfiguration(clName, clConfig);
		final NodeList clNodes = node.getChildNodes();
		for (int j = 0; j < clNodes.getLength(); j++) {
			final Node clNode = clNodes.item(j);
			if (clNode.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			final String nodeName = clNode.getNodeName();
			if (!nodeName.equals("adapter")) {
				LOGGER.e(String.format(
						"  Unknow node \"%s\" [IGNORING]",
						nodeName
				));
				
				continue;
			}
			
			final Properties aConfig = readNodeAttributes(clNode, clConfig);
			final String hostname = aConfig.remove("hostname");
			if (hostname != null) {
				if (!EID.isValidHostname(hostname)) {
					LOGGER.e("  Adapter found with invalid adapter hostname. [IGNORING]");
					continue;
				} else {
					LOGGER.v(String.format(
							"  Adapter using \"%s\" as custom hostname",
							hostname
					));
					
					config.addAdapter(new AdapterConfiguration(
							this,
							EID.forHost(hostname),
							aConfig
					));
				}
			} else {
				LOGGER.v(String.format(
						"  Adapter have no custom hostname defined. Using general EID.",
						clName
				));
				config.addAdapter(new AdapterConfiguration(this, null, aConfig));
			}
		}
		
		if (config.getAdapters().isEmpty()) {
			LOGGER.v("  No adapters defined. Generating an adapter with general EID.");
			config.addAdapter(new AdapterConfiguration(this, null));
		}
		
		LOGGER.v(String.format("Convergence Layer %s parsed", clName));
		return config;
	}
	
	private RouterConfiguration parseRouter(Node node) throws ParserConfigurationException {
		final Properties configuration = readNodeAttributes(node, null);
		
		final String rClass = configuration.remove("class");
		if (rClass == null) {
			LOGGER.e("Router does not have class attribute. [IGNORING]");
			return null;
		}

		LOGGER.v("Parsing router " + rClass);
		final RouterConfiguration rConfig = new RouterConfiguration(rClass, configuration);
		final String registration = rConfig.getRegistration();
		if (registration == null) {
			LOGGER.e("  Router does not have registration attribute. [IGNORING]");
			return null;
		} else if (!EID.isValidScheme(registration)) {
			LOGGER.e(String.format(
					"  Router have invalid registration attribute: \"%s\" [IGNORING]",
					registration
			));
			return null;
		}
		
		LOGGER.v(String.format("Router %s parsed", rClass));
		return rConfig;
	}

	
	private synchronized void checkLock() {
		if (locked)
			throw new IllegalStateException(lockReason);
	}
	
	public void lock() {
		lock("Configuration locked");
	}
	
	public synchronized void lock(String reason) {
		if (locked)
			throw new IllegalStateException("Configuration already locked");
		
		lockReason = reason;
		locked = true;
	}
}
