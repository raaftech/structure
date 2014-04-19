package org.openpcf.universal.model;

import java.util.Set;

/**
 * A building object. Usually has floors, rooms.
 * A less-modeled environment might directly have
 * enclosures or devices here.
 * You can use this to model a data center.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.1
 * @since 2014-04-19
 */
public class Building extends Place {
    private Set<Room> rooms;
    private Set<Floor> floors;
}
