package br.ufpa.adtn.routing.dlife;

import java.util.Collection;
import java.util.HashSet;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.routing.MessageLinkConnection;
import br.ufpa.adtn.routing.dlife.DLifeUtil.BundleSpec;
import br.ufpa.adtn.routing.dlife.SocialInformation.NeighborWeight;
import br.ufpa.adtn.util.Logger;

public class DLifeLinkConnection extends MessageLinkConnection<DLifeLinkConnection, DLifeBundleRouter, DLifeMessageConnection, DLifeTLV> {
	private static final Logger LOGGER = new Logger("DLifeLinkConnection");
	private final Collection<BundleSpec> carried;
	private final Collection<BundleSpec> acked;
	private final SocialInformation sInfo;
	
	public DLifeLinkConnection(SocialInformation sInfo) {
		super(DLifeTLV.PARSER);
		this.carried = new HashSet<BundleSpec>();
		this.acked = new HashSet<BundleSpec>();
		this.sInfo = sInfo;
	}

	@Override
	protected void onParked() {
		LOGGER.v("onParked event");
		
		if (getRouter().updatePresence(this))
			getProvider().create();
	}

	@Override
	public DLifeMessageConnection createMessageConnection() {
		return new DLifeMessageConnection();
	}

	public SocialInformation getSocialInformation() {
		return sInfo;
	}
	
//	protected boolean decisionRoutine(Bundle b) {
//		final int b_id = DLifeUtil.getBundleID(b);
//		// TODO Passar a usar Mapeamento do Dicionario aqui dentro!!
//		final EID dest = b.getDestination();
//		final Integer destStr_id = dict.stringIdOf(dest);
//
//		// TODO Mais tarde eh bom notificar de cada
//		// return desse metodo, nao sendo so um True
//		// ou False
//		if (social.getCarrList().containsKey(b_id)) {
//			return false;
//		} else if (social.getAckList().containsKey(b_id)) {
//			DLifeBundleRouter.notifyAckedBundle(b);
//			return false;
//		}
//		if (b.length() > storageCapacity)
//			return false;
//
//		// Se nao há mapeamento no dicionario do vizinho
//		// para o EID destino de um Bundle meu, teoricamente
//		// o mesmo nao conhece esse destino. Posso pular para
//		// a segunda parte. So entra aqui se alem de conhecer,
//		// estar na SWNIList.
//		if (destStr_id == null && social.getSWNI().containsKey(destStr_id)) {
//			if (sInfo.getWeight(dest) < social.getSWNI().get(
//					destStr_id))
//				return true;
//			// RoutingTable.setRoute(dest, eid)
//			else
//				return false;
//		} else {
//			if (sInfo.getTECDi() < social.getImportance())
//				return true;
//			// RoutingTable.setRoute(dest, eid)
//			else
//				return false;
//		}
//	}
	
	public void update(NeighborWeight[] weights, BundleSpec[] carried, BundleSpec[] acked, int storage, float importance) {
		for (int i = 0, len = carried.length; i < len; i++)
			this.carried.add(carried[i]);
		
		final DLifeBundleRouter router = getRouter();
		for (int i = 0, len = acked.length; i < len; i++) {
			final BundleSpec spec = acked[i];
			
			router.updateAcked(spec);
			this.acked.add(spec);
		}
		
		sInfo.setNeighborTECDi(getRegistrationEndpointID(), importance);
	}
	
	public boolean isCarrying(Bundle bundle) {
		return carried.contains(new BundleSpec(bundle));
	}
}
