package org.swellrt.beta.client.js;


import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;


/**
 * Overlay type of JS native Promise object
 *
 * @param <S>
 *          Type of result for a successful invocation
 * @param <R>
 *          Type of result for an failed invocation
 */
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


