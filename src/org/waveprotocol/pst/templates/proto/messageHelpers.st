/** Get or create a $t.javaType$ from a $t.name$. */
private $t.javaType$ getOrCreate$t.javaType$($t.name$ message) {
  if (message instanceof $t.javaType$) {
    return ($t.javaType$) message;
  } else {
    $t.javaType$ messageImpl = new $t.javaType$();
    messageImpl.copyFrom(message);
    return messageImpl;
  }
}
