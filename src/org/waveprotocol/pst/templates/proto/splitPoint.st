$!!
 ! NOTE: template unused, since split points (aka multi-stage parsing) isn't
 ! supported. If it is, some time, then this template could be used.
 !$

((raw != null && gson != null) ? new JsonPrimitive(raw.addString(gson.toJson($e$))) : $e$)
