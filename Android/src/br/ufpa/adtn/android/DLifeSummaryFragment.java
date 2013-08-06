package br.ufpa.adtn.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import br.ufpa.adtn.core.InformationHub;
import br.ufpa.adtn.core.InformationHub.DLifeHub;
import br.ufpa.adtn.core.InformationHub.OnChangeListener;
import br.ufpa.adtn.routing.dlife.SocialInformation;
import br.ufpa.adtn.routing.dlife.SocialInformation.NeighborWeight;

public class DLifeSummaryFragment extends FragmentPager {
	private LayoutInflater inflater;
	private ListView lNear;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_dlife, container, false);
		
		final DLifeHub dlife = InformationHub.DLIFE;
		dlife.setListener(new OnChangeListener<SocialInformation>() {
			
			@Override
			public void onChanged(final SocialInformation data) {
				view.post(new Runnable() {
					
					@Override
					public void run() {
						onSocialInformationChange(data);
					}
				});
			}
		});
		
		this.lNear = (ListView) view.findViewById(R.id.near_neighbors);
		this.inflater = inflater;
		return view;
	}
	
	private void onSocialInformationChange(SocialInformation sInfo) {
		final List<Map<String, ?>> data = new ArrayList<Map<String, ?>>();
		for (NeighborWeight w : sInfo.getSampleWeights()) {
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put("tecd", w.getWeight());
			entry.put("eid", w.getEID());
			data.add(entry);
		}
		
		lNear.setAdapter(new SimpleAdapter(
				inflater.getContext(),
				data,
				R.layout.neighbor_entry,
				new String[] { "eid", "tecdi", "tecd" },
				new int[] {
						R.id.neighbor_eid,
						R.id.text_tecdi,
						R.id.text_tecd
				}
		));
	}

	@Override
	public String getTitle() {
		return "DLife Summary";
	}
}
