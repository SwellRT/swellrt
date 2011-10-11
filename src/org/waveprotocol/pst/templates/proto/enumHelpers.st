/** Translates a pojo enum to a proto enum. */
private $t.protoType$ toProtoEnum$t.javaType$($t.javaType$ value) {
  return $t.protoType$.valueOf(value.getValue());
}

/** Translates a proto enum to a pojo enum. */
private $t.javaType$ toPojoEnum$t.javaType$($t.protoType$ value) {
  return ProtoEnums.valOf(value.getNumber(), $t.javaType$.values());
}
