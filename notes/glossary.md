# Glossary

The words that Structure uses, and what they mean in Structure. Some words have
more than one meaning. For these words, the entry gives each meaning and the
words to use for each one.

- **Abstract type**: A type with `abstract` set. Structure does not make
  instances of an abstract type.

- **Actor**: An object on the event bus. It receives requests on its bus
  addresses and sends a response to each request. An actor can also send
  requests to other actors, and publish events on the events address. Each actor
  is a subclass of `GenericActor` and is a verticle. The actor ID is its Vert.x
  deployment ID. There are managers, repository actors and node actors. See
  **Actor class**.

- **Actor class**: The Java class of an actor, for example `TypeActor`. All
  actors of a class have the same message handlers. The name of an actor class
  is the full class name in lowercase, for example
  `tech.raaf.structure.bus.manager.entitymanager`. A manager has one actor, and
  this name is also its bus address.

- **Address**: An address is a name on the event bus that an actor listens on,
  for example `tech.raaf.structure.bus.manager.entitymanager`. this is distinct
  from addresses in the graph, like an `IpAddress` type, with the identity
  `addr`. Be explicit and write "bus address" or "IP address".

- **Agent**: The group that represents the Structure host. The instrument runner
  sends commands from the agent to the subject. See **Self**.

- **APPLIES_TO**: The relation from an instrument or a profile to the type that
  it acts on, for example `OpenRcStop APPLIES_TO Service`.

- **Argument**: Two meanings. The `args` of a request are the arguments of a
  message handler. The `arguments` of a controller are the values that its
  template uses. Each controller argument comes (`from`) the subject, the
  origin, the agent or a static value. The caller can give a static argument
  that has `canBeUserSupplied`.

- **Argument schema**: The JSON schema of the `args` of a message handler. The
  `input` field of the handler's `@SchemaPath` annotation selects it from
  `src/main/resources/schema/`. If `input` is omitted, the handler uses
  `base/args.schema.json`.

- **Bootstrap**: The first detection of the Structure host. The `selfDiscovery`
  handler runs the payload of the `StructureBootstrap` instrument. This payload
  adds the host, its OS, an IPv4 address, Structure and the tools that it finds
  to the graph. Structure can run instruments only after bootstrap.

- **Bridge**: The SockJS event bus bridge at `/eventbus`. Clients outside
  Structure use it to send and receive event bus messages over a WebSocket, at
  `wss://host:port/eventbus/websocket`. It permits only bus addresses that start
  with `tech.raaf.structure.`, except in unsafe mode.

- **Bundle**: A directory in the bundle directory with type files
  (`.type.json`), instrument and profile files (`.inst.json`), data schemas,
  templates and payloads. Structure loads the bundles into the graph when it
  starts, and on reload.

- **Bundle directory**: The directory with all bundles, set with the
  `bundle-dir` setting.

- **Capability**: A type below `Capability`, for example `Start`, `Execute` or
  `Detect`. A capability is an operation that Structure can do on a component.
  Instruments give capabilities, and act on real systems. The `README.md` lists
  all capabilities. Some message handlers have names near to the names of
  capabilities: `create`, `delete`, `list`, `read` and `update`. These handlers
  change or read only the graph.

- **Component**: An instance of `Component` or of a type below it, for example
  an OS, software or a service. Structure manages components. A component can
  have child components through `HAS`. The Java class `Component` represents
  one.

- **ComponentGroup**: A group of components, for example a host with its OS,
  addresses and software.

- **Configuration**: Two meanings. `Configuration.java` gives the settings of
  Structure. The `Configuration` type is a component: the configuration of a
  system, which a `ConfigurationManager` manages.

- **Constraint**: Two meanings. A type constraint (`meta.constraints`, the
  `CONSTRAINTS` relation) tells which relations the instances of two types can
  have. Structure checks type constraints when it makes an instance. A database
  constraint is a Neo4j rule, for example that type names are unique. Write
  "type constraint" or "database constraint".

