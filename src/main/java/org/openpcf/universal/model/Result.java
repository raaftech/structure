package org.openpcf.universal.model;

import java.util.Date;
import java.util.Set;

/**
 * Give a description of Result here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public class Result extends Node {

    private String data;
    private Date dateCreated;

    public Result() {}

    public Result(String name, String desc, String uuid, String data, Date dateCreated, Set<Node> isa) {
        super(name, desc, uuid, isa);
        this.data = data;
        this.dateCreated = dateCreated;
    }

    public Result(String name, String desc, String data, Date dateCreated, Set<Node> isa) {
        super(name, desc, isa);
        this.data = data;
        this.dateCreated = dateCreated;
    }
}
