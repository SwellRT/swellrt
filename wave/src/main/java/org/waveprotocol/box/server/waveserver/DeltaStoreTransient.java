package org.waveprotocol.box.server.waveserver;

/**
 * A delta store suitable to store transient wavelets. Transient wavelets must
 * store no relevant information for applications. <br>
 * <p>
 * WARNING! Deletion of transient wavelets is only safe between server reboots
 * and when the wavelet is not shared across domains (federated).
 * <p>
 * For federated wavelets, a coordinated deletion protocol should be put in
 * place.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface DeltaStoreTransient extends DeltaStore {

}
