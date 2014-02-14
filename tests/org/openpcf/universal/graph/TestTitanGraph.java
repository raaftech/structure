/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.graph;

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import org.junit.Test;

/**
 * A quick test with Titan as graph backend, using TinkerPop Blueprints.
 * Testing persistence behaviour.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 * @version 0.6
 */
public class TestTitanGraph {
    @Test
    public void quickTest() {

        TitanGraph g = UniversalGraphFactory.create("target/test-titandb");

        for (Vertex vertex : g.getVertices()) {
            System.out.println(vertex.getProperty("name"));
        }
    }
}
