package co.technove.air;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ValueType<T> {

    private static final List<ValueType<?>> internalValues = new ArrayList<>();
    public static final List<ValueType<?>> values = Collections.unmodifiableList(internalValues);

    public static final ValueType<Boolean> BOOL = new ValueType<Boolean>() {
        @Override
        public Optional<Boolean> apply(String str) {
            if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                return Optional.of(Boolean.parseBoolean(str));
            }
            return Optional.empty();
        }

        @Override
        public String serialize(Boolean value) {
            return Boolean.toString(value);
        }
    };
    public static final ValueType<Integer> INT = new ValueType<Integer>() {
        @Override
        public Optional<Integer> apply(String str) {
            if (Character.isDigit(str.charAt(0)) && !str.contains(".")) {
                return Optional.of(Integer.parseInt(str));
            }
            return Optional.empty();
        }

        @Override
        public String serialize(Integer value) {
            return value.toString();
        }
    };
    public static final ValueType<Double> DOUBLE = new ValueType<Double>() {
        @Override
        public Optional<Double> apply(String str) {
            if (Character.isDigit(str.charAt(0)) && str.contains(".")) {
                return Optional.of(Double.parseDouble(str));
            }
            return Optional.empty();
        }

        @Override
        public String serialize(Double value) {
            return value.toString();
        }
    };
    public static final ValueType<String> STRING = new ValueType<String>() {
        @Override
        public Optional<String> apply(String str) {
            if (str.length() > 2 && str.startsWith("\"")) {
                if (!str.endsWith("\"")) {
                    throw new RuntimeException("Missing ending quote");
                }
                return Optional.of(str.substring(1, str.length() - 1));
            }
            return Optional.empty();
        }

        @Override
        public String serialize(String value) {
            return "\"" + value + "\"";
        }
    };

    private ValueType() {
        internalValues.add(this);
    }

    public abstract Optional<T> apply(String str);

    public abstract String serialize(T value);

}
