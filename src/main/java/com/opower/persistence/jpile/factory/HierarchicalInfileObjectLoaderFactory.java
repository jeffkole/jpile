package com.opower.persistence.jpile.factory;

import com.opower.persistence.jpile.loader.HierarchicalInfileObjectLoader;

/**
 * <p>
 * Used to create a new instance of {@code HierarchicalInfileObjectLoader}
 * </p>
 *
 * <p>
 * Meant to be used with {@code ServiceLocatorFactoryBean}
 * </p>
 *
 * <pre>
 * &lt;!-- will lookup the above 'myService' bean by *TYPE* -->
 * &lt;bean id="myServiceFactory" class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean">
 *     &lt;property name="serviceLocatorInterface" value="poscore.db.infile.HierarchicalInfileObjectLoaderFactory"/>
 * &lt;/bean>
 * </pre>
 *
 * @author amir.raminfar
 * @see org.springframework.beans.factory.config.ServiceLocatorFactoryBean
 * @since 1.0
 */
public interface HierarchicalInfileObjectLoaderFactory {
    /**
     * Returns a new instance every time
     *
     * @return the instance
     */
    HierarchicalInfileObjectLoader getHierarchicalInfileObjectLoader();
}
