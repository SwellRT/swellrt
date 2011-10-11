$if (f.optional)$
if ($source$.has$f.capName$()) {
  $toGsonFieldInner()$
}
$elseif (f.repeated)$
{
  $toGsonFieldInner()$
}
$else$
$toGsonFieldInner()$
$endif$
