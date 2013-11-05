package org.mybatis.spring.asyncsynchronization;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;


import org.springframework.transaction.support.TransactionSynchronization;

/**
 * For use as ByteMan helper
 * 
 * @author Alex Rykov
 *
 */
public class AsyncAfterCompletionHelper {
	/**
	 * 
	 * Invocation handler that performs afterCompletion on a separate thread
	 * 
	 * @author Alex Rykov
	 * 
	 */
	static class AsynchAfterCompletionInvocationHandler implements
			InvocationHandler {

		private Object target;

		AsynchAfterCompletionInvocationHandler(Object target) {
			this.target = target;
		}

		public Object invoke(final Object proxy, final Method method,
				final Object[] args) throws Throwable {
			if ("afterCompletion".equals(method.getName())) {
				final Set<Object> retValSet = new HashSet<Object>();
				final Set<Throwable> exceptionSet = new HashSet<Throwable>();
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							retValSet.add(method.invoke(target, args));
						} catch (InvocationTargetException ite) {
							exceptionSet.add(ite.getCause());

						} catch (IllegalArgumentException e) {
							exceptionSet.add(e);

						} catch (IllegalAccessException e) {
							exceptionSet.add(e);
						}
					}
				};
				thread.start();
				thread.join();
				if (exceptionSet.isEmpty()) {
					return retValSet.iterator().next();
				} else {
					throw exceptionSet.iterator().next();
				}
			} else {
				return method.invoke(target, args);
			}
		}

	}

	/**
	 * Creates proxy that performs afterCompletion call on a separate thread
	 * 
	 * @param synchronization
	 * @return
	 */
	public TransactionSynchronization createSynchronizationWithAsyncAfterComplete(
			TransactionSynchronization synchronization) {
		if (Proxy.isProxyClass(synchronization.getClass())
				&& Proxy.getInvocationHandler(synchronization) instanceof AsynchAfterCompletionInvocationHandler) {
			// avoiding double wrapping just in case
			return synchronization;
		}
		Class<?>[] interfaces = { TransactionSynchronization.class };
		return (TransactionSynchronization) Proxy.newProxyInstance(
				synchronization.getClass().getClassLoader(), interfaces,
				new AsynchAfterCompletionInvocationHandler(synchronization));

	}

}
