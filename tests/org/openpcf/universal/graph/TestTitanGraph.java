/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.graph;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Test;


/**
 * A quick test with Titan as graph backend, using TinkerPop Blueprints,
 * all through gremlin as a JSR223 scripting handler.
 * Testing persistence behaviour.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class TestTitanGraph {

    @Test
    public void quickTest() {

        try{
            UniversalGraphFactory.load();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }

        TitanGraph g = UniversalGraphFactory.create("target/test-titandb");

        for (Vertex v: g.getVertices("name", "genericUnixLikeOperatingSystem")){
            System.out.println(v);
        }
    }
}
