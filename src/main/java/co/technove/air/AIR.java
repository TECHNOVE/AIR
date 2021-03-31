package co.technove.air;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// todo probably needs lists eventually
public class AIR {
    private final Map<String, Section> sections = new LinkedHashMap<>();

    private static class ManualObject {
        public final String key;
        public final List<String> comments;

        private ManualObject(String key, List<String> comments) {
            this.key = key;
            this.comments = comments == null ? new ArrayList<>() : comments;
        }
    }

    private static class Section extends ManualObject {
        public final Map<String, Value<?>> values;

        private Section(String key, List<String> comments) {
            super(key, comments);
            this.values = new LinkedHashMap<>();
        }

        public void add(String key, Value<?> value) {
            this.values.put(key, value);
            value.parent = this;
        }

        public <T> Value<T> get(String key, ValueType<T> type) {
            Value<?> val = this.values.computeIfAbsent(key, k -> {
                Value<T> value = new Value<>(type, k, null, null);
                value.parent = this;
                return value;
            });
            if (val.type != type) {
                throw new IllegalArgumentException("Failed to retrieve value for " + key + " of type " + type + " when type is already " + val.type);
            }
            return (Value<T>) val;
        }
    }

    static class Value<T> extends ManualObject {
        public ValueType<T> type;
        public T value;
        public Section parent;

        private Value(ValueType<T> type, String key, T value, List<String> comments) {
            super(key, comments);
            this.type = type;
            this.value = value;
        }

        public String serialize() {
            if (this.type == null) {
                throw new RuntimeException("Cannot serialize unknown value");
            }
            return this.type.serialize(this.value);
        }
    }

    public AIR(){}

    public AIR(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            Section currentSection = null;
            List<String> currentComment = new ArrayList<>();
            String listKey = null;
            List<Object> currentList = null;

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() == 0) {
                    continue; // empty line
                }

