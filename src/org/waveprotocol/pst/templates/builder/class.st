public $if(m.inner)$static$endif$ final class $m.javaType$ {

  /** Factory to pass to {@link #build()}. */
  public interface Factory {
    $m.name$ create();
  }

  $m.nestedMessages: {nested|$class(m=nested)$}$

  $m.fields: {f|$fieldDeclaration(f=f)$}$

  public $m.javaType$() {
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  /** Builds a {@link $m.name$} using this builder and a factory. */
  public $m.name$ build(Factory factory) {
    $m.name$ message = factory.create();
    $m.fields: {f|$buildField(f=f, target="message")$}$
    return message;
  }
}
