#
# Copyright (c) 2023 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Check a Rune JSON document against the Rune model, without either language's runtime.

The model is read from the .rosetta files. A document is malformed when its data doesn't match
the model:
- a required attribute is missing, or a list has fewer or more items than its cardinality allows;
- an attribute isn't in the model;
- a value has the wrong shape (an object, a list or a single value where the model has another);
- a value isn't of its basic type or isn't one of its enum's values;
- a choice has no option, or more than one;
- an @type isn't the attribute's type or a subtype of it;
- a reference names a key that isn't in its scope, on a required attribute;
- a reference names a key on an object of the wrong type;
- the same key is on two different objects in one scope.

Keys are scoped as CDM Java's reference resolver scopes them (CdmReferenceConfig): an external or
scoped key, and a reference to one, belongs to the nearest enclosing TradeState (SCOPE_TYPE), or
to the document when there is none. A global key is a hash of its object and belongs to the
document. A reference whose key is only in another scope is dangling, and its detail says so.

An attribute with optional cardinality doesn't need a value. So a reference to a missing key on
an optional attribute is reported, but doesn't make the document malformed
(DANGLING_OPTIONAL_IS_MALFORMED). Conditions aren't checked here.
"""
import re
from datetime import date, datetime, time
from decimal import Decimal, InvalidOperation
from pathlib import Path

DANGLING_OPTIONAL_IS_MALFORMED = False

# The type whose objects scope external and scoped keys, as in CDM's CdmReferenceConfig
SCOPE_TYPE = 'cdm.event.common.TradeState'
DOCUMENT = '(document)'

NAMESPACE = re.compile(r'^namespace\s+([\w.]+)')
IMPORT = re.compile(r'^import\s+([\w.]+(?:\.\*)?)')
BLOCK = re.compile(r'^(type|choice|enum)\s+(\w+)(?:\s+extends\s+([\w.]+))?\s*:')
ALIAS = re.compile(r'^typeAlias\s+(\w+)(?:\([^)]*\))?\s*:\s*(?:([\w.]+))?')
ATTRIBUTE = re.compile(
    r'^\s{1,8}(?:override\s+)?\^?(\w+)\s+([\w.]+)(?:\([^()]*\))?\s*\((\d+)\.\.(\d+|\*)\)')
OPTION = re.compile(r'^\s{1,8}([\w.]+)(?:\([^()]*\))?\s*(?:<"|\[|$)')
ENUM_VALUE = re.compile(r'^\s{1,8}(\w+)(?:\s+displayName\s+"([^"]*)")?')
ALIAS_BASE = re.compile(r'^\s{1,8}([\w.]+)')

BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.S)
BASIC = {'string', 'int', 'number', 'boolean', 'date', 'dateTime', 'zonedDateTime', 'time'}
EMPTY = (None, [])

# The kinds of issue, and whether each makes a document malformed
MALFORMED_KINDS = {
    'required attribute missing': True,
    'too few items': True,
    'too many items': True,
    'unknown attribute': True,
    'wrong shape': True,
    'invalid basic value': True,
    'invalid enum value': True,
    'choice without one option': True,
    'wrong @type': True,
    'metadata without a value': True,
    'dangling reference (required attribute)': True,
    'dangling reference (optional attribute)': DANGLING_OPTIONAL_IS_MALFORMED,
    'dangling reference (cardinality unknown)': True,
    'reference to wrong type': True,
    'duplicate key': True,
}


class RuneModel:
    '''The data types, choices, enums and type aliases of a Rune model.'''

    def __init__(self, rosetta_dir: str):
        self.types = {}      # FQN -> {'super': name, 'attrs': {name: (type, min, max)}, 'ns': ns}
        self.choices = {}    # FQN -> {'options': [type name], 'ns': ns}
        self.enums = {}      # FQN -> {'super': name, 'values': set, 'ns': ns}
        self.aliases = {}    # FQN -> (base type name, ns)
        self.imports = {}    # namespace -> set of imports
        self.by_name = {}    # simple name -> [FQN]
        for path in sorted(Path(rosetta_dir).rglob('*.rosetta')):
            text = path.read_text(encoding='utf-8')
            self._read(BLOCK_COMMENT.sub(lambda m: '\n' * m.group(0).count('\n'), text))

    def _declare(self, namespace: str, name: str) -> str:
        fqn = f'{namespace}.{name}'
        self.by_name.setdefault(name, []).append(fqn)
        return fqn

    def _read(self, text: str) -> None:
        namespace, current = '', None
        for line in text.splitlines():
            stripped = line.strip()
            if m := NAMESPACE.match(line):
                namespace = m.group(1)
                self.imports.setdefault(namespace, set())
                continue
            if m := IMPORT.match(line):
                self.imports.setdefault(namespace, set()).add(m.group(1))
                continue
            if line and not line[0].isspace():
                current = None
                if m := BLOCK.match(line):
                    kind, name, parent = m.groups()
                    fqn = self._declare(namespace, name)
                    if kind == 'type':
                        self.types[fqn] = {'super': parent, 'attrs': {}, 'ns': namespace}
                    elif kind == 'choice':
                        self.choices[fqn] = {'options': [], 'ns': namespace}
                    else:
                        self.enums[fqn] = {'super': parent, 'values': set(), 'ns': namespace}
                    current = (kind, fqn)
                elif m := ALIAS.match(line):
                    fqn = self._declare(namespace, m.group(1))
                    self.aliases[fqn] = (m.group(2), namespace)
                    current = ('alias', fqn) if not m.group(2) else None
                continue
            if current is None or not stripped or stripped.startswith(('[', '<"', '//')):
                continue
            if stripped.startswith('condition'):
                current = None  # the attributes and values come before the conditions
                continue
            kind, fqn = current
            if kind == 'type' and (m := ATTRIBUTE.match(line)):
                name, typ, low, high = m.groups()
                self.types[fqn]['attrs'][name] = (typ, int(low), None if high == '*' else int(high))
            elif kind == 'choice' and (m := OPTION.match(line)):
                self.choices[fqn]['options'].append(m.group(1))
            elif kind == 'enum' and (m := ENUM_VALUE.match(line)):
                self.enums[fqn]['values'].add(m.group(1))
                if m.group(2) is not None:
                    self.enums[fqn]['values'].add(m.group(2))
            elif kind == 'alias' and (m := ALIAS_BASE.match(line)):
                self.aliases[fqn] = (m.group(1), self.aliases[fqn][1])
                current = None

    def known(self, fqn: str) -> bool:
        return fqn in self.types or fqn in self.choices or fqn in self.enums or fqn in self.aliases

    def resolve(self, name: str | None, namespace: str = '') -> str | None:
        '''The FQN of a name: its own namespace first, then the namespace's imports, then CDM.'''
        if not name:
            return None
        if name in BASIC:
            return name
        if self.known(name):
            return name
        simple = name.rsplit('.', 1)[-1]
        candidates = self.by_name.get(simple, [])
        if f'{namespace}.{simple}' in candidates:
            return f'{namespace}.{simple}'
        for imported in sorted(self.imports.get(namespace, ())):
            if imported.endswith('.*'):
                fqn = f'{imported[:-2]}.{simple}'
            else:
                fqn = imported if imported.rsplit('.', 1)[-1] == simple else None
            if fqn in candidates:
                return fqn
        for fqn in candidates:
            if fqn.startswith('cdm.'):
                return fqn
        return candidates[0] if candidates else None

    def base(self, fqn: str | None) -> str | None:
        '''A type alias's underlying type; other types are their own.'''
        seen = set()
        while fqn in self.aliases and fqn not in seen:
            seen.add(fqn)
            name, namespace = self.aliases[fqn]
            fqn = self.resolve(name, namespace) if name else None
        return fqn

    def attributes(self, fqn: str) -> dict:
        '''All the attributes of a type, with its supertypes', as {name: (type FQN, min, max)}.'''
        found, chain, seen = {}, [], set()
        while fqn in self.types and fqn not in seen:
            seen.add(fqn)
            chain.append(fqn)
            info = self.types[fqn]
            fqn = self.resolve(info['super'], info['ns'])
        for fqn in reversed(chain):  # a subtype's override wins
            info = self.types[fqn]
            for name, (typ, low, high) in info['attrs'].items():
                found[name] = (self.base(self.resolve(typ, info['ns'])) or typ, low, high)
        return found

    def options(self, fqn: str) -> dict:
        '''A choice's options, as {simple name: type FQN}.'''
        info = self.choices[fqn]
        return {o.rsplit('.', 1)[-1]: self.base(self.resolve(o, info['ns'])) or o
                for o in info['options']}

    def enum_values(self, fqn: str) -> set:
        values, seen = set(), set()
        while fqn in self.enums and fqn not in seen:
            seen.add(fqn)
            info = self.enums[fqn]
            values |= info['values']
            fqn = self.resolve(info['super'], info['ns'])
        return values

    def conforms(self, actual: str | None, expected: str | None) -> bool | None:
        '''Whether an object of type actual can be used where expected is.'''
        if not actual or not expected:
            return None
        if actual == expected:
            return True
        if expected in self.choices:
            return any(self.conforms(actual, o) for o in self.options(expected).values())
        seen = set()
        while actual and actual not in seen:
            if actual == expected:
                return True
            seen.add(actual)
            info = self.types.get(actual)
            actual = self.resolve(info['super'], info['ns']) if info else None
        return False


def short(fqn: str | None) -> str:
    return fqn.rsplit('.', 1)[-1] if fqn else '?'


def basic_ok(typ: str, value) -> bool:
    if typ == 'string':
        return isinstance(value, str)
    if typ == 'boolean':
        return isinstance(value, bool)
    if typ in ('int', 'number'):
        if isinstance(value, bool):
            return False
        if isinstance(value, str):
            try:
                value = Decimal(value)
            except InvalidOperation:
                return False
        if isinstance(value, float):
            value = Decimal(str(value))
        if not isinstance(value, (int, Decimal)):
            return False
        return typ == 'number' or Decimal(value) == Decimal(value).to_integral_value()
    if not isinstance(value, str):
        return False
    try:
        if typ == 'date':
            date.fromisoformat(value)
        elif typ == 'time':
            time.fromisoformat(value)
        else:
            datetime.fromisoformat(value.replace('Z', '+00:00').split('[')[0])
    except ValueError:
        return False
    return True


class Checker:
    '''Walks one document and collects its issues.'''

    def __init__(self, model: RuneModel):
        self.model = model
        self.issues = []
        self.keys = {}       # (kind, value, scope) -> [(type FQN, node)]
        self.refs = []       # {'targets', 'path', 'expected', 'optional', 'scope'}
        self.scope = DOCUMENT  # the path of the enclosing TradeState, or DOCUMENT

    def key_scope(self, kind: str) -> str:
        '''The scope a key or reference of this kind belongs to: '' is a global key.'''
        return DOCUMENT if kind == '' else self.scope

    def issue(self, kind: str, path: str, detail: str) -> None:
        self.issues.append({'kind': kind, 'path': path or '(root)', 'detail': detail,
                            'malformed': MALFORMED_KINDS[kind]})

    def check(self, document: dict, root: str) -> list[dict]:
        self.value(document, root, '', (1, 1), 'root')
        self.check_references()
        return self.issues

    def value(self, node, typ: str | None, path: str, cardinality, where: str) -> None:
        '''One attribute's value: a list, a single value, or absent.'''
        low, high = cardinality
        items = node if isinstance(node, list) else ([] if node is None else [node])
        if isinstance(node, list) and high == 1:
            self.issue('wrong shape', path, f'{where}: a list where the model has one value')
        present = [i for i in items if self.present(i, typ)]
        if len(present) < low:
            if not present:
                self.issue('required attribute missing', path, f'{where} ({low}..'
                           f'{"*" if high is None else high})')
            else:
                self.issue('too few items', path, f'{where}: {len(present)}, needs {low}')
        if high is not None and high > 1 and len(present) > high:
            self.issue('too many items', path, f'{where}: {len(present)}, at most {high}')
        for i, item in enumerate(items):
            self.single(item, typ, f'{path}[{i}]' if isinstance(node, list) else path,
                        low == 0, where)

    def present(self, item, typ) -> bool:
        if item is None or item == []:
            return False
        if isinstance(item, dict) and not self.is_complex(typ):
            return '@data' in item or any(k.startswith('@ref') for k in item)
        return True

    def is_complex(self, typ) -> bool:
        return typ in self.model.types or typ in self.model.choices

    def single(self, node, typ, path, optional, where) -> None:
        model = self.model
        if node is None:
            return
        if isinstance(node, dict):
            outer = self.scope
            if model.conforms(self.actual_type(node, typ), SCOPE_TYPE) is True:
                self.scope = path or '(root)'
            try:
                self.single_object(node, typ, path, optional, where)
            finally:
                self.scope = outer
            return
        if isinstance(node, list):
            self.issue('wrong shape', path, f'{where}: a nested list')
            return
        if self.is_complex(typ):
            self.issue('wrong shape', path, f'{where}: a value where {short(typ)} is an object')
            return
        self.scalar(node, typ, path, where)

    def single_object(self, node: dict, typ, path, optional, where) -> None:
        '''One value that is a JSON object, within the current key scope.'''
        model = self.model
        self.metadata(node, typ, path, optional)
        data_keys = [k for k in node if not k.startswith('@')]
        if '@data' not in node and not data_keys and any(k.startswith('@ref') for k in node):
            return  # a reference only: the referenced object is checked where it is
        if self.is_complex(typ) and '@data' in node:
            # an object with metadata, or a choice: the value is in @data
            if data_keys:
                self.issue('wrong shape', path, f'{where}: attributes beside @data')
            self.single(node['@data'], typ, path, optional, where)
            return
        if not self.is_complex(typ):
            if '@data' in node:
                self.scalar(node['@data'], typ, path, where)
            elif not any(k.startswith('@ref') for k in node):
                self.issue('metadata without a value', path, where)
            return
        declared = node.get('@type')
        if isinstance(declared, str):
            actual = model.resolve(declared)
            if actual is None or model.conforms(actual, typ) is False:
                self.issue('wrong @type', path, f'{declared} where {short(typ)} is expected')
            elif actual in model.types or actual in model.choices:
                typ = actual
        if typ in model.choices:
            self.choice(node, typ, path)
        else:
            self.object(node, typ, path)

    def scalar(self, value, typ, path, where) -> None:
        if isinstance(value, (dict, list)):
            self.issue('wrong shape', path, f'{where}: an object or list where a value is expected')
        elif typ in BASIC:
            if not basic_ok(typ, value):
                self.issue('invalid basic value', path, f'{where}: {value!r} is not a {typ}')
        elif typ in self.model.enums:
            if value not in self.model.enum_values(typ):
                self.issue('invalid enum value', path, f'{where}: {value!r} not in {short(typ)}')

    def metadata(self, node, typ, path, optional) -> None:
        targets = []
        for key, value in node.items():
            if key.startswith('@key') and isinstance(value, str):
                kind = key.replace('@key', '')
                self.keys.setdefault((kind, value, self.key_scope(kind)), []).append(
                    (self.actual_type(node, typ), node))
            elif key.startswith('@ref') and isinstance(value, str):
                targets.append((key.replace('@ref', ''), value))
        if targets:
            self.refs.append({'targets': targets, 'path': path, 'expected': typ,
                              'optional': optional, 'scope': self.scope})

    @staticmethod
    def attribute(path: str) -> str:
        '''The attribute a path ends in, without list indices.'''
        return re.sub(r'\[\d+\]', '', path).rsplit('.', 1)[-1]

    def actual_type(self, node, typ):
        '''The type of the object a node holds: its @type, or the @type inside its @data.'''
        while isinstance(node, dict):
            declared = node.get('@type')
            if isinstance(declared, str) and self.model.resolve(declared):
                typ = self.model.resolve(declared)
            node = node.get('@data')
        return typ

    def object(self, node, typ, path) -> None:
        attributes = self.model.attributes(typ) if typ in self.model.types else None
        for key, value in node.items():
            if key.startswith('@'):
                continue
            child = f'{path}.{key}' if path else key
            if attributes is None:
                continue  # a type the model doesn't describe: nothing to check against
            if key not in attributes:
                self.issue('unknown attribute', child, f'{short(typ)}.{key}')
                continue
        if attributes is None:
            return
        for name, (child_type, low, high) in attributes.items():
            child = f'{path}.{name}' if path else name
            self.value(node.get(name), child_type, child, (low, high),
                       f'{short(typ)}.{name}')

    def choice(self, node, typ, path) -> None:
        options = self.model.options(typ)
        chosen = [k for k in node if not k.startswith('@') and node[k] not in EMPTY]
        for key in chosen:
            if key not in options:
                self.issue('unknown attribute', f'{path}.{key}', f'{short(typ)}.{key}')
        valid = [k for k in chosen if k in options]
        if len(valid) != 1:
            self.issue('choice without one option', path,
                       f'{short(typ)}: {len(valid)} options ({", ".join(valid) or "none"})')
        for key in valid:
            self.single(node[key], options[key], f'{path}.{key}', False, f'{short(typ)}.{key}')

    def check_references(self) -> None:
        for (kind, value, scope), holders in self.keys.items():
            # External and scoped keys are identifiers, so they must be unique in their scope.
            # A global key is a hash of the object, so two equal objects can have the same one.
            nodes = [n for _, n in holders]
            if len(nodes) > 1 and (kind or any(nodes[0] != n for n in nodes[1:])):
                where = '' if scope == DOCUMENT else f' in the TradeState at {scope}'
                self.issue('duplicate key', '' if scope == DOCUMENT else scope,
                           f'@key{kind} on {len(nodes)} objects{where}: {value}')
        for ref in self.refs:
            names = ', '.join(f'@ref{k}={v}' for k, v in ref['targets'])
            found = [(k, v, DOCUMENT if k == '' else ref['scope']) for k, v in ref['targets']]
            found = [t for t in found if t in self.keys]
            if not found:
                kind = ('dangling reference (optional attribute)' if ref['optional']
                        else 'dangling reference (required attribute)')
                elsewhere = any(k == kind_ and v == value and k != ''
                                for k, v in ref['targets'] for kind_, value, _ in self.keys)
                self.issue(kind, ref['path'], f'{self.attribute(ref["path"])}: {names}'
                           + (' (the key is only in another scope)' if elsewhere else ''))
                continue
            keyed_type = self.keys[found[0]][0][0]
            if self.model.conforms(keyed_type, ref['expected']) is False:
                self.issue('reference to wrong type', ref['path'],
                           f'{self.attribute(ref["path"])}, {short(keyed_type)} where '
                           f'{short(ref["expected"])} is expected: {names}')
        self.ref_count = len(self.refs)


def check(document: dict, root: str, model: RuneModel) -> tuple[list[dict], int]:
    '''The issues in a document, and how many references it holds.'''
    checker = Checker(model)
    issues = checker.check(document, root)
    return issues, checker.ref_count
