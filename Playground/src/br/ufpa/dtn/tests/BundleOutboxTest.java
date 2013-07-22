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
package br.ufpa.dtn.tests;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.util.BundleOutbox;

public class BundleOutboxTest {
	
	
	private static void against(BundleOutbox outbox, Map<Bundle, String> bMap, EID dst) {
		System.err.println("Against " + dst.getSSP());
		for (Bundle bundle : outbox.searchBundles(dst))
			System.err.println(bMap.get(bundle));
		
		System.err.println();
	}
	
	private static void baseMain() {
		final ByteBuffer payload = ByteBuffer.wrap("Payload".getBytes());
		final EID eSource = EID.get("dtn:source");
		final EID e0 = EID.get("dtn:e0");
		final EID e1 = EID.get("dtn:e1");
		final EID e2 = EID.get("dtn:e2");
		final EID e3 = EID.get("dtn:e3");
		final EID e4 = EID.get("dtn:e4");
		final EID e5 = EID.get("dtn:e5");

		final Bundle b1 = new Bundle(eSource, e1, payload);
		final Bundle b2 = new Bundle(eSource, e1, payload);
		final Bundle b3 = new Bundle(eSource, e2, payload);
		final Bundle b4 = new Bundle(eSource, e2, payload);
		final Bundle b5 = new Bundle(eSource, e3, payload);
		
		final Map<Bundle, String> bMap = new HashMap<Bundle, String>();
		bMap.put(b1, "Bundle 1");
		bMap.put(b2, "Bundle 2");
		bMap.put(b3, "Bundle 3");
		bMap.put(b4, "Bundle 4");
		bMap.put(b5, "Bundle 5");
		
		final BundleOutbox outbox = new BundleOutbox();
		
		outbox.link(e3, e2);
		outbox.link(e3, e4);
		
		outbox.link(e1, e0);
		outbox.link(e2, e0);
		outbox.link(e3, e0);
		outbox.link(e4, e0);

		outbox.link(e1, e5);
		outbox.link(e2, e5);

		outbox.add(b1);
		outbox.add(b2);
		outbox.add(b3);
		outbox.add(b4);
		outbox.add(b5);

		against(outbox, bMap, e0);
		against(outbox, bMap, e1);
		against(outbox, bMap, e2);
		against(outbox, bMap, e3);
		against(outbox, bMap, e4);
		against(outbox, bMap, e5);
	}
	
	@SuppressWarnings("unused")
	private static Map<Bundle, String> randomFill(BundleOutbox outbox, int bundles, int eids, int links, long seed) {
		final Map<Bundle, String> bMap = new HashMap<Bundle, String>();
		final Random rand = new Random(seed);
		final EID[] _eids = new EID[eids];
		
		for (int i = 0; i < eids; i++)
			_eids[i] = EID.get("dtn:e" + i);
		
		final ByteBuffer payload = ByteBuffer.wrap("BundlePayload".getBytes());
		for (int i = 0; i < bundles; i++) {
			final Bundle bundle = new Bundle(
					_eids[rand.nextInt(eids)],
					_eids[rand.nextInt(eids)],
					payload
			);
			
			bMap.put(bundle, "Bundle " + i);
			outbox.add(bundle);
		}
		
		for (int i = 0; i < links; i++)
			outbox.link(
					_eids[rand.nextInt(eids)],
					_eids[rand.nextInt(eids)]
			);
		
		return bMap;
	}
	
	@SuppressWarnings("unused")
	private static long search(BundleOutbox outbox, EID dst) {
		final long start = System.nanoTime();
		outbox.searchBundles(dst);
		return System.nanoTime() - start;
	}
	
	public static void main(String[] args) {
		baseMain();
		
//		final BundleOutbox outbox = new BundleOutbox();
//		final int eids = 200;
//		randomFill(outbox, 10000, eids, 1000, 1L);
//		
//		long total = 0L;
//		for (int i = 0; i < eids; i++)
//			total += search(outbox, EID.get("dtn:e" + i));
//		
//		System.err.printf("Total: %.3f ms\n", total / 1e6);
	}
}
