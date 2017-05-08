package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by bardofrings on 08/05/2017.
 */
public class CancelJobTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of("schema/canceljobGood01.json");
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of("schema/canceljobBad01.json");
    }

    @Override
    protected String schema() {
        return "resource:/schema/manager/taskmanager/canceljob.schema.json";
    }
}
