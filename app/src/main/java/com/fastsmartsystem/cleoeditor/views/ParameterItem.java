package com.fastsmartsystem.cleoeditor.views;

public class ParameterItem {
    public String name;
    public boolean output;
    public int type_expected;
    public int type_found;
    public int source_expected;
    public int source_found;

    public ParameterItem(String name,boolean out,int type_expected,int type_found, int source_expected, int source_found) {
        this.name = name;
        this.output = out;
        this.type_expected = type_expected;
        this.type_found = type_found;
        this.source_expected = source_expected;
        this.source_found = source_found;
    }
}
