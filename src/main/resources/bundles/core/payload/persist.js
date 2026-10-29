"use strict";

// Create dependencies first and reuse nodes that violate an identity rule.
module.exports = async function persist(client, entities, related = []) {
  const uuids = [];
  for (const entity of entities) {
    const result = await client.createInstance({ entities: [entity] });
    if (Array.isArray(result) && result.includes(entity.meta.uuid)) {
      uuids.push(entity.meta.uuid);
      continue;
    }
    const conflict = typeof result === "string" ? JSON.parse(result) : {};
    if (conflict.code !== 409 || !conflict.conflict) {
      throw new Error("Could not create entity: " + JSON.stringify(result));
    }
    const uuid = entity.meta.uuid;
    entity.meta.uuid = conflict.conflict;
    for (const item of [...entities, ...related]) {
      for (const relation of Object.values(item.meta.relations)) {
        relation.to = [...new Set(relation.to.map((target) => target === uuid ? conflict.conflict : target))];
      }
    }
    uuids.push(conflict.conflict);
  }
  return uuids;
};
