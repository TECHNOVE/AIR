package co.technove.air;

import java.io.*;
import java.util.*;

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

        public Value<?> get(String key) {
            return this.values.computeIfAbsent(key, k -> {
                Value<?> value = new Value<>(null, k, null, null);
                value.parent = this;
                return value;
            });
        }
    }

    private static class Value<T> extends ManualObject {
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
                    int equals = line.indexOf("=");
                    if (equals <= 1 || equals == line.length() - 1) {
                        throw new IllegalArgumentException("Invalid configuration, assignment invalid " + line);
                    }

                    String key = line.substring(0, equals).trim();

                    String value = line.substring(equals + 1).trim();
                    if (value.length() == 0) {
                        throw new IllegalArgumentException("Invalid configuration, value does not exist " + line);
                    }
                    boolean found = false;
                    for (ValueType<?> valueType : ValueType.values) {
                        Optional<?> possible = valueType.apply(value);
                        if (possible.isPresent()) {
                            found = true;

                            currentSection.add(key, new Value(valueType, key, possible.get(), currentComment));
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
                    writer.write("  " + value.key + " = " + serialize(value.value) + "\n");
                }
                writer.write("\n");
            }
        }
    }

    private ManualObject getObject(String key) {
        String[] split = key.split("\\.", 2);
        if (split.length == 1) {
            return this.sections.computeIfAbsent(key, k -> new Section(k, null));
        }
        return this.sections.computeIfAbsent(split[0], k -> new Section(k, null)).get(split[1]);
    }

    public void setComment(String key, String... comment) {
        ManualObject object = this.getObject(key);
        object.comments.clear();
        object.comments.addAll(Arrays.asList(comment));
    }

    public <T> T get(String key, T defaultValue, String... comment) {
        String[] split = key.split("\\.", 2);
        if (split.length == 1) {
            throw new IllegalArgumentException("Key " + key + " does not include section");
        }
        Section section = this.sections.computeIfAbsent(split[0], k -> new Section(k, null));
        if (!section.values.containsKey(split[1])) {
            Value value = section.get(split[1]);
            value.value = defaultValue;
            value.comments.addAll(Arrays.asList(comment));
            return defaultValue;
        }
        Value value = section.get(split[1]);
        if (value.comments.isEmpty()) {
            value.comments.addAll(Arrays.asList(comment));
        }
        return (T) value.value;
    }

    public void set(String key, Object value) {
        ManualObject object = getObject(key);
        if (!(object instanceof Value)) {
            throw new IllegalArgumentException("Invalid key for value " + key);
        }
        ((Value) object).value = value;
    }

    private String serialize(Object object) {
        if (object instanceof String) {
            return "\"" + object + "\"";
        }
        return String.valueOf(object);
    }

}
