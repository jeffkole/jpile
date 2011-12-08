package com.opower.persistence.jpile.reflection;

import java.lang.reflect.Method;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import com.opower.persistence.jpile.sample.Contact;
import com.opower.persistence.jpile.sample.Customer;
import com.opower.persistence.jpile.sample.Product;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.*;
import static org.junit.Assert.*;

/**
 * @author amir.raminfar
 */
public class CacheablePersistenceAnnotationInspectorTest {
    private PersistenceAnnotationInspector persistenceAnnotationInspector = new CacheablePersistenceAnnotationInspector();

    @Test
    public void testHasTableAnnotation() throws Exception {
        assertTrue(persistenceAnnotationInspector.hasTableAnnotation(Customer.class));
        assertTrue(persistenceAnnotationInspector.hasTableAnnotation(Product.class));
    }

    @Test
    public void testTableName() throws Exception {
        assertEquals("customer", persistenceAnnotationInspector.tableName(Customer.class));
        assertEquals("product", persistenceAnnotationInspector.tableName(Product.class));
    }

    /*@Test
    public void testSecondaryTable() throws Exception {
        assertEquals("site_location", persistenceAnnotationInspector.secondaryTable(Site.class));
    }*/

    @Test
    public void testIdGetter() throws Exception {
        assertEquals("getId", persistenceAnnotationInspector.idGetter(Customer.class).getName());
    }

    @Test
    public void testSetterFromGetter() throws Exception {
        assertEquals("setId", persistenceAnnotationInspector.setterFromGetter(
                persistenceAnnotationInspector.idGetter(Customer.class)).getName()
        );
    }

    @Test
    public void testFieldFromGetter() throws Exception {
        assertEquals("id", persistenceAnnotationInspector.fieldFromGetter(
                persistenceAnnotationInspector.idGetter(Customer.class)).getName()
        );
    }

    @Test
    public void testMethodsAnnotatedWith() {
        List<PersistenceAnnotationInspector.AnnotatedMethod<Column>> methods =
                persistenceAnnotationInspector.annotatedMethodsWith(Customer.class, Column.class);
        for(PersistenceAnnotationInspector.AnnotatedMethod<Column> methodWithAnnotations : methods) {
            assertNotNull("Must have @Column", methodWithAnnotations.getMethod().getAnnotation(Column.class));
        }
    }

    @Test
    public void testMethodsWithMultipleAnnotations() {
        List<Method> methods = persistenceAnnotationInspector.methodsAnnotatedWith(
                Customer.class, OneToOne.class, PrimaryKeyJoinColumn.class
        );
        assertEquals(1, methods.size());

    }

    @Test
    public void testFindAnnotation() {
        assertNotNull(persistenceAnnotationInspector.findAnnotation(Customer.class, Table.class));
    }

    @Test
    public void testHasAnnotation() {
        assertTrue(persistenceAnnotationInspector.hasAnnotation(Customer.class, Entity.class));
    }

    @Test
    public void testFindSecondaryTableAnnotations() throws Exception {
        assertEquals(
                copyOf(Contact.class.getAnnotation(SecondaryTables.class).value()),
                persistenceAnnotationInspector.findSecondaryTables(Contact.class)
        );
    }
}
