package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class EntityTest extends SchemaTest {


    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/entityGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/entityBad01.json", // testing with bad UUID
                "schema/entityBad02.json" // testing with bad UUID array
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/entity.schema.json";
    }
}
