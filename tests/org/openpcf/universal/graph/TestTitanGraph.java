/*
 * Copyright © 2008-2014 RAAF Technology bv
 */

package org.openpcf.universal.graph;

import com.tinkerpop.gremlin.groovy.jsr223.GremlinGroovyScriptEngineFactory;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.FileReader;
import java.io.IOException;

/**
 * A quick test with Titan as graph backend, using TinkerPop Blueprints,
 * all through gremlin as a JSR223 scripting handler.
 * Testing persistence behaviour.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-01-06
 * @version 0.6
 */
public class TestTitanGraph {
    @Test
    public void quickTest() throws IOException, InterruptedException, ScriptException {

        ScriptEngine engine = new GremlinGroovyScriptEngineFactory().getScriptEngine();

        engine.eval(new FileReader("scripts/dbinit.grm"));
    }
}
