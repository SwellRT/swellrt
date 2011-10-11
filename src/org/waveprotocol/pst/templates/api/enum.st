enum $e.javaType$ implements ProtoEnums.HasIntValue {
  $e.values: {v|$v.name$($v.number$),$\n$}$
  UNKNOWN(-1);

  private final int value;

  private $e.javaType$(int value) {
    this.value = value;
  }

  @Override
  public int getValue() {
    return value;
  }
}
