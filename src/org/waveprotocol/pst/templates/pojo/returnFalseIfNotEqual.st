$if (f.optional)$

  if (has$f.capName$() != $target$.has$f.capName$()) return false;
  $if (f.primitive)$
    if (has$f.capName$() && ($f.getter$() != $target$.$f.getter$())) return false;
  $else$
    if (has$f.capName$() && !$f.getter$().equals($target$.$f.getter$())) return false;
  $endif$

$elseif (f.repeated)$

  if (!$f.getter$().equals($target$.$f.getter$())) return false;

$else$

  $if (f.primitive)$
    if ($f.getter$() != $target$.$f.getter$()) return false;
  $else$
    if (!$f.getter$().equals($target$.$f.getter$())) return false;
  $endif$

$endif$
