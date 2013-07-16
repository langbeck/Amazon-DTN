package br.ufpa.dtn.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class Mapping {
	private static final Map<Class<?>, Mapping> ref;
	
	static {
		ref = new HashMap<Class<?>, Mapping>();
	}
	
	public synchronized static Mapping get(Class<?> cl) {
		Mapping mapping = ref.get(cl);
		if (mapping == null) {
			mapping = new Mapping(cl);
			ref.put(cl, mapping);
		}
		
		return mapping;
	}
	
	
	private final Collection<Field> fields;
	private final Field identifier;
	private final Class<?> mClass;
	private final long signature;
	private final String prefix;
	
	private Mapping(Class<?> cl) {
		if (!cl.isAnnotationPresent(Entity.class))
			throw new RuntimeException("Not a valid Entity");
		
		this.fields = new ArrayList<Field>();
		
		Field lIdentifier = null;
		long sig = 0L;
		for (Field f : cl.getDeclaredFields()) {
			f.setAccessible(true);
			
			if ((f.getModifiers() & Modifier.TRANSIENT) != 0)
				continue;

			final Class<?> t = f.getType();
			if (f.isAnnotationPresent(Identifier.class)) {
				if (lIdentifier == null) {
					if (t != Integer.class)
						throw new RuntimeException("Identifier field must have Integer type");
					
					lIdentifier = f;
					continue;
				}
				
				throw new RuntimeException("An Entity must have only one identifier field");
			}
			
			if (!(t.isPrimitive() || t == String.class))
				continue;
			
			final int th = t.getName().hashCode();
			final int fh = f.getName().hashCode();
			sig ^= (th << 0) | (fh << 32) | (th ^ fh) << 16;

			fields.add(f);
		}
		
		if (lIdentifier == null)
			throw new RuntimeException("No identifier found");
		
		this.prefix = String.format(
				"%s(%s)",
				Persistence.NAMESPACE,
				cl.getName()
		);
		this.identifier = lIdentifier;
		this.signature = sig;
		this.mClass = cl;
	}
	
	public String serialize(Object obj) {
		if (!mClass.isInstance(obj))
			throw new IllegalArgumentException();

		final Map<String, String> values = new HashMap<String, String>();
		try {
			for (Field f : fields)
				values.put(f.getName(), String.valueOf(f.get(obj)));
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
		return values.toString();
	}
	
	public Integer getId(Object obj) {
		if (!mClass.isInstance(obj))
			throw new IllegalArgumentException();
		
		try {
			return (Integer) identifier.get(obj);
		} catch (Throwable t) {
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
			
			if (t instanceof Error)
				throw (Error) t;
			
			throw new RuntimeException(t);
		}
	}
	
	public long getSignature() {
		return signature;
	}
	
	public String getPrefix() {
		return prefix;
	}
}
