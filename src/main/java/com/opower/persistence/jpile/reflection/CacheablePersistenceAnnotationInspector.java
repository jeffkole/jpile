package com.opower.persistence.jpile.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.googlecode.ehcache.annotations.Cacheable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static com.google.common.collect.ImmutableList.*;
import static com.google.common.collect.Lists.*;

/**
 * The default implementation which parses the annotations. You should not create an instance of this. It should
 * be created by Spring so that the @Cacheable annotations are parsed correctly.
 *
 * @author amir.raminfar
 * @since 1.0
 */
public class CacheablePersistenceAnnotationInspector implements PersistenceAnnotationInspector {

    /**
     * Uses <code>AnnotationUtils.findAnnotation()</code> from Spring framework. Searches all subclasses and class
     * {@inheritDoc}
     *
     * @see org.springframework.core.annotation.AnnotationUtils
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(clazz, annotationType);
    }

    /**
     * Uses <code>AnnotationUtils.findAnnotation()</code> from Spring framework. Searches all subclasses and class
     * {@inheritDoc}
     *
     * @see org.springframework.core.annotation.AnnotationUtils
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(method, annotationType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType) {
        return findAnnotation(method, annotationType) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return findAnnotation(clazz, annotationType) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public String tableName(Class<?> aClass) {
        Table table = findAnnotation(aClass, Table.class);
        Preconditions.checkNotNull(table);
        if(table.name().isEmpty()) {
            return aClass.getSimpleName().toLowerCase();
        }
        else {
            return table.name();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public boolean hasTableAnnotation(Class<?> aClass) {
        return hasAnnotation(aClass, Table.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public String secondaryTable(Class<?> aClass) {
        SecondaryTable table = findAnnotation(aClass, SecondaryTable.class);
        if(table != null) {
            return table.name();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public List<SecondaryTable> findSecondaryTables(Class<?> aClass) {
        List<SecondaryTable> annotations = ImmutableList.of();
        SecondaryTable secondaryTable = findAnnotation(aClass, SecondaryTable.class);
        SecondaryTables secondaryTables = findAnnotation(aClass, SecondaryTables.class);
        if(secondaryTables != null) {
            annotations = copyOf(secondaryTables.value());
        }
        else if(secondaryTable != null) {
            annotations = of(secondaryTable);
        }
        return annotations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public Method idGetter(Class<?> aClass) {
        List<AnnotatedMethod<Id>> methods = annotatedMethodsWith(aClass, Id.class);
        return methods.size() > 0 ? methods.get(0).getMethod() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public Method setterFromGetter(Method getter) {
        Preconditions.checkNotNull(getter, "Cannot find setter from null getter");
        Class aClass = getter.getDeclaringClass();
        return ReflectionUtils.findMethod(aClass, getter.getName().replace("get", "set"), getter.getReturnType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public Method getterFromSetter(Method setter) {
        Preconditions.checkNotNull(setter, "Cannot find getter from null setter");
        Class aClass = setter.getDeclaringClass();
        return ReflectionUtils.findMethod(aClass, setter.getName().replace("set", "get"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public Field fieldFromGetter(Method getter) {
        Class aClass = getter.getDeclaringClass();
        String fieldName = getter.getName().replaceFirst("get", "");
        fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
        return ReflectionUtils.findField(aClass, fieldName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public <A extends Annotation> List<AnnotatedMethod<A>> annotatedMethodsWith(Class<?> aClass, Class<A> annotation) {
        List<AnnotatedMethod<A>> methods = newArrayList();
        for(Method m : ReflectionUtils.getAllDeclaredMethods(aClass)) {
            A a = findAnnotation(m, annotation);
            if(a != null) {
                methods.add(new AnnotatedMethod<A>(m, a));
            }
        }

        return methods;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public List<Method> methodsAnnotatedWith(Class<?> aClass, final Class... annotations) {
        return methodsAnnotatedWith(aClass, new Predicate<Method>() {
            @Override
            public boolean apply(Method m) {
                for(Class aClass : annotations) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Annotation> a = (Class<? extends Annotation>) aClass;
                    if(!hasAnnotation(m, a)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(cacheName = "reflectionCache")
    public List<Method> methodsAnnotatedWith(Class<?> aClass, Predicate<Method> predicate) {
        return newArrayList(Iterables.filter(copyOf(ReflectionUtils.getAllDeclaredMethods(aClass)), predicate));
    }


    /**
     * A helper method for getting an id from a persist object with annotated @Id
     *
     * The reason is static and takes an instance of self for caching reasons. @Cacheable does not work when calling
     * <code>this.someCachedMethod()</code> so I am passing the object instead which caches everything.
     *
     * @param utils an instance of this class or sub-class
     * @param o     the object
     * @return the id
     */
    public static Object getIdValue(PersistenceAnnotationInspector utils, Object o) {
        Preconditions.checkNotNull(o, "Cannot get id on a null object");
        if(utils.hasTableAnnotation(o.getClass())) {
            Method getter = utils.idGetter(o.getClass());
            if(getter != null) {
                try {
                    return getter.invoke(o);
                }
                catch(InvocationTargetException e) {
                    throw Throwables.propagate(e);
                }
                catch(IllegalAccessException e) {
                    throw Throwables.propagate(e);
                }
            }
        }

        return null;
    }

    /**
     * Sets the value by find a getter with @Id and the setter that goes with that field. If a setter doesn't exist
     * then it falls back looking for the field
     *
     * The reason is static and takes an instance of self for caching reasons. @Cacheable does not work when calling
     * <code>this.someCachedMethod()</code> so I am passing the object instead which caches everything.
     *
     * @param utils  an instance of this class or sub-class
     * @param entity the object
     * @param id     the new value
     */
    public static void setIdValue(PersistenceAnnotationInspector utils, Object entity, Object id) {
        Preconditions.checkNotNull(entity, "Cannot update id on a null object");
        Method getter = utils.idGetter(entity.getClass());
        Method setter = utils.setterFromGetter(getter);
        Field field = utils.fieldFromGetter(getter);

        try {
            if(setter != null) {
                ReflectionUtils.makeAccessible(setter);
                setter.invoke(entity, id);
            }
            else if(field != null) {
                ReflectionUtils.makeAccessible(field);
                field.set(entity, id);
            }
        }
        catch(InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
        catch(IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
