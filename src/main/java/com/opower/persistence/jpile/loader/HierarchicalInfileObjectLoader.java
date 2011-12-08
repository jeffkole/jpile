package com.opower.persistence.jpile.loader;

import java.io.Closeable;
import java.io.Flushable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.opower.persistence.jpile.config.JPileApplicationConfig;
import com.opower.persistence.jpile.infile.InfileDataBuffer;
import com.opower.persistence.jpile.reflection.CacheablePersistenceAnnotationInspector;
import com.opower.persistence.jpile.reflection.PersistenceAnnotationInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static com.google.common.base.Throwables.*;
import static com.google.common.collect.ImmutableList.*;
import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Maps.*;
import static com.google.common.collect.Sets.*;

/**
 * Save any type of data using a collection of SingleInfileObjectLoaders
 *
 * @author amir.raminfar
 * @since 1.0
 */
public class HierarchicalInfileObjectLoader implements Flushable, Closeable {
    private static ApplicationContext applicationContext;
    private static Logger logger = LoggerFactory.getLogger(HierarchicalInfileObjectLoader.class);

    static {
        applicationContext = new AnnotationConfigApplicationContext(JPileApplicationConfig.class);
    }


    /**
     * This is bad practice because it is breaking dependency injection guidelines. I don't have a better solution as of now
     * but I will revisit to see if I can improve this without needing an application context.
     */
    private PersistenceAnnotationInspector persistenceAnnotationInspector =
            applicationContext.getBean(PersistenceAnnotationInspector.class);

    private CallBack eventCallback = new NoOpCallBack();
    private JdbcTemplate jdbcTemplate = null;

    // linked for consistent error message
    private Map<Class<?>, SingleInfileObjectLoader<Object>> primaryObjectLoaders = newLinkedHashMap();
    private Map<Class<?>, SingleInfileObjectLoader<Object>> secondaryTableObjectLoaders = newLinkedHashMap();
    private Map<Class<?>, Set<Method>> parentDependent = newHashMap();
    private Map<Class<?>, Set<Method>> childDependent = newHashMap();
    private Set<Class> classesToIgnore = ImmutableSet.of();
    private Set<String> secondaryClassesToIgnore = ImmutableSet.of();


    public HierarchicalInfileObjectLoader() {
        this.persistenceAnnotationInspector = applicationContext.getBean(PersistenceAnnotationInspector.class);
    }

    /**
     * Disables fk (if not already disabled) and saves each object
     *
     * @param firstObject the first object to save
     * @param moreObjects optional more objects
     */
    public void persist(Object firstObject, Object... moreObjects) {
        Preconditions.checkNotNull("Connection is null, did you call setConnection()?", jdbcTemplate);
        for(Object o : concat(of(firstObject), copyOf(moreObjects))) {
            persistWithCyclicCheck(o, new HashSet<Object>());
        }
    }

    private void persistWithCyclicCheck(Object entity, Set<Object> cyclicCheck) {
        Preconditions.checkNotNull(entity, "Cannot persist null");

        // If we already saved this object then ignore
        if(cyclicCheck.contains(entity)) {
            logger.debug("Skipping in file persist on [{}] because it has already been saved.", entity);
            return;
        }

        // If we are supposed to ignore this class then also ignore
        if(classesToIgnore.contains(entity.getClass())) {
            logger.debug("Ignoring [{}].", entity);
            return;
        }

        logger.debug("Persisting [{}].", entity);

        // Initialize for this class
        initForClass(entity.getClass());

        // Add to a set so we don't save this object again
        cyclicCheck.add(entity);

        // Save dependent children first because there is a key that depends on these items
        for(Method dependent : childDependent.get(entity.getClass())) {
            Object o = invoke(dependent, entity);
            if(o != null) {
                persistWithCyclicCheck(o, cyclicCheck);
            }
        }

        // Save this entity now that we know all children have been saved
        callOnBeforeEvent(entity);
        primaryObjectLoaders.get(entity.getClass()).add(entity);
        callOnAfterEvent(entity);

        // Get generated id
        Object id = CacheablePersistenceAnnotationInspector.getIdValue(persistenceAnnotationInspector, entity);

        // Find all objects that depend entity's id being generated and save these now
        for(Method dependent : parentDependent.get(entity.getClass())) {
            Object o = invoke(dependent, entity);
            if(o != null) {
                if(o instanceof Collection) {
                    for(Object item : (Collection) o) {
                        persistWithCyclicCheck(item, cyclicCheck);
                    }
                }
                else {
                    CacheablePersistenceAnnotationInspector.setIdValue(persistenceAnnotationInspector, o, id);
                    persistWithCyclicCheck(o, cyclicCheck);
                }
            }
        }

        // Check to see if there is a secondary
        if(secondaryTableObjectLoaders.containsKey(entity.getClass())) {
            secondaryTableObjectLoaders.get(entity.getClass()).add(entity);
        }
    }

    private void callOnBeforeEvent(Object entity) {
        eventCallback.onBeforeSave(entity);
    }

    private void callOnAfterEvent(Object entity) {
        eventCallback.onAfterSave(entity);
    }

    private void initForClass(Class<?> aClass) {
        findParentDependents(aClass);
        findChildDependents(aClass);
        createObjectLoader(aClass);
    }

