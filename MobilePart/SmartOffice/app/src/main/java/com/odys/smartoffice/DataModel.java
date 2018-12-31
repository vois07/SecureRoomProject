package com.odys.smartoffice;

public class DataModel {
    private String name;
    private String value;

    public DataModel(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
