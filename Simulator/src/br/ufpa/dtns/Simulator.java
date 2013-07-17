package br.ufpa.dtns;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.EventQueue.Event;
import br.ufpa.dtns.DeviceLoader.LocalDevice;

public class Simulator {
	
	private static class ExitEvent extends Event {
	
		@Override
		public void execute() throws Throwable {
			System.exit(0);
		}
	}
	
	
	private static class DiscoveryEvent extends Event {
		private final LocalDevice d1;
		private final LocalDevice d2;
		
		public DiscoveryEvent(LocalDevice d1, LocalDevice d2) {
			this.d1 = d1;
			this.d2 = d2;
		}
	
		@Override
		public void execute() throws Throwable {
			System.err.println("\n\n");
			d1.discovery(d2);
			d2.discovery(d1);
		}
	}

	
	private static class PSHook extends PrintStream {
		private final PrintStream copyTo;

		public PSHook(PrintStream copyTo, OutputStream out) {
			super(out);
			this.copyTo = copyTo;
		}
		
		@Override
		public void write(byte[] buf, int off, int len) {
			copyTo.write(buf, off, len);
			super.write(buf, off, len);
		}
		
		@Override
		public void flush() {
			copyTo.flush();
			super.flush();
		}
	}
	
	
	private static void setup() throws Exception {
		System.setErr(new PSHook(
				System.err,
				new BufferedOutputStream(new FileOutputStream("last.log"))
		));

		BPAgent.init(true);
		SimulationConfiguration.load(
				new FileInputStream("contact.conf")
		);
		
		DeviceLoader.init(10000);
	}

	public static void main(String[] args) throws Exception {
		setup();

		final int payloadSize = 512;
		final int loopInterval = 60 * 10;
		final int loopDays = 2;
		
		
		final int loopLimit = (int) ((86400.0 / loopInterval) * 24 * loopDays);
		final ByteBuffer payload = ByteBuffer.allocate(payloadSize);
		for (int i = 0; i < payloadSize; i++)
			payload.put((byte) ((i % 26) + 0x41));

		System.err.println("--[ Creating Virtual Devices ]---------");
		final LocalDevice d1 = DeviceLoader.create("langbeck.node");
		System.err.println("---------------------------------------");
		
		final LocalDevice d2 = DeviceLoader.create("dorian.node");
		System.err.println("--[ Devices Created ]------------------");
		
		d1.addBundle(new Bundle(d1.getEID(), d2.getEID(), payload));
		d1.addBundle(new Bundle(d1.getEID(), d2.getEID(), payload));
		
		d2.addBundle(new Bundle(d2.getEID(), d1.getEID(), payload));
		d2.addBundle(new Bundle(d2.getEID(), d1.getEID(), payload));
		

		final EventQueue eQueue = new EventQueue();
		for (int i = 0; i < loopLimit; i++)
			eQueue.schedule(
					new DiscoveryEvent(d1, d2),
					loopInterval * i,
					TimeUnit.SECONDS
			);

		eQueue.schedule(
				new ExitEvent(),
				loopLimit * loopInterval,
				TimeUnit.SECONDS
		);
		for (;;) Thread.sleep(1000);
	}
}
