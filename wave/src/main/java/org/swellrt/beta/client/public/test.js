
//
// Tests
//


describe("Object basic operations suite", function() {

	var obj; // shared by all tests
	
	
	beforeAll(function() {
		var service = swell.runtime.get();
		obj = service.openSync("object-1");
	});	


	it("Fresh new object is empty", function() {
		expect(obj.get()).toEqual({});
	});
	
	
	it("set(), get() with map", function() {
		
		obj.set("mapOne", swell.Map.create());
		var map = obj.node("mapOne");
		
		map.set("string","Hello World");
		map.set("number", 2345);
		map.set("boolean:true", true);
		map.set("boolean:false", false);
		
		expect(map.get("string")).toEqual("Hello World");
		expect(map.get("number")).toEqual(2345);
		expect(map.get("boolean:true")).toEqual(true);
		expect(map.get("boolean:false")).toEqual(false);
		
	});
	
	it("get() using path expression", function() {
		
		expect(obj.get("mapOne.string")).toEqual("Hello World");
		expect(obj.get("mapOne.number")).toEqual(2345);
		expect(obj.get("mapOne.boolean:true")).toEqual(true);
		expect(obj.get("mapOne.boolean:false")).toEqual(false);
		
	});
	
	it("contains() map properties", function() {
				
		expect(obj.contains("","mapOne")).toBe(true);
		expect(obj.contains("","yyyy")).toBe(false);
		
		expect(obj.contains("mapOne","string")).toBe(true);
		expect(obj.contains("mapOne","number")).toBe(true);
		expect(obj.contains("mapOne","xxxx")).toBe(false);
			
	});
	
	it("remove() property fires change event", function() {
		
		var deleteEvents = [];
		var map = obj.node("mapOne");
		map.addListener(function(event) {
			
			if (event.type == swell.Event.REMOVED_VALUE) {
				console.log("removed mapOne."+event.key);
				deleteEvents.push(event);
			}		
			
		});
		
		obj.delete("mapOne.string");
		map.delete("number");
		
		expect(deleteEvents.length).toEqual(2);
		
		expect(deleteEvents[0].type).toEqual(swell.Event.REMOVED_VALUE);
		expect(deleteEvents[0].key).toEqual("string");
		
		expect(deleteEvents[1].type).toEqual(swell.Event.REMOVED_VALUE);
		expect(deleteEvents[1].key).toEqual("number");
		
	});

});


describe("Participants", function() {

	var obj; // shared by all tests
	
	
	beforeAll(function() {
		var service = swell.runtime.get();
		obj = service.openSync("object-2");
	});	

	it("get/add/remove participants", function() {
		
		expect(obj.getParticipants()[0]).toEqual("fake@local.net");
		
		obj.addParticipant('tom@local.net');
		expect(obj.getParticipants()[0]).toEqual("fake@local.net");
		expect(obj.getParticipants()[1]).toEqual("tom@local.net");
		
		obj.removeParticipant('tom@local.net');
		expect(obj.getParticipants()[0]).toEqual("fake@local.net");
		expect(obj.getParticipants().length).toEqual(1);
		
	});

});