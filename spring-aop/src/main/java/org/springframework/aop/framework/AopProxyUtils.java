/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * AOP代理工厂的实用方法。主要用于AOP框架内的内部使用。
 *
 * Utility methods for AOP proxy factories.
 * Mainly for internal use within the AOP framework.
 *
 * <p>See {@link org.springframework.aop.support.AopUtils} for a collection of
 * generic AOP utility methods which do not depend on AOP framework internals.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.aop.support.AopUtils
 */
public abstract class AopProxyUtils {

	/**
	 * 获取给定代理背后的单例目标对象(如果有的话)。
	 *
	 * Obtain the singleton target object behind the given proxy, if any.
	 * @param candidate the (potential) proxy to check
	 * @return the singleton target object managed in a {@link SingletonTargetSource},
	 * or {@code null} in any other case (not a proxy, not an existing singleton target)
	 * @since 4.3.8
	 * @see Advised#getTargetSource()
	 * @see SingletonTargetSource#getTarget()
	 */
	@Nullable
	public static Object getSingletonTarget(Object candidate) {
		//如果当前获取的目标对象是一个Advised类型对象(aop代理对象都实现该借款)
		if (candidate instanceof Advised) {
			//通过getTargetSource方法获取代理目标
			TargetSource targetSource = ((Advised) candidate).getTargetSource();
			//如果代理目标对象是一个SingletonTargetSource
			if (targetSource instanceof SingletonTargetSource) {
				//获取当前代理对象的真正目标对象（可能还是一个代理对象）
				return ((SingletonTargetSource) targetSource).getTarget();
			}
		}
		return null;
	}

	/**
	 * 获取一个代理对象的最终对象类型
	 *
	 * Determine the ultimate target class of the given bean instance, traversing
	 * not only a top-level proxy but any number of nested proxies as well &mdash;
	 * as long as possible without side effects, that is, just for singleton targets.
	 * @param candidate the instance to check (might be an AOP proxy)
	 * @return the ultimate target class (or the plain class of the given
	 * object as fallback; never {@code null})
	 * @see org.springframework.aop.TargetClassAware#getTargetClass()
	 * @see Advised#getTargetSource()
	 */
	public static Class<?> ultimateTargetClass(Object candidate) {
		Assert.notNull(candidate, "Candidate object must not be null");
		//current用于判断
		Object current = candidate;
		Class<?> result = null;
		//直到当前获得的对象不是TargetClassAware类型
		while (current instanceof TargetClassAware) {
			//获得当前对象（一个代理对象）代理的目标对象（这个对象可能还是一个代理对象）类型；
			result = ((TargetClassAware) current).getTargetClass();
			current = getSingletonTarget(current);
		}
		//如果获取到的目标对象是一个cglib代理对象，获取父类类型（才是目标类型）
		if (result == null) {
			result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
		}
		return result;
		/**
		 * 这里面涉及到了很多SpringAOP中的接口，比如TargetClassAware，Advised，TargetSource，SingletonTargetSource等；
		 * 这些接口都是SpringAOP中的一些重要接口:
		 *
		 * TargetClassAware：所有的Aop代理对象或者代理工厂（proxy factory)都要实现的接口，该接口用于暴露出被代理目标对象类型；
		 *
		 * TargetSource：该接口代表一个目标对象，在aop调用目标对象的时候，使用该接口返回真实的对象。
		 * 				比如该接口的一个实现：PrototypeTargetSource，那就是每次调用都返回一个全新的对象实例；
		 *
		 * Advised：该接口用于保存一个代理的相关配置（简单理解为这个对象保存了怎么创建一个代理对象的信息），
		 * 			比如这个代理配置相关的拦截器，建议(advisor)或者增强器（advice)；所有的代理对象都实现了该接口
		 * 			（我们就能够通过一个代理对象获取这个代理对象怎么被代理出来的相关信息）；
		 *
		 * 		Advised接口中的getTargetSource返回的就是TargetSource。意思就是Advised和TargetSource接口虽然在继承关系上，
		 * 	都是继承了TargetClassAware接口，看似平级关系，实际上确实组合关系：
		 */
	}

