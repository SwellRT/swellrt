$if (f.optional)$

  $result$ = ($shift$ * $result$) + (has$f.capName$() ? $f.name$.hashCode() : 0);

$else$

  $result$ = ($shift$ * $result$) + $f.name$.hashCode();

$endif$
