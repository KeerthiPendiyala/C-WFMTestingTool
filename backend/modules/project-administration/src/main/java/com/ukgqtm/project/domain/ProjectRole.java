package com.ukgqtm.project.domain;

import java.util.Arrays;

public enum ProjectRole {
    TEST_MANAGER("Test Manager"),
    TEST_LEAD("Test Lead"),
    TEST_ANALYST("Test Analyst");

    private final String databaseValue;

    ProjectRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public boolean hasDatabaseValue(String value) {
        return databaseValue.equals(value);
    }

    public static ProjectRole fromDatabaseValue(String value) {
        return Arrays.stream(values())
                .filter(role -> role.hasDatabaseValue(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported project role."));
    }
}
