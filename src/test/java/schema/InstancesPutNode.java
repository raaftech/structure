package schema;

import com.google.common.collect.ImmutableSet;

import javax.management.ImmutableDescriptor;
import java.util.Set;

/**
 * Created by bardofrings on 08/05/17.
 */
public class InstancesPutNode extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of("schema/instancesPutNodeGood01.json");
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of("schema/instancesPutNodeBad01.json");
    }

    @Override
    protected String schema() {
        return "resource:/schema/repository/instances/putNode.schema.json";
    }
}
