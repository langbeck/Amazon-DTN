package br.ufpa.adtn.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.routing.dlife.SocialInformation;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.PeriodicEvent;
import br.ufpa.adtn.util.TrafficMeter;

public class InformationHub {
	public final static TrafficMeter COMPRESSED_CONVERGENCE_LAYER_METER;
	public final static TrafficMeter CONVERGENCE_LAYER_METER;
	public final static TrafficMeter DISCOVERY_METER;
	
	public final static BluetoothHub BLUETOOTH;
	public final static BundleHub DATA_BUNDLE;
	public final static BundleHub BUNDLE;
	
	public final static ProphetHub PROPHET;
	public final static DLifeHub DLIFE;
	
	static {
		COMPRESSED_CONVERGENCE_LAYER_METER = new TrafficMeter();
		CONVERGENCE_LAYER_METER = new TrafficMeter();
		DISCOVERY_METER = new TrafficMeter();
		BLUETOOTH = new BluetoothHub();
		DATA_BUNDLE = new BundleHub();
		PROPHET = new ProphetHub();
		BUNDLE = new BundleHub();
		DLIFE = new DLifeHub();
	}
	
	
	public static class BundleHub {
		private final static Logger LOGGER = new Logger("BundleHub");
		private int dataReceived;
		private int dataSent;
		private int received;
		private int sent;
		
		private BundleHub() {
			this.dataReceived = 0;
			this.dataSent = 0;
			this.received = 0;
			this.sent = 0;
		}
		
		public int getDataReceived() {
			return dataReceived;
		}
		
		public int getDataSent() {
			return dataSent;
		}

		public int getReceived() {
			return received;
		}

		public int getSent() {
			return sent;
		}

		public void onReceived(Bundle bundle) {
			LOGGER.i(String.format(
					"Received %s -> %s",
					bundle.getSource(),
					bundle.getDestination()
			));
			
			dataReceived += bundle.getPayload().getLength();
			received++;
		}
		
		public void onSent(Bundle bundle) {
			LOGGER.i(String.format(
					"Sent %s -> %s",
					bundle.getSource(),
					bundle.getDestination()
			));
			
			dataSent += bundle.getPayload().getLength();
			sent++;
		}
	}
	
	
	public static class DLifeHub {
		private OnChangeListener<SocialInformation> listener;
		
		private DLifeHub() { }
		
		public void setListener(OnChangeListener<SocialInformation> listener) {
			this.listener = listener;
		}
		
		public void update(SocialInformation sInfo) {
			if (listener == null)
				return;
			
			listener.onChanged(sInfo);
		}
	}
	
	
	public static class ProphetHub {
		private OnChangeListener<SocialInformation> listener;
		
		private ProphetHub() { }
		
		public void setListener(OnChangeListener<SocialInformation> listener) {
			this.listener = listener;
		}
		
		public void update(SocialInformation sInfo) {
			if (listener == null)
				return;
			
			listener.onChanged(sInfo);
		}
	}
	
	
	public static class BluetoothHub {
		private final List<String> devicesNear;
		
		private BluetoothHub() {
			devicesNear = new ArrayList<String>();
		}
		
		public void removeDeviceNear(String device) {
			devicesNear.remove(device);
		}
		
		public void addDeviceNear(String device) {
			devicesNear.add(device);
		}
		
		
	}
	
	
	public static interface OnChangeListener<T> {
		public void onChanged(T data);
	}
	
	

	@SuppressWarnings("unused")
	public static class InfoLogger extends PeriodicEvent {
		private final static Logger LOGGER = new Logger("InfoLogger");
		private boolean log_prophet;
		private boolean log_dlife;
		
		public InfoLogger(long interval, TimeUnit unit) {
			super(interval, unit);
			
			this.log_prophet = false;
			this.log_dlife = false;
		}
		
		public void enableProphetRouter() {
			log_prophet = true;
		}
		
		public void enableDLifeRouter() {
			log_dlife = true;
		}

		@Override
		protected void onEvent() {
			final Date now = new Date(SystemClock.millis());

			final BundleHub bundle = InformationHub.BUNDLE;
			LOGGER.i(String.format(
					"(%s) Bundles [ %d / %d ]",
					now,
					bundle.getReceived(),
					bundle.getSent()
			));
			
			TrafficMeter meter;

			meter = InformationHub.COMPRESSED_CONVERGENCE_LAYER_METER;
			final int cl_received_compressed = meter.getTotalReceived();
			final int cl_sent_compressed = meter.getTotalSent();

			meter = InformationHub.CONVERGENCE_LAYER_METER;
			final int cl_received_normal = meter.getTotalReceived();
			final int cl_sent_normal = meter.getTotalSent();

			LOGGER.i(String.format(
					"(%s) CL-Traffic [ Normal: %d / %d ; Compressed: %d / %d ; Ratio: %.2f / %.2f ]",
					now,
					cl_received_normal,
					cl_sent_normal,
					cl_received_compressed,
					cl_sent_compressed,
					(cl_received_compressed * 100f) / cl_received_normal,
					(cl_sent_compressed * 100f) / cl_sent_normal
			));
			
			meter = InformationHub.DISCOVERY_METER;
			LOGGER.i(String.format(
					"(%s) Discovery-Traffic [ %d / %d ]",
					now,
					meter.getTotalReceived(),
					meter.getTotalSent()
			));
			
//			if (log_dlife) {
//				LOGGER.i(String.format(
//						"(%s) DLife [ %d / %d ]",
//						now,
//						meter.getTotalReceived(),
//						meter.getTotalSent()
//				));
//			}
		}
	}
}
