package org.swellrt.beta.client.platform.web.editor;

import org.waveprotocol.wave.model.document.util.Annotations;

public class CaretAnnotationConstants {

  /** Prefix for user annotations. */
  public static final String USER_PREFIX = Annotations.LOCAL + "user";

  /** The range of text selected by the user. */
  public static final String USER_RANGE = USER_PREFIX + "/r/";

  /** The user's selection focus, always extends to the document end. */
  public static final String USER_END = USER_PREFIX + "/e/";

  /** User activity annotation, always covers the whole document.  */
  public static final String USER_DATA = USER_PREFIX + "/d/";

  /**
   * Handy method for getting the full annotation key, given a session id
   *
   * Session id does not have to be THE session id - it can just be any globally
   * unique key for the current client.
   *
   * @param sessionId
   * @return full annotation key
   */
  public static String rangeKey(String sessionId) {
    return CaretAnnotationConstants.USER_RANGE + sessionId;
  }

  public static boolean isRangeKey(String key) {
    return key.startsWith(CaretAnnotationConstants.USER_RANGE);
  }

  public static String endKey(String sessionId) {
    return CaretAnnotationConstants.USER_END + sessionId;
  }

  public static boolean isEndKey(String key) {
    return key.startsWith(CaretAnnotationConstants.USER_END);
  }

  public static String dataKey(String sessionId) {
    return CaretAnnotationConstants.USER_DATA + sessionId;
  }

  public static boolean isDataKey(String key) {
    return key.startsWith(CaretAnnotationConstants.USER_DATA);
  }

  public static String rangeSuffix(String rangeKey) {
    return rangeKey.substring(CaretAnnotationConstants.USER_RANGE.length());
  }

  public static String endSuffix(String endKey) {
    return endKey.substring(CaretAnnotationConstants.USER_END.length());
  }

  public static String dataSuffix(String dataKey) {
    return dataKey.substring(CaretAnnotationConstants.USER_DATA.length());
  }

}