    private void createObjectLoader(Class<?> aClass) {
        if(primaryObjectLoaders.containsKey(aClass)) {
            return;
        }
        @SuppressWarnings("unchecked")
        SingleInfileObjectLoader<Object> primaryLoader = new SingleInfileObjectLoaderBuilder<Object>((Class<Object>) aClass)
                .withBuffer(newInfileDataBuffer())
                .withDefaultTableName()
                .withJdbcTemplate(jdbcTemplate)
                .usingHibernateBeanUtils(persistenceAnnotationInspector)
                .build();

        primaryObjectLoaders.put(aClass, primaryLoader);

        for(SecondaryTable secondaryTable : persistenceAnnotationInspector.findSecondaryTables(aClass)) {
            if(!secondaryClassesToIgnore.contains(secondaryTable.name())) {
                @SuppressWarnings("unchecked")
                SingleInfileObjectLoader<Object> secondaryLoader
                        = new SingleInfileObjectLoaderBuilder<Object>((Class<Object>) aClass)
                        .withBuffer(newInfileDataBuffer())
                        .withDefaultTableName()
                        .usingSecondaryTable(secondaryTable)
                        .withJdbcTemplate(jdbcTemplate)
                        .usingHibernateBeanUtils(persistenceAnnotationInspector)
                        .build();

                secondaryTableObjectLoaders.put(aClass, secondaryLoader);
            }
        }
    }


    private void findParentDependents(Class<?> aClass) {
        if(parentDependent.containsKey(aClass)) {
            return;
        }
        Set<Method> methods = newHashSet(persistenceAnnotationInspector.methodsAnnotatedWith(aClass, OneToMany.class));
        methods.addAll(persistenceAnnotationInspector.methodsAnnotatedWith(aClass, OneToOne.class,
                                                                           PrimaryKeyJoinColumn.class));
        parentDependent.put(aClass, methods.size() > 0 ? methods : ImmutableSet.<Method>of());

        // Do all children again
        for(Method m : methods) {
            findParentDependents(getReturnType(m));
        }
    }

    private void findChildDependents(Class<?> aClass) {
        if(childDependent.containsKey(aClass)) {
            return;
        }

        Set<Method> methods = newHashSet(persistenceAnnotationInspector.methodsAnnotatedWith(aClass, ManyToOne.class));
        methods.addAll(persistenceAnnotationInspector.methodsAnnotatedWith(aClass, new Predicate<Method>() {
            @Override
            public boolean apply(Method m) {
                // Must have OneToOne but not PrimaryKeyJoinColumn annotations
                return persistenceAnnotationInspector.hasAnnotation(m, OneToOne.class)
                       && !persistenceAnnotationInspector.hasAnnotation(m, PrimaryKeyJoinColumn.class);
            }
        }));

        childDependent.put(aClass, methods.size() > 0 ? methods : ImmutableSet.<Method>of());

        // Do all children again
        for(Method m : methods) {
            findChildDependents(getReturnType(m));
        }
    }

    private Class getReturnType(Method m) {
        final Class returnType;
        if(m.getGenericReturnType() instanceof ParameterizedType) {
            // For List<String> etc...
            ParameterizedType type = (ParameterizedType) m.getGenericReturnType();
            returnType = (Class) type.getActualTypeArguments()[0];
        }
        else {
            returnType = (Class) m.getGenericReturnType();
        }
        return returnType;
    }

    private InfileDataBuffer newInfileDataBuffer() {
        return new InfileDataBuffer();
    }

    private Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        }
        catch(InvocationTargetException e) {
            throw propagate(e);
        }
        catch(IllegalAccessException e) {
            throw propagate(e);
        }
    }

    /**
     * Flushes all object loaders
     */
    @Override
    public void flush() {
        logger.debug("Flushing all object loaders.");
        for(SingleInfileObjectLoader<?> loader : primaryObjectLoaders.values()) {
            loader.flush();
        }
        for(SingleInfileObjectLoader<?> loader : secondaryTableObjectLoaders.values()) {
            loader.flush();
        }
    }

    /**
     * Closes all object loaders
     */
    @Override
    public void close() {
        flush();
        logger.debug("Closing all object loaders.");
        primaryObjectLoaders.clear();
        secondaryTableObjectLoaders.clear();
    }

    /**
     * Wraps the connection in a {@link SingleConnectionDataSource} because foreign keys need to be disabled. With
     * multiple connections foreign keys and not guaranteed to be always disabled.
     *
     * @param connection the connection use for the database
     */
    public void setConnection(Connection connection) {
        this.jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        this.jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
    }

    public void setClassesToIgnore(Set<Class> classToIgnore) {
        this.classesToIgnore = classToIgnore;
    }

    public void setSecondaryClassesToIgnore(Set<String> secondaryClassesToIgnore) {
        this.secondaryClassesToIgnore = secondaryClassesToIgnore;
    }

    public void setEventCallback(CallBack eventCallback) {
        this.eventCallback = eventCallback;
    }

    void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<Class<?>, Set<Method>> getChildDependent() {
        return childDependent;
    }

    Map<Class<?>, Set<Method>> getParentDependent() {
        return parentDependent;
    }

    Map<Class<?>, SingleInfileObjectLoader<Object>> getPrimaryObjectLoaders() {
        return primaryObjectLoaders;
    }

    Map<Class<?>, SingleInfileObjectLoader<Object>> getSecondaryTableObjectLoaders() {
        return secondaryTableObjectLoaders;
    }

    public interface CallBack {
        void onBeforeSave(Object o);

        void onAfterSave(Object o);
    }
}