	/**
	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
	 * {@link org.springframework.aop.SpringProxy} marker interface.
	 * @param advised the proxy config
	 * @return the complete set of interfaces to proxy
	 * @see SpringProxy
	 * @see Advised
	 */
	/**
	 * 判断一个advised真正需要代理的目标接口列表。简单理解，比如在spring使用JDK proxy做代理的时候，
	 * 这个方法返回的类型列表就是真正需要交给Proxy.newProxyInstance方法的接口列表
	 */
	public static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised) {
		return completeProxiedInterfaces(advised, false);
	}

	/**
	 * Determine the complete set of interfaces to proxy for the given AOP configuration.
	 * <p>This will always add the {@link Advised} interface unless the AdvisedSupport's
	 * {@link AdvisedSupport#setOpaque "opaque"} flag is on. Always adds the
	 * {@link org.springframework.aop.SpringProxy} marker interface.
	 * @param advised the proxy config
	 * @param decoratingProxy whether to expose the {@link DecoratingProxy} interface
	 * @return the complete set of interfaces to proxy
	 * @since 4.3
	 * @see SpringProxy
	 * @see Advised
	 * @see DecoratingProxy
	 */
	static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
		Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
		if (specifiedInterfaces.length == 0) {
			// No user-specified interfaces: check whether target class is an interface.
			Class<?> targetClass = advised.getTargetClass();
			if (targetClass != null) {
				if (targetClass.isInterface()) {
					advised.setInterfaces(targetClass);
				}
				else if (Proxy.isProxyClass(targetClass)) {
					advised.setInterfaces(targetClass.getInterfaces());
				}
				specifiedInterfaces = advised.getProxiedInterfaces();
			}
		}
		boolean addSpringProxy = !advised.isInterfaceProxied(SpringProxy.class);
		boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
		boolean addDecoratingProxy = (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class));
		int nonUserIfcCount = 0;
		if (addSpringProxy) {
			nonUserIfcCount++;
		}
		if (addAdvised) {
			nonUserIfcCount++;
		}
		if (addDecoratingProxy) {
			nonUserIfcCount++;
		}
		Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
		System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
		int index = specifiedInterfaces.length;
		if (addSpringProxy) {
			proxiedInterfaces[index] = SpringProxy.class;
			index++;
		}
		if (addAdvised) {
			proxiedInterfaces[index] = Advised.class;
			index++;
		}
		if (addDecoratingProxy) {
			proxiedInterfaces[index] = DecoratingProxy.class;
		}
		return proxiedInterfaces;
	}

	/**
	 * Extract the user-specified interfaces that the given proxy implements,
	 * i.e. all non-Advised interfaces that the proxy implements.
	 * @param proxy the proxy to analyze (usually a JDK dynamic proxy)
	 * @return all user-specified interfaces that the proxy implements,
	 * in the original order (never {@code null} or empty)
	 * @see Advised
	 */
	/**
	 * 用于获取一个代理对象中的用户定义的接口，即非（Advised接口体系）之外的其他接口
	 */
	public static Class<?>[] proxiedUserInterfaces(Object proxy) {
		//得到所有接口
		Class<?>[] proxyInterfaces = proxy.getClass().getInterfaces();
		int nonUserIfcCount = 0;
		//如果是代理，一定实现了SpringProxy
		if (proxy instanceof SpringProxy) {
			nonUserIfcCount++;
		}
		//如果是代理，可能实现了Advised；
		if (proxy instanceof Advised) {
			nonUserIfcCount++;
		}
		//如果是代理，可能实现了DecoratingProxy，装饰代理
		if (proxy instanceof DecoratingProxy) {
			nonUserIfcCount++;
		}
		//拷贝proxyInterfaces中从第0位~第proxyInterfaces.length - nonUserIfcCount个
		//去掉尾巴上的nonUserIfcCount个；
		Class<?>[] userInterfaces = new Class<?>[proxyInterfaces.length - nonUserIfcCount];
		System.arraycopy(proxyInterfaces, 0, userInterfaces, 0, userInterfaces.length);
		Assert.notEmpty(userInterfaces, "JDK proxy must implement one or more interfaces");
		return userInterfaces;
	}

	/**
	 * 判断两个（即将）代理出来的对象是否相同；
	 *
	 * Check equality of the proxies behind the given AdvisedSupport objects.
	 * Not the same as equality of the AdvisedSupport objects:
	 * rather, equality of interfaces, advisors and target sources.
	 */
	public static boolean equalsInProxy(AdvisedSupport a, AdvisedSupport b) {
		return (a == b ||
				(equalsProxiedInterfaces(a, b) && equalsAdvisors(a, b) && a.getTargetSource().equals(b.getTargetSource())));
	}

	/**
	 * 判断两个（即将）代理出来的对象是否拥有相同接口；
	 *
	 * Check equality of the proxied interfaces behind the given AdvisedSupport objects.
	 */
	public static boolean equalsProxiedInterfaces(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getProxiedInterfaces(), b.getProxiedInterfaces());
	}

	/**
	 * 判断两个（即将）代理出来的对象是否拥有相同的建议者（Advisor）
	 *
	 * Check equality of the advisors behind the given AdvisedSupport objects.
	 */
	public static boolean equalsAdvisors(AdvisedSupport a, AdvisedSupport b) {
		return Arrays.equals(a.getAdvisors(), b.getAdvisors());
	}


	/**
	 * 将给定的参数调整为给定方法中的目标签名，如果需要:特别是，如果一个给定的变量数组不需要
	 * 匹配方法中声明的可变参数的数组类型。
	 *
	 * Adapt the given arguments to the target signature in the given method,
	 * if necessary: in particular, if a given vararg argument array does not
	 * match the array type of the declared vararg parameter in the method.
	 * @param method the target method
	 * @param arguments the given arguments
	 * @return a cloned argument array, or the original if no adaptation is needed
	 * @since 4.2.3
	 */
	static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
		if (ObjectUtils.isEmpty(arguments)) {
			return new Object[0];
		}
		if (method.isVarArgs()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			if (paramTypes.length == arguments.length) {
				int varargIndex = paramTypes.length - 1;
				Class<?> varargType = paramTypes[varargIndex];
				if (varargType.isArray()) {
					Object varargArray = arguments[varargIndex];
					if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
						Object[] newArguments = new Object[arguments.length];
						System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
						Class<?> targetElementType = varargType.getComponentType();
						int varargLength = Array.getLength(varargArray);
						Object newVarargArray = Array.newInstance(targetElementType, varargLength);
						System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
						newArguments[varargIndex] = newVarargArray;
						return newArguments;
					}
				}
			}
		}
		return arguments;
	}

}
