public $if(m.inner)$static$endif$ class $m.javaType$ implements $m.name$ {

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|$fieldDeclaration(f=f)$}$

  public $m.javaType$() {
  }

  public $m.javaType$($m.name$ message) {
    copyFrom(message);
  }

  @Override
  public void copyFrom($m.name$ message) {
    $m.fields: {f|$copyField(f=f, target="message")$}$
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  /** Provided to subclasses to clear all fields, for example when deserializing. */
  protected void reset() {
    $m.fields: {f|$clearField(f=f)$}$
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof $m.javaType$) && isEqualTo(o);
  }

  @Override
  public boolean isEqualTo(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof $m.name$) {
      return $m.name$Util.isEqual(this, ($m.name$) o);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return $m.name$Util.getHashCode(this);
  }  
}
