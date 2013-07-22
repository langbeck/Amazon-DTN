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
package br.ufpa.dtn;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.EID;

public class Routing {
	
	public static void main(String[] args) {
		final List<Bundle> bundles = new ArrayList<Bundle>();
		bundles.add(new Bundle(EID.NULL, EID.get("dtn://langbeck.node"), ByteBuffer.wrap("PaYLoAD".getBytes())));
		bundles.add(new Bundle(EID.NULL, EID.get("dtn://dorian.node"), ByteBuffer.wrap("PaYLoAD".getBytes())));
		
		System.err.println("--");
		for (Bundle bundle : bundles) {
			System.err.println(bundle);
		}
	}
}


class RoutingTable {
	private final Map<String, Destination> rTable;
	
	public RoutingTable() {
		this.rTable = new HashMap<String, Destination>();
	}
	
	public void addRoute(String dst, String next, float cost) {
		getDestination(dst).add(next, cost);
	}
	
	public void removeRoute(String dst, String next) {
		getDestination(dst).remove(next);
	}
	
	public void purgeDestination(String dst) {
		rTable.remove(dst);
	}
	
	public String getNextHop(String dst) {
		final Entry entry = getDestination(dst).queue.poll();
		return entry == null ? null : entry.next;
	}
	
	public Collection<String> getNextHops(String dst) {
		return getDestination(dst).nexts;
	}
	
	private Destination getDestination(String dst) {
		Destination destination = rTable.get(dst);
		if (destination != null)
			return destination;
		
		destination = new Destination();
		rTable.put(dst, destination);
		return destination;
	}
	

	private static class Destination {
		private final PriorityQueue<Entry> queue;
		private final Map<String, Entry> table;
		private final Collection<String> nexts;
		
		public Destination() {
			this.table = new HashMap<String, Entry>();
			this.queue = new PriorityQueue<Entry>();
			
			this.nexts = Collections.unmodifiableCollection(table.keySet());
		}
		
		public void add(String next, float cost) {
			Entry entry = table.get(next);
			if (entry != null)
				queue.remove(entry);

			entry = new Entry(next, cost);
			table.put(next, entry);
			queue.add(entry);
		}
		
		public void remove(String next) {
			table.remove(next);
			queue.remove(next);
		}
	}
	
	
	private static class Entry implements Comparable<Entry> {
		private final String next;
		private final float cost;
		
		public Entry(String next, float cost) {
			this.next = next;
			this.cost = cost;
		}
		
		@Override
		public int compareTo(Entry o) {
			return Float.compare(cost, o.cost);
		}
	}
}

