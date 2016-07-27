package org.swellrt.server.box.objectapi;

import java.util.Map.Entry;

import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

/**
 * Perform operations in collaborative objects using JSON data
 * 
 * TODO implement unit tests
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ObjectApi {

	private final static String APPEND_MARKER = "$";
	
	public static void doUpdate(Model model, String method, String path, JsonElement jsonData)
			throws ObjectApiException {

		Type parent = model.getRoot();
		if (path.contains(".")) {
			parent = Model.getField(parent, path.substring(path.lastIndexOf(".") + 1));
		}
		doUpdate(model, path, parent, jsonData);

	}
	

	/**
	 * Recursive method to create/update fields in a collaborative object with JSON expressions.
	 * <br>
	 * Having the following parameters:
	 * <pre>
	 * <code>
	 *  path = a.b.c
	 *  data = { json object }
	 * </code>
	 * </pre>
	 * 
	 * The field <code>c</code> will be set or updated by the <code>data</code> object.
	 * <br>
	 * To update a specific field in an array, use a valid index as field:
	 * 
	 * <pre>
	 * <code>
	 * path = a.b.c.3
	 * data = { "hello world" }
	 * </code>
	 * </pre>
	 * 
	 * or use the special index <code>$</code> to append the data to the array:
	 * 
	 * <pre>
	 * <code>
	 * path = a.b.c.$
	 * data = { "hello world" }
	 * </code>
	 * </pre>
	 * 
	 * @param model the collaborative object 
	 * @param path route to the field where perform updates 
	 * @param parent the parent field of the field pointed by path
	 * @param data a JSON object with new data to update or add
	 */
	private static void doUpdate(Model model, String path, Type parent, JsonElement data) {
				
		String keyOrIndex = path.substring(path.lastIndexOf(".")+1);
		
		if (data.isJsonNull()) {
			return;
			
		} else if (data.isJsonObject()) {
			//
			// POST /object/1234/map/key
			// { 
			//	fieldOne : "valueOne",
			//  fieldTwo : "valueTwo"
			// }
			//
			JsonObject jsonObject = data.getAsJsonObject();
			MapType map = model.createMap();
			Type newField = setValue(parent, keyOrIndex, map);
			for (Entry<String, JsonElement> e: jsonObject.entrySet()) {
				//
				// POST /object/1234/map/key/fieldOne
				// { "valueOne" }
				//
				doUpdate(model, path+"."+e.getKey(),  newField, e.getValue());
			}
			
		} else if (data.isJsonArray()) {
			//
			// POST /object/1234/map/key
			// { 
			//	['a', 'b', 'c']
			// }
			//			
			JsonArray jsonArray = data.getAsJsonArray();
			ListType list = model.createList();
			Type newField = setValue(parent, keyOrIndex, list);
			for (int i = 0; i < jsonArray.size(); i++) {
				//
				// POST /object/1234/map/key/0
				// { "a" }
				//
				doUpdate(model, path+"."+APPEND_MARKER, newField, jsonArray.get(i));
			}
			
		} else if (data.isJsonPrimitive()) {

			Type newField = null;
		
			try {
				Double d = data.getAsDouble();		
				newField = model.createNumber(d);					
			} catch (Exception e) {			
			}
			
			if (newField == null) {				
				try {
					String s = data.getAsString();		
					newField = model.createString(s);					
				} catch (Exception e) {				
				}				
			}
			
			if (newField == null) {				
				try {
					Boolean b = data.getAsBoolean();		
					newField = model.createBoolean(b);					
				} catch (Exception e) {
				
				}			
			}
			
			if (newField != null)
				setValue(parent, keyOrIndex, newField);
			
		}
		
	}
	
	private static Type setValue(Type container, String indexOrKey, Type newField) {
		Preconditions.checkArgument(container != null, "Container field can't be null");
		Preconditions.checkArgument(indexOrKey != null, "Index or Key can't be null");	
		Preconditions.checkArgument(newField != null, "Value can't be null");	
		
		if (container instanceof ListType) {				
			ListType list = (ListType) container;
			if  (indexOrKey.equals(APPEND_MARKER)) {
				return list.add(newField);
			}
			try {
				int index = Integer.valueOf(indexOrKey);
				return list.add(index, newField);
			} catch (NumberFormatException e) {
				throw new RuntimeException("Bad list index");
			}				
		} else if (container instanceof MapType) {
			MapType map = (MapType) container;
			return map.put(indexOrKey, newField);
		}
		return null;
	}
	
}
