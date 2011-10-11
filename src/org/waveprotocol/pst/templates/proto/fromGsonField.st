if ($target$.has("$f.number$")) {
  JsonElement elem = $target$.get("$f.number$");

  $if (f.optional)$
  if (!elem.isJsonNull()) {
  $endif$

    $if (f.repeated)$
    {
      JsonArray array = elem.getAsJsonArray();
      for (int i = 0; i < array.size(); i++) {
        $if (f.message)$
          $f.javaType$ payload = new $f.javaType$();
          GsonUtil.extractJsonObject(payload, array.get(i), gson, raw);
          add$f.capName$(payload);
        $elseif (f.enum)$
          add$f.capName$(ProtoEnums.valOf(array.get(i).getAsInt(), $f.javaType$.values()));
        $elseif (f.blob)$
          add$f.capName$(new Blob(array.get(i).getAsString()));
        $elseif (f.long)$
          add$f.capName$(GsonUtil.fromJson(array.get(i)));
        $else$
          add$f.capName$(array.get(i).getAs$f.capJavaType$());
        $endif$
      }
    }
    $else$
      $if (f.message)$
      {
        $f.javaType$ payload = new $f.javaType$();
        GsonUtil.extractJsonObject(payload, elem, gson, raw);
        $f.setter$(payload);
      }
      $elseif (f.enum)$
        $f.setter$(ProtoEnums.valOf(elem.getAsInt(), $f.javaType$.values()));
      $elseif (f.blob)$
        $f.setter$(new Blob(elem.getAsString()));
      $elseif (f.long)$
        $f.setter$(GsonUtil.fromJson(elem));
      $else$
        $f.setter$(elem.getAs$f.capJavaType$());
      $endif$
    $endif$

  $if (f.optional)$
  }
  $endif$
}
