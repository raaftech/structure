package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class DeleteTest extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/deleteGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/deleteBad01.json" // testing with bad UUID
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/manager/entitymanager/delete.schema.json";
    }
}
