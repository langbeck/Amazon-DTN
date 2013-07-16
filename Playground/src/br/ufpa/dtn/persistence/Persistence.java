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
