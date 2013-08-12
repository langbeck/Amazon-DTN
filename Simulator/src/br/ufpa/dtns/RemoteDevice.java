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
package br.ufpa.dtns;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.InformationHub.InfoLogger;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.util.BundleGenerator;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Logger.LogHandler;
import br.ufpa.adtn.util.Logger.Priority;
import br.ufpa.dtns.cl.VirtualConvergenceLayer;
import br.ufpa.dtns.cl.VirtualConvergenceLayer.VirtualAdapter;
import br.ufpa.dtns.util.PrintStreamHooker;


public class RemoteDevice implements DeviceConnector, Serializable {
	private static final long serialVersionUID = 2003202773331224425L;
	
	private VirtualAdapter adapter;
	private boolean configured;
	private final String eid;
	
	private RemoteDevice(String eid, int regPort) throws Exception {
		final Registry reg = LocateRegistry.getRegistry(regPort);
		final RegisterNotifier register = (RegisterNotifier) reg.lookup("register");
		reg.bind(eid, UnicastRemoteObject.exportObject(this, regPort));
		register.register(eid);

		this.eid = eid;
		configured = false;
	}
	
	@SuppressWarnings({ "resource", "unused" })
	public void init(final Priority gPriority, String config, String simulation) throws RemoteException {
		if (configured)
			throw new IllegalStateException("Device already configured");
		
		
		final PrintStream out;
		try {
			out = true ? System.err : new PrintStreamHooker(
					System.err,
					new FileOutputStream("logs/" + eid + ".log")
			);
		} catch (Exception e) {
			throw new RemoteException(e.getMessage());
		}
		
		Logger.setLogHandler(new LogHandler() {
			@Override
			public void println(Priority priority, String tag, String message) {
				if (priority.ordinal() < gPriority.ordinal())
					return;
				
				out.printf("%7s %-27s %30s: %s\n", priority, eid, tag, message);
			}
		});
		
		BPAgent.setClassLoader(getClass().getClassLoader());

		try {
			BPAgent.init(new SimulationConfiguration(new FileReader(simulation)));
			BPAgent.registerPeriodicEvent(new BundleGenerator(102400, 30, TimeUnit.MINUTES));
			BPAgent.registerPeriodicEvent(new InfoLogger(5, TimeUnit.MINUTES));
			BPAgent.load(new FileInputStream(config));
		} catch (Exception e) {
			throw new RemoteException("BPAgent startup failure", e);
		}

		BPAgent.setHostname(eid);
		BPAgent.start();
		
		final VirtualAdapter adapter = VirtualConvergenceLayer.getMainAdapter();
		if (adapter == null)
			throw new IllegalStateException("Can not get VirtualAdapter");
		
		this.adapter = adapter;
		this.configured = true;
	}

	@Override
	public SocketAddress getAddress() throws RemoteException {
		return adapter.getAddress();
	}

	@Override
	public void discovery(String eid, SocketAddress addr) throws RemoteException {
		adapter.discovery(EID.get(eid), addr);
	}

	@Override
	public void addBundle(Bundle bundle) throws RemoteException {
		BPAgent.addBundle(bundle);
	}
}
