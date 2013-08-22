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
package br.ufpa.adtn.core;


public final class SystemClock extends ClockHooker {
	private static ClockHooker HOOKER = new SystemClock();

	public static long millis() {
		return HOOKER.getMilliseconds();
	}

	public static long nanos() {
		return HOOKER.getNanoseconds();
	}
	
	public static long secs() {
		return HOOKER.getSeconds();
	}

	
	public static void setHooker(ClockHooker hooker) throws IllegalStateException {
		if (!BPAgent.isSimulated())
			throw new IllegalStateException("BPAgent is not in simulated mode.");

		if (BPAgent.getState().ordinal() >= BPAgent.State.LOADING.ordinal())
			throw new IllegalStateException("BPAgent state must be lower than LOADING");

		HOOKER = hooker;
	}
	
	
	private SystemClock() { }

	@Override
	public long getMilliseconds() {
		return System.currentTimeMillis();
	}
	
	@Override
	public long getNanoseconds() {
		return System.nanoTime();
	}
}
