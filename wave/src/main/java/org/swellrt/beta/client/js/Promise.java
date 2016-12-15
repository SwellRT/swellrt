package org.swellrt.beta.client.js;


import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;



@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Promise<S, R> {

	@JsFunction
	public interface FunctionParam<T> {
		void exec(T o);
	}

	@JsFunction
	public interface ConstructorParam<S, R> {
		void exec(FunctionParam<S> resolve, FunctionParam<R> reject);
	}

	@JsConstructor
	public Promise(ConstructorParam<S, R> parameters) {
	}


}


