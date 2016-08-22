package com.raaftech.structure.helper.loader;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by boy on 8/19/16.
 */
public class BundleLoaderTest {

    final String testPath = "/home/boy/checkout/raaf/structure/bundles/raaf/bundle.json";
    @Test
    public void foo() throws IOException {
//        new BundleLoader(Paths.get(testPath)).load();
        new InstrumentLoader().load(Paths.get("bundles/raaf/instruments/bash/bashscriptable.hbs"));
    }

}