public $if(m.inner)$static$endif$ final class $m.javaType$ extends $m.name$Impl
    implements GsonSerializable {

  $m.nestedMessages: {nested|$class(m=nested)$}$

  public $m.javaType$() {
    super();
  }

  public $m.javaType$($m.name$ message) {
    super(message);
  }
  
  @Override
  public JsonElement toGson(RawStringData raw, Gson gson) {
    return toGsonHelper(this, raw, gson);
  }

  /**
   * Static implementation-independent GSON serializer.  Call this from
   * {@link #toGson} to avoid subclassing issues with inner message types.
   */
  public static JsonElement toGsonHelper($m.name$ message, RawStringData raw, Gson gson) {
    JsonObject json = new JsonObject();
    $m.fields: {f|$toGsonField(f=f, source="message", target="json")$}$
    return json;
  }

  @Override
  public void fromGson(JsonElement json, Gson gson, RawStringData raw) throws GsonException {
    reset();
    JsonObject jsonObject = json.getAsJsonObject();
    // NOTE: always check with has(...) as the json might not have all required
    // fields set.
    $m.fields: {f|$fromGsonField(f=f, target="jsonObject")$}$
  }
}
