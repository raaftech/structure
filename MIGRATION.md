# Migration: embedded Neo4j to a Neo4j server

## Goal

Structure stops using embedded Neo4j 2.1.8 and Spring Data Neo4j (SDN)
3.2.1. It connects to an external Neo4j 2026.10 server with the Neo4j
Java driver.

## Decisions

| Topic         | Decision                                                                                       |
|---------------|------------------------------------------------------------------------------------------------|
| Driver        | `org.neo4j.driver:neo4j-java-driver:4.4.27`. Java 8 bytecode. Its Netty is shaded.             |
| Java, Vert.x  | No change: Java 8, Vert.x 3.4.1.                                                               |
| Connection    | Bolt on port 7687. Plain: `bolt://`. TLS: `bolt+s://`, or `bolt+ssc://` for self-signed certs. |
| Spring        | Remove. Spring is in the build only for SDN.                                                   |
| Data model    | The Structure source is the reference. `doc/cypher` is historical. Do not use it.              |
| Stored format | Keep it, with two changes. See "Stored format".                                                |
| SDN artifacts | Remove the `_` labels (`_Type`, `_Component`, ...) and the `__type__` relationship property.   |
| Old data      | Do not migrate `target/structure.db`. Structure loads the bundles at startup.                  |
| Out of scope  | The JS code in `rest/` and `cmd/`, `doc/cypher`, the Dockerfile, Java and Vert.x upgrades.     |
| Backend seam  | Only `Graph` uses the driver API. See "Graph access".                                          |

The shaded Netty of the driver does not conflict with the Netty 4.1.8
of Vert.x 3.4.1. Ports 7474 (HTTP) and 7473 (HTTPS) serve Neo4j Browser
and the HTTP Query API. The driver does not use them.

## Stored format

Each node has these labels:

- Its class labels. Each node has `Node` and one or more of `Type`,
  `Instance`, `Component`, `Group`, `Profile`, `Result`, `Instrument`,
  `Controller`, `Marker` and `Parser`. For example, a controller has
  `Controller`, `Instrument`, `Instance` and `Node`.
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
| `created`    | All nodes           | Milliseconds after 1970-01-01 UTC. New, see below.                |
| `path`       | Nodes from a bundle | Path of the bundle file. Run-time nodes have no `path`.           |
| `isAbstract` | Types               | Boolean.                                                          |
| `glyphId`    | Types               | Integer. The value is always 0. See below.                        |
| `data-*`     | Nodes with data     | Primitive, or a JSON string for objects and arrays.               |
| `file-*`     | Nodes with files    | JSON string: `encoding`, `format`, `path`, `content`, `checksum`. |
| `kv-*`       | Types               | `Instance.save` reads it. No bundle writes it now.                |

`CONSTRAINTS` relationships have the properties `forRelationship` and
`cardinality`. Other relationships have no properties.

The migration makes two changes to this format:

- `created` is new. The new `persist` sets it when it makes a node.
- `glyphId`: now SDN writes it only on types with constraints, because
  only `BundleLoader` calls `type.save()` for those types. The new
  `persist` writes it on each type.

`StoreFormatTest` checks this format. Change its expected values for
`created` and `glyphId` in the same change as the new `persist`.

## Server facts (checked in October 2026)

- Neo4j 2025.x and 2026.x accept Bolt 4.4. The Neo4j support table
  lists driver 4.4 as compatible up to 2026.10. A later release can
  remove Bolt 4.4. There is no date for this.
- New 2026.x installations set `db.query.default_language=CYPHER_25`.
  Upgraded installations keep their setting. All queries must be valid
  in Cypher 5 and in Cypher 25.
- `id()` is deprecated. Do not use it. The order of internal ids is not
  the creation order.
- Driver 4.4 API: `session.beginTransaction`, `session.readTransaction`,
  `session.writeTransaction`. A driver `Node` has only the numeric
  `id()`. For a string id, return `elementId(n)` from Cypher.
