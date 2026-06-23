package atlas.core.utils;

import atlas.core.AtlasCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ClassUtils {

	public static Object getField(Object object, String fieldName) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(object);
		} catch(Exception exception) {
			exception.printStackTrace();
			AtlasCore.getInstance().logException("An error occurred while trying to get field \"" + fieldName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
			return null;
		}
	}

	public static Object getField(Class<?> clazz, String fieldName) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(null);
		} catch(Exception exception) {
			exception.printStackTrace();
			AtlasCore.getInstance().logException("An error occurred while trying to get field \"" + fieldName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
			return null;
		}
	}

	public static void setField(Object object, String fieldName, Object value) {
		try {
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(object, value);
		} catch(Exception exception) {
			exception.printStackTrace();
			AtlasCore.getInstance().logException("An error occurred while trying to set field \"" + fieldName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
		}
	}

	public static void setField(Class<?> clazz, String fieldName, Object value) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(null, value);
		} catch(Exception exception) {
			exception.printStackTrace();
			AtlasCore.getInstance().logException("An error occurred while trying to set field \"" + fieldName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
		}
	}

	public static Object invokeMethod(Object object, String methodName, Object... args) {
		try {
			Method method = findMethod(object.getClass(), methodName, args);
			if(method == null) throw new NoSuchMethodException(methodName + " (" + args.length + " args)");
			method.setAccessible(true);
			return method.invoke(object, args);
		} catch(Exception exception) {
			AtlasCore.getInstance().logException("An error occurred while trying to invoke method \"" + methodName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
			return null;
		}
	}

	public static Object invokeMethod(Class<?> clazz, String methodName, Object... args) {
		try {
			Method method = findMethod(clazz, methodName, args);
			if(method == null) throw new NoSuchMethodException(methodName + " (" + args.length + " args)");
			method.setAccessible(true);
			return method.invoke(null, args);
		} catch(Exception exception) {
			AtlasCore.getInstance().logException("An error occurred while trying to invoke method \"" + methodName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
			return null;
		}
	}

	/**
	 * Finds a method by name and argument compatibility, walking the class hierarchy
	 * and tolerating null arguments, primitive parameters (auto-boxing), and
	 * supertype/interface parameters — none of which {@code getDeclaredMethod} with
	 * exact arg classes can handle.
	 */
	private static Method findMethod(Class<?> type, String name, Object[] args) {
		for(Class<?> c = type; c != null; c = c.getSuperclass()) {
			for(Method m : c.getDeclaredMethods()) {
				if(m.getName().equals(name) && m.getParameterCount() == args.length && parametersMatch(m.getParameterTypes(), args)) return m;
			}
		}
		// Catch inherited/interface (default) methods not found by getDeclaredMethods.
		for(Method m : type.getMethods()) {
			if(m.getName().equals(name) && m.getParameterCount() == args.length && parametersMatch(m.getParameterTypes(), args)) return m;
		}
		return null;
	}

	private static boolean parametersMatch(Class<?>[] paramTypes, Object[] args) {
		for(int i = 0; i < paramTypes.length; i++) {
			if(args[i] == null) {
				if(paramTypes[i].isPrimitive()) return false; // null can't satisfy a primitive
				continue;
			}
			if(!box(paramTypes[i]).isAssignableFrom(args[i].getClass())) return false;
		}
		return true;
	}

	private static Class<?> box(Class<?> c) {
		if(!c.isPrimitive()) return c;
		if(c == int.class) return Integer.class;
		if(c == long.class) return Long.class;
		if(c == boolean.class) return Boolean.class;
		if(c == double.class) return Double.class;
		if(c == float.class) return Float.class;
		if(c == short.class) return Short.class;
		if(c == byte.class) return Byte.class;
		if(c == char.class) return Character.class;
		return c;
	}
}
