package schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class ManualTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/manualGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/manualBad01.json" // testing with bad string pattern
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/base/manual.schema.json";
    }
}
