package org.openpcf.universal.model;

/**
 * Persistable represents all objects that can 'persist' themselves
 * in a database layer, in our case Graphs, initially focused on
 * Titan but might become more generic in the future.
 *
 * @author Freek Alleman <freek.alleman@raaftech.com>
 * @since 2014-02-28
 * @version 0.6
 */
public interface Persistable {
    public void init();
    public void load();
    public void save();
}
