# Migration: embedded Neo4j to a Neo4j server

## Goal

Structure stops using embedded Neo4j 2.1.8 and Spring Data Neo4j (SDN)
3.2.1. It connects to an external Neo4j 2026.10 server with the Neo4j
Java driver.

## Decisions

| Topic         | Decision                                                                                     |
|---------------|----------------------------------------------------------------------------------------------|
| Driver        | `org.neo4j.driver:neo4j-java-driver:4.4.27`. Java 8 bytecode. Netty is shaded (see below).   |
| Java, Vert.x  | No change: Java 8, Vert.x 3.4.1.                                                             |
| Connection    | Bolt on port 7687. Plain: `bolt://localhost:7687`. TLS: `bolt+s://` or `bolt+ssc://`.        |
| Spring        | Remove. Spring is in the build only for SDN.                                                 |
| Data model    | The Structure source is the authority. `doc/cypher` is historical. Do not use it.            |
| Stored format | Keep the format. See "Stored format" below for the full list and the two changes.            |
| SDN artifacts | Remove the `_` labels (`_Type`, `_Component`, ...) and the `__type__` relationship property. |
| Existing data | Do not migrate `target/structure.db`. Structure loads the bundles at startup.                |
| Out of scope  | The JS code in `rest/` and `cmd/`, `doc/cypher`, the Dockerfile, Java and Vert.x upgrades.   |
| Backend seam  | Only `Graph` uses the driver API. See "Embedded mode later".                                 |

The driver has its own shaded Netty. Thus it does not conflict with the
Netty 4.1.8 of Vert.x 3.4.1. Use `bolt+ssc://` for a self-signed
certificate. The class labels are `Node`, `Type`, `Instance`,
`Component`, `Group`, `Profile`, `Result`, `Instrument`, `Controller`,
`Marker` and `Parser`.

Ports 7474 (HTTP) and 7473 (HTTPS) serve Neo4j Browser and the HTTP
Query API. The driver does not use them. Neo4j Browser on the server
replaces the web console and the remote shell that unsafe mode starts
today.

## Stored format

Each node has these labels:

- The class labels, for example `Controller`, `Instrument`, `Instance`
  and `Node`.
- `type:<Name>` for each type in the `ISA` path of the node's type, from
  `type:Type` down. A type node gets the labels of its own path.

Each node has these properties:

| Property     | On                  | Value                                                             |
|--------------|---------------------|-------------------------------------------------------------------|
| `uuid`       | All nodes           | String.                                                           |
| `name`       | All nodes           | String.                                                           |
| `desc`       | All nodes           | String.                                                           |
| `bundle`     | All nodes           | Name of the bundle. `core` if the JSON has no bundle.             |
| `checksum`   | All nodes           | SHA-1 of the entity JSON.                                         |
| `path`       | Nodes from a bundle | Path of the bundle file. Run-time nodes have no `path`.           |
| `isAbstract` | Types               | Boolean.                                                          |
| `glyphId`    | Types               | Integer. See the changes below.                                   |
| `data-*`     | Nodes with data     | Primitive, or a JSON string for objects and arrays.               |
| `file-*`     | Nodes with files    | JSON string: `encoding`, `format`, `path`, `content`, `checksum`. |
| `kv-*`       | Types               | `Instance.save` reads it. No bundle writes it today.              |

`CONSTRAINTS` relationships have the properties `forRelationship` and
`cardinality`. Other relationships have no properties.

The migration makes two changes to this format:

- `created`: the new `persist` sets it on each new node. See "Writes".
- `glyphId`: today SDN writes it only on types that have constraints,
  because only `BundleLoader` calls `type.save()` for those. The value
  is always 0. The new `persist` writes it on each type.

`StoreFormatTest` checks this format. Change its expected values for
`created` and `glyphId` in the same change as the new `persist`.

## Server facts (checked in October 2026)

- Neo4j 2025.x and 2026.x accept Bolt 4.4. The Neo4j support table
  lists driver 4.4 as compatible up to 2026.10. Neo4j can remove Bolt
  4.4 in a later release. There is no date for this.
- New 2026.x installations set `db.query.default_language=CYPHER_25`.
  Existing installations keep their setting. All queries must be valid
  in Cypher 5 and in Cypher 25.
- `id()` is deprecated. Do not use it. The order of internal ids is not
  the creation order.
