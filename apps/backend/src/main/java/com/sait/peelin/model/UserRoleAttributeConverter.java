package com.sait.peelin.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link UserRole} to PostgreSQL's native {@code user_role} enum.
 * <p>
 * Hibernate 6 {@code SqlTypes.NAMED_ENUM} maps the Java type to PG type {@code userrole}, which
 * does not exist ({@code user_role} is the real type). Plain strings are rejected (varchar vs enum).
 * We bind the driver's {@code PGobject} via reflection so this module compiles without a compile-time
 * dependency on {@code org.postgresql} (IDE + Maven stay aligned; driver remains on the runtime classpath).
 */
@Converter(autoApply = false)
public class UserRoleAttributeConverter implements AttributeConverter<UserRole, Object> {

    private static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";

    @Override
    public Object convertToDatabaseColumn(UserRole attribute) {
        if (attribute == null) {
            return null;
        }
        return toPgEnumObject(attribute.name());
    }

    @Override
    public UserRole convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        String v = extractEnumLabel(dbData);
        if (v == null || v.isBlank()) {
            return null;
        }
        return UserRole.valueOf(v.trim());
    }

    private static Object toPgEnumObject(String label) {
        try {
            Class<?> pgClass = Class.forName(PG_OBJECT_CLASS);
            Object pg = pgClass.getDeclaredConstructor().newInstance();
            pgClass.getMethod("setType", String.class).invoke(pg, "user_role");
            pgClass.getMethod("setValue", String.class).invoke(pg, label);
            return pg;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "PostgreSQL JDBC driver is required to persist user_role (install org.postgresql:postgresql).",
                    e);
        }
    }

    private static String extractEnumLabel(Object dbData) {
        if (dbData instanceof String s) {
            return s;
        }
        if (PG_OBJECT_CLASS.equals(dbData.getClass().getName())) {
            try {
                Object val = dbData.getClass().getMethod("getValue").invoke(dbData);
                return val != null ? val.toString() : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return dbData.toString();
    }
}
