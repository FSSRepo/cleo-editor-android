package com.fastsmartsystem.cleoeditor.code;

import com.fastsmartsystem.cleo.OpcodeInfo;

import java.util.ArrayList;

public class OpcodeLine {
    public int id;
    public boolean invert;
    public boolean using = false;
    public ArrayList<CodeParam> arguments = new ArrayList<>();
    public OpcodeInfo info;
    public int num_conditions = 0;
    public int logical_op = 0;

    public int line = 0;
}
