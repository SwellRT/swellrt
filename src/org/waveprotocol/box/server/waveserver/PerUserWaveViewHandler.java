package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.box.server.waveserver.PerUserWaveViewBus.Listener;

/**
 * Listens on wavelet update events and updates the per user wave view
 * accordingly.
 *
 * Provides unified interface for typical implementations that track/provide the
 * state of per user waves view.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public interface PerUserWaveViewHandler extends Listener, PerUserWaveViewProvider {

}
