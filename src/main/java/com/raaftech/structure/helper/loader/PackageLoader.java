package com.raaftech.structure.helper.loader;

import java.nio.file.Path;

/**
 * A S(ingle) A(bstract) M(ethod) aka Functional interface for package loaders.
 */

@FunctionalInterface
public interface PackageLoader {
    /**
     * Load a package defined in a bundle.
     * @param filePath The location of the package on filesystem.
     */
    public void load(Path filePath);
}
