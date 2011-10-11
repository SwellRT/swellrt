// We have to use fully-qualified name of the GsonSerializable class here in order to make it
// visible in case of nested classes.
public $if(m.inner)$static$endif$ final class $m.javaType$ extends org.waveprotocol.wave.communication.gwt.JsonMessage
    implements $m.name$ {

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|
    private static final String key$f.capName$ = "$f.number$";
  }$

  protected $m.javaType$() {
  }

  public static $m.javaType$ create() {
    $m.javaType$ instance = ($m.javaType$) JsonMessage.createJsonMessage();
    
    // Force all lists to start with an empty list rather than no property for
    // the list. This is so that the native JS equality works, since (obviously)
    // {} != {"foo": []} while in the context of messages they should be.
    $m.fields: {f|
      $if (f.repeated)$
        instance.clear$f.capName$();
      $endif$
    }$
    
    return instance;
  }

  @Override
  public void copyFrom($m.name$ message) {
    super.copyFrom(($m.javaType$) message);
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  @Override
  public boolean isEqualTo(Object o) {
    if (o instanceof $m.javaType$) {
      return nativeIsEqualTo(o);
    } else if (o instanceof $m.name$) {
      return $m.name$Util.isEqual(this, ($m.name$) o);
    } else {
      return false;
    }
  }
}
