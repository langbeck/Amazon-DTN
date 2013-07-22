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
package br.ufpa.adtn.util;

import java.util.Collection;
import java.util.HashSet;


public final class ContactRaceConditionHelper {
	private final TimeoutHelper<String> helper;
	private final Collection<String> force;
	private final int name_hash;
	
	public ContactRaceConditionHelper(String name, long timeout) {
		this(name, new EventQueue(), timeout);
	}
	
	public ContactRaceConditionHelper(String name, EventQueue eQueue, long timeout) {
		this.force = new HashSet<String>();
		this.name_hash = name.hashCode();
		
		this.helper = TimeoutHelper.create(eQueue, new TimeoutHelper.AbstractHandler<String>() {

			@Override
			public void onTimeout(String o, long t) {
				synchronized (force) { force.add(o); }
			}
		}, timeout);
	}
	
	public void remove(String name) {
		synchronized (force) {
			helper.purge(name);
			force.remove(name);
		}
	}
	
	public boolean update(String name) {
		for (
				int i = 0, me = name_hash, other = name.hashCode();
				i < 32;
				i++, me >>>= 1, other >>>= 1
		) {
			final int _other = other & 0x01;
			final int _me = me & 0x01;
			
			if (_me == _other)
				continue;
			
			if (_me == 1) {
				Logger.d("RACE", "Winner");
				return true;
			}
			

			synchronized (force) {
				if (force.contains(name)) {
					Logger.d("RACE", "Forced");
					helper.purge(name);
					force.remove(name);
					return true;
				}
			}
			
			if (!helper.contains(name))
				helper.refresh(name);
			
			Logger.d("RACE", "Looser");
			return false;
		}
		
		Logger.d("RACE", "Hash colision");
		return true;
	}
}