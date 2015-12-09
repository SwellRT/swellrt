package org.swellrt.server.box.events;

public class EventRuleClass {

  public static EventRuleClass ofEvent(Event e) {
    return new EventRuleClass(e.getApp(), e.getDataType());
  }

  public static EventRuleClass ofEventRule(EventRule r) {
    return new EventRuleClass(r.getApp(), r.getDataType());
  }

  String app;
  String dataType;

  protected EventRuleClass(String app, String dataType) {
    super();
    this.app = app;
    this.dataType = dataType;
  }

  public String getApp() {
    return app;
  }

  public String getDataType() {
    return dataType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((app == null) ? 0 : app.hashCode());
    result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EventRuleClass other = (EventRuleClass) obj;
    if (app == null) {
      if (other.app != null) return false;
    } else if (!app.equals(other.app)) return false;
    if (dataType == null) {
      if (other.dataType != null) return false;
    } else if (!dataType.equals(other.dataType)) return false;
    return true;
  }


}
