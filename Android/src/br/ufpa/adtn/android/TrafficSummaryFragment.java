package br.ufpa.adtn.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.BPAgent.State;
import br.ufpa.adtn.core.InformationHub;
import br.ufpa.adtn.core.InformationHub.BundleHub;
import br.ufpa.adtn.util.TrafficMeter;

public class TrafficSummaryFragment extends FragmentPager implements Runnable {
	private TextView clSentCompressed;
	private TextView clSentNormal;
	private TextView clSentRatio;
	private TextView clRecvCompressed;
	private TextView clRecvNormal;
	private TextView clRecvRatio;
	private TextView bSent;
	private TextView bRecv;
	private TextView dSent;
	private TextView dRecv;
	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (view != null)
			return view;
		
		view = inflater.inflate(R.layout.fragment_traffic, container, false);

		dRecv = (TextView) view.findViewById(R.id.text_discovery_received);
		bRecv = (TextView) view.findViewById(R.id.text_bundles_received);

		clRecvCompressed = (TextView) view.findViewById(R.id.text_cl_received_compressed);
		clRecvNormal = (TextView) view.findViewById(R.id.text_cl_received_normal);
		clRecvRatio = (TextView) view.findViewById(R.id.text_cl_received_ratio);

		clSentCompressed = (TextView) view.findViewById(R.id.text_cl_sent_compressed);
		clSentNormal = (TextView) view.findViewById(R.id.text_cl_sent_normal);
		clSentRatio = (TextView) view.findViewById(R.id.text_cl_sent_ratio);

		dSent = (TextView) view.findViewById(R.id.text_discovery_sent);
		bSent = (TextView) view.findViewById(R.id.text_bundles_sent);

		view.postDelayed(this, 500);
		return view;
	}

	@Override
	public void run() {
		if (BPAgent.getState() != State.STARTED) {
			view.postDelayed(this, 1000);
			return;
		}
		
		final BundleHub bundle = InformationHub.BUNDLE;
		bRecv.setText(String.valueOf(bundle.getReceived()));
		bSent.setText(String.valueOf(bundle.getSent()));
		
		TrafficMeter meter;

		meter = InformationHub.COMPRESSED_CONVERGENCE_LAYER_METER;
		final int cl_received_compressed = meter.getTotalReceived();
		final int cl_sent_compressed = meter.getTotalSent();

		meter = InformationHub.CONVERGENCE_LAYER_METER;
		final int cl_received_normal = meter.getTotalReceived();
		final int cl_sent_normal = meter.getTotalSent();

		clRecvRatio.setText(String.format("%.2f %%", (cl_received_compressed * 100f) / cl_received_normal));
		clRecvCompressed.setText(String.valueOf(cl_received_compressed));
		clRecvNormal.setText(String.valueOf(cl_received_normal));

		clSentRatio.setText(String.format("%.2f %%", (cl_sent_compressed * 100f) / cl_sent_normal));
		clSentCompressed.setText(String.valueOf(cl_sent_compressed));
		clSentNormal.setText(String.valueOf(cl_sent_normal));

		meter = InformationHub.DISCOVERY_METER;
		dRecv.setText(String.valueOf(meter.getTotalReceived()));
		dSent.setText(String.valueOf(meter.getTotalSent()));
		
		view.postDelayed(this, 500);
	}

	@Override
	public String getTitle() {
		return "Traffic Summary";
	}
}
