package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by bardofrings on 07/05/2017.
 */
public class DeleteTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of("schema/deleteGood01.json");
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of("schema/deleteBad01.json");
    }

    @Override
    protected String schema() {
        return "resource:/schema/manager/entitymanager/delete.schema.json";
    }
}
