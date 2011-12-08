package com.opower.persistence.jpile.loader;

import java.lang.reflect.Method;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.opower.persistence.jpile.infile.InfileDataBuffer;
import com.opower.persistence.jpile.reflection.PersistenceAnnotationInspector;
import org.springframework.jdbc.core.JdbcTemplate;

import static java.lang.String.*;


/**
 * The builder for creating a SingleInfileObjectLoader. This class does the building and parsing of the annotations.
 *
 * @param <E> the type of object which this class will support
 * @author amir.raminfar
 * @see SingleInfileObjectLoader
 */
public class SingleInfileObjectLoaderBuilder<E> {
    private Class<E> aClass;
    private JdbcTemplate jdbcTemplate;
    private InfileDataBuffer infileDataBuffer;
    private PersistenceAnnotationInspector persistenceAnnotationInspector;
    private String tableName;
    private boolean defaultTableName = false;
    private boolean allowNull = false;
    private boolean embedded = false;
    private SecondaryTable secondaryTable;


    public SingleInfileObjectLoaderBuilder(Class<E> aClass) {
        Preconditions.checkNotNull(aClass, "Class cannot be null");
        this.aClass = aClass;
    }

    public SingleInfileObjectLoaderBuilder<E> withBuffer(InfileDataBuffer infileDataBuffer) {
        this.infileDataBuffer = infileDataBuffer;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> withJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> usingHibernateBeanUtils(
            PersistenceAnnotationInspector persistenceAnnotationInspector) {
        this.persistenceAnnotationInspector = persistenceAnnotationInspector;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> withTableName(String tableName) {
        this.defaultTableName = false;
        this.tableName = tableName;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> withDefaultTableName() {
        this.defaultTableName = true;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> allowNull() {
        this.allowNull = true;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> doNotAllowNull() {
        this.allowNull = false;
        return this;
    }

    public SingleInfileObjectLoaderBuilder<E> usingSecondaryTable(SecondaryTable secondaryTable) {
        this.secondaryTable = secondaryTable;
        return this;
    }


    private SingleInfileObjectLoaderBuilder<E> isEmbedded() {
        this.embedded = true;
        return this;
    }

    /**
     * Builds the object loader by looking at all the annotations and returns a new object loader.
     *
     * @return a new instance of object loader
     */
    public SingleInfileObjectLoader<E> build() {
        Preconditions.checkNotNull(jdbcTemplate, "jdbcTemplate cannot be null");
        Preconditions.checkNotNull(persistenceAnnotationInspector, "persistenceAnnotationInspector cannot be null");
        Preconditions.checkNotNull(infileDataBuffer, "infileDataBuffer cannot be null");

        SingleInfileObjectLoader<E> objectLoader = new SingleInfileObjectLoader<E>(aClass);
        objectLoader.jdbcTemplate = jdbcTemplate;
        objectLoader.infileDataBuffer = infileDataBuffer;
        objectLoader.persistenceAnnotationInspector = persistenceAnnotationInspector;
        objectLoader.allowNull = allowNull;
        objectLoader.embedChild = embedded;
        if(defaultTableName) {
            this.tableName = secondaryTable != null
                    ? secondaryTable.name()
                    : persistenceAnnotationInspector.tableName(aClass);
        }
        Preconditions.checkNotNull(tableName, "tableName cannot be null");
        this.findAnnotations(objectLoader);
        if(!embedded) {
            this.findPrimaryId(objectLoader);
            this.generateLoadInfileSql(objectLoader);
        }

        return objectLoader;
    }


    private void findAnnotations(SingleInfileObjectLoader<E> objectLoader) {
        // Finds all columns that are annotated with @Column
        for(PersistenceAnnotationInspector.AnnotatedMethod<Column> annotatedMethod
                : persistenceAnnotationInspector.annotatedMethodsWith(aClass, Column.class)) {

            Preconditions.checkState(!annotatedMethod.getAnnotation().name().isEmpty(),
                                     "@Column.name is not found on method [%s]",
                                     annotatedMethod.getMethod());
            Column column = annotatedMethod.getAnnotation();
            if(secondaryTable != null) {
                if(column.table().equals(this.tableName)) {
                    objectLoader.mappings.put(annotatedMethod.getAnnotation().name(), annotatedMethod.getMethod());
                }
            }
            else if(column.table().isEmpty() || column.table().equals(this.tableName)) {
                objectLoader.mappings.put(annotatedMethod.getAnnotation().name(), annotatedMethod.getMethod());
            }
        }


        // Ignore all these when using secondary table
        if(secondaryTable == null) {
            // Finds all one to one columns with @OneToOne
            // Finds all columns with @ManyToOne
            // If @JoinColumn is not there then there is nothing to write
            for(PersistenceAnnotationInspector.AnnotatedMethod<JoinColumn> annotatedMethod
                    : persistenceAnnotationInspector.annotatedMethodsWith(aClass, JoinColumn.class)) {
                if(persistenceAnnotationInspector.hasAnnotation(annotatedMethod.getMethod(), ManyToOne.class)
                   || persistenceAnnotationInspector.hasAnnotation(annotatedMethod.getMethod(), OneToOne.class)) {
                    objectLoader.mappings.put(annotatedMethod.getAnnotation().name(), annotatedMethod.getMethod());
                }
            }
            // Finds all columns with @Embedded
            for(PersistenceAnnotationInspector.AnnotatedMethod<Embedded> annotatedMethod
                    : persistenceAnnotationInspector.annotatedMethodsWith(aClass, Embedded.class)) {
                Method method = annotatedMethod.getMethod();
                @SuppressWarnings("unchecked")
                SingleInfileObjectLoader<Object> embededObjectLoader
                        = new SingleInfileObjectLoaderBuilder<Object>((Class<Object>) method.getReturnType())
                        .withBuffer(infileDataBuffer)
                        .withDefaultTableName()
                        .withJdbcTemplate(jdbcTemplate)
                        .withTableName(tableName)
                        .usingHibernateBeanUtils(persistenceAnnotationInspector)
                        .allowNull()
                        .isEmbedded()
                        .build();
                objectLoader.embeds.put(method, embededObjectLoader);
            }
        }
    }

    private void findPrimaryId(SingleInfileObjectLoader<E> objectLoader) {
        Method primaryIdGetter = persistenceAnnotationInspector.idGetter(aClass);
        Preconditions.checkNotNull(format("Primary id with @Id annotation is not found on [%s]", aClass), primaryIdGetter);
        Column column = persistenceAnnotationInspector.findAnnotation(primaryIdGetter, Column.class);
        String name = persistenceAnnotationInspector.fieldFromGetter(primaryIdGetter).getName();
        if(secondaryTable != null) {
            PrimaryKeyJoinColumn[] primaryKeyJoinColumns = secondaryTable.pkJoinColumns();
            Preconditions.checkState(primaryKeyJoinColumns.length == 1, "There needs to be one pkJoinColumns");
            name = primaryKeyJoinColumns[0].name();
        }
        else if(column != null && !column.name().isEmpty()) {
            name = column.name();
        }
        objectLoader.mappings.put(name, primaryIdGetter);
        GeneratedValue generatedValue = persistenceAnnotationInspector.findAnnotation(primaryIdGetter, GeneratedValue.class);
        objectLoader.autoGenerateId = secondaryTable == null
                                      && generatedValue != null
                                      && generatedValue.strategy() == GenerationType.AUTO;
    }

    private void generateLoadInfileSql(SingleInfileObjectLoader<E> objectLoader) {
        objectLoader.loadInfileSql = "LOAD DATA LOCAL INFILE 'stream' INTO TABLE "
                                     + tableName
                                     + " ("
                                     + Joiner.on(", ").join(objectLoader.getAllColumns())
                                     + ")";
    }

}
