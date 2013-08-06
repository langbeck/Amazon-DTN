package br.ufpa.adtn.core;

import java.util.ArrayList;
import java.util.List;

import br.ufpa.adtn.routing.dlife.SocialInformation;
import br.ufpa.adtn.util.TrafficMeter;

public class InformationHub {
	public final static TrafficMeter COMPRESSED_CONVERGENCE_LAYER_METER;
	public final static TrafficMeter CONVERGENCE_LAYER_METER;
	public final static TrafficMeter DISCOVERY_METER;
	
	public final static BluetoothHub BLUETOOTH;
	public final static BundleHub BUNDLE;
	public final static DLifeHub DLIFE;
	
	static {
		COMPRESSED_CONVERGENCE_LAYER_METER = new TrafficMeter();
		CONVERGENCE_LAYER_METER = new TrafficMeter();
		DISCOVERY_METER = new TrafficMeter();
		BLUETOOTH = new BluetoothHub();
		BUNDLE = new BundleHub();
		DLIFE = new DLifeHub();
	}
	
	
	public static class BundleHub {
		private int received;
		private int sent;
		
		private BundleHub() { }

		public int getReceived() {
			return received;
		}

		public int getSent() {
			return sent;
		}
		

		public void onReceived() {
			received++;
		}
		
		public void onSent() {
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
}
