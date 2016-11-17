package org.swellrt.server.box.events;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.waveprotocol.wave.model.id.ModernIdSerialiser;

import com.google.common.base.Preconditions;


public class ExpressionParser {

  public final static String EXP_OP_VALUE = "$";
  public final static String EXP_OP_HASH = "$hash";


  public final static String EXP_OP_AUTHOR = "$author";
  public final static String EXP_OP_AUTHOR_ID = "$author_id";
  public final static String EXP_OP_TIMESTAMP = "$timestamp";
  public final static String EXP_OP_PARTICIPANT = "$participant";
  public final static String EXP_OP_PARTICIPANT_ID = "$participant_id";
  public final static String EXP_OP_OBJECT_ID = "$objectId";
  public final static String EXP_OP_OBJECT_TYPE = "$objectType";
  public final static String EXP_OP_APP = "$app";
  public final static String EXP_OP_PATH = "$path";

  public final static String[] EXP_NON_PATH = {EXP_OP_AUTHOR, EXP_OP_AUTHOR_ID, EXP_OP_TIMESTAMP,
      EXP_OP_PARTICIPANT, EXP_OP_PARTICIPANT_ID, EXP_OP_OBJECT_ID, EXP_OP_OBJECT_TYPE, EXP_OP_APP,
      EXP_OP_PATH};

  // The reg exp for expresion
  private static final Pattern EXP_PATTERN = Pattern
      .compile("(\\$||\\$hash)\\{(([a-zA-Z0-9_]*|\\?)\\.)*(([a-zA-Z0-9_]*|\\?)*)\\}");


  protected static String extractExpressionPath(String expresion)
      throws InvalidEventExpressionException {

    if (expresion == null)
      throw new InvalidEventExpressionException("Unable to extract path from a null expression.");


    Matcher m = EXP_PATTERN.matcher(expresion);
    if (m.matches()) {

      if (m.group(1).equals(EXP_OP_VALUE)) {
        return expresion.substring(EXP_OP_VALUE.length() + 1, expresion.length() - 1);

      } else if (m.group(1).equals(EXP_OP_HASH)) {
        return expresion.substring(EXP_OP_HASH.length() + 1, expresion.length() - 1);
      }

    }

    throw new InvalidEventExpressionException("Expresion has wrong syntax");

  }

  protected static String getParticipantAddress(String participant) {
    int separatorIndex = participant.indexOf("@");
    if (separatorIndex != -1)
      return participant.substring(0, separatorIndex);
    else
      return participant;
  }

  protected static String evaluateExpression(Event event, String expression)
      throws InvalidEventExpressionException {

    // No path-based expresions

    if (expression.equals(EXP_OP_AUTHOR)) {
      return getParticipantAddress(event.getAuthor());

    } else if (expression.equals(EXP_OP_AUTHOR_ID)) {
      return event.getAuthor();

    } else if (expression.equals(EXP_OP_PARTICIPANT)) {
      return getParticipantAddress(event.getParticipant());

    } else if (expression.equals(EXP_OP_PARTICIPANT_ID)) {
      return event.getParticipant();

    } else if (expression.equals(EXP_OP_TIMESTAMP)) {
      return String.valueOf(event.getTimestamp());

    } else if (expression.equals(EXP_OP_APP)) {
      return event.getApp() != null ? event.getApp() : "<null>";

    } else if (expression.equals(EXP_OP_OBJECT_ID)) {
      return ModernIdSerialiser.INSTANCE.serialiseWaveId(event.getWaveId());

    } else if (expression.equals(EXP_OP_OBJECT_TYPE)) {
      return event.getDataType() != null ? event.getDataType() : "<null>";

    } else if (expression.equals(EXP_OP_PATH)) {
      return event.getPath() != null ? event.getPath() : "<null>";

    }


    // Path-based expresions

    String path = extractExpressionPath(expression);
    String value = event.getContextData().get(path);

    if (value == null) return "<null>";

    if (expression.startsWith(EXP_OP_HASH)) {

      value = String.valueOf(value.hashCode());

    }

    // Secure JSON, delete string delimeter chars
    value = value.replaceAll("\"", "");
    value = value.replaceAll("'", "");

    return value;
  }


  public static boolean isNonPathExpresion(String expresion) {
    for (String exp : EXP_NON_PATH) {
      if (expresion.equals(exp)) return true;
    }
    return false;
  }

  public static boolean isPathExpresion(String expresion) {
    return (expresion.startsWith((EXP_OP_VALUE + "{")) || expresion.startsWith((EXP_OP_HASH + "{")));
  }

  protected boolean isPathExpStart(String s, int pos) {
    String n = s.substring(pos);
    return n.startsWith(EXP_OP_VALUE + "{") || n.startsWith(EXP_OP_HASH + "{");
  }

