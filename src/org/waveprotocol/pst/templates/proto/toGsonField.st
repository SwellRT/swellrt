$if (f.optional)$
if (has$f.capName$()) {
$endif$

  $if (f.repeated)$
  {
    JsonArray array = new JsonArray();
    for (int i = 0; i < $f.getter$Size(); i++) {
      $if (f.message)$
        JsonElement elem = ((GsonSerializable) $f.getter$(i)).toGson(raw, gson);
        // NOTE(kalman): if multistage parsing worked, split point would go here.
        array.add(elem);
      $elseif (f.enum)$
        array.add(new JsonPrimitive($f.getter$(i).getValue()));
      $elseif (f.blob)$
        array.add(new JsonPrimitive($f.getter$(i).getData()));
      $elseif (f.long)$
        array.add(GsonUtil.toJson($f.getter$(i)));
      $else$
        array.add(new JsonPrimitive($f.getter$(i)));
      $endif$
    }
    $target$.add("$f.number$", array);
  }
  $else$
    $if (f.message)$
    {
      JsonElement elem = ((GsonSerializable) $f.getter$()).toGson(raw, gson);
      $target$.add("$f.number$", elem);
    }
    $elseif (f.enum)$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$().getValue()));
    $elseif (f.blob)$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$().getData()));
    $elseif (f.long)$
      $target$.add("$f.number$", GsonUtil.toJson($f.getter$()));
    $else$
      $target$.add("$f.number$", new JsonPrimitive($f.getter$()));
    $endif$
  $endif$

$if (f.optional)$
}
$endif$
