package br.ufpa.dtn;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

public class Priority {
	
	private static class Unit implements Comparable<Unit> {
		private final short value;
		
		public Unit(short value) {
			this.value = value;
		}
		
		public short getValue() {
			return value;
		}

		@Override
		public int compareTo(Unit o) {
			return o.value - value;
		}
	}
	
	public static void main(String[] args) {
		final SortedSet<Unit> units = new TreeSet<Unit>();
		units.add(new Unit((short) -10));
		units.add(new Unit((short) -2));
		units.add(new Unit((short) -5));
		units.add(new Unit((short) -15));
		
		for (Unit unit : units) {
			System.err.println(unit.getValue());
		}
	}
}
