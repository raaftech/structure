// Generated from output/info.schema.json by scripts/model.ts. Do not edit.

/** The bootstrap and the host of Structure, the number of nodes in the graph, and data about the JVM. */
export type Info = {
  structure: {
    /** True if the self group is in the graph. */
    bootstrapped: boolean;
    /** The hostname of the Structure host. */
    host: string;
  };
  /** The number of nodes with each label. */
  graph: {
    types: Count;
    groups: Count;
    results: Count;
    components: Count;
    instruments: Count;
  };
  jvm: {
    /** The feature release of Java, for example 25. */
    javaVersion: number;
    /** The full version of Java, for example 25+36-LTS. */
    vmVersion: string;
    /** The number of processors that the JVM can use. */
    processors: number;
    /** The memory of the JVM, in bytes. */
    memory: {
      max: Count;
      total: Count;
      free: Count;
      used: Count;
    };
  };
};

export type Count = number;
