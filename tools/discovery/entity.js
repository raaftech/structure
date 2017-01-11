"use strict";

/**
 * This class is used to relate one entity to another.
 */
class Relation {
    /**
     * The relation constructor.
     * @param direction {string} The direction of the relation, defaults to 'outgoing'.
     */
    constructor(direction = 'outgoing') {
        this.direction = direction;
        this.entities = [];
        this.uuids = [];
    }

    /**
     * Add an entity between the entity that has this relation, and a given entity.
     * @param {Entity} entity The given entity to create a relation to.
     * @returns {Relation}
     */
    addEntity(entity) {
        this.uuids.push(entity.meta.uuid);
        this.entities.push(entity);
        return this;
    }
}

/**
 * The Entity class is used the base data structure for Structure.
 */
class Entity {
    /**
     * The entity constructor.
     * @param {string} type The type of the entity.
     * @param {string} name The name of the entity.
     * @param {string} desc The description of the entity.
     * @param {string} uuid The UUID of the entity.
     * @param {object} data A simple object containing keys and values.
     * @param {object} relations An object that maps relation names to Relation objects.
     * @param {object} file An object containing file references.
     */
    constructor(type, name, desc, uuid, data = null, relations = null, file = null) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.meta = {};
        this.meta.uuid = uuid;
        if (data !== null) {
            this.data = data;
        }
        if (relations !== null) {
            this.meta.relations = relations;
        }
        if (file !== null) {
            this.file = file;
        }
    }

    /**
     * Create a new relation object on this entity.
     * @param {string} name The name of the relation.
     * @param {string} direction The direction, either 'incoming' or 'outgoing', with the latter being default.
     * @returns {Relation} The created relation object, to which entities can be added, see {@link Relation}.
     */
    relationCreate(name, direction='outgoing') {
        if (!this.meta.hasOwnProperty('relations')) {
            this.meta.relations = {};
        }
        const relation = new Relation(direction);
        this.meta.relations[name] = relation;
        return relation;
    }
}

/**
 * Given a flat array of entity definitions that adhere to entity.schema.json, create a hierarchy based on relations.
 * @param {Object[]} entities
 * @returns {Object.<string, Entity>} A map of entity UUID's to the entities themselves.
 */
const readBundle = (entities) => {
    const entityMap = {};
    entities.forEach((e) => entityMap[e.meta.uuid] =
        new Entity(e.type, e.name, e.desc, e.meta.uuid, e.data, e.meta.relations, e.file));
    for (const key of Object.keys(entityMap)) {
        const relations = entityMap[key].meta.relations;
        for (const relName of Object.keys(relations)) {
            const rel = relations[relName];
            rel.uuids.forEach((relatedUuid) => {
                if (relatedUuid in entityMap) {
                    if (rel.entities == null) {
                        rel.entities = [];
                    }
                    rel.entities.push(entityMap[relatedUuid]);
                }
            });
        }
    }
    return entityMap;
};

/**
 * Given an array of {@link Entity entities} that might contain nested entities, flatten and transform them
 * so they validate against entity.schema.json.
 * @param {Entity|Entity[]} entities An array of {@link Entity entities}, or a single {@link Entity entity} that may
 * contain nested entities.
 * @returns {Array} A flat array or {@link Entity entities} where relations are flattened with UUID references.
 */
const flatten = (entities, done = [], flattened = []) => {
    const flattenEntity = (e) => {
        if (!done.includes(e.meta.uuid)) {
            done.push(e.meta.uuid);
            const copy = JSON.parse(JSON.stringify(e));
            for (const key of Object.keys(copy.meta.relations)) {
                const relEntities = copy.meta.relations[key].entities;
                delete copy.meta.relations[key].entities;

                relEntities.forEach((relEnt) => flatten([relEnt], done, flattened));
            }
            flattened.push(copy);
        }
    };

    if (Array.isArray(entities)) {
        entities.forEach(flattenEntity);
    } else {
        flattenEntity(entities);
    }

    return flattened;
};

exports.Entity = Entity;
exports.readBundle = readBundle;
exports.flatten = flatten;
