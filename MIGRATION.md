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
  lists driver 4.4 as compatible up to 2026.09. Neo4j can remove Bolt
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

On the current code with JDK 1.8.0_504, `mvn test` runs 111 tests with
0 failures and 0 skipped. The migrated code must give the same result.

## Inventory

| Area                   | Where                                                           | Count                  |
|------------------------|-----------------------------------------------------------------|------------------------|
| Embedded database      | `SpringConfig`, `SpringTestConfig`                              | 2 files                |
| Spring context         | `Context`, `ConverterConfig`, `Application`                     | 3 files                |
| Spring converters      | `graph/converters/*`, Spring `Converter` interface              | 8 files                |
| `beginTx` transactions | `EntityManager` (7), `Application` (4), `Type` (2), `Types` (2) | 19 in main             |
|                        | `Entity`, `Instance`, `Component`, `NodeActor` (1 each)         |                        |
|                        | 6 test classes                                                  | 13 in tests            |
| SDN mapping            | 13 domain classes in `graph/`                                   | 26 relationship fields |
| `DynamicProperties`    | `Node`, `Entity`, `Type`, `Instance`                            | 4 files                |
| SDN repositories       | `graph/repositories/*`                                          | 9 files, 56 `@Query`   |
| Inherited SDN methods  | `save`, `delete`, `findAll`, `count`, generated finders         | See "Queries"          |
| Core API writes        | `Entity.persist`, `persistRelations`, `persistTypeLabels`       | 3 methods              |
| Core API reads         | `NodeSerializer`, `NodeRepository.findGraphNodeByUuid`          | 2 files                |
| Traversal API          | `EntityManager.labelBasedTraversal`, `safeDeleteTraversal`      | 2 methods              |
| `CypherQueryExecutor`  | `Application`, `BundleLoader` (2), `Entity.findDuplicates`      | 4 sites                |
| Verticle deploys       | `Application` (7), `NodeActor.deploy`, `Instance.save`          | 9 sites                |
| `commons-lang` 2.4     | `Type`, `Constraint`, `ConfigurationProxy`                      | 3 files                |
| Tests                  | 11 files import SDN or Neo4j. 4 of them use `SpringTestConfig`. | 11 files               |

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
    public static List<Record> query(String cypher, Map<String, Object> params) {
        Transaction tx = bound.get();
        if (tx != null) {
            return tx.run(cypher, params).list();
        }
        try (Session session = driver.session()) {
            return session.run(cypher, params).list();
        }
    }
}
```

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

### Configuration

Read three keys through `ConfigurationProxy`. It looks at the command
line, then the environment, then the system properties. `Application`
adds the prefix `STRUCTURE_` to environment keys.

| Key              | Environment variable       | Default                 |
|------------------|----------------------------|-------------------------|
| `neo4j-uri`      | `STRUCTURE_NEO4J_URI`      | `bolt://localhost:7687` |
| `neo4j-username` | `STRUCTURE_NEO4J_USERNAME` | `neo4j`                 |
| `neo4j-password` | `STRUCTURE_NEO4J_PASSWORD` | None. It is necessary.  |

Do not add a command line option for the password. Other users can see
command lines with `ps`.

Unsafe mode no longer starts a web console or a remote shell.

### Mapping

- Add `NodeMapper`. It makes a domain object from a driver `Node`. It
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

`MERGE` replaces the check for an existing relationship. "undirected"
makes two relationships, as today.

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
today.

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
have one of the labels:

```cypher
MATCH p = (s:Node {uuid: $uuid})-[*0..]->(n)
WHERE all(x IN tail(nodes(p)) WHERE any(l IN labels(x) WHERE l IN $labels))
  AND ($includeStart OR n <> s)
RETURN DISTINCT n
```

The Traversal API visited each node one time. Cypher examines all
paths. Measure this query on a full graph. If it is slow, walk the
graph breadth-first in Java with one query for each level.

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

A worker verticle handles one message at a time on one thread. Thus
the thread-bound transaction of `Graph` works on worker threads.

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
`mvn test` after each step. The result must agree with the baseline.

1. Replace the two Traversal API methods with Cypher (see
   "Traversals").
2. Change `NodeSerializer` to read relationships with a query. It must
   not use `org.neo4j.graphdb.Node`.
3. Change `Entity.persist`, `persistRelations` and `persistTypeLabels`
   to Cypher through `CypherQueryExecutor`.
4. Remove the `id()` ordering (see "Queries").
5. Fix `HAS()` and the node patterns without parentheses.
6. Replace `org.apache.commons.lang.*` with `org.apache.commons.lang3.*`.
   Declare `commons-lang3` in `pom.xml`.

If a statement does not work on Neo4j 2.1, do that step in Phase 3.

### Phase 2: move database work off the event loop (on embedded Neo4j)

This change does not depend on the driver. Do it before the driver
change, so that thread problems and server problems do not mix.

1. Make the changes in "Threads".
2. Run `mvn test`. The result must agree with the baseline.
3. Start Structure and use the frontend. Make sure that the log has no
   `has been blocked` warnings.

### Phase 3: change to the driver

The application does not run between the steps of this phase. Do all
steps on one branch.

1. In `pom.xml`, remove `spring-data-neo4j`, `neo4j`, `neo4j-kernel`,
   both `neo4j-server` artifacts, `spring-test` and the commented
   `spring-data-neo4j-rest` block. Add `neo4j-java-driver` 4.4.27.
2. Add `Graph` and the configuration keys. Change `Context` and
   `Application` (see "Spring removal").
3. Replace the 19 `beginTx` sites with `Graph.transaction`. At each
   site, look for writes followed by a close without `success()`.
   Today this rolls back the writes. With `Graph.transaction`, throw an
   exception to roll back.
4. Change the domain classes and add `NodeMapper` (see "Mapping").
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
   - Skip the database tests with `Assume` if the variables are not
     set.
   - Remove the Spring runner from the 4 classes that use
     `SpringTestConfig`. Remove the other Spring annotations (see
     "Spring removal").
   - Replace the 13 `beginTx` sites in the tests. Replace the
     `getPersistentState()` assertions in `EntityTest` with queries.

`neo4j-harness` is not an option, because the harness for 2025.x and
later needs a newer Java. Testcontainers is possible, but it adds a
dependency and needs Docker.

### Phase 4: verify

1. Run `mvn test` against 2026.10 with
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
   build. Each lazy getter now makes a network call.
5. Make sure that `mvn dependency:tree` shows no `org.springframework`
   and no `org.neo4j` artifact other than the driver.
6. Make sure that the log has no `has been blocked` warnings for
   event-loop threads during step 3.

### Phase 5: clean up

1. Remove `jersey-core`, `jersey-server`, `persistence-api`,
   `validation-api` and `javassist` if the build compiles without them.
   No code uses them.
2. Update `README.md`. Structure needs a Neo4j server, reads the
   `STRUCTURE_NEO4J_*` variables and no longer serves port 7474.

## Open decisions

- `Entity.toGraph()` in `Instances.putNode` adds schema and constraint
  validation that the SDN path did not do. Accept this, or write a
  minimal path for `putNode`.
- Specialize does not update the `type:` labels today. Decide if the
  new code adds the labels of the new type.
- `BundleLoader.createIndices` has no callers. Call it after
  `loadTypes` in its own transaction, or delete it.

## Risks

- Neo4j can remove Bolt 4.4. Then you need Java 17 and driver 5.x or
  6.x. The API change is small: `readTransaction` and
  `writeTransaction` become `executeRead` and `executeWrite`.
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
