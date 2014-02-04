/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

import com.thinkaurelius.titan.core.*;
import com.tinkerpop.blueprints.*;
import org.junit.Test;

/**
 * A quick test with Titan as graph backend, unsing TinkerPop Blueprints.
 * Testing persistence behaviour.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @version 0.6
 * @since 2014-01-06
 */
public class TestTitanGraph {
    @Test
    public void quickTest() {

        TitanGraph g = TitanFactory.open("C:/Data/Services/servers/titan04/data/gods");

        //Vertex a = g.addVertex(null);
        //Vertex b = g.addVertex(null);
        //a.setProperty("name","marko");
        //b.setProperty("name","peter");
        //Edge e = g.addEdge(null, a, b, "knows");
        //e.setProperty("since", 2006);
        //g.shutdown();

        for (Vertex vertex : g.getVertices()) {
            System.out.println(vertex.getProperty("name"));
        }
    }
}
