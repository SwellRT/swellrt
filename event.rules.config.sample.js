// JavaScript SwellRT script to  generate events covering the event.rules.config.sample

var testModelEvents = function(model) {

  model.root.put("cond1", "yes");
  model.root.put("cond2", "yes");

  model.root.put("map", model.createMap());
  map = model.root.get("map");
  map.put("field1", "VALUE 1");
  map.put("field2", "VALUE 2");
  map.put("field3", "VALUE 3");


  map.put("fieldtestnew", "NEW VALUE");
  map.put("fieldtestupdate", " NOT NEW VALUE YET");
  map.put("fieldtestremove", " NOT REMOVED VALUE YET");
  map.remove("fieldtestremove");


  model.root.put("list", model.createList());
  list = model.root.get("list");
  item = list.add(model.createMap());
  item.put("value", "ITEM VALUE 0");

  model.root.put("doc", model.createText("A BRAND NEW SWELLRT TEXT"));

  // Register a dummy device (for GCM)
  SwellRT.notifications.register("_fake_device_id_");

  // Subscribe to notifications in this data model
  SwellRT.notifications.subscribe(modelid);

}

