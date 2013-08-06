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
package br.ufpa.dtns;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import br.ufpa.dtns.util.PrintStreamHooker;

public class Simulator {	
	
	private static void setup() throws Exception {
		System.setErr(new PrintStreamHooker(
				System.err,
				new BufferedOutputStream(new FileOutputStream("logs/general.log"))
		));
		
		DeviceLoader.init(10000);
	}

	
	public static void main(String[] args) throws Exception {
		setup();
		
//		final LocalDevice d1 = DeviceLoader.create("langbeck.node");
//		final LocalDevice d2 = DeviceLoader.create("dorian.node");
//		
//		d1.init("config.dlife.xml", "contact.conf");
//		d2.init("config.dlife.xml", "contact.conf");
//
//		d1.discovery(d2);
//		d2.discovery(d1);
//		Thread.sleep(20);
//		
//		d1.discovery(d2);
//		d2.discovery(d1);
//		Thread.sleep(20);
//		
//		d1.discovery(d2);
//		d2.discovery(d1);
//		Thread.sleep(20);
//		
//		d1.discovery(d2);
//		d2.discovery(d1);
//		Thread.sleep(20);
//		
//		d1.discovery(d2);
//		d2.discovery(d1);
//		Thread.sleep(20);
		
		Container.create("prophet", "config.prophet.xml", "contact.conf");
//		Container container = Container.create("dlife", "config.dlife.xml", "contact.conf");
//		System.err.println(container.getFirstEvent());
//		System.err.println(container.getLastEvent());
	}
}
