/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.base;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.locks.ReentrantLock;

/**
 * SpringContext singleton/wrapper.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-12
 */
public enum SpringContext {
    INSTANCE;

    private final ApplicationContext context;
    private static final ReentrantLock initLock = new ReentrantLock();

    private SpringContext() {
        System.out.println("CREATING SINGLETON");
        context = new AnnotationConfigApplicationContext(SpringConfig.class);
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }
}