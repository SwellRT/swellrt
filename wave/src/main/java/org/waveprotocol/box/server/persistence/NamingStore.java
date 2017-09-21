package org.waveprotocol.box.server.persistence;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.model.id.WaveId;

/**
 * Stores Waves naming, in other words, maps between a WaveId and multiple
 * human-readable names, suitable for using as URL.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface NamingStore {

  public class WaveName {

    public final String name;
    public final long created;

    public WaveName(String name, long created) {
      super();
      this.name = name;
      this.created = created;
    }

  }

  /**
   * All the human readable names associated with a Wave Id
   *
   *
   */
  public class WaveNaming {

    public WaveId waveId;
    public final List<WaveName> names;

    public WaveNaming(WaveId waveId) {
      super();
      this.waveId = waveId;
      this.names = new ArrayList<WaveName>();
    }

  }

  /** @return names of a wave looked up by wave id */
  WaveNaming getWaveNamingById(WaveId waveId);

  /** @return names of a wave looked up by a synonymous name */
  WaveNaming getWaveNamingsByName(String name) throws PersistenceException;

  /**
   * Add a new name for a Wave. Name can't be duplicated.
   * 
   * @throws PersistenceException
   */
  WaveNaming addWaveName(WaveId waveId, String name) throws PersistenceException;

  /** Remove an existing name of Wave. */
  WaveNaming removeWaveName(WaveId waveId, String name);


}
