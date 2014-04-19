package org.openpcf.universal.model;


import java.util.Set;

/**
 * A place resembles a type of location with a collection of
 * devices there. Some places exist within other places!
 *
 * @author Your name <your.name@raaftech.com>
 * @version 0.1
 * @since 2014-04-19
 */
abstract class Place extends Component {
    private Set<AssembledDevice> devices;
}
