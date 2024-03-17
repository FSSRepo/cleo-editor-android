package com.fastsmartsystem.cleo.compiler;

import com.fastsmartsystem.cleo.OpcodeInfo;

import java.util.ArrayList;

public class OpcodeProcessed {
    public int id = 0;
    public boolean invert = false;
    public int code_address = 0;
    public ArrayList<String> arguments = new ArrayList<>();
    public OpcodeInfo info;
}