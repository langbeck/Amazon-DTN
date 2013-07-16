package br.ufpa.dtns.util.transform;

import java.io.ByteArrayInputStream;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import br.ufpa.dtns.util.ClassTransformer;

public class LoggerTransformer implements ClassTransformer {
	private static final String IFLOG = "if ($__logger.isLoggable(java.util.logging.Level.INFO))";
	private static final String DEF = "private static java.util.logging.Logger $__logger;";
	private static final String[] DEFAULT_IGNORE = new String[] {
		"java.", "javax.", "javassist."
	};

	private final String[] ignore;
	private final Class<?> marker;
	
	public LoggerTransformer(String ... ignorePrefixes) throws IllegalArgumentException {
		this(null, ignorePrefixes);
	}
	
	public LoggerTransformer(Class<?> marker, String ... ignorePrefixes) throws IllegalArgumentException {
		this.ignore = ignorePrefixes == null ? ignorePrefixes : DEFAULT_IGNORE;
		this.marker = marker;
		
		if (marker != null && !marker.isAnnotation())
			throw new IllegalArgumentException();
	}

	private boolean shouldIgnoreClass(String name) {
		for (String pkg : ignore)
			if (name.startsWith(pkg))
				return true;
		
		return false;
	}

	@Override
	public final byte[] transform(String name, byte[] data) {
		if (shouldIgnoreClass(name))
			return null;
		
		CtClass cl = null;
		try {
			cl = ClassPool.getDefault().makeClass(new ByteArrayInputStream(data));
			if (cl.isInterface())
				return null;
			
			cl.addField(CtField.make(DEF, cl), String.format(
				"java.util.logging.Logger.getLogger(%s.class.getName())",
				name
			));
			
			for (CtBehavior behavior : cl.getDeclaredBehaviors()) {
				if (behavior.isEmpty())
					continue;
				
				if (marker != null && !behavior.hasAnnotation(marker))
					continue;
				
//				System.err.println(behavior.getLongName());
//				System.err.println("    " + Arrays.deepToString(behavior.getAnnotations()));
//				System.err.println();
				
				final String signature = behavior.getSignature();
			    behavior.insertBefore(String.format(
			    		"%s { $__logger.info(\">> %s\"); }",
			    		IFLOG,
			    		signature
	    		));

			    behavior.insertAfter(String.format(
			    		"%s { $__logger.info(\"<< %s\"); }",
			    		IFLOG,
			    		signature
	    		));
			}
			
			return cl.toBytecode();
		} catch (Exception e) {
			throw new RuntimeException("Class " + name, e);
		} finally {
			if (cl != null)
				cl.detach();
		}
	}

}