                if (line.startsWith("#")) {
                    currentComment.add(line.substring(1).trim());
                } else if (line.startsWith("[")) {
                    if (!line.endsWith("]")) {
                        throw new IllegalArgumentException("Invalid configuration, section '" + line + "' does not end with ]");
                    }
                    if (line.length() < 3) {
                        throw new IllegalArgumentException("Invalid configuration, section '" + line + "' does not have a name");
                    }
                    String sectionName = line.substring(1, line.length() - 1);
                    Section newSection = new Section(sectionName, currentComment);
                    currentComment = new ArrayList<>();
                    currentSection = newSection;
                    this.sections.put(sectionName, newSection);
                } else {
                    if (currentSection == null) {
                        throw new IllegalArgumentException("Invalid configuration, found value outside of section " + line);
                    }
                    String key;
                    String value;

                    if (currentList == null) {
                        int equals = line.indexOf("=");
                        if (equals <= 1 || equals == line.length() - 1) {
                            throw new IllegalArgumentException("Invalid configuration, assignment invalid " + line);
                        }

                        key = line.substring(0, equals).trim();
                        value = line.substring(equals + 1).trim();

                        if (value.length() == 0) {
                            throw new IllegalArgumentException("Invalid configuration, value does not exist " + line);
                        }
                        if (value.equals("[")) {
                            // start reading list
                            listKey = key;
                            currentList = new ArrayList<>();
                            continue;
                        }

                    } else {
                        key = null;
                        value = line.trim();

                        if (value.equals("]")) {
                            currentSection.add(listKey, new Value(ValueType.LIST, listKey, currentList, currentComment));
                            currentList = null;
                            listKey = null;
                            continue;
                        }
                    }

                    boolean found = false;
                    for (ValueType<?> valueType : ValueType.values) {
                        Optional<?> possible = valueType.apply(value);
                        if (possible.isPresent()) {
                            found = true;

                            if (currentList == null) {
                                currentSection.add(key, new Value(valueType, key, possible.get(), currentComment));
                            } else {
                                currentList.add(new Value(valueType, listKey, possible.get(), Collections.emptyList()));
                            }
                        }
                    }
                    if (!found) {
                        throw new IllegalArgumentException("Invalid configuration, unknown type for " + line);
                    }
                    if (!currentComment.isEmpty()) {
                        currentComment = new ArrayList<>();
                    }
                }
            }
        }
    }

    public void save(OutputStream stream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {
            for (Map.Entry<String, Section> entry : this.sections.entrySet()) {
                Section section = entry.getValue();
                if (section.comments != null) {
                    for (String comment : section.comments) {
                        writer.write("# " + comment + "\n");
                    }
                }
                writer.write("[" + section.key + "]" + "\n");
                for (Value value : section.values.values()) {
                    if (value.comments != null) {
                        for (String comment : value.comments) {
                            writer.write("  # " + comment + "\n");
                        }
                    }
                    writer.write("  " + value.key + " = " + value.serialize() + "\n");
                }
                writer.write("\n");
            }
        }
    }

    private ManualObject getObject(ValueType<?> type, String key) {
        String[] split = key.split("\\.", 2);
        if (split.length == 1) {
            return this.sections.computeIfAbsent(key, k -> new Section(k, null));
        }
        return this.sections.computeIfAbsent(split[0], k -> new Section(k, null)).get(split[1], type);
    }

    public void setComment(String key, String... comment) {
        String[] split = key.split("\\.", 2);
        if (!this.sections.containsKey(split[0])) {
            throw new IllegalArgumentException("Cannot set comment for section that does not exist.");
        }
        ManualObject object = this.sections.get(split[0]);
        object.comments.clear();
        object.comments.addAll(Arrays.asList(comment));
    }

    private <T> T get(ValueType<T> type, String key, T defaultValue, String... comment) {
        String[] split = key.split("\\.", 2);
        if (split.length == 1) {
            throw new IllegalArgumentException("Key " + key + " does not include section");
        }
        Section section = this.sections.computeIfAbsent(split[0], k -> new Section(k, null));
        if (!section.values.containsKey(split[1])) {
            Value value = section.get(split[1], type);
            value.value = defaultValue;
            value.comments.addAll(Arrays.asList(comment));
            return defaultValue;
        }
        Value value = section.get(split[1], type);
        if (value.type != type) {
            throw new IllegalArgumentException("Failed to retrieve " + key + " because it already exists with type " + value.type + " when requested type is " + type);
        }
        if (value.comments.isEmpty()) {
            value.comments.addAll(Arrays.asList(comment));
        }
        return (T) value.value;
    }

    public boolean getBoolean(String key, boolean defaultValue, String... comment) {
        return this.get(ValueType.BOOL, key, defaultValue, comment);
    }

    public int getInt(String key, int defaultValue, String... comment) {
        return this.get(ValueType.INT, key, defaultValue, comment);
    }

    public double getDouble(String key, double defaultValue, String... comment) {
        return this.get(ValueType.DOUBLE, key, defaultValue, comment);
    }

    public String getString(String key, String defaultValue, String... comment) {
        return this.get(ValueType.STRING, key, defaultValue, comment);
    }

    public <T> List<T> getList(String key, ValueType<T> type, List<T> defaultValue, String... comment) throws IOException {
        String[] split = key.split("\\.", 2);
        if (split.length == 1) {
            throw new IllegalArgumentException("Key " + key + " does not include section");
        }
        Section section = this.sections.computeIfAbsent(split[0], k -> new Section(k, null));
        if (!section.values.containsKey(split[1])) {
            Value<List<AIR.Value<?>>> value = section.get(split[1], ValueType.LIST);
            value.value = defaultValue.stream().map(val -> new Value<>(type, null, val, null)).collect(Collectors.toList());
            value.comments.addAll(Arrays.asList(comment));
            return defaultValue;
        }
        Value<List<AIR.Value<?>>> value = section.get(split[1], ValueType.LIST);
        if (value.comments.isEmpty()) {
            value.comments.addAll(Arrays.asList(comment));
        }
        List<Value<?>> list = value.value;
        for (Value<?> val : list) {
            if (val.type != type) {
                throw new IOException("Found invalid type " + val.type + " when looking for " + type);
            }
        }
        return list.stream().map(val -> (T) val.value).collect(Collectors.toList());
    }

    public <T> void setList(ValueType<T> listType, String key, List<T> value) {
        ManualObject object = getObject(ValueType.LIST, key);
        if (!(object instanceof Value)) {
            throw new IllegalArgumentException("Invalid key for value " + key);
        }
        ((Value<List<Value<T>>>) object).value = value.stream().map(val -> new Value<T>(listType, null, val, null)).collect(Collectors.toList());
    }

    public <T> void set(ValueType<T> type, String key, T value) {
        ManualObject object = getObject(type, key);
        if (!(object instanceof Value)) {
            throw new IllegalArgumentException("Invalid key for value " + key);
        }
        ((Value) object).value = value;
    }

}
