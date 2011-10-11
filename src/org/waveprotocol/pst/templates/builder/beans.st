$if (f.optional)$

  public $m.javaType$ clear$f.capName$() {
    $f.name$ = null;
    return this;
  }

$endif$

$if (f.repeated)$

  $if (f.message)$
    public $m.javaType$ addAll$f.capName$(List<? extends $f.messageType$> messages) {
      for ($f.messageType$ message : messages) {
      	add$f.capName$(message);
      }
      return this;
    }
  $else$
    public $m.javaType$ addAll$f.capName$(List<$f.boxedJavaType$> values) {
      this.$f.name$.addAll(values);
      return this;
    }
  $endif$

  $if (f.message)$
    public $m.javaType$ $f.setter$(int n, $f.messageType$ message) {
      this.$f.name$.set(n, message);
      return this;
    }
  $else$
    public $m.javaType$ $f.setter$(int n, $f.javaType$ value) {
      this.$f.name$.set(n, value);
      return this;
    }
  $endif$

  $if (f.message)$
    public $m.javaType$ add$f.capName$($f.messageType$ message) {
      this.$f.name$.add(message);
      return this;
    }
  $else$
    public $m.javaType$ add$f.capName$($f.javaType$ value) {
      this.$f.name$.add(value);
      return this;
    }
  $endif$

  public $m.javaType$ clear$f.capName$() {
    $f.name$.clear();
    return this;
  }

$else$

  $if (f.message)$
    public $m.javaType$ $f.setter$($f.messageType$ message) {
      this.$f.name$ = message;
      return this;
    }
  $else$
    public $m.javaType$ $f.setter$($f.javaType$ value) {
      this.$f.name$ = value;
      return this;
    }
  $endif$

$endif$
