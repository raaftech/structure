package schema;

import java.util.Collections;
import java.util.Set;

/**
 * Empty JSON schema test class for reference.
 */
public class EmptyTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return Collections.emptySet();
    }

    @Override
    protected Set<String> bad() {
        return Collections.emptySet();
    }

    @Override
    protected String schema() {
        return "resource:/foo.schema.json";
    }
}
