package br.ufpa.dtns.util.transform;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import br.ufpa.dtns.Simulator;
import br.ufpa.dtns.util.ClassTransformer;

public class SystemHookTransformer implements ClassTransformer {

	@Override
	public byte[] transform(String name, byte[] data) {
		if (!name.equals(Simulator.class.getName()))
			return null;
		
		CtClass cl = null;
		try {
			cl = ClassPool.getDefault().makeClass(new ByteArrayInputStream(data));
			if (cl.isInterface())
				return null;
			
			for (CtBehavior behavior : cl.getDeclaredBehaviors()) {
				if (behavior.isEmpty())
					continue;
				
				if (!behavior.getName().equals("print"))
					continue;

				behavior.setModifiers(behavior.getModifiers() & ~Modifier.NATIVE);
				behavior.setBody("java.lang.System.err.println(\"Dorian\");");
				
				System.err.println(behavior.getLongName());
				System.err.println("    " + Arrays.deepToString(behavior.getAnnotations()));
				System.err.println();
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
