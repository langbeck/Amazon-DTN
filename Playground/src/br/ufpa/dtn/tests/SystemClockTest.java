package br.ufpa.dtn.tests;

import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.SystemClock;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.util.EventQueue;

public class SystemClockTest {
	
	public static void main(String[] args) throws Exception {
		BPAgent.init(true);
		SimulationConfiguration.load(new FileInputStream("contact.conf"));
		
		SimulationConfiguration config = SimulationConfiguration.getInstance();
		System.err.println(config.getTimescale());
		System.err.println(config.getStart());

		final double ts = config.getTimescale();
		final long times = 300;
		final long time0 = 1000 * 60 * 60 * 24 * 5L;
		final long time1 = time0 * times;
		final long scaled0 = (long) (time0 / ts);
		final long scaled1 = (long) (time1 / ts);
		final long scaled2 = scaled0 * times;
		
		System.err.println(scaled0);
		System.err.println(scaled1);
		System.err.println(scaled2);
		System.err.println(scaled1 - scaled2);
		System.err.println();
		
		for (int i = 0; i < 1; i++) {
			final long start0 = System.currentTimeMillis();
			final long start1 = SystemClock.millis();
			Thread.sleep(700);
			final long end0 = System.currentTimeMillis();
			final long end1 = SystemClock.millis();
	
			System.err.println(start0);
			System.err.println(end0);
			System.err.println(end0 - start0);
			System.err.println();
			System.err.println(start1);
			System.err.println(end1);
			System.err.println(end1 - start1);
			System.err.println();
			System.err.println();
		}

		
		final EventQueue eQueue = new EventQueue();
		System.err.println("Start");

		final long _start = SystemClock.nanos();
		final long start = System.nanoTime();
		eQueue.schedule(new Runnable() {
			
			@Override
			public void run() {
				System.err.printf(
						"Exiting: %.3f ms / %.3f ms",
						(SystemClock.nanos() - _start) / 1e6,
						(System.nanoTime() - start) / 1e6
				);
				System.exit(0);
			}
		}, 1, TimeUnit.SECONDS);
	}
}
