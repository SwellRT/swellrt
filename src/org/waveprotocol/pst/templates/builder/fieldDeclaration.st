$if (f.repeated)$

  $if (f.message)$
    private final List<$f.messageType$> $f.name$ = new ArrayList<$f.messageType$>();
  $else$
    private final List<$f.boxedJavaType$> $f.name$ = new ArrayList<$f.boxedJavaType$>();
  $endif$

$else$

  $if (f.message)$  
    private $f.messageType$ $f.name$;
  $else$
    private $f.boxedJavaType$ $f.name$;
  $endif$

$endif$
