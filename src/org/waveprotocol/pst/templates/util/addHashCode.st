$if (f.primitiveAndNotRepeated)$
  $if (f.optional)$
    $result$ = ($shift$ * $result$) + ($target$.has$f.capName$() ? $f.boxedJavaType$.valueOf($target$.$f.getter$()).hashCode() : 0);
  $else$
    $result$ = ($shift$ * $result$) + $f.boxedJavaType$.valueOf($target$.$f.getter$()).hashCode();
  $endif$
$else$
  $if (f.optional)$
    $result$ = ($shift$ * $result$) + ($target$.has$f.capName$() ? $target$.$f.getter$().hashCode() : 0);
  $else$
    $result$ = ($shift$ * $result$) + $target$.$f.getter$().hashCode();
  $endif$
$endif$