  /**
   * Expresions are like this: root.list.?.field
   * Paths are like this: root.list.2.field
   *
   * The only supported wildcard is ?
   *
   * @param expr
   * @param path
   * @return
   */
  protected static boolean comparePaths(String expr, String path) {

    if (expr == null && path == null) return true;
    if (expr == null || path == null) return false;
    if (expr.equals(path)) return true;

    Preconditions.checkNotNull(expr);
    Preconditions.checkNotNull(path);

    String[] exprParts = expr.split("\\.");
    String[] actualParts = path.split("\\.");

    if (exprParts.length != actualParts.length) return false;

    // Paths must point to object under root obejct
    if (exprParts.length <= 1 || actualParts.length <= 1) return false;

    boolean doesItMatch = true;
    for (int i = 1; i < exprParts.length; i++) {
      if (!exprParts[i].equals("?")) {
        doesItMatch = exprParts[i].equals(actualParts[i]);
      }
      if (!doesItMatch) break;
    }

    return doesItMatch;
  }



    /**
   * Create a new path replacing part of an expression with a provided path
   * part.
   *
   *
   * if
   *
   * expr = root.list.?.array.?.field path = root.list.2.array.5
   *
   * then
   *
   * return root.list.2.array.5.field
   *
   *
   * @param expr
   * @param path
   * @return
   */
  protected static String matchSubpath(String expr, String path) {

    if (expr == null || path == null) return null;

    String[] exprParts = expr.split("\\.");
    String[] pathParts = path.split("\\.");

    if (pathParts.length > exprParts.length) return null;

    boolean isMatch = true;
    String newPath = "";

    for (int i = 0; i < exprParts.length; i++) {

      if (i < pathParts.length) {

        if (!pathParts[i].equals(exprParts[i])) {
          if (exprParts[i].equals("?")) {

            if (newPath.length() == 0)
              newPath = pathParts[i];
            else
              newPath += "." + pathParts[i];

          } else {
            isMatch = false;
            break;
          }
        } else {

          if (newPath.length() == 0)
            newPath = exprParts[i];
          else
            newPath += "." + exprParts[i];
        }

      } else {

        if (newPath.length() == 0)
          newPath = exprParts[i];
        else
          newPath += "." + exprParts[i];
      }
    }


    if (!isMatch) return null;

    return newPath;
  }



  public interface Operation {

    public String onExpression(String expression);

  }

  private final String string;
  private String rstring;
  private final Operation op;

  public ExpressionParser(String string, Operation op) {
    this.string = string;
    this.rstring = new String(string);
    this.op = op;
  }

  protected int extractNonPathExp(String s, int pos) {
    String n = s.substring(pos);

    for (String exp : EXP_NON_PATH) {
      if (n.startsWith(exp)) {
        return pos + exp.length();
      }
    }


    return -1;
  }

  protected String escapeInsecureChars(String s) {
    s = s.replace('\"', '\0');
    s = s.replace('\'', '\0');
    return s;
  }

  public void parse() {
    // Looks for expressions in the value
    int exprMark = string.indexOf("$");
    while (exprMark >= 0) {

      if (isPathExpStart(string, exprMark)) {
        int startMark = string.indexOf("{", exprMark);
        int endMark = string.indexOf("}", exprMark);

        if (exprMark < startMark && startMark < endMark) {
          op.onExpression(string.substring(exprMark, endMark + 1));
        }
      } else {

        int endExprPos = extractNonPathExp(string, exprMark);

        if (endExprPos > 0) {
          op.onExpression(string.substring(exprMark, endExprPos));
          // go to next $
          exprMark = endExprPos;
        }
      }

      exprMark = string.indexOf("$", exprMark + 1);
    }

  }


  public String replaceParse() {
    // Looks for expressions in the value
    int exprMark = rstring.indexOf("$");
    while (exprMark >= 0) {

      if (isPathExpStart(rstring, exprMark)) {

        int startMark = rstring.indexOf("{", exprMark);
        int endMark = rstring.indexOf("}", exprMark);

        if (exprMark < startMark && startMark < endMark) {

          // We must avoid exceptions if expressions can't be evaluated in the
          // model
          String value = op.onExpression(rstring.substring(exprMark, endMark + 1));

          if (value == null) value = "<missing value>";

          // An effective way to replace the expression
          rstring =
              rstring.substring(0, exprMark) + escapeInsecureChars(value)
                  + rstring.substring(endMark + 1);

        }

      } else {

        int endExprPos = extractNonPathExp(rstring, exprMark);

        if (endExprPos > 0) {
          String replacement =
              escapeInsecureChars(op.onExpression(rstring.substring(exprMark, endExprPos)));
          rstring = rstring.substring(0, exprMark) + replacement + rstring.substring(endExprPos);
        }
      }

      // go to next $
      exprMark = rstring.indexOf("$", exprMark + 1);
    }

    return rstring;

  }

}
