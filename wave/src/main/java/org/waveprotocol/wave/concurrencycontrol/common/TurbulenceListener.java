package org.waveprotocol.wave.concurrencycontrol.common;

/**
 * A listener for failures and exceptions thrown by the channel or protocol.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface TurbulenceListener {

	public void onFailure(ChannelException e);
	
}
