package org.swellrt.beta.client.wave;


public class WaveFactories {

  public static interface Random {
    int nextInt();
  }



  public static WaveLoader.Factory loaderFactory = null;
  public static Random randomGenerator = null;
  public static Log.Factory logFactory = null;
  public static ProtocolMessageUtils protocolMessageUtils = null;
  public static VersionSignatureManager versionSignatureManager = new VersionSignatureManager();

}