- Neo4j does not accept schema changes and data writes in one
  transaction.
- If a query fails in an explicit transaction, the transaction rolls
  back. The driver then rejects each query in that transaction.

References:

- [Bolt protocol and Neo4j compatibility](https://neo4j.com/docs/bolt/current/bolt-compatibility/)
- [Neo4j supported versions](https://neo4j.com/developer/kb/neo4j-supported-versions/)
- [Cypher deprecations, additions and removals](https://neo4j.com/docs/cypher-manual/current/deprecations-additions-removals-compatibility/)

## Baseline

On the current code with JDK 1.8.0_504, `mvn verify` runs 119 tests
with 0 failures and 0 skipped. It also builds the fat jar and checks
the line coverage with JaCoCo. The minimum is `jacoco.line.minimum`
(0.60) in `pom.xml`, and the coverage is now 62%. The report is in
`target/site/jacoco`. The migrated code must give the same result.

Maven runs the build and the tests with the JDK 8 from
`~/.m2/toolchains.xml`. Maven itself can run on a newer JDK. Newer JDKs
reject `aspectjweaver-1.8.2.jar`. `.mvn/jvm.config` lets Maven read it.

`TaskManagerTest.resultChain` can fail on Neo4j 2.1 when the machine is
busy. The test reads the graph while `TaskManager` commits, and Neo4j
2.1 lets a reader see a commit that is not fully applied. On Neo4j
2026.10 this test must pass each time.

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
| Tests                  | Import SDN or Neo4j. 4 of them use `SpringTestConfig`.          | 15 files               |

`commons-lang` 2.4 (`org.apache.commons.lang.*`) comes only through
`neo4j-server`. In total, 38 main files import Spring and 19 main files
import `org.neo4j`.

## Target design

### Graph access

Add one class, `tech.raaf.structure.base.Graph`. It holds the `Driver`
and binds a transaction to the current thread. Nested calls use the
bound transaction. Thus a read in a transaction sees the uncommitted
writes of that transaction, as with embedded Neo4j now. The current
code uses this. For example, `EntityManager` writes a list of entities
in sequence, and each `toGraph()` reads the entities written before it.

```java
public final class Graph {
    private static final Logger log = LoggerFactory.getLogger(Graph.class);
    private static Driver driver;
    private static final ThreadLocal<Bound> bound = new ThreadLocal<>();

    // The transaction of a thread, its state and its commit actions.
    private static final class Bound {
        final Transaction tx;
        final List<Runnable> afterCommit = new ArrayList<>();
        boolean failed;

        Bound(Transaction tx) {
            this.tx = tx;
        }
    }

    public static void connect(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        try {
            driver.verifyConnectivity();
        } catch (Neo4jException e) {
            driver.close();
            throw new GraphException(e);
        }
    }

    public static void close() {
        driver.close();
    }

    // Run work in a transaction on this thread; nested calls use it.
    public static <T> T transaction(Supplier<T> work) {
        Bound outer = bound.get();
        if (outer != null) {
            try {
                return work.get();
            } catch (RuntimeException e) {
                outer.failed = true;
                throw e;
            }
        }
        T result;
        List<Runnable> actions;
        try (Session session = driver.session();
             Transaction tx = session.beginTransaction()) {
            Bound b = new Bound(tx);
            bound.set(b);
            result = work.get();
            if (b.failed) {
                throw new GraphException("Nested transaction failed");
            }
            tx.commit();
            actions = b.afterCommit;
        } catch (Neo4jException e) {
            throw new GraphException(e);
        } finally {
            bound.remove();
        }
        for (Runnable action : actions) {
            try {
                action.run();
            } catch (RuntimeException e) {
                log.error("Action after commit failed", e);
            }
        }
        return result;
    }

    public static void transaction(Runnable work) {
        transaction(() -> {
            work.run();
            return null;
        });
    }

    // Run action after the commit, or now if there is no transaction.
    public static void afterCommit(Runnable action) {
        Bound b = bound.get();
        if (b == null) {
            action.run();
        } else {
            b.afterCommit.add(action);
        }
    }

    // Use the bound transaction if there is one, else auto-commit.
    public static List<Map<String, Object>> query(String cypher, Map<String, Object> params) {
        Bound b = bound.get();
        try {
            if (b != null) {
                return rows(b.tx.run(cypher, params));
            }
            try (Session session = driver.session()) {
                return rows(session.run(cypher, params));
            }
        } catch (Neo4jException e) {
            throw new GraphException(e);
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
        if (value instanceof Relationship || value instanceof Path) {
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

`GraphNode` and `GraphException` are small classes in
`tech.raaf.structure.base`. `GraphNode` has two fields:
`Set<String> labels` and `Map<String, Object> properties`. It holds
copies, thus it stays valid after the transaction closes.
`GraphException` is a `RuntimeException`. All driver exceptions extend
`Neo4jException`.

Rules:

- Send all values as parameters. Put only labels, relationship types
  and property keys in the query text. Put them in backticks, and
  double each backtick in the name. This also corrects
  `Entity.findDuplicates`, which now puts values into the query text.
  Its `data-*` keys stay in the query text.
- An exception in the work rolls back the transaction. If nested work
  throws an exception, `Graph` marks the outer transaction as failed.
  The outer transaction then rolls back, also if a caller catches the
  exception. Neo4j 2.1 does the same after `tx.failure()` in a nested
  transaction.
- If the outer work catches an exception, `Graph` commits. Thus put
  each `catch` that calls `tx.failure()` outside `Graph.transaction`.
  The `create`, `update`, `delete` and `group` handlers of
  `EntityManager` have such a `catch`.
- `Graph` changes driver exceptions to `GraphException`. Do not catch
  driver exceptions outside `Graph`.
- Catch a `GraphException` only outside `Graph.transaction`. After a
  failed query, the driver rejects each query in the transaction.
- Use `Graph.afterCommit` for side effects, for example verticle
  deploys. Then a rollback does not deploy an actor, and the new actor
  can read the committed node. If an action fails, `Graph` logs the
  error and runs the next action.
- The driver does not retry explicit transactions. This is the same as
  now. Do not use `writeTransaction`, because the driver can run the
  work again.
- A bound transaction stays on its thread. All `beginTx` sites now run
  on one thread. Keep it that way.
- Only `Graph` imports `org.neo4j`. Other classes and the tests use
  `Graph`, `GraphNode`, `GraphException` and plain values. Then a
  change to embedded Neo4j changes only `Graph`.
- Queries return nodes, primitives, strings, lists and maps. Do not
  return relationships or paths. Return their properties or their nodes
  as columns, for example `type(r) AS rel, b.uuid AS peer`.

### Configuration

Read three keys through `ConfigurationProxy`. It looks at the command
line, then the environment, then the system properties. `Application`
adds the prefix `STRUCTURE_` to environment keys.

| Key              | Environment variable       | Default                 |
|------------------|----------------------------|-------------------------|
| `neo4j-uri`      | `STRUCTURE_NEO4J_URI`      | `bolt://localhost:7687` |
| `neo4j-username` | `STRUCTURE_NEO4J_USERNAME` | `neo4j`                 |
| `neo4j-password` | `STRUCTURE_NEO4J_PASSWORD` | None. It is necessary.  |

Read the password only from the environment. Other users can see
command lines and `-D` options with `ps`.

Unsafe mode no longer starts a web console or a remote shell. Use Neo4j
Browser on the server.

### Mapping

- Add `NodeMapper`. It makes a domain object from a `GraphNode`. It
  selects the class from the labels in this sequence: `Controller`,
  `Marker`, `Parser`, `Component`, `Profile`, `Result`, `Group`,
  `Type`. `Entity.getClassLabels` uses the same table.
- Replace `DynamicProperties` with `Map<String, Object>`. The mapper
  fills the maps from the `data-`, `file-` and `kv-` properties.
- The driver returns integers as `Long`. Convert them for `int` fields,
  for example `glyphId`.
- Replace each `@RelatedTo` and `@RelatedToVia` field with a getter
  that runs a query. Remove `@Fetch` and the `fetch()` calls in
  `Type.getIsa`, `Type.getChildIsaSet` and `Component`.
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

Use one write path for nodes. If the node is in the database,
`Entity.persist` first reads its current state:

```cypher
MATCH (n:Node {uuid: $uuid})
RETURN labels(n) AS labels, keys(n) AS keys
```

Then it writes the properties:

```cypher
MERGE (n:Node {uuid: $uuid})
ON CREATE SET n.created = timestamp()
SET n += $props
```

Before the write, add each key from the read that is not in `$props`,
with the value `null`. Do not add `created` and the `kv-*` keys.
`SET n += $props` removes the keys with a `null` value. The current
`persist` never removes a property. Thus a key that an update or a
changed bundle removes stays on the node, and the next read returns it.
This is an error, because the `update` handler sends the full entity
and its checksum.

Then, in the same transaction, replace the labels with a second
statement. `REMOVE` the labels from the read that are not in the new
list, and `SET` the new list. This applies to the `type:` labels too.
Make the label lists from escaped names. The current `persist` removes
all labels, and `persistTypeLabels` adds them again.

Property values must be primitives, strings or lists. Change `JsonNode`
values to strings with `toString()`, as `JsonNodeWriter` does now.

Change `persistRelations` to one `MERGE` for each relationship. Keep
the current peer check before the writes. A peer is a `uuid`. If the
relationship goes to a type (`outgoingToType`, `incomingFromType`), the
peer can also be the name of the type. Find the `uuid` of each peer. If
a peer is missing, log the error and write no relationship, as now.
Then:

```cypher
MATCH (a:Node {uuid: $from}), (b:Node {uuid: $to})
MERGE (a)-[:`REL`]->(b)
```

`MERGE` replaces the current check for a duplicate relationship. The
entity schema accepts only "incoming" and "outgoing" relationships.

Replace the SDN write calls:

| Call site                      | Replacement                                                              |
|--------------------------------|--------------------------------------------------------------------------|
| `BundleLoader`: `type.save()`  | Constraint statement below. Delete `createReflexiveConstraint`.          |
| `TaskManager`: `origin.save()` | Move `RESULTS` to the new result. Add `(oldHead)-[:NEXT_RESULT]->(new)`. |
| `EntityManager` specialize     | Replace `INSTANCE_OF`, then call `toGraph()`. See below.                 |
| `Instances.putNode`            | Make an `Entity` and call `toGraph()`. See below.                        |
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

Specialize deletes the old `INSTANCE_OF` relationship and makes the
new one. Now it does not update the `type:` labels. After the new
`INSTANCE_OF`, make an `Entity` from the graph and call `toGraph()`.
This replaces the `type:` labels of the old type with the labels of the
new type. It also checks the data against the schema of the new type.
If the check fails, the specialization rolls back.

`toGraph()` in `Instances.putNode` adds the schema and constraint
checks. The SDN path did not do them. This is correct, and it gives one
write path.

The result chain keeps the relationship directions that SDN writes
now: `(instance)-[:RESULTS]->(newest)` and
`(older)-[:NEXT_RESULT]->(newer)`. `TaskManager` calls `origin.save()`
and `result.save()` in one transaction. Replace the two calls with one
statement. `ResultBuilder.buildAndStore()` already writes the result
node, thus the statement must match it and not create it.
`TaskManagerTest` checks the chain.

`Instrument.save()` and `Result.save()` override `Instance.save()`.
They only write the node. For a new node, `Instance.save()` also checks
the data against the type schema, copies the `kv` values of the type
and deploys an `InstanceActor`. Thus results and instruments get no
actor. `ResultTest` and `InstrumentRepositoryTest` call the overrides
directly. Keep the three methods, and change only their bodies to the
new write path. In `Instance.save()`, remove `tx.failure()` and keep
the exception after it. Deploy the actor with `Graph.afterCommit`.

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
have one of the labels. The start node can have no label from the
list. Go through the graph breadth-first in Java, with one query for
each level:

```cypher
MATCH (a:Node)-->(n)
WHERE a.uuid IN $frontier AND any(l IN labels(n) WHERE l IN $labels)
RETURN DISTINCT n
```

Keep a set of visited `uuid` values. The next frontier is the nodes of
this level that are not in the set. Stop when the frontier is empty.
Add the start node to the result only if `includeStart` is true.

The Traversal API visits each node one time, and this walk does the
same. A single Cypher query with `[*0..]` examines all paths. On a full
graph, this can be slow.

### Queries

Change each repository interface to a class with the same method names,
so that the callers do not change. Also write the inherited methods
that the code uses: `findByUuid`, `findByName` and `findOneByName` for
each label, `findAll` for `Type` and `Instance`, `count` and `delete`.

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

- `getTypeHierarchyForTypeUuid` returns a set. Remove the `ORDER BY`.
- `getHierarchyPath`: find the `ISA` path to the root type and join the
  names of `reverse(nodes(p))` with dots.
- `getNLastCreated`: order by `n.created DESC, n.uuid DESC`. This
  changes the contract. Now the result is in exact creation sequence.
  After the migration, the resolution is 1 ms, and nodes from the same
  millisecond sort by `uuid`. Only bulk loads make nodes this fast. The
  "recent instances" list in the UI does not use their exact sequence.
- Change `OrderingTest.nLastCreated` in the same change. Set `created`
  on the test nodes, with a minimum of two equal values. Check the
  `(created, uuid)` sequence and the limit. The test must not use the
  clock.
- `NodeSerializer` sorts relationships by the `uuid` of the peer. Keep
  this sequence when it reads relationships with a query.

Indexes: SDN made legacy full-text indexes for the `@Indexed` fields.
Only SDN `save()` writes them. The current `persist` does not. The
`@Query` methods do not read them. The generated finders of
`GenericRepository` read them. Only one generated finder has a caller:
`ComponentRepository.findOneByName`, in `ComponentRepositoryTest`. Its
test nodes come from `save()`. The new repository classes use label
queries. Do not make the legacy indexes again. Add
`CREATE INDEX node_name IF NOT EXISTS FOR (n:Node) ON (n.name)`,
because many queries find nodes by name.

### Spring removal

Spring comes only through `spring-data-neo4j` and `spring-test`. After
Phase 3, the build has no Spring artifact.

- `Context` keeps its static getters. It makes the repository objects
  itself. Remove `@Component` and `ApplicationContextAware`.
- `Application` calls `Graph.connect(...)` in place of
  `new AnnotationConfigApplicationContext(...)`. The shutdown hook
  calls `Graph.close()`.
- Delete `SpringConfig`, `SpringTestConfig` and `ConverterConfig`.
- Delete the 8 classes in `graph/converters`. They implement Spring's
  `Converter` interface.

### Threads

All actors are now standard verticles. Their handlers run on event-loop
threads and call the database synchronously. With embedded Neo4j, a
call takes microseconds. With a server, each query is a network round
trip. While a handler waits, its event loop cannot serve the SockJS
bridge or HTTP requests. Move all database calls to worker threads:

- Deploy each verticle that uses the graph as a worker on a named pool:
  `new DeploymentOptions().setWorker(true).setWorkerPoolName("structure-actors")`.
  This applies to 8 of the 9 deploy sites: `Application`
  (`StructureManager`, `EntityManager`, `TaskManager`, `Nodes`,
  `Instances`, `Types`), `NodeActor.deploy` and `Instance.save`.
  `InstrumentManager` does not use the graph. It can stay a standard
  verticle.
- Move the `setPeriodic` call in the `TaskManager` constructor to
  `start()`. A timer that the constructor makes runs on an event-loop
  context. The timer handlers write results to the graph.
- Change the `/schemas/:schema` route to
  `blockingHandler(schemaManager::getSchema, false)`. The handler
  queries the graph.
- Pass `ordered = false` to `executeBlocking` at 3 sites:
  `InstanceActor.runController` and the `selfDiscovery` and reinit
  handlers of `StructureManager`. On a worker context, ordered blocking
  code and the message handlers use one task queue. Thus an ordered
  call stops all messages to the verticle until it ends, and a
  controller run can take minutes. With `false`, the blocking code runs
  on a different thread of the pool, at the same time as the handlers.
  This is safe now. The blocking code uses only fields that `start()`
  sets, and the repositories, which have no state.

A worker verticle handles one message at a time. Two handler calls can
run on different threads of the pool, but never at the same time. Each
transaction starts and ends in one handler call. Thus the thread-bound
transaction of `Graph` works on worker threads.

The actors use the named pool, and the `/schemas` route uses the
default worker pool. Keep them separate. `Entity.toGraph()` validates
data against the `/schemas` URL of Structure itself. With one shared
pool, the actors can use all threads and wait for schema requests that
cannot start.

Vert.x logs `Thread ... has been blocked` when an event loop is blocked
for more than 2 seconds. Worker threads can run for 5 minutes
(`workertimeout`).

## Phases

### Phase 0: prepare

1. Start two Neo4j 2026.10 servers for development, one for Structure
   and one for the tests. Use different ports. Community Edition is
   enough. Set a password on each server.
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
   in this phase. Thus keep the `_` labels from `getClassLabels`.
4. Correct `HAS()` and the node patterns without parentheses.
5. Replace `org.apache.commons.lang.*` with `org.apache.commons.lang3.*`.
   Declare `commons-lang3` in `pom.xml`.
6. Add `Graph` and `GraphException` with only `transaction` and
   `afterCommit`. `Graph` gets the `GraphDatabaseService` from
   `SpringConfig` and binds an embedded `Transaction`. SDN calls in the
   work use this transaction. Replace the 20 `beginTx` sites in main
   with `Graph.transaction`. At each site, look for writes followed by
   a close without `success()`. Now this rolls back the writes. With
   `Graph.transaction`, throw an exception to roll back. Some sites
   return before `close()`, for example specialize in `EntityManager`.
   `Graph.transaction` closes the transaction on all paths. Put each
   `catch` that calls `tx.failure()` outside `Graph.transaction` (see
   "Graph access"). The baseline tests check these rules before the
   driver change.

Do not remove the `id()` sequence in this phase. SDN `save()` writes
nodes without `created`, and `ORDER BY ... DESC` puts `null` first.

If a statement does not operate on Neo4j 2.1, do that step in Phase 3.

### Phase 2: move database calls off the event loop (on embedded Neo4j)

This change does not use the driver. Do it before the driver change,
so that thread problems and server problems do not mix.

1. Make the changes in "Threads".
2. Run `mvn verify`. The result must agree with the baseline.
3. Start Structure and use the frontend. Make sure that the log has no
   `has been blocked` warnings.

### Phase 3: change to the driver

The application does not run between the steps of this phase. Do all
steps on one branch.

1. In `pom.xml`, remove `spring-data-neo4j`, `neo4j`, `neo4j-kernel`,
   the two `neo4j-server` artifacts, `spring-test` and the Xebia
   repository. Add `neo4j-java-driver` 4.4.27.
2. Change `Graph` to the driver (see "Graph access"). Add `GraphNode`
   and the configuration keys. Change `Context` and `Application` (see
   "Spring removal").
3. Change the domain classes and add `NodeMapper` (see "Mapping").
   Remove the `_` labels from `getClassLabels`, `EntityTest` and
   `StoreFormatTest`.
4. Change the repositories to classes and update the syntax (see
   "Queries").
5. Replace the SDN write calls (see "Writes").
6. Remove the `id()` sequence (see "Queries"). Now all nodes have
   `created`.
7. Run the schema statements (constraints and indexes) in their own
   transactions, before the data loads. Call
   `BundleLoader.createIndices` after `loadTypes`, also in its own
   transaction. `Entity.findDuplicates` finds nodes by the identity
   properties, and these indexes make this fast.
8. Change the tests:
   - Add a test base class. It connects `Graph` to
     `STRUCTURE_TEST_NEO4J_URI` with `STRUCTURE_TEST_NEO4J_USERNAME`
     (default `neo4j`) and `STRUCTURE_TEST_NEO4J_PASSWORD`. It runs the
     schema statements one time, as `Application` does. It runs
     `MATCH (n) DETACH DELETE n` before each test. Community
     Edition has one user database. Thus the test server must not be
     the server of a normal run.
   - `mvn verify` must have a test server. If the variables are not
     set, the tests fail. Do not skip them, because the JaCoCo minimum
     and the baseline (0 skipped) count all tests.
   - Remove the Spring runner from the 4 classes that use
     `SpringTestConfig`. Remove `@Transactional` and `@Rollback` from
     the 6 classes that use them. The base class replaces the Spring
     rollback. Replace the `@Autowired` fields with the `Context`
     getters.
   - Replace the 29 `beginTx` sites in the tests. Replace the
     `getPersistentState()` assertions in `EntityTest` with queries.

You cannot use `neo4j-harness`, because the harness for 2025.x and
later must have a newer Java. Testcontainers is possible, but it adds a
dependency and Docker.

### Phase 4: verify

1. Run `mvn verify` against 2026.10 with
   `db.query.default_language=CYPHER_25`. Then run it with
   `CYPHER_5`. The two results must agree with the baseline.
2. Run the tests one time with `bolt+s://` or `bolt+ssc://`.
3. Start Structure on an empty database. Check these items:
   - The bundles load. `SHOW CONSTRAINTS` and `SHOW INDEXES` show the
     new schema.
   - Self-discovery finds the host group.
   - Entity create, update, delete, group, ungroup and specialize
     operate correctly on the event bus.
   - A controller run adds a `Result` at the head of the result chain.
   - The frontend shows types and instances.
4. Compare the startup time and the bundle load time with the old
   build. Write the times in this file. They are the reference for a
   later change to embedded mode.
5. Make sure that `mvn dependency:tree` shows no `org.springframework`
   and no `org.neo4j` artifact other than the driver.
6. Make sure that the log has no `has been blocked` warnings for
   event-loop threads during step 3.
7. Make sure that `grep -rl org.neo4j src` shows only `Graph.java`.

### Phase 5: clean up

1. Remove `jersey-core`, `jersey-server`, `persistence-api`,
   `validation-api` and `javassist` if the build compiles without them.
   No code uses them.
2. Remove `.mvn/jvm.config` if no dependency uses it.
3. Update `README.md`. Structure must have a Neo4j server, reads the
   `STRUCTURE_NEO4J_*` variables and no longer serves port 7474.
   `mvn verify` must have a test server and the `STRUCTURE_TEST_NEO4J_*`
   variables.

## Embedded mode later

Embedded Neo4j 2026.x is faster than a server. A test on one machine,
with one thread, Java 21 and the same Cypher on the two sides, gave
these median times for one query in one transaction:

| Operation | Embedded      | Server over Bolt on localhost |
|-----------|---------------|-------------------------------|
| Read      | 31 µs         | 430 µs                        |
| Write     | 400 to 600 µs | 1100 to 1800 µs               |

Most of the write time is the commit to disk. With batches of 1000
nodes, embedded is 1.3 to 3 times faster.

Structure cannot use embedded Neo4j 2026.x now:

- Neo4j 2026.x must have Java 21.
- `org.neo4j:neo4j` brings Netty 4.2 without shading, through
  `netty-common` and `neo4j-bolt`. Vert.x 3.4.1 must have Netty 4.1.8.
  One class path cannot hold the two versions. Vert.x 5 uses Netty 4.2.
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
   - Change the embedded exceptions to `GraphException`.
3. Replace the three `neo4j-*` configuration keys with one key for the
   database directory.
4. The test base class opens a database in a temporary directory. Then
   the tests do not use a server or the `STRUCTURE_TEST_*` variables.

The worker threads from Phase 2 can stay. For Neo4j Browser or
`cypher-shell`, enable the Bolt connector of the embedded database on
localhost.

Embedded Community Edition is GPLv3, as Neo4j 2.1.8 is now. The driver
is Apache 2.0.

## Unused and unfinished code

The code below has no callers. The git history shows that a refactor
did not cause this. Keep it until there is a decision. If the migration
keeps it, port it with the rest.

Never used:

- `NaiveSorter.getNode`, `arrayContains` and `setGlobPattern` (#211).
  They do not use the graph.
- `Result.getStdout`, `getStderr`, `getNext` and `getPrevious` (#51).
  `getNext` and `getPrevious` read `@RelatedTo` fields. If they stay,
  they become query getters (see "Mapping").
- `Controller.getStateMap` and `Controller.Builder.addExitCodeToState`
  (#55). The state map operates through the bundles and
  `getStateMapAsJson`. `getStateMap` reads the `data` map, thus it
  changes with `DynamicProperties`.

Unfinished or disconnected features:

- `BundleLoader.createIndices` (#266). It makes an index for the
  identity properties of each type. Merge 67ed554b, one day after #266,
  removed its call from `Application` in error. Its `CREATE INDEX`
  statement is in `createEntityIndex` (see "Queries"). Phase 3 calls
  it again (step 7).
- Protocol compatibility: `Type.findCommonProtocols`,
  `implementsProtocol`, `addImplementedProtocol` and
  `TypeRepository.getCommonProtocols` (#62, STR-35). The ssh, smb,
  samba and winexe bundles have `IMPLEMENTS` relationships, but no code
  calls this logic. If it stays, it adds one `@Query` and one
  `@RelatedTo` field to port.
- `InstrumentManager`, with `Context.setInstrumentsLoaded`,
  `getInstrumentsLoaded` and the `reloadInstruments` and
  `removeInstruments` handlers (#16). The load code is commented out.
  It does not use the graph.
- `EntityDeserializer.dataSchema`. Only a validation that is commented
  out uses it.
- `Actor`, an empty `Instance` subclass from #68. No type, bundle or
  code uses it. It is in the SDN base package. Delete it, or keep it
  as a plain class without mapping.
- Bus handlers that no client sends: `InstanceActor.getComponentsByType`
  (uses `ComponentRepository.findChildrenByType`) and
  `TaskManager.taskStatus`.

## Risks

- Neo4j can remove Bolt 4.4 (see "Server facts"). Then Java 17 and
  driver 5.x or 6.x are necessary. The API change is small:
  `readTransaction` and `writeTransaction` become `executeRead` and
  `executeWrite`. Embedded mode is then also possible.
- Each lazy getter is a network round trip. Constraint validation and
  instrument lookup call many getters.
- The code can use SDN operation that is not documented, for example
  the sequence of returned sets. The baseline tests find some of this,
  and the checks in Phase 4 find more.
- Long `executeBlocking` code in an actor, for example
  `InstanceActor.runController`, uses a thread of the actor pool. If
  actors wait for threads, make the pool larger with
  `setWorkerPoolSize`.
- With `ordered = false`, the handlers and the blocking code of an
  actor can run at the same time. A new field that the two use must be
  safe for more than one thread.
- `StructureManager`, `InstrumentManager` and `EntityManager` did not
  run from 2018 until the correction of the package rename. They can
  contain other old errors.