- **Consumer**: A Vert.x registration on a bus address. Each actor registers one
  consumer for each of its bus addresses. The consumer gives each message to the
  actor, and the actor sends it to a message handler.

- **Context**: Two meanings. `Context.java` holds the objects that all of
  Structure shares: Vert.x, the graph repositories, the self group and the self
  identifier. A Vert.x context is the thread context of a Vert.x handler. Write
  "Vert.x context" for the second meaning.

- **Controller**: An instrument that runs a command. It has a template,
  arguments, a weight and an allocated time. Each Result node that a controller
  makes has a `PRODUCED` relation from the controller.

- **Data schema**: The JSON schema of the `data` of the instances of a type, in
  `bundles/*/schema/`. The `file.dataSchema` of a type points to it. Structure
  stores the file content on the type node and serves it at `/schemas/`.
  Instances use their type's stored schema. A change to the file on disk alone
  does not change that stored content.

- **Deployment**: Two meanings. A Vert.x deployment is one verticle that Vert.x
  runs, with a deployment ID. Each actor is one Vert.x deployment, and its
  deployment ID is the actor ID. The `list` handler returns the deployment IDs.
  In systems management, to deploy is to install and start software. Write
  "Vert.x deployment" for the first meaning.

- **Detect**: The capability that finds components and adds them to the graph.
  `StructureBootstrap`, `OsDiscover`, `PythonDiscover`, `NmapNetworkDiscover`
  and `NmapIpAddressDiscover` give it.

- **Discover, discovery**: The use of `Detect` to find components in the
  environment. The `*Discover` instruments and the `selfDiscovery` handler do
  this. To find what is on the event bus, see **Introspect**.

- **Entity**: The JSON form of a node, in the format of `entity.schema.json`:
  `name`, `desc`, `meta` (`uuid`, relations), `data` and `file`. Bundle files
  hold entities, and the `create` and `update` handlers take entities. The Java
  class `Entity` builds them. The descriptions of capabilities also use "entity"
  for a node that a capability acts on.

- **Entity schema**: `src/main/resources/schema/entity.schema.json`. The bundle
  loader, the entity deserializer and the `create` and `update` handlers check
  entities against it.

- **Event bus**: The Vert.x message bus in Structure. Actors listen on bus
  addresses, and clients outside Structure use it through the bridge. Messages
  are JSON requests and responses, and events on the events address.

- **Events address**: The bus address `tech.raaf.structure.bus.events`, where
  actors publish events, for example `taskScheduled` or `selfDiscovery`. Each
  event has a `source`, an `event` and `data`. Clients register on this address
  to follow what Structure does.

- **GIVES_CAPABILITY**: The relation from an instrument to the capability that
  it gives.

- **Glyph**: The icon number of a type, for a web interface. The
  `findAllTypeGlyphs` handler of `Types` returns them.

- **Group**: An instance that has members, through `MEMBER_OF`. The `group` and
  `ungroup` handlers add and remove members. See **ComponentGroup**.

- **Handler**: Three meanings. A message handler is a named command of an actor,
  which a request selects with `msgh`. Each message handler is a public method
  of the actor that returns `MessageHandler`, and the method name is the handler
  name. A Vert.x handler (`Handler<T>`) is a callback. A route handler answers
  an HTTP route, for example `/schemas/`. "Handler" alone means message handler.

- **HAS**: The relation from a component to a child component.

- **HAS_RESULT**: The relation from the origin of a run to its Result node.

- **Help**: Two meanings. Each message handler has help: a JSON document in
  `src/main/resources/help/` with a name, a synopsis and a description, in the
  format of `base/manual.schema.json`. The `help` handler, which each actor has,
  returns the help of all message handlers of the actor.

- **Hierarchy path**: The path of a type from the root type, in lowercase, for
  example `type.component.software.service`. Controller arguments use it to
  select a type. A type actor also listens on
  `tech.raaf.structure.bus.node.<hierarchy path>`.

- **Identity**: The properties that identify an instance of a type, set in
  `meta.identity`, for example `name`, `cidr` or `addr`. Do not confuse identity
  with the `Identify` capability, which finds details such as the name or the
  version of a component. Also see **Self**.

