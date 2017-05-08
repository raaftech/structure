package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by bardofrings on 08/05/2017.
 */
public class ReadTest extends SchemaTest{
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/readGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/readBad01.json"
        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/manager/entitymanager/read.schema.json";
    }
}
