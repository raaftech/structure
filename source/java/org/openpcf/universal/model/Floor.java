package org.openpcf.universal.model;

import java.util.Set;

/**
 * A floor within a building.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.1
 * @since 2014-04-19
 */
public class Floor extends Place {
    private Set<Room> rooms;
}
