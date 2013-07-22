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
package br.ufpa.dtn.persistence;

import java.util.Map;

import br.ufpa.adtn.util.Properties;

public class Persistence {
	public static final String NAMESPACE = "@DS";
	
	private final Properties data;
	
	public Persistence() {
		this.data = new Properties();
	}
	
	public void persist(Object obj) throws IllegalArgumentException, IllegalAccessException {
		if (obj == null)
			return;
		
		final Mapping map = Mapping.get(obj.getClass());
		final String prefix = map.getPrefix();
		data.setLong(
				prefix + ".signature",
				map.getSignature()
		);
		
		data.setString(
				String.format("%s.instance[%d]", prefix, map.getId(obj)),
				map.serialize(obj)
		);
	}
	
	public static void main(String[] args) throws Exception {
		final Persistence pu = new Persistence();
		pu.persist(new DataHolder("Dorian", 24));
		
		for (Map.Entry<String, String> e : pu.data.getInternalTable().entrySet())
			System.err.printf("%-40s\t%s\n", e.getKey(), e.getValue());
	}
}


@Entity
class DataHolder {
	@Identifier
	Integer id;
	
	String name;
	int age;
	
	DataHolder(String name, int age) {
		this.name = name;
		this.age = age;
	}
}
