/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.initialization;


import org.openpcf.universal.model.DataPool;

/**
 * A preliminary Universal Pool Singleton.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2014-02-25
 * @version 0.6
 */
public enum UniversalPoolSingleton {
    INSTANCE;

    private DataPool dataPool = new DataPool();

    public DataPool getDataObject() {
        return dataPool;
    }
}
