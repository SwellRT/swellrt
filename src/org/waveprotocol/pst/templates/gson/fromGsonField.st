if ($target$.has("$f.number$")) {
  $if (f.optional)$
    JsonElement elem = $target$.get("$f.number$");
    if (!elem.isJsonNull()) {
      $fromGsonFieldInner(expr="elem")$
    }
  $else$
    $fromGsonFieldInner(expr={$target$.get("$f.number$")})$
  $endif$
}
