package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.version.HashedVersion;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Fetch contributions for wavelets at specific versions.
 *
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
public interface DocContributionsFetcher {


  public interface Factory {

    public DocContributionsFetcher create(WaveId waveId);

  }


  @JsType(isNative = true)
  public interface DocContributionValue {

    @JsProperty String getKey();
    @JsProperty String getValue();
  }


  /**
   * Data representation of a doc contribution. A contribution is analog to
   * an annotation instance.
   *
   * @author pablojan@apache.org (Pablo Ojanguren)
   *
   */
  @JsType(isNative = true)
  public interface DocContribution  {

    @JsProperty int getStart();
    @JsProperty int getEnd();
    @JsProperty DocContributionValue[] getValues();

  }

  public interface WaveletContributions {

    public DocContribution[] getDocContributions(String blipId);
  }

  /**
   * Async callback to fetch contributions
   */
  public interface Callback {

    public void onSuccess(WaveletContributions waveletContributions);

    public void onException(Exception e);

  }

  public void fetchContributions(String waveletId, HashedVersion waveletVersion, Callback fetchCallback);

}
