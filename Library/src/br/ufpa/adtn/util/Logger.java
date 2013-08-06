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

public final class Logger {
	public static final LogHandler LOG_TO_SYSERR = new LogHandler() {
		
		@Override
		public void println(Priority priority, String tag, String message) {
			System.err.printf("%-7s %-20s: %s\n", priority.toString(), tag, message);
		}
	};
	
	public static enum Priority {
		VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT
	}
	
	private final Logger parent;
	private final String tag;

	public Logger(String tag) {
		this.parent = null;
		this.tag = tag;
	}

	public Logger(Logger parent, String tag) {
		this.tag = (parent != null) ?
				String.format("%s.%s", parent.tag, tag) :
				tag;
		
		this.parent = parent;
	}
	
	public Logger getParent() {
		return parent;
	}
	
	public String getTag() {
		return tag;
	}
	
	@Override
	public String toString() {
		return tag;
	}

	public void v(String message) {
		println(Priority.VERBOSE, tag, message);
	}

	public void v(String message, Throwable t) {
		println(Priority.VERBOSE, tag, message, t);
	}

	public void d(String message) {
		println(Priority.DEBUG, tag, message);
	}

	public void d(String message, Throwable t) {
		println(Priority.DEBUG, tag, message, t);
	}

	public void i(String message) {
		println(Priority.INFO, tag, message);
	}

	public void i(String message, Throwable t) {
		println(Priority.INFO, tag, message, t);
	}

	public void w(String message) {
		println(Priority.WARN, tag, message);
	}

	public void w(String message, Throwable t) {
		println(Priority.WARN, tag, message, t);
	}

	public void e(String message) {
		println(Priority.ERROR, tag, message);
	}

	public void e(String message, Throwable t) {
		println(Priority.ERROR, tag, message, t);
	}
	
	
	
	private static LogHandler handler = LOG_TO_SYSERR;

	public static void v(String tag, String message) {
		println(Priority.VERBOSE, tag, message);
	}

	public static void v(String tag, String message, Throwable t) {
		println(Priority.VERBOSE, tag, message, t);
	}

	public static void d(String tag, String message) {
		println(Priority.DEBUG, tag, message);
	}

	public static void d(String tag, String message, Throwable t) {
		println(Priority.DEBUG, tag, message, t);
	}

	public static void i(String tag, String message) {
		println(Priority.INFO, tag, message);
	}

	public static void i(String tag, String message, Throwable t) {
		println(Priority.INFO, tag, message, t);
	}

	public static void w(String tag, String message) {
		println(Priority.WARN, tag, message);
	}

	public static void w(String tag, String message, Throwable t) {
		println(Priority.WARN, tag, message, t);
	}

	public static void e(String tag, String message) {
		println(Priority.ERROR, tag, message);
	}

	public static void e(String tag, String message, Throwable t) {
		println(Priority.ERROR, tag, message, t);
	}
	
	private static void println(Priority priority, String tag, String message, Throwable t) {
		if (handler != null)
			handler.println(priority, tag, message, t);
	}
	
	private static void println(Priority priority, String tag, String message) {
		if (handler != null)
			handler.println(priority, tag, message);
	}
	
	public static void setLogHandler(LogHandler handler) {
		Logger.handler = handler;
	}
	
	
	public static abstract class LogHandler {
		
		public void println(Priority priority, String tag, String message, Throwable t) {
			println(priority, tag, message + ": " + t);
			for (StackTraceElement trace : t.getStackTrace())
				println(priority, tag, " at " + trace.toString());
			
			flush();
		}
		
		public abstract void println(Priority priority, String tag, String message);
		
		public void flush() { }
	}
}
