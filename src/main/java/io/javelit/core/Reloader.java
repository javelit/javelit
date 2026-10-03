/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javelit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import jakarta.annotation.Nullable;

// not using an interface to not expose this in the public API
abstract class Reloader {

  abstract AppEntrypoint reload();

  record AppEntrypoint(JtRunnable runnable, ClassLoader classLoader) {

    static AppEntrypoint of(final Class<?> klass, final ClassLoader classLoader) {
      final Method method = resolveMainMethod(klass);
      final boolean isStatic = Modifier.isStatic(method.getModifiers());
      method.setAccessible(true);

      return new AppEntrypoint(() -> {
        try {
          final Object receiver = isStatic ? null : newInstance(klass);
          final Object[] args = method.getParameterCount() == 0 ? null : new Object[]{new String[]{}};
          method.invoke(receiver, args);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException |
                 NoSuchMethodException e) {
          throw new PageRunException(e);
        }
      }, classLoader);
    }

    private static Method resolveMainMethod(final Class<?> klass) {
      // resolution order: static(args) > static(no-args) > instance(args) > instance(no-args)
      Method method = findMainMethod(klass, true, String[].class);
      if (method == null) {
        method = findMainMethod(klass, true);
      }
      if (method == null) {
        method = findMainMethod(klass, false, String[].class);
      }
      if (method == null) {
        method = findMainMethod(klass, false);
      }
      if (method == null) {
        throw new CompilationException("No main method found in class %s".formatted(klass.getName()));
      }
      return method;
    }

    private static @Nullable Method findMainMethod(final Class<?> klass,
                                                    final boolean requireStatic,
                                                    final Class<?>... parameterTypes) {
      try {
        final Method method = klass.getDeclaredMethod("main", parameterTypes);
        return Modifier.isStatic(method.getModifiers()) == requireStatic ? method : null;
      } catch (NoSuchMethodException e) {
        return null;
      }
    }

    private static Object newInstance(final Class<?> klass) throws NoSuchMethodException,
                                                                     InvocationTargetException,
                                                                     IllegalAccessException,
                                                                     InstantiationException {
      final Constructor<?> constructor = klass.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    }
  }
}
