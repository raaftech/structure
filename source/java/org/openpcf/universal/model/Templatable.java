package org.openpcf.universal.model;

/**
 * Persistable represents all objects that can 'persist' themselves
 * in a database layer, in our case Graphs, initially focused on
 * Titan but might become more generic in the future.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-03-01
 * @version 0.6
 */
public interface Templatable {
    public void template();
}
