"""Small class-file reader for linking controller adapters to their real libraries."""
from dataclasses import dataclass
from pathlib import Path
import re
import struct
import zipfile


class Reader:
    def __init__(self, data):
        self.data, self.offset = data, 0

    def take(self, count):
        value = self.data[self.offset:self.offset + count]
        assert len(value) == count, "Truncated class file"
        self.offset += count
        return value

    def u1(self):
        return self.take(1)[0]

    def u2(self):
        return struct.unpack(">H", self.take(2))[0]

    def u4(self):
        return struct.unpack(">I", self.take(4))[0]


@dataclass
class ClassFile:
    name: str
    access: int
    parents: list
    fields: set
    methods: set
    method_flags: dict
    references: list
    classes: set
    mixin_targets: list
    injections: list


def read_class(data):
    reader = Reader(data)
    assert reader.take(4) == b"\xca\xfe\xba\xbe", "Invalid class file"
    reader.take(4)  # minor/major; the constant-pool format is stable across supported JDKs
    pool = [None] * reader.u2()
    index = 1
    while index < len(pool):
        tag = reader.u1()
        if tag == 1:
            pool[index] = (tag, reader.take(reader.u2()).decode("utf-8", errors="replace"))
        elif tag in (3, 4):
            pool[index] = (tag, reader.take(4))
        elif tag in (5, 6):
            pool[index] = (tag, reader.take(8))
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, reader.u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, reader.u2(), reader.u2())
        elif tag == 15:
            pool[index] = (tag, reader.u1(), reader.u2())
        else:
            raise AssertionError(f"Unknown constant-pool tag {tag}")
        index += 1

    def utf8(position):
        assert pool[position][0] == 1
        return pool[position][1]

    def class_name(position):
        return utf8(pool[position][1]) if position else None

    def annotation_value(source):
        tag = chr(source.u1())
        if tag in "BCDFIJSZs":
            value = pool[source.u2()]
            return value[1]
        if tag == "e":
            return (utf8(source.u2()), utf8(source.u2()))
        if tag == "c":
            return utf8(source.u2())
        if tag == "@":
            return annotation(source)
        if tag == "[":
            return [annotation_value(source) for _ in range(source.u2())]
        raise AssertionError(f"Unknown annotation tag {tag}")

    def annotation(source):
        name = utf8(source.u2())
        values = {}
        for _ in range(source.u2()):
            key = utf8(source.u2())
            values[key] = annotation_value(source)
        return name, values

    def attributes(source):
        annotations = []
        for _ in range(source.u2()):
            name, raw = utf8(source.u2()), source.take(source.u4())
            if name in ("RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations"):
                nested = Reader(raw)
                annotations.extend(annotation(nested) for _ in range(nested.u2()))
        return annotations

    access = reader.u2()
    name, parent = class_name(reader.u2()), class_name(reader.u2())
    parents = ([parent] if parent else []) + [class_name(reader.u2()) for _ in range(reader.u2())]
    definitions, method_annotations, method_flags = [], [], {}
    for kind in ("field", "method"):
        members = set()
        for _ in range(reader.u2()):
            flags = reader.u2()
            member = (utf8(reader.u2()), utf8(reader.u2()))
            members.add(member)
            found = attributes(reader)
            if kind == "method":
                method_flags[member] = flags
                method_annotations.extend(found)
        definitions.append(members)
    class_annotations = attributes(reader)
    references, classes = [], set()
    for position, entry in enumerate(pool[1:], 1):
        if entry is None:
            continue
        if entry[0] == 7:
            classes.add(class_name(position))
        elif entry[0] == 1:
            classes.update(re.findall(r"L([A-Za-z0-9_$/]+)[;<]", entry[1]))
        elif entry[0] in (9, 10, 11):
            owner = class_name(entry[1])
            pair = pool[entry[2]]
            references.append(("field" if entry[0] == 9 else "method", owner, utf8(pair[1]), utf8(pair[2])))
    mixin_targets = []
    for annotation_name, values in class_annotations:
        if annotation_name == "Lorg/spongepowered/asm/mixin/Mixin;":
            targets = values.get("value", [])
            if isinstance(targets, str):
                targets = [targets]
            mixin_targets.extend(value[1:-1] for value in targets)
            targets = values.get("targets", [])
            if isinstance(targets, str):
                targets = [targets]
            mixin_targets.extend(value.replace(".", "/") for value in targets)
    injections = []
    for annotation_name, values in method_annotations:
        if annotation_name.startswith("Lorg/spongepowered/asm/mixin/injection/"):
            selectors = values.get("method", [])
            if isinstance(selectors, str):
                selectors = [selectors]
            injections.extend(selectors)
    return ClassFile(name, access, parents, definitions[0], definitions[1], method_flags, references, classes,
                     mixin_targets, injections)


class ClassIndex:
    """Reads only referenced classes; Minecraft/library archives can be large."""
    def __init__(self, paths):
        self.archives, self.locations, self.cache = [], {}, {}
        for item in paths:
            path = Path(item)
            if path.is_dir():
                for file in path.rglob("*.class"):
                    self.locations.setdefault(file.relative_to(path).as_posix()[:-6], file)
            elif path.is_file() and path.suffix == ".jar":
                archive = zipfile.ZipFile(path)
                self.archives.append(archive)
                for name in archive.namelist():
                    if name.endswith(".class") and not name.startswith("META-INF/versions/"):
                        self.locations.setdefault(name[:-6], (archive, name))

    def close(self):
        for archive in self.archives:
            archive.close()

    def get(self, name):
        if name not in self.cache:
            location = self.locations.get(name)
            if location is None:
                return None
            data = location.read_bytes() if isinstance(location, Path) else location[0].read(location[1])
            self.cache[name] = read_class(data)
        return self.cache[name]

    def member(self, owner, kind, name, descriptor=None, visited=None):
        visited = set() if visited is None else visited
        if owner in visited:
            return False
        visited.add(owner)
        clazz = self.get(owner)
        if clazz is None:
            return False
        members = clazz.fields if kind == "field" else clazz.methods
        if any(member_name == name and (descriptor is None or member_desc == descriptor)
               for member_name, member_desc in members):
            return True
        if name == "<init>":
            return False
        return any(self.member(parent, kind, name, descriptor, visited) for parent in clazz.parents)

    def concrete_method(self, owner, member, visited=None):
        visited = set() if visited is None else visited
        if owner in visited:
            return False
        visited.add(owner)
        clazz = self.get(owner)
        if clazz is None:
            return False
        flags = clazz.method_flags.get(member)
        if flags is not None and not flags & 0x0400:  # ACC_ABSTRACT
            return True
        return any(self.concrete_method(parent, member, visited) for parent in clazz.parents)

    def native_obligations(self, clazz, prefixes):
        """Abstract contracts can change without appearing in a call site's constant pool."""
        visited, pending, required = set(), list(clazz.parents), set()
        while pending:
            name = pending.pop()
            if name in visited:
                continue
            visited.add(name)
            ancestor = self.get(name)
            if ancestor is None:
                continue
            if name.startswith(prefixes):
                required.update((name, member) for member, flags in ancestor.method_flags.items() if flags & 0x0400)
            pending.extend(ancestor.parents)
        return required
