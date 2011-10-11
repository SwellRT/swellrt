$if (f.repeated)$
  $target$.clear$f.capName$();
  $target$.addAll$f.capName$($f.name$);
$else$
  $target$.set$f.capName$($f.name$);
$endif$
