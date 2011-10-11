public $if(m.inner)$static$endif$ final class $m.javaType$ {
  $m.nestedMessages: {nested|$class(m=nested)$}$

  private $m.javaType$() {}

  /** Returns true if m1 and m2 are structurally equal. */
  public static boolean isEqual($m.name$ m1, $m.name$ m2) {
    $m.fields: {f|$returnFalseIfNotEqual(f=f, target1="m1", target2="m2")$}$
    return true;
  }

  /** Returns true if m1 and m2 are equal according to isEqual. */
  public static boolean areAllEqual(List<? extends $m.name$> m1,
      List<? extends $m.name$> m2) {
    if (m1.size() != m2.size()) return false;
    Iterator<? extends $m.name$> i1 = m1.iterator();
    Iterator<? extends $m.name$> i2 = m2.iterator();
    while (i1.hasNext()) {
      if (!isEqual(i1.next(), i2.next())) return false;
    }
    return true;
  }

  /** Returns a structural hash code of message. */
  public static int getHashCode($m.name$ message) {
    int result = 1;
    $m.fields: {f|$addHashCode(f=f, shift="31", result="result", target="message")$}$
    return result;
  }
}
