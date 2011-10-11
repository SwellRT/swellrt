$if (f.optional)$

  if ($target1$.has$f.capName$() != $target2$.has$f.capName$()) return false;
  $if (f.primitive)$
    if ($target1$.has$f.capName$() && ($target1$.$f.getter$() != $target2$.$f.getter$())) return false;
  $elseif (f.message)$
    if ($target1$.has$f.capName$() && !$f.javaType$.isEqual($target1$.$f.getter$(), $target2$.$f.getter$())) return false;
  $else$
    if ($target1$.has$f.capName$() && !$target1$.$f.getter$().equals($target2$.$f.getter$())) return false;
  $endif$

$elseif (f.repeated)$

  $if (f.message)$
    if (!$f.javaType$.areAllEqual($target1$.$f.getter$(), $target2$.$f.getter$())) return false;
  $else$
    if (!$target1$.$f.getter$().equals($target2$.$f.getter$())) return false;
  $endif$

$else$

  $if (f.primitive)$
    if ($target1$.$f.getter$() != $target2$.$f.getter$()) return false;
  $elseif (f.message)$
    if (!$f.javaType$.isEqual($target1$.$f.getter$(), $target2$.$f.getter$())) return false;
  $else$
    if (!$target1$.$f.getter$().equals($target2$.$f.getter$())) return false;
  $endif$

$endif$
