package schema;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by bardofrings on 08/05/17.
 */
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
                "schema/templatePutNodeBad01.json",
                "schema/templatePutNodeBad02.json"

        );
    }

    @Override
    protected String schema() {
        return "resource:/schema/repository/templates/putNode.schema.json";
    }
}
