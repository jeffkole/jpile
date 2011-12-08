package com.opower.persistence.jpile.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.persistence.SecondaryTable;
import com.google.common.base.Predicate;


/**
 * A helper class for working annotations. This class is mostly for hibernate annotations. It provides simple operations
 * such as getting an annotations and more complex logic. For example, finding the @Id for a class.
 *
 * @author amir.raminfar
 * @since 1.0
 */
public interface PersistenceAnnotationInspector {

    /**
     * Finds the annotation on a class or subclasses
     *
     * @param clazz          the class to look
     * @param annotationType the annotation type
     * @param <A>            the annotation
     * @return return the annotation or null if it doesn't exist
     */
    <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType);


    /**
     * Finds the annotation on a method or parent's methods
     *
     * @param method         the method to look
     * @param annotationType the annotation class
     * @param <A>            the annotation type
     * @return the annotation on the method or null if it doesn't exist
     */
    <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType);


    /**
     * Checks to see if an annotation exists on a method
     *
     * @param method         the method
     * @param annotationType the annotation to look for
     * @return true if it exists
     */
    boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType);


    /**
     * Checks to see if an annotation exists on a class
     *
     * @param clazz          the class to look for
     * @param annotationType the annotation class
     * @return true if it exists
     */
    boolean hasAnnotation(Class<?> clazz, Class<? extends Annotation> annotationType);


    /**
     * Gets the table name for the {@link javax.persistence.Table &#064;Table} annotation on a class
     *
     * @param aClass the class to look
     * @return the table name, null if it doesn't exist
     */
    String tableName(Class<?> aClass);

    /**
     * Checks to see if {@link javax.persistence.Table &#064;Table} exist
     *
     * @param aClass the class to search
     * @return true if @Table exists on class
     */
    boolean hasTableAnnotation(Class<?> aClass);


    /**
     * Look for {@link javax.persistence.SecondaryTable &#064;SecondaryTable} annotations and return the name
     *
     * @param aClass the class to look for
     * @return the table name or null if it doesn't exist
     */
    String secondaryTable(Class<?> aClass);


    /**
     * Looks for {@link javax.persistence.Id &#064;Id} on all methods and returns the getter
     *
     * @param aClass the class to look for
     * @return the getter method
     */
    Method idGetter(Class<?> aClass);


    /**
     * Finds the setter for a getter by replacing 'get' with 'set'
     *
     * @param getter the getter
     * @return the setter if it exists, otherwise null
     */
    Method setterFromGetter(Method getter);

    /**
     * Finds the getter for a setter by replacing 'set' with 'get'
     *
     * @param setter the setter
     * @return the getter if it exists, otherwise null
     */
    Method getterFromSetter(Method setter);


    /**
     * Searches for field that matches the getter
     *
     * @param getter the getter
     * @return the field or null
     */
    Field fieldFromGetter(Method getter);

    /**
     * Looks for all methods with an annotation and returns the annotation with the method
     *
     * @param aClass     the class
     * @param annotation the annotation class
     * @param <A>        the annotation type
     * @return list of annotations and methods together
     */
    <A extends Annotation> List<AnnotatedMethod<A>> annotatedMethodsWith(Class<?> aClass, Class<A> annotation);

    /**
     * Returns all methods that are annotated with multiple annotations
     *
     * @param aClass      the class to search
     * @param annotations all annotations
     * @return the list of methods
     */
    List<Method> methodsAnnotatedWith(Class<?> aClass, Class... annotations);

    /**
     * Returns all methods filtered by a predicate
     *
     * @param aClass    the class to search
     * @param predicate using this predicate to filter
     * @return the list of methods
     */
    List<Method> methodsAnnotatedWith(Class<?> aClass, Predicate<Method> predicate);

    /**
     * Parses and returns all SecondaryTables on a class. This includes {@link javax.persistence.SecondaryTable &#064;
     * SecondaryTable}
     * and {@link javax.persistence.SecondaryTables &#064;SecondaryTables}. If no annotations are found then
     * an empty list is returned.
     *
     * @param aClass the class to search
     * @return collection of secondary table annotations, empty if none found
     */
    List<SecondaryTable> findSecondaryTables(Class<?> aClass);


    /**
     * For paring annotations and methods
     *
     * @param <E> the annotation type
     */
    class AnnotatedMethod<E extends Annotation> {
        private final Method method;
        private final E annotation;

        public AnnotatedMethod(Method method, E annotation) {
            this.method = method;
            this.annotation = annotation;
        }

        public Method getMethod() {
            return method;
        }

        public E getAnnotation() {
            return annotation;
        }
    }
}

