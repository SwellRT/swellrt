$if (f.repeated)$

  clear$f.capName$();
  $if (f.message)$
    for ($f.messageType$ field : $target$.$f.getter$()) {
      add$f.capName$(new $f.javaType$(field));
    }
  $else$
    for ($f.javaType$ field : $target$.$f.getter$()) {
      add$f.capName$(field);
    }
  $endif$

$else$

  $if (f.optional)$
  if ($target$.has$f.capName$()) {
  $endif$
    $if (f.message)$
      $f.setter$(new $f.javaType$($target$.$f.getter$()));
    $else$
      $f.setter$($target$.$f.getter$());
    $endif$
  $if (f.optional)$
  } else {
    clear$f.capName$();
  }
  $endif$

$endif$
