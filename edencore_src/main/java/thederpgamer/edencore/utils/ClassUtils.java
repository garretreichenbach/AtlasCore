package thederpgamer.edencore.utils;

import thederpgamer.edencore.EdenCore;

import java.lang.reflect.Field;

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
			EdenCore.getInstance().logException("An error occurred while trying to get field \"" + fieldName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
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
			EdenCore.getInstance().logException("An error occurred while trying to get field \"" + fieldName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
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
			EdenCore.getInstance().logException("An error occurred while trying to set field \"" + fieldName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
		}
	}

	public static void setField(Class<?> clazz, String fieldName, Object value) {
		try {
			Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(null, value);
		} catch(Exception exception) {
			exception.printStackTrace();
			EdenCore.getInstance().logException("An error occurred while trying to set field \"" + fieldName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
		}
	}

	public static void invokeMethod(Object object, String methodName, Object... args) {
		try {
			Class<?>[] argTypes = new Class<?>[args.length];
			for(int i = 0; i < args.length; i++) argTypes[i] = args[i].getClass();
			object.getClass().getDeclaredMethod(methodName, argTypes).invoke(object, args);
		} catch(Exception exception) {
			exception.printStackTrace();
			EdenCore.getInstance().logException("An error occurred while trying to invoke method \"" + methodName + "\" from object \"" + object.getClass().getSimpleName() + "\"", exception);
		}
	}

	public static void invokeMethod(Class<?> clazz, String methodName, Object... args) {
		try {
			Class<?>[] argTypes = new Class<?>[args.length];
			for(int i = 0; i < args.length; i++) argTypes[i] = args[i].getClass();
			clazz.getDeclaredMethod(methodName, argTypes).invoke(null, args);
		} catch(Exception exception) {
			exception.printStackTrace();
			EdenCore.getInstance().logException("An error occurred while trying to invoke method \"" + methodName + "\" from class \"" + clazz.getSimpleName() + "\"", exception);
		}
	}
}
