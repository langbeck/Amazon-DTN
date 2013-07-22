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