- **IMPLEMENTS**: The relation from a type to a protocol that it implements, for
  example `SshClient IMPLEMENTS SSH`.

- **Info**: The `info` handler of `StructureManager`. It returns if Structure is
  bootstrapped, the hostname, the number of types, groups, results, components
  and instruments in the graph, and data about the JVM.

- **Instance**: A node that is `INSTANCE_OF` a type. Components, groups,
  profiles, instruments and Result nodes are instances. Write "Vert.x instance"
  for the running Vert.x.

- **INSTANCE_OF**: The relation from an instance to its type.

- **Instrument**: An instance that gives a capability. The kinds of instruments
  are controller, marker and parser. `PROVIDED_BY` tells which component
  provides it, `APPLIES_TO` tells what it acts on, and `GIVES_CAPABILITY` tells
  which capability it gives.

- **Interpreter**: A component that runs commands or scripts, for example a
  shell. `Invoke` instruments, such as `BashInvoke`, put a command in a script
  for an interpreter.

- **Introspect**: The `introspect` handler of `StructureManager`. It returns the
  actors on the event bus, each with its actor class and bus addresses. It also
  returns the message handlers of each actor class once, with `inputSchemaUri`,
  `outputSchemaUri` and help. Its `schemas` map contains the common request and
  response schemas, handler schemas and all referenced application schema
  documents, by absolute document URI.

- **ISA**: The relation from a type to its parent type. What applies to a type
  also applies to the types below it, for example its instruments.

- **Job**: One process of a task, with the status QUEUED, RUNNING or CANCELLED.
  `ProcessRunner` runs it.

- **Label**: A Neo4j label on a node, for example `Node`, `Type`, `Instance`,
  `Component` or `Result`. Handlers such as `read` filter on labels.

- **Manager**: Two meanings. A manager is an actor with message handlers for one
  area: `StructureManager`, `EntityManager`, `InstrumentManager` and
  `TaskManager`. `SchemaManager` only answers HTTP requests at `/schemas/`. The
  types `SystemManager`, `ServiceManager`, `PackageManager` and
  `ConfigurationManager` are software that manages systems. The type `Structure`
  is a `SystemManager`.

- **Marker**: An instrument without a command. It marks a component with a
  capability, for example `SmbReceiveString` gives `ReceiveString` to
  `SmbServer`. Structure uses markers to find send and receive paths between the
  agent and a subject.

- **MEMBER_OF**: The relation from a member to its group.

- **Message**: One request or one response on the event bus.

- **msgh**: The name of the message handler in a request.

- **Node**: Four meanings. A Neo4j node is a vertex in the graph. `Node` is the
  Neo4j label on each type and instance, with a `uuid` and a `name`. A node
  actor is the actor of one node. Node.js is the runtime of the REPL. Always
  write "Node.js" for the last meaning.

- **Node actor**: The actor of one type (`TypeActor`) or one instance
  (`InstanceActor`). It listens on `tech.raaf.structure.bus.node.<uuid>`.
  Structure starts a node actor for each node.

- **Origin**: The instance that a run acts on, for example the service to start.

- **Output schema**: The JSON schema of successful `result.data` from a message
  handler. Each handler must declare it in the `output` field of `@SchemaPath`.
  It does not describe error responses or the response envelope.

- **Parser**: An instrument that parses raw results into graph data, with the
  `PARSED` and `PRODUCED` relations. The bundles have no parsers at this time.

- **Payload**: A script in the `payload` directory of a bundle, which an
  instrument runs. Payloads can connect to the bridge. For the content of an
  event bus message, write "body".

- **Profile**: An instance of `Profile` that tells how to detect software: its
  files, directories, ports and command output. `APPLIES_TO` gives the type that
  it detects. `PythonDiscover` uses profiles.

- **Protocol**: A type for a protocol, for example `SSH` or `SMB`. Software
  `IMPLEMENTS` protocols. A send instrument and its receive instrument use the
  same protocol.

