package org.swellrt.model.shared;

import java.util.Stack;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableModel;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableText;
import org.swellrt.model.ReadableType;
import org.swellrt.model.ReadableTypeVisitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ModelToJsonVisitor implements ReadableTypeVisitor {
	
	Stack<JsonElement> stack = new Stack<JsonElement>();
	JsonParser parser = new JsonParser();
	
	public ModelToJsonVisitor() {
		stack = new Stack<JsonElement>();				
	}
	
	public JsonElement getResult() {
		return stack.pop();
	}
	
	@Override
	public void visit(ReadableModel instance) {
		instance.getRoot().accept(this);
	}

	@Override
	public void visit(ReadableString instance) {
		String text = instance.getValue();
		text = text.replace("\"","").replace("'", "");
		stack.push(parser.parse("\""+text+"\""));
	}

	@Override
	public void visit(ReadableMap instance) {
		JsonObject o = new JsonObject();
		for(String k: instance.keySet()) {
			ReadableType t = instance.get(k);
			if (t == null) continue;
			t.accept(this);
			o.add(k, stack.pop());
		}
		stack.push(o);
	}

	@Override
	public void visit(ReadableList<? extends ReadableType> instance) {
		JsonArray o = new JsonArray();
		for(int i = 0; i < instance.size(); i++) {
			ReadableType t = instance.get(i);
			if (t == null) continue;
			t.accept(this);
			o.add(stack.pop());
		}
		stack.push(o);
	}

	@Override
	public void visit(ReadableText instance) {
		String text = instance.getText(0, instance.getSize());
		text = text.replace("\"","").replace("'", "");
		stack.push(parser.parse("\""+text+"\""));
	}

	@Override
	public void visit(ReadableFile instance) {
		stack.push(parser.parse("\""+instance.getValue().serialise()+"\""));
	}

	@Override
	public void visit(ReadableNumber instance) {
		stack.push(parser.parse(instance.getValue()));

	}

	@Override
	public void visit(ReadableBoolean instance) {
		stack.push(parser.parse(instance.toString()));
	}

}
