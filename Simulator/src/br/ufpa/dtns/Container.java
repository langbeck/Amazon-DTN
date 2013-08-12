package br.ufpa.dtns;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.core.configuration.SimulationConfiguration.ContactInfo;
import br.ufpa.adtn.core.configuration.SimulationConfiguration.DeviceInfo;
import br.ufpa.adtn.util.Logger;
import br.ufpa.dtns.DeviceLoader.LocalDevice;

public class Container {
	
	public static Container create(String name, String config, String simulation) throws ParsingException, SecurityException, FileNotFoundException, NotBoundException, IOException {
		return new Container(name, config, simulation);
	}
	
	private static final long DISCOVERY_INTERVAL = 20000;
	private final Date first_event;
	private final Date last_event;
	private final Timer timer;
	
	private Container(String name, String config, String simulation) throws SecurityException, NotBoundException, ParsingException, FileNotFoundException, IOException {
		final SimulationConfiguration sConfig = new SimulationConfiguration(new FileReader(simulation));
		final Map<String, LocalDevice> devices = new HashMap<String, LocalDevice>();
		final double dInterval = DISCOVERY_INTERVAL * sConfig.getTimescale();
		this.timer = new Timer();
		
		for (String alias : sConfig.getAliases()) {
			final LocalDevice device = DeviceLoader.create(String.format(
					"%s.%s.node",
					alias.toLowerCase(),
					name
			));
			
			device.init(config, simulation);
			devices.put(alias, device);
		}
		
		final Date limit = new Date(System.currentTimeMillis() + 5000);
		Date first_event = new Date(0x7FFFFFFFFFFFFFFFL);
		Date last_event = limit;
		
		for (String alias : sConfig.getAliases()) {
			final DeviceInfo info = sConfig.getInfoByAlias(alias);
			final LocalDevice d1 = devices.get(alias);
			
			for (ContactInfo ci : info.getContacts()) {
				final LocalDevice d2 = devices.get(ci.getAlias());
				
				final Date start = ci.getStart();
				if (limit.after(start))
					throw new IllegalArgumentException("Invalid event start time: " + start + " / " + limit);
				
				for (double now = start.getTime(), end = ci.getEnd().getTime(); now < end; now += dInterval) {
					final Date dNow = new Date((long) now);
					
					if (dNow.before(first_event))
						first_event = dNow;

					if (dNow.after(last_event))
						last_event = dNow;
					
//					System.err.printf("[%s] %40s --> %-40s\n", dNow, d1.getEID(), d2.getEID());
					timer.schedule(new DiscoveryEvent(d1, d2), dNow);
				}
			}
		}
		
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				Logger.w("Container", "Simulation finished");
				timer.cancel();
			}
		}, last_event);
		this.first_event = first_event;
		this.last_event = last_event;
	}
	
	public Date getFirstEvent() {
		return first_event;
	}
	
	public Date getLastEvent() {
		return last_event;
	}
	
	private void onRemoteException(RemoteException e) {
		// TODO Improve it
		throw new RuntimeException("Unexcpected", e);
	}
	
	
	private class DiscoveryEvent extends TimerTask {
		private final LocalDevice d1;
		private final LocalDevice d2;
		
		public DiscoveryEvent(LocalDevice d1, LocalDevice d2) {
			this.d1 = d1;
			this.d2 = d2;
		}

		@Override
		public void run() {
			try {
				d1.discovery(d2);
			} catch (RemoteException e) {
				onRemoteException(e);
			}
		}
	}
}
