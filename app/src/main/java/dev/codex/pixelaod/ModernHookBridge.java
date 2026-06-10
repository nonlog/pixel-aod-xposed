package dev.codex.pixelaod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import io.github.libxposed.api.XposedInterface;

final class ModernHookBridge {
    private static volatile XposedInterface framework;

    interface HookCallback {
        void call(HookParam param) throws Throwable;
    }

    static final class HookParam {
        final Object thisObject;
        final Object[] args;
        private Object result;
        private boolean resultSet;

        HookParam(Object thisObject, Object[] args) {
            this.thisObject = thisObject;
            this.args = args;
        }

        Object getResult() {
            return result;
        }

        void setResult(Object value) {
            result = value;
            resultSet = true;
        }
    }

    private ModernHookBridge() {
    }

    static void attach(XposedInterface xposed) {
        framework = xposed;
        PixelAodLog.attach(xposed);
    }

    static void hookBefore(Method method, HookCallback callback) {
        hook(method, true, callback);
    }

    static void hookAfter(Method method, HookCallback callback) {
        hook(method, false, callback);
    }

    static void hookBefore(Class<?> clazz, String name, HookCallback callback,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        hookBefore(findMethod(clazz, name, parameterTypes), callback);
    }

    static void hookAfter(Class<?> clazz, String name, HookCallback callback,
            Class<?>... parameterTypes) throws NoSuchMethodException {
        hookAfter(findMethod(clazz, name, parameterTypes), callback);
    }

    private static void hook(Method method, boolean before, HookCallback callback) {
        XposedInterface local = framework;
        if (local == null) {
            throw new IllegalStateException("modern Xposed framework is not attached");
        }
        method.setAccessible(true);
        local.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object[] args = argsFrom(chain, method.getParameterCount());
                    HookParam param = new HookParam(chain.getThisObject(), args);
                    if (before) {
                        callback.call(param);
                        if (param.resultSet) {
                            return param.result;
                        }
                        return chain.proceed(args);
                    }
                    Object result = chain.proceed(args);
                    param.result = result;
                    callback.call(param);
                    return param.resultSet ? param.result : result;
                });
    }

    static Class<?> findClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        return Class.forName(name, false, classLoader);
    }

    static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "#" + name
                + Arrays.toString(parameterTypes));
    }

    static Object callMethod(Object receiver, String name, Object... args) throws ReflectiveOperationException {
        if (receiver == null) {
            throw new NullPointerException("receiver");
        }
        Method method = findCompatibleMethod(receiver.getClass(), name, args);
        return method.invoke(receiver, args);
    }

    static Object getObjectField(Object receiver, String name) throws ReflectiveOperationException {
        if (receiver == null) {
            throw new NullPointerException("receiver");
        }
        Field field = findField(receiver.getClass(), name);
        return field.get(receiver);
    }

    private static Method findCompatibleMethod(Class<?> clazz, String name, Object[] args)
            throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName())
                        || method.getParameterCount() != args.length
                        || !parametersMatch(method.getParameterTypes(), args)) {
                    continue;
                }
                method.setAccessible(true);
                return method;
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(clazz.getName() + "#" + name);
    }

    private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            if (args[i] == null) {
                continue;
            }
            Class<?> expected = wrap(parameterTypes[i]);
            if (!expected.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    private static Object[] argsFrom(XposedInterface.Chain chain, int count) {
        Object[] args = new Object[count];
        for (int i = 0; i < count; i++) {
            args[i] = chain.getArg(i);
        }
        return args;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return Void.class;
    }
}
