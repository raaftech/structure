package schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class InstancesPutNode extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/instancesPutNodeGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/instancesPutNodeBad01.json" // testing with bad UUID
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/repository/instances/putNode.schema.json";
    }
}
