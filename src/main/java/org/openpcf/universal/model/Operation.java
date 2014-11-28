package org.openpcf.universal.model;

import org.openpcf.universal.graph.Node;

import java.util.Date;
import java.util.Set;

/**
 * Give a description of Operation here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Operation extends Node {

    private int timesInvoked;
    private String data;
    private Date dateCreated;
    private Date dateStarted;
    private Date dateFinished;

    public Operation() {}

    public Operation(String name, String desc, String uuid, int timesInvoked,
                     String data, Date dateCreated, Date dateStarted,
                     Date dateFinished, Set<Node> isa) {
        super(name, desc, uuid, isa);
        this.timesInvoked = timesInvoked;
        this.data = data;
        this.dateCreated = dateCreated;
        this.dateStarted = dateStarted;
        this.dateFinished = dateFinished;
    }

    public Operation(String name, String desc, int timesInvoked, String data,
                     Date dateCreated, Date dateStarted,
                     Date dateFinished, Set<Node> isa) {
        super(name, desc, isa);
        this.timesInvoked = timesInvoked;
        this.data = data;
        this.dateCreated = dateCreated;
        this.dateStarted = dateStarted;
        this.dateFinished = dateFinished;
    }
}
