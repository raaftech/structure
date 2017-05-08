package schema;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class TemplatesPutNode extends SchemaTest {
    @Override
    protected Set<String> good() {
        return ImmutableSet.of(
                "schema/templatePutNodeGood01.json"
        );
    }

    @Override
    protected Set<String> bad() {
        return ImmutableSet.of(
                "schema/templatePutNodeBad01.json", // testing with bad UUID
                "schema/templatePutNodeBad02.json" // testing with bad UUID

        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/repository/templates/putNode.schema.json";
    }
}
