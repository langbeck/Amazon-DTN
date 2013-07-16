package br.ufpa.adtn.routing;

public interface ResponseListener<T extends TLV> {
	public void onReceived(int id, T tlv);
}