- **PROVIDED_BY**: The relation from an instrument or a profile to the type that
  provides it, for example `OpenRcStop PROVIDED_BY OpenRc`.

- **Reinitialize**: The `reinitialize` handler of `StructureManager`. It stops
  all node actors, deletes all nodes from the graph and loads the bundles again.

- **Relate, unrelate**: The `relate` and `unrelate` handlers of `EntityManager`.
  They add and remove one relation from a node to another node, for example
  `HAS`. The type constraints of the first node must permit the result. Use
  specialize for `INSTANCE_OF` and group for `MEMBER_OF`. The bundles give `ISA`
  and `CONSTRAINTS`.

- **Reload**: The `reload` handler of `StructureManager`. It loads the types and
  instances of the bundles again.

- **Repository**: Three meanings. A graph repository is a Java class in
  `graph/repositories` with the queries for one kind of node. A repository actor
  (`Types`, `Instances`, `Nodes`) gives some of these queries on the event bus.
  A package repository is a source of software packages for a package manager.

- **Request**: A message to a message handler, with `msgh` and optional `args`,
  in the format of `base/request.schema.json`.

- **Response**: The reply to a request, in the format of
  `base/response.schema.json`. It has the `request`, and a `result` with `data`
  or an `error` text, never both. `GenericActor` checks the response envelope,
  then checks successful `data` against the handler's output schema. It checks
  error responses against the envelope only. If a response is not valid, it
  logs the failure and replies with an error. Vert.x calls it a reply.

- **Result**: Three meanings. A Result node holds the outcome of a run of a
  controller, with a state (`stateCode`, `stateInterpretation`,
  `stateDescription`). Its origin has `HAS_RESULT` to it, and its controller has
  `PRODUCED` to it. The response result is the `result` of a response, with
  `data` or `error`. The job result is the outcome of the process of a job.
  Write "Result node", "response result" or "job result".

- **Self**: The Structure host in the graph. The self group is the group that
  represents it, and the self identifier is the UUID of this group. The
  instrument runner uses the self group as the agent.

- **selfDiscovery**: The handler of `StructureManager` that bootstraps
  Structure. See **Bootstrap**.

- **Setting**: One configuration value of Structure, for example `bundle-dir` or
  `unsafe`. Settings come from settings files, environment variables and the
  command line. Code reads settings only through `Configuration.java`.

- **Specialize**: The `specialize` handler of `EntityManager`. It changes the
  type of an instance to a type below its current type.

- **State**: Two meanings. The `State` capability checks the state of a
  component, for example if a service runs. The state of a Result node is its
  code, interpretation and description. For tasks and jobs, write "status".

- **State directory**: The directory where Structure writes its runtime files,
  set with the `state-dir` setting.

- **Status**: The status of a task or a job, for example READY or RUNNING. For
  components and Result nodes, write "state".

- **Subject**: The group that the origin is in, for example the host of a
  service. If a controller has `subjectIsLocal`, its command runs on the
  subject, and the agent sends the command there with send instruments. If not,
  the command runs on the agent.

- **Task**: A group of jobs with one ID, with the status READY or PAUSED. The
  `queueTask` and `runCapability` handlers make tasks, and `TaskManager` runs
  them.

- **Template**: The Handlebars file of a controller, for example `*.hbs.sh`. It
  makes the command from the arguments.

- **Type**: A node that defines a kind of thing, for example `Linux` or
  `Service`. A type has a parent (`ISA`), a data schema, an identity and type
  constraints. All types are below the root type `Type`.

- **Unsafe mode**: The `unsafe` setting. The bridge then permits all bus
  addresses.

- **Update**: The `update` handler of `EntityManager`. It changes the name, the
  description, the data and the files of nodes. It does not change relations;
  see **Relate, unrelate**, **Specialize** and **Group**.

- **Verticle**: A unit of code that Vert.x deploys and runs. Each actor is a
  verticle.

- **Weight**: A number of a controller. If more than one controller gives a
  capability, Structure uses the controller with the highest weight.

- **Worker pool**: The Vert.x threads that run the actors that use the graph.