- Driver 4.4 API: `session.beginTransaction`, `session.readTransaction`,
  `session.writeTransaction`. Nodes have `id()` only. If you need a
  string id, return `elementId(n)` from Cypher.
- Neo4j does not accept schema changes and data writes in one
  transaction.

References:

- [Bolt protocol and Neo4j compatibility](https://neo4j.com/docs/bolt/current/bolt-compatibility/)
- [Neo4j supported versions](https://neo4j.com/developer/kb/neo4j-supported-versions/)
- [Cypher deprecations, additions and removals](https://neo4j.com/docs/cypher-manual/current/deprecations-additions-removals-compatibility/)

## Baseline

On the current code with JDK 1.8.0_504, `mvn verify` runs 119 tests
with 0 failures and 0 skipped. It also builds the fat jar and checks
the line coverage with JaCoCo. The minimum is `jacoco.line.minimum`
(0.60) in `pom.xml`. The coverage is 62% now. The migrated code must
give the same result. The report is in `target/site/jacoco`.

Maven runs the build and the tests with the JDK 8 from
`~/.m2/toolchains.xml`. Maven itself can run on a newer JDK.
`.mvn/jvm.config` lets it read `aspectjweaver-1.8.2.jar`, which newer
JDKs reject without this setting.

`TaskManagerTest.resultChain` can fail on Neo4j 2.1 when the machine is
busy. Neo4j 2.1 lets a reader see a commit that is not fully applied,
and the test reads the graph while `TaskManager` commits. On Neo4j 5
this test must pass each time.

## Inventory

| Area                   | Where                                                           | Count                  |
|------------------------|-----------------------------------------------------------------|------------------------|
| Embedded database      | `SpringConfig`, `SpringTestConfig`                              | 2 files                |
| Spring context         | `Context`, `ConverterConfig`, `Application`                     | 3 files                |
| Spring converters      | `graph/converters/*`, Spring `Converter` interface              | 8 files                |
| `beginTx` transactions | `EntityManager` (7), `Application` (4), `Type` (2), `Types` (2) | 20 in main             |
|                        | `Entity`, `Instance`, `Component`, `NodeActor`, `TaskManager`   |                        |
|                        | (1 each)                                                        |                        |
|                        | 10 test classes                                                 | 29 in tests            |
| SDN mapping            | 13 domain classes in `graph/`                                   | 26 relationship fields |
| `DynamicProperties`    | `Node`, `Entity`, `Type`, `Instance`                            | 4 files                |
| SDN repositories       | `graph/repositories/*`                                          | 9 files, 58 `@Query`   |
| Inherited SDN methods  | `save`, `delete`, `findAll`, `count`, generated finders         | See "Queries"          |
| Core API writes        | `Entity.persist`, `persistRelations`, `persistTypeLabels`       | 3 methods              |
| Core API reads         | `NodeSerializer`, `NodeRepository.findGraphNodeByUuid`          | 2 files                |
| Traversal API          | `EntityManager.labelBasedTraversal`, `safeDeleteTraversal`      | 2 methods              |
| `CypherQueryExecutor`  | `Application`, `BundleLoader` (2), `Entity.findDuplicates`      | 4 sites                |
| Verticle deploys       | `Application` (7), `NodeActor.deploy`, `Instance.save`          | 9 sites                |
| `commons-lang` 2.4     | `Type`, `Constraint`, `ConfigurationProxy`                      | 3 files                |
| Tests                  | 15 files import SDN or Neo4j. 4 of them use `SpringTestConfig`. | 15 files               |

`commons-lang` 2.4 (`org.apache.commons.lang.*`) arrives only through
`neo4j-server`. In total, 38 main files import Spring and 19 main files
import `org.neo4j`.

## Target design

### Graph access

Add one class, `tech.raaf.structure.base.Graph`. It holds the `Driver`
and binds a transaction to the current thread. Nested calls join the
bound transaction. Thus a read in a transaction sees the uncommitted
writes of that transaction, as with embedded Neo4j today. The current
code needs this. For example, `EntityManager` writes a list of entities
in order, and each `toGraph()` reads the entities written before it.

```java
public final class Graph {
    private static Driver driver;
    private static final ThreadLocal<Transaction> bound = new ThreadLocal<>();

    public static void connect(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        driver.verifyConnectivity();
    }

    public static void close() {
        driver.close();
    }

    // Run work in a transaction on this thread; nested calls join it.
    public static <T> T transaction(Supplier<T> work) {
        if (bound.get() != null) {
            return work.get();
        }
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {
            bound.set(tx);
            T result = work.get();
            tx.commit();
            return result;
        } finally {
            bound.remove();
        }
    }

    // Use the bound transaction if there is one, else auto-commit.
    public static List<Map<String, Object>> query(String cypher, Map<String, Object> params) {
        Transaction tx = bound.get();
        if (tx != null) {
            return rows(tx.run(cypher, params));
        }
        try (Session session = driver.session()) {
            return rows(session.run(cypher, params));
        }
    }

    private static List<Map<String, Object>> rows(Result result) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Record record : result.list()) {
            rows.add((Map<String, Object>) plain(record.asMap()));
        }
        return rows;
    }

    // Convert driver values to plain Java values and GraphNode.
    private static Object plain(Object value) {
        if (value instanceof Node) {
            Node node = (Node) value;
            Set<String> labels = new HashSet<>();
            node.labels().forEach(labels::add);
            return new GraphNode(labels, node.asMap());
        }
        if (value instanceof Entity || value instanceof Path) {
            throw new IllegalArgumentException("Return nodes or values only");
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(Graph::plain).collect(toList());
        }
        if (value instanceof Map) {
            Map<String, Object> map = new LinkedHashMap<>();
            ((Map<String, ?>) value).forEach((k, v) -> map.put(k, plain(v)));
            return map;
        }
        return value;
    }
}
```

`GraphNode` is a small class in `tech.raaf.structure.base` with two
fields: `Set<String> labels` and `Map<String, Object> properties`.

Rules:

- Send all values as parameters. Put only labels and relationship types
  in the query text. Put them in backticks and double each backtick in
  the name. This also fixes `Entity.findDuplicates`, which puts values
  into the query text today.
- An exception in `work` rolls back the transaction.
- The driver does not retry explicit transactions. This is the same as
  today. Do not use `writeTransaction` around work with side effects,
  for example verticle deploys, because the driver can run the work
  again.
- A bound transaction stays on its thread. All `beginTx` sites today
  run on one thread. Keep it that way.
- Only `Graph` imports `org.neo4j`. Other classes, also the tests, use
  `Graph.transaction`, `Graph.query`, `GraphNode` and plain values.
  Then a change to embedded Neo4j changes only `Graph`.
- Queries return nodes, primitives, strings, lists and maps. Do not
  return relationships or paths. Return their properties or their nodes
  as columns, for example `type(r) AS rel, b.uuid AS peer`.
- Do not catch driver exceptions outside `Graph`. If a caller must
  handle an error, for example a constraint violation, `Graph` throws
  its own exception type for it.

### Configuration

Read three keys through `ConfigurationProxy`. It looks at the command
line, then the environment, then the system properties. `Application`
adds the prefix `STRUCTURE_` to environment keys.

| Key              | Environment variable       | Default                 |
|------------------|----------------------------|-------------------------|
| `neo4j-uri`      | `STRUCTURE_NEO4J_URI`      | `bolt://localhost:7687` |
| `neo4j-username` | `STRUCTURE_NEO4J_USERNAME` | `neo4j`                 |
| `neo4j-password` | `STRUCTURE_NEO4J_PASSWORD` | None. It is necessary.  |

Read the password only from the environment. Do not read it from the
command line or from a system property. Other users can see command
lines and `-D` options with `ps`.

Unsafe mode no longer starts a web console or a remote shell.

### Mapping

- Add `NodeMapper`. It makes a domain object from a `GraphNode`. It
  selects the class from the labels in this order: `Controller`,
  `Marker`, `Parser`, `Component`, `Profile`, `Result`, `Group`,
  `Type`. This is the same table as `Entity.getClassLabels`.
- Replace `DynamicProperties` with `Map<String, Object>`. The mapper
  fills the maps from the `data-`, `file-` and `kv-` properties.
- The driver returns integers as `Long`. Convert them for `int` fields,
  for example `glyphId`.
- Replace each `@RelatedTo` and `@RelatedToVia` field with a getter
  that runs a query. `@Fetch` loading stops. This also removes the
  `fetch()` work-arounds in `Type.getIsa`, `Type.getChildIsaSet` and
  `Component`.
- Map `Constraint` from rows of `(a)-[r:CONSTRAINTS]->(b)`: `a.uuid`,
  `b.uuid`, `r.forRelationship` and `r.cardinality`.
- Use `uuid` for `equals` and `hashCode`. Remove the `@GraphId` fields.
  `Instance.save` uses `getId() == null` to find new nodes. Replace
  this with a flag that the mapper sets.
- Replace `org.neo4j.graphdb.Direction` with a small enum in
  `tech.raaf.structure.graph`.
- Remove `@Transient`, `@Indexed`, `@NodeEntity`, `@RelationshipEntity`,
  `@Query`, `@Param` and `@QueryResult`.

### Writes

Use one write path for nodes. Change `Entity.persist` to:

```cypher
MERGE (n:Node {uuid: $uuid})
ON CREATE SET n.created = timestamp()
SET n += $props
```

Then set the labels with a second statement in the same transaction.
Make the label list from escaped names.

`SET n += $props` keeps the properties that are not in `$props`. This
is the same as today. Property values must be primitives, strings or
lists. Convert `JsonNode` values to strings, as `JsonNodeWriter` does
today.

Change `persistRelations` to one `MERGE` for each relationship:

```cypher
MATCH (a:Node {uuid: $from}), (b:Node {uuid: $to})
MERGE (a)-[:`REL`]->(b)
```

`MERGE` replaces the check for an existing relationship. A relationship
is "incoming" or "outgoing". The entity schema does not allow
"undirected".

Replace the SDN write calls:

| Call site                      | Replacement                                                              |
|--------------------------------|--------------------------------------------------------------------------|
| `BundleLoader`: `type.save()`  | Constraint statement below. Delete `createReflexiveConstraint`.          |
| `TaskManager`: `origin.save()` | Move `RESULTS` to the new result. Add `(oldHead)-[:NEXT_RESULT]->(new)`. |
| `EntityManager` specialize     | Delete the old `INSTANCE_OF` relationship. Make the new one.             |
| `Instances.putNode`            | Make an `Entity` and call `toGraph()`.                                   |
| `EntityManager` ungroup        | Ungroup statement below.                                                 |
| `EntityManager` delete         | Delete statement below.                                                  |

```cypher
// Constraint, also when a and b are the same node
MATCH (a:Type {uuid: $from}), (b:Type {uuid: $to})
MERGE (a)-[c:CONSTRAINTS {forRelationship: $rel}]->(b)
SET c.cardinality = $card

// Ungroup
MATCH (m:Node {uuid: $member})-[r:MEMBER_OF]->(g:Group {uuid: $group})
DELETE r

// Delete
MATCH (n:Node {uuid: $uuid})
DETACH DELETE n
```

The result chain keeps the relationship directions that SDN writes
today: `(instance)-[:RESULTS]->(newest)` and
`(older)-[:NEXT_RESULT]->(newer)`. `TaskManager` calls `origin.save()`
and `result.save()` in one transaction. Replace the two calls with one
statement. `ResultBuilder.buildAndStore()` already writes the result
node, so the statement must match it and not create it.
`TaskManagerTest` checks the chain.

`Instrument.save()` and `Result.save()` override `Instance.save()`.
They only write the node. For a new node, `Instance.save()` also checks
the data against the type schema, copies the `kv` values of the type
and deploys an `InstanceActor`. Thus results and instruments get no
actor. Tests call the overrides directly (`ResultTest`,
`InstrumentRepositoryTest`). Keep the three methods. Change only their
bodies to the new write path, so that this difference stays.

### Traversals

`safeDeleteTraversal` looks one step from the start node. Deletion is
not safe if an outgoing neighbour has one of the labels, or if an
incoming neighbour has none of the labels:

```cypher
MATCH (s:Node {uuid: $uuid})
OPTIONAL MATCH (s)-->(o) WHERE any(l IN labels(o) WHERE l IN $labels)
WITH s, count(o) AS badOut
OPTIONAL MATCH (s)<--(i) WHERE none(l IN labels(i) WHERE l IN $labels)
RETURN badOut + count(i) = 0 AS safe
```

`labelBasedTraversal` follows outgoing relationships through nodes that
have one of the labels. The start node does not need a label. Walk the
graph breadth-first in Java with one query for each level:

```cypher
MATCH (a:Node)-->(n)
WHERE a.uuid IN $frontier AND any(l IN labels(n) WHERE l IN $labels)
RETURN DISTINCT n
```

Keep a set of visited `uuid` values. The next frontier is the nodes of
this level that are not in the set. Stop when the frontier is empty.
Add the start node to the result only if `includeStart` is true.

The Traversal API visited each node one time. This walk does the same.
A single Cypher query with `[*0..]` examines all paths, which can be
slow on a full graph.

### Queries

Change each repository interface to a class with the same method names.
Then the callers do not change. Also write the inherited methods that
the code uses: `findByUuid`, `findByName` and `findOneByName` for each
label, `findAll` for `Type` and `Instance`, `count` and `delete`.

Change the syntax:

| Old                               | New                                    | Where                          |
|-----------------------------------|----------------------------------------|--------------------------------|
| `{name}`, `{0}`                   | `$name`                                | All queries                    |
| `CREATE CONSTRAINT ON ... ASSERT` | `... IF NOT EXISTS FOR ... REQUIRE`    | `Application`                  |
| `CREATE INDEX ON :L(p)`           | `... IF NOT EXISTS FOR (n:L) ON (n.p)` | `createEntityIndex`            |
| `HAS(n.identity)`                 | `n.identity IS NOT NULL`               | `inheritsIdentity`             |
| `LOWER(x)`                        | `toLower(x)`                           | `findOneByCaseInsensitiveName` |
| `OPTIONAL MATCH ... DELETE r, n`  | `DETACH DELETE n`                      | The three `purge*` queries     |
| `->protocol<-`                    | `->(protocol)<-`                       | Three queries, see below       |
| `ORDER BY id(...)`                | See below                              | Three queries, see below       |

The new constraint statements:

```cypher
CREATE CONSTRAINT type_name IF NOT EXISTS FOR (n:Type) REQUIRE n.name IS UNIQUE
CREATE CONSTRAINT node_uuid IF NOT EXISTS FOR (n:Node) REQUIRE n.uuid IS UNIQUE
```

The node patterns without parentheses are in `getCompatibleSendString`,
`getCompatibleSendFile` and `getCommonProtocols`.

Replacements for `id()`:

- `getTypeHierarchyForTypeUuid` returns a set. Remove the ordering.
- `getHierarchyPath`: find the `ISA` path to the root type and join the
  names of `reverse(nodes(p))` with dots.
- `getNLastCreated`: order by `n.created DESC, n.uuid DESC`. The new
  `persist` sets `created`. This changes the contract on purpose.
  Today the result is in exact creation order. After the migration,
  the order has a resolution of 1 ms, and nodes made in the same
  millisecond sort by `uuid`. Only bulk loads make nodes this fast,
  and the "recent instances" list in the UI does not need their exact
  order. Change `OrderingTest.nLastCreated` in the same change: set
  `created` on the test nodes, with at least two equal values, and
  check the `(created, uuid)` order and the limit. The test must not
  depend on the clock.
- `NodeSerializer` sorts relationships by the `uuid` of the peer. Keep
  this order when it reads relationships with a query.

Indexes: SDN made legacy full-text indexes for the `@Indexed` fields.
No query uses them. Do not make them again. Add
`CREATE INDEX node_name IF NOT EXISTS FOR (n:Node) ON (n.name)`,
because many queries find nodes by name.

### Spring removal

After Phase 3, no Spring artifact is in the build. Spring arrives only
through `spring-data-neo4j` and `spring-test`.

- `Context` keeps its static getters. It makes the repository objects
  itself. Remove `@Component` and `ApplicationContextAware`.
- `Application` calls `Graph.connect(...)` in place of
  `new AnnotationConfigApplicationContext(...)`. The shutdown hook
  calls `Graph.close()`.
- Delete `SpringConfig`, `SpringTestConfig` and `ConverterConfig`.
- Delete the 8 classes in `graph/converters`. They implement Spring's
  `Converter` interface. In `Entity.persist`, replace
  `new JsonNodeWriter().convert(x)` with `x.toString()`.
- In the tests, replace the `@Autowired` fields with the `Context`
  getters. Remove `@Transactional` and `@Rollback` from the 6 test
  classes that use them. Spring rolls back each test today. The new
  test base class clears the database before each test.

### Threads

All actors are standard verticles today. Their handlers run on
event-loop threads and call the database synchronously. With embedded
Neo4j, a call takes microseconds. With a server, each query is a
network round trip. While a handler waits, its event loop cannot serve
the SockJS bridge or HTTP requests. Move all database work to worker
threads:

- Deploy each verticle that uses the graph as a worker on a named pool:
  `new DeploymentOptions().setWorker(true).setWorkerPoolName("structure-actors")`.
  This applies to 8 of the 9 deploy sites: `Application`
  (`StructureManager`, `EntityManager`, `TaskManager`, `Nodes`,
  `Instances`, `Types`), `NodeActor.deploy` and `Instance.save`.
  `InstrumentManager` does not use the graph. It can stay a standard
  verticle.
- Move the `setPeriodic` call in the `TaskManager` constructor to
  `start()`. A timer made in the constructor runs on an event-loop
  context, not on the worker context of the verticle. The timer
  handlers write results to the graph.
- Change the `/schemas/:schema` route to
  `blockingHandler(schemaManager::getSchema, false)`. The handler
  queries the graph.

A worker verticle handles one message at a time. Two handler calls can
run on different threads of the pool, but never at the same time. Each
transaction starts and ends in one handler call. Thus the thread-bound
transaction of `Graph` works on worker threads.

The actors use the named pool and the `/schemas` route uses the default
worker pool. Keep them apart. `Entity.toGraph()` validates data against
the `/schemas` URL of Structure itself. With one shared pool, the
actors can use all threads and wait for schema requests that cannot
start.

Vert.x logs `Thread ... has been blocked` when an event loop is blocked
for more than 2 seconds. Worker threads can run for 5 minutes
(`workertimeout`).

## Phases

### Phase 0: prepare

1. Start a Neo4j 2026.10 server for development. Community Edition is
   sufficient. Set a password.
2. For TLS tests, configure the Bolt SSL policy
   (`dbms.ssl.policy.bolt.*`) and set `server.bolt.tls_level`.

### Phase 1: remove the embedded-only API (on embedded Neo4j and SDN)

In this phase, the current tests check the most difficult changes
before the transport changes. Use the `{param}` syntax for now. Run
`mvn verify` after each step. The result must agree with the baseline.

1. Replace the two Traversal API methods with Cypher (see
   "Traversals").
2. Change `NodeSerializer` to read relationships with a query. It must
   not use `org.neo4j.graphdb.Node`.
3. Change `Entity.persist`, `persistRelations` and `persistTypeLabels`
   to Cypher through `CypherQueryExecutor`. SDN still reads the nodes
   in this phase. Thus keep writing the `_` labels from
   `getClassLabels`. Phase 3 removes them.
4. Remove the `id()` ordering (see "Queries").
5. Fix `HAS()` and the node patterns without parentheses.
6. Replace `org.apache.commons.lang.*` with `org.apache.commons.lang3.*`.
   Declare `commons-lang3` in `pom.xml`.

If a statement does not work on Neo4j 2.1, do that step in Phase 3.

### Phase 2: move database work off the event loop (on embedded Neo4j)

This change does not depend on the driver. Do it before the driver
change, so that thread problems and server problems do not mix.

1. Make the changes in "Threads".
2. Run `mvn verify`. The result must agree with the baseline.
3. Start Structure and use the frontend. Make sure that the log has no
   `has been blocked` warnings.

### Phase 3: change to the driver

The application does not run between the steps of this phase. Do all
steps on one branch.

1. In `pom.xml`, remove `spring-data-neo4j`, `neo4j`, `neo4j-kernel`,
   both `neo4j-server` artifacts, `spring-test` and the Xebia
   repository. Add `neo4j-java-driver` 4.4.27.
2. Add `Graph` and the configuration keys. Change `Context` and
   `Application` (see "Spring removal").
3. Replace the 20 `beginTx` sites with `Graph.transaction`. At each
   site, look for writes followed by a close without `success()`.
   Today this rolls back the writes. With `Graph.transaction`, throw an
   exception to roll back.
4. Change the domain classes and add `NodeMapper` (see "Mapping").
   Remove the `_` labels from `getClassLabels`, `EntityTest` and
   `StoreFormatTest`.
5. Change the repositories to classes and update the syntax (see
   "Queries").
6. Replace the SDN write calls (see "Writes").
7. Run the schema statements (constraints and indexes) in their own
   transactions, before the data loads.
8. Change the tests:
   - Add a test base class. It connects `Graph` to
     `STRUCTURE_TEST_NEO4J_URI` with `STRUCTURE_TEST_NEO4J_PASSWORD`.
     It runs `MATCH (n) DETACH DELETE n` before each test. Use separate
     variables, so that a test run cannot delete the data of a normal
     run by accident.
   - `mvn verify` needs a test server. If the variables are not set,
     the tests fail. Do not skip them, because the JaCoCo minimum and
     the baseline (0 skipped) need all tests.
   - Remove the Spring runner from the 4 classes that use
     `SpringTestConfig`. Remove the other Spring annotations (see
     "Spring removal").
   - Replace the 29 `beginTx` sites in the tests. Replace the
     `getPersistentState()` assertions in `EntityTest` with queries.
   - The tests use only `Graph`, `GraphNode` and plain values. They do
     not import `org.neo4j`.

`neo4j-harness` is not an option, because the harness for 2025.x and
later needs a newer Java. Testcontainers is possible, but it adds a
dependency and needs Docker.

### Phase 4: verify

1. Run `mvn verify` against 2026.10 with
   `db.query.default_language=CYPHER_25`. Then run it with
   `CYPHER_5`. Both results must agree with the baseline.
2. Run the tests one time with `bolt+s://` or `bolt+ssc://`.
3. Start Structure on an empty database. Check these items:
   - The bundles load. `SHOW CONSTRAINTS` and `SHOW INDEXES` show the
     new schema.
   - Self-discovery finds the host group.
   - Entity create, update, delete, group, ungroup and specialize work
     on the event bus.
   - A controller run adds a `Result` at the head of the result chain.
   - The frontend shows types and instances.
4. Compare the startup time and the bundle load time with the old
   build. Each lazy getter now makes a network call. Write the times in
   this file. They are the reference for a later change to embedded
   mode.
5. Make sure that `mvn dependency:tree` shows no `org.springframework`
   and no `org.neo4j` artifact other than the driver.
6. Make sure that the log has no `has been blocked` warnings for
   event-loop threads during step 3.
7. Make sure that `grep -rl org.neo4j src` shows only `Graph.java`.

### Phase 5: clean up

1. Remove `jersey-core`, `jersey-server`, `persistence-api`,
   `validation-api` and `javassist` if the build compiles without them.
   No code uses them.
2. Remove `.mvn/jvm.config` if no dependency needs it.
3. Update `README.md`. Structure needs a Neo4j server, reads the
   `STRUCTURE_NEO4J_*` variables and no longer serves port 7474.
   `mvn verify` needs a test server and the `STRUCTURE_TEST_NEO4J_*`
   variables.

## Embedded mode later

Embedded Neo4j 2026.x is faster than a server. A test on one machine,
with one thread, Java 21 and the same Cypher on both sides, gave these
median times for one query in one transaction:

| Operation | Embedded      | Server over Bolt on localhost |
|-----------|---------------|-------------------------------|
| Read      | 31 µs         | 430 µs                        |
| Write     | 400 to 600 µs | 1100 to 1800 µs               |

Most of the write time is the commit to disk. With batches of 1000
nodes, embedded is 1.3 to 3 times faster.

Structure cannot use embedded Neo4j 2026.x now:

- Neo4j 2026.x needs Java 21.
- `org.neo4j:neo4j` brings Netty 4.2 without shading, through
  `netty-common` and `neo4j-bolt`. Vert.x 3.4.1 needs Netty 4.1.8. One
  class path cannot hold both. Vert.x 5 uses Netty 4.2.
- The embedded artifacts add approximately 230 jars and 150 MB to the
  fat jar. The shade plugin must merge `META-INF/services`.

Thus do the change to embedded mode after an upgrade to Java 21 and
Vert.x 5. The rules in "Graph access" keep this change inside `Graph`:

1. In `pom.xml`, replace the driver with `org.neo4j:neo4j`.
2. Change `Graph`:
   - `connect` opens a `DatabaseManagementService` on a directory.
     `close` shuts it down.
   - The bound transaction is an `org.neo4j.graphdb.Transaction`.
   - `query` without a bound transaction runs in its own transaction.
   - `plain` makes a `GraphNode` from an `org.neo4j.graphdb.Node`. It
     also converts Java arrays to lists, because embedded Neo4j returns
     list properties as arrays.
   - Map the embedded exceptions to the exception types of `Graph`.
3. Replace the three `neo4j-*` configuration keys with one key for the
   database directory.
4. The test base class opens a database in a temporary directory. The
   tests then need no server and no `STRUCTURE_TEST_*` variables.

A `GraphNode` holds copies of the labels and the properties. Thus it
stays valid after the transaction closes. An embedded `Node` does not.

The worker threads from Phase 2 can stay. For Neo4j Browser or
`cypher-shell`, enable the Bolt connector of the embedded database on
localhost.

Embedded Community Edition is GPLv3, as Neo4j 2.1.8 is today. The
driver is Apache 2.0.

## Open decisions

- `Entity.toGraph()` in `Instances.putNode` adds schema and constraint
  validation that the SDN path did not do. Accept this, or write a
  minimal path for `putNode`.
- Specialize does not update the `type:` labels today. Decide if the
  new code adds the labels of the new type.
- `BundleLoader.createIndices` has no callers. Call it after
  `loadTypes` in its own transaction, or delete it (see "Unused and
  unfinished code").

## Unused and unfinished code

The code below has no callers. The git history shows that it is not a
left-over of a refactor. Keep it until a decision is made. If the
migration keeps it, port it with the rest.

Never used since it was written:

- `NaiveSorter.getNode`, `arrayContains` and `setGlobPattern` (#211).
  They do not use the graph.
- `Result.getStdout`, `getStderr`, `getNext` and `getPrevious` (#51).
  `getNext` and `getPrevious` read `@RelatedTo` fields. If they stay,
  they become query getters (see "Mapping").
- `Controller.getStateMap` and `Controller.Builder.addExitCodeToState`
  (#55). The state map works through the bundles and
  `getStateMapAsJson`. `getStateMap` reads the `data` map, so it
  changes with `DynamicProperties`.

Unfinished or disconnected features:

- `BundleLoader.createIndices` (#266). It creates an index for the
  identity properties of each type. Merge 67ed554b removed its call
  from `Application` by accident, one day after #266. It runs
  `CREATE INDEX ON :Label(prop)` through the SDN query executor.
  Neo4j 5 does not accept this syntax. Use
  `CREATE INDEX IF NOT EXISTS FOR (n:Label) ON (n.prop)`.
- Protocol matching: `Type.findCommonProtocols`, `implementsProtocol`,
  `addImplementedProtocol` and `TypeRepository.getCommonProtocols`
  (#62, STR-35). The ssh, smb, samba and winexe bundles have
  `IMPLEMENTS` relationships, but no code calls this logic. If it
  stays, it adds one `@Query` and one `@RelatedTo` field to port.
- `InstrumentManager`, with `Context.setInstrumentsLoaded`,
  `getInstrumentsLoaded` and the `reloadInstruments` and
  `removeInstruments` handlers (#16). The loading code is commented
  out. It does not use the graph.
- `EntityDeserializer.dataSchema`. Only a validation that is commented
  out uses it.
- `Actor`, an empty `Instance` subclass since #68. No type, bundle or
  code uses it. It is in the SDN base package. Delete it, or keep it
  as a plain class without mapping.
- Bus handlers that no client sends: `InstanceActor.getComponentsByType`
  (uses `ComponentRepository.findChildrenByType`) and
  `TaskManager.taskStatus`.

## Risks

- Neo4j can remove Bolt 4.4. Then you need Java 17 and driver 5.x or
  6.x. The API change is small: `readTransaction` and
  `writeTransaction` become `executeRead` and `executeWrite`. Embedded
  mode is also possible then (see "Embedded mode later").
- Each lazy getter is a network round trip. Constraint validation and
  instrument lookup call many getters.
- The code can depend on SDN behaviour that is not written down, for
  example the order of returned sets. The baseline tests find some of
  this. The checks in Phase 4 find more.
- Long `executeBlocking` work in an actor, for example
  `InstanceActor.runController`, uses a thread of the actor pool. If
  actors wait for threads, make the pool larger with
  `setWorkerPoolSize`.
- `StructureManager`, `InstrumentManager` and `EntityManager` did not
  run from 2018 until the fix of the package rename. They can contain
  other old defects.
