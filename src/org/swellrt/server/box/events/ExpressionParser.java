package org.swellrt.server.box.events;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExpressionParser {

  public final static String EXP_OP_VALUE = "$";
  public final static String EXP_OP_HASH = "$hash";
  public final static String EXP_OP_AUTHOR = "$author";
  public final static String EXP_OP_TIMESTAMP = "$timestamp";
  public final static String EXP_OP_PARTICIPANT = "$participant";


  // The reg exp for expresion
  private static final Pattern EXP_PATTERN = Pattern
      .compile("(\\$|\\$hash)\\{(([a-zA-Z0-9_]*|\\?)\\.)*(([a-zA-Z0-9_]*|\\?)*)\\}");


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

  protected static String evaluateExpression(Event event, String expression)
      throws InvalidEventExpressionException {

    // No path-based expresions

    if (expression.equals(EXP_OP_AUTHOR)) {
      return event.getAuthor();
    } else if (expression.equals(EXP_OP_PARTICIPANT)) {
      return event.getParticipant();
    } else if (expression.equals(EXP_OP_TIMESTAMP)) {
      // TODO provide formatted dates
      return String.valueOf(event.getTimestamp());
    }

    // Path-based expresions

    String path = extractExpressionPath(expression);
    String value = event.getContextData().get(path);

    if (expression.startsWith(EXP_OP_HASH)) {
      // TODO calculate a hash
      return value;
    }

    return value;
  }


  public static boolean isNonPathExpresion(String expresion) {
    return (expresion.equals(EXP_OP_AUTHOR)) || (expresion.equals(EXP_OP_PARTICIPANT))
        || (expresion.equals(EXP_OP_TIMESTAMP));
  }

  public static boolean isPathExpresion(String expresion) {
    return (expresion.startsWith((EXP_OP_VALUE + "{")) || (expresion.startsWith(EXP_OP_HASH + "{")));
  }


  /**
   * Expresions are like this: root.list.?.field Path are like this:
   * root.list.2.field
   *
   * The only supported wildcard is ?
   *
   * @param expr
   * @param actual
   * @return
   */
  protected static boolean comparePaths(String expr, String actual) {
    Preconditions.checkNotNull(expr);
    Preconditions.checkNotNull(actual);

    String[] exprParts = expr.split("\\.");
    String[] actualParts = actual.split("\\.");

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

  protected boolean isPathExpStart(String s, int pos) {
    String n = s.substring(pos);
    return n.startsWith(EXP_OP_VALUE + "{") || n.startsWith(EXP_OP_HASH + "{");
  }

  protected int extractNonPathExp(String s, int pos) {
    String n = s.substring(pos);
    if (n.startsWith(EXP_OP_AUTHOR)) {
      return pos + EXP_OP_AUTHOR.length();

    } else if (n.startsWith(EXP_OP_PARTICIPANT)) {
      return pos + EXP_OP_PARTICIPANT.length();

    } else if (n.startsWith(EXP_OP_TIMESTAMP)) {
      return pos + EXP_OP_TIMESTAMP.length();
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
          String replacement =
              escapeInsecureChars(op.onExpression(rstring.substring(exprMark, endMark + 1)));
          // An effective way to replace the expression
          rstring = rstring.substring(0, exprMark) + replacement + rstring.substring(endMark + 1);
        }

      } else {

        int endExprPos = extractNonPathExp(rstring, exprMark);

        if (endExprPos > 0) {
          String replacement =
              escapeInsecureChars(op.onExpression(rstring.substring(exprMark, endExprPos)));
          rstring = rstring.substring(0, exprMark) + replacement + rstring.substring(endExprPos);
          // go to next $
          exprMark = endExprPos;
        }
      }

      exprMark = rstring.indexOf("$");
    }

    return rstring;

  }

}
