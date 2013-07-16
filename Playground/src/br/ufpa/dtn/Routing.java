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

