package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class QueueTaskTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/queuetaskGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/queuetaskBad01.json" // testing with bad UUID
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/manager/taskmanager/queuetask.schema.json";
    }
}
