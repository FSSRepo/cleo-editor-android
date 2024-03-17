package com.fastsmartsystem.cleoeditor.code;

public class CodeError {
    public String info;
    public int line;

    public CodeError(String info, int line) {
        this.info = info;
        this.line = line;
    }
}
