package br.ufpa.adtn.android.virtual;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import br.ufpa.adtn.core.configuration.SimulationConfiguration;


public final class VirtualClock {
	private static final SimulationConfiguration CONFIG = SimulationConfiguration.getInstance();
	private static final long START = CONFIG.getStart().getTime();
	private static final double TS = CONFIG.getTimescale();
	
	public static Timer createTimer(String name) {
		return new VirtualTimer(name);
	}
	
	public static Timer createTimer() {
		return createTimer("VirtualTimer-" + VirtualTimer.G_COUNTER++);
	}
	
	public static Calendar getCalendar() {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(currentTimeMillis());
		return calendar;
	}
	
    public static long getElapsedSeconds() {
        final long now = System.currentTimeMillis();
        if (now < START)
            return -1;
        
        return (long) ((now - START) / TS) / 1000;
    }
    
    public static long getElapsedMillis() {
        final long now = System.currentTimeMillis();
        if (now < START)
            return -1;
        
        return (long) ((now - START) / TS);
    }
    
	public static long currentTimeMillis() {
		return (long) ((System.currentTimeMillis() - START) / TS) + START;
	}
	
	
	private static class VirtualTimer extends Timer {
		private static int G_COUNTER = 0;
		
		public VirtualTimer(String name) {
			super(name);
		}
		
		@Override
		public void schedule(TimerTask task, Date when) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void schedule(TimerTask task, Date when, long period) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void scheduleAtFixedRate(TimerTask task, Date when, long period) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void schedule(TimerTask task, long delay) {
			super.schedule(task, (long) (delay * TS));
		}
		
		@Override
		public void schedule(TimerTask task, long delay, long period) {
			super.schedule(task, (long) (delay * TS), (long) (period * TS));
		}
		
		@Override
		public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
			super.scheduleAtFixedRate(task, (long) (delay * TS), (long) (period * TS));
		}
	}
}
