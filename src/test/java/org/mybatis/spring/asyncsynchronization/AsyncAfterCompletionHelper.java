/**
 * Copyright 2010-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 */
@SuppressWarnings("unused")
public class AsyncAfterCompletionHelper {
  /**
   *
   * Invocation handler that performs afterCompletion on a separate thread See Github issue #18
   *
   * @author Alex Rykov
   *
   */
  static class AsyncAfterCompletionInvocationHandler implements InvocationHandler {

    private Object target;

    AsyncAfterCompletionInvocationHandler(Object target) {
      this.target = target;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if ("afterCompletion".equals(method.getName())) {
        final Set<Object> retValSet = new HashSet<>();
        final Set<Throwable> exceptionSet = new HashSet<>();
        Thread thread = new Thread(() -> {
          try {
            retValSet.add(method.invoke(target, args));
          } catch (InvocationTargetException ite) {
            exceptionSet.add(ite.getCause());

          } catch (IllegalArgumentException | IllegalAccessException e) {
            exceptionSet.add(e);

          }
        });
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
   */
  public TransactionSynchronization createSynchronizationWithAsyncAfterComplete(
      TransactionSynchronization synchronization) {
    if (Proxy.isProxyClass(synchronization.getClass())
        && Proxy.getInvocationHandler(synchronization) instanceof AsyncAfterCompletionInvocationHandler) {
      // avoiding double wrapping just in case
      return synchronization;
    }
    Class<?>[] interfaces = { TransactionSynchronization.class };
    return (TransactionSynchronization) Proxy.newProxyInstance(synchronization.getClass().getClassLoader(), interfaces,
        new AsyncAfterCompletionInvocationHandler(synchronization));

  }

}
