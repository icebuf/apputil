package com.icebuf.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Reflect util.
 *
 * @author IceTang
 * @version 1.0  Data: 2020/8/11 E-mail：bflyff@hotmail.com
 */
public class ReflectUtil {

    /**
     * Gets annotation.
     *
     * @param <T>             the type parameter
     * @param clazz           the clazz
     * @param annotationClazz the annotation clazz
     * @return the annotation
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationClazz) {
        T annotation = clazz.getAnnotation(annotationClazz);
        if(annotation == null) {
            Class<?> superClazz = clazz.getSuperclass();
            if(superClazz != null) {
                annotation = getAnnotation(superClazz, annotationClazz);
            }
        }
        return annotation;
    }

    /**
     * 设置对象的私有成员变量值
     *
     * @param object    目标对象
     * @param fieldName 成员名称
     * @param value     目标值
     * @return true为成功 field
     */
    public static boolean set(@NonNull Object object, @NonNull String fieldName,
                              Object value) {
        return set(object, fieldName, value, false);
    }

    /**
     * 设置对象的私有成员变量值
     *
     * @param object       目标对象
     * @param fieldName    成员名称
     * @param value        目标值
     * @param includeSuper 如果未找到字段，则在第一个找到字段的父类中设置
     * @return true为成功 field
     */
    public static boolean set(@NonNull Object object, @NonNull String fieldName,
                              Object value, boolean includeSuper) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            return set(object, field, value);
        } catch (NoSuchFieldException e) {
            return includeSuper && setSuperField(object, fieldName, value);
        }
    }


    public static boolean set(@NonNull Object object,
                               Field field, @Nullable Object value) {
        if(field == null) {
            return false;
        }
        accessible(field);
        try {
            field.set(object, value);
            return true;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static <T> T get(Object object, String fieldName) {
        try {
            return get(object, object.getClass().getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T get(Object object, Field field) {
        if(field == null) {
            return null;
        }
        accessible(field);
        try {
            return cast(field.get(object));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean getBoolean(Object object, String fieldName) {
        try {
            return getBoolean(object, object.getClass().getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean getBoolean(Object object, Field field) {
        accessible(field);
        try {
            return field.getBoolean(object);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void accessible(AccessibleObject object) {
        if(!object.isAccessible()) {
            object.setAccessible(true);
        }
    }

    /**
     * Sets super field.
     *
     * @param object     the object
     * @param fieldName  the field name
     * @param fieldValue the field value
     * @return the super field
     */
    public static boolean setSuperField(Object object, String fieldName, Object fieldValue) {
        Field field = getSuperField(object.getClass(), fieldName);
        return set(object, field, fieldValue);
    }

    /**
     * Gets super field.
     *
     * @param clazz     the clazz
     * @param fieldName the field name
     * @return the super field
     */
    public static Field getSuperField(Class<?> clazz, String fieldName) {
        if(clazz == null) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getSuperField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * New instance safe t.
     *
     * @param assignableClazz the assignable clazz
     * @param clazzName       the clazz name
     * @param objects         the objects
     * @return the t
     */
    public static <T> T newInstanceSafe(Class<?> assignableClazz, String clazzName, Object... objects) {
        try {
            Class<T> clazz = cast(Class.forName(clazzName));
            return newInstanceSafe(assignableClazz, clazz, objects);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        if(obj != null) {
            Class<T> clazz = (Class<T>) obj.getClass();
            return clazz.cast(obj);
        }
        return null;
    }

    /**
     * New instance t.
     *
     * @param <T>       the type parameter
     * @param clazzName the clazz name
     * @param objects   the objects
     * @return the t
     */
    public static <T> T newInstance(String clazzName, Object... objects) {
        try {
            Class<T> clazz = cast(Class.forName(clazzName));
            return newInstance(clazz, objects);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * New instance safe t.
     *
     * @param <T>             the type parameter
     * @param assignableClazz the assignable clazz
     * @param clazz           the clazz
     * @param objects         the objects
     * @return the t
     */
    public static <T> T newInstanceSafe(Class<?> assignableClazz, Class<T> clazz, Object... objects) {
        if (assignableClazz.isAssignableFrom(clazz)) {
            return newInstance(clazz, objects);
        }
        throw new RuntimeException("the param class of " + clazz.getName()
                + " must be assignable from " + assignableClazz.getName());
    }

    /**
     * New instance t.
     *
     * @param <T>     the type parameter
     * @param clazz   the clazz
     * @param objects the objects
     * @return the t
     */
    public static <T> T newInstance(Class<T> clazz, Object... objects) {
        try {
            if (objects == null || objects.length <= 0) {
                return clazz.newInstance();
            }
            List<Class<?>> clazzList = new ArrayList<>();
            for (Object object : objects) {
                clazzList.add(object.getClass());
            }
            Constructor<?> constructor = clazz.getConstructor(clazzList.toArray(new Class[0]));
            return clazz.cast(constructor.newInstance(objects));
        } catch(NoSuchMethodException
                | IllegalAccessException
                | InstantiationException
                | InvocationTargetException e){
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Invoke.
     *
     * @param object the object
     * @param name   the name
     * @param params the params
     */
    public static <T> T invoke(Object object, String name, Object... params) {
        Class<?> clazz = object.getClass();
        Class<?>[] paramsClazz = null;
        if(params != null) {
            paramsClazz = new Class<?>[params.length];
            for (int i = 0; i < paramsClazz.length; i++) {
                paramsClazz[i] = params[i].getClass();
            }
        }
        try {
            Method method = clazz.getDeclaredMethod(name, paramsClazz);
            accessible(method);
            return cast(method.invoke(object, params));
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }
}
