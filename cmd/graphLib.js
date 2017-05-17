'use strict';

const R = require('ramda');
const uuidV4 = require('uuid');
const FuzzyMatching = require('fuzzy-matching');

const getId = R.path(['meta','uuid']);
const getRelations = R.path(['meta','relations']);
const notNil = R.compose(R.not, R.isNil);

function createRelation(name, direction, uuids) {
    return R.objOf(name, R.zipObj(['direction', 'uuids'], [direction, uuids]));
}

function createInstance(baseType, data) {
    let relation = createRelation('INSTANCE_OF', 'outgoing',[getId(baseType)]);
    let instantiate = R.pipe(
        R.clone,
        R.assocPath(['meta','uuid'], uuidV4()),
        R.assoc('data', data)
    );

    let instance = instantiate(baseType);
    instance.meta.relations = R.omit(['ISA','CONSTRAINTS'], getRelations(instance));
    instance.meta.relations = R.merge(getRelations(instance), relation);
    return instance;
}

function relateEntity(entity, relationName, direction, relatedEntity) {
    if(notNil(entity.meta.relations[relationName])) {
        entity.meta.relations[relationName].uuids.push(getId(relatedEntity));
    } else {
        let rel = createRelation(relationName, direction,[getId(relatedEntity)]);
        entity.meta.relations = R.merge(getRelations(entity), rel);
    }
    
    return [entity, relatedEntity];
}

function findEntity(predicateFn, graph) {
    return R.find(predicateFn, graph);
}

function filterGraph(predicateFn, graph) {
    return R.filter(predicateFn, graph);
}

function updateEntity(propPath, value, entity) {
    return R.assocPath(propPath, value, entity);
}

function getInstanceOfUuid(instance) {
    let uuidPath = R.path(['meta','relations','INSTANCE_OF','uuids']);
    let id = R.compose(R.head, uuidPath);
    let hasUuid = R.compose(R.not, R.isNil, uuidPath);
    let predicate = R.ifElse(hasUuid,id,R.always(undefined));

    return predicate(instance);
}

function findTypeByName(typeDb, name) {
    return R.find(R.propEq('name',name), typeDb);
}

function findTypeByUuid(typeDb, uuid) {
    let fn = R.compose(R.equals(uuid), R.path(['meta','uuid']));
    return R.find(fn, typeDb);
}

function findTypeByFuzzyNameMatch(typeDb, name) {
    const matcher = new FuzzyMatching(R.pluck('name',typeDb));
    const condition = R.ifElse(
                            R.pipe(R.prop('distance'), R.lte(0.5)),
                            R.prop('value'),
                            R.always(name)
                          );
    let m = matcher.get(name);
    let entity = R.find(R.propEq('name', condition(m)), typeDb);
    return R.defaultTo(null, entity);
}

function getTypeHierarchy(typeDb, instance) {
    let getType = R.curry(findTypeByUuid) (typeDb);
    let getIsaType = R.compose(getType, R.head, R.path(['meta','relations','ISA','uuids']));
    let getFirstType = R.compose(getIsaType,R.head);
    let hasISA = R.compose(notNil, R.path(['meta','relations','ISA']));
    let whileCondition = R.compose(hasISA, R.head);
    let getInstanceOfType = R.compose(getType, R.head, R.path(['meta','relations','INSTANCE_OF','uuids']));

    let hierarchy = [getInstanceOfType(instance)];
    while(whileCondition(hierarchy)) {
        hierarchy = R.prepend(getFirstType(hierarchy), hierarchy);
    }

    return hierarchy;
}

function getChildren(typeDb, uuid) {
    let isaPath = R.path(['meta','relations','ISA','uuids']);
    let isChild = R.compose(R.equals(uuid), R.head, isaPath);
    return R.filter(item => {
        if(notNil(isaPath(item))) {
            return isChild(item);
        } else return false;
    }, typeDb);
}

module.exports = {
    createRelation: R.curry(createRelation),
    createInstance: R.curry(createInstance),
    relateEntity: R.curry(relateEntity),
    findEntity: R.curry(findEntity),
    filterGraph: R.curry(filterGraph),
    updateEntity: R.curry(updateEntity),
    getInstanceOfUuid: getInstanceOfUuid,
    findTypeByName: R.curry(findTypeByName),
    findTypeByUuid: R.curry(findTypeByUuid),
    findTypeByFuzzyNameMatch: R.curry(findTypeByFuzzyNameMatch),
    getTypeHierarchy: R.curry(getTypeHierarchy),
    getChildren: R.curry(getChildren)
};