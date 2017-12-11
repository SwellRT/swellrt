package org.swellrt.beta.client.rest.operations.params;

public class ObjectIdImpl implements ObjectId {

  protected String id;
  protected String prefix;

  public ObjectIdImpl() {
    super();
  }

  public ObjectIdImpl(String id) {
    super();
    this.id = id;
  }

  public ObjectIdImpl(String id, String prefix) {
    super();
    this.id = id;
    this.prefix = prefix;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

}
