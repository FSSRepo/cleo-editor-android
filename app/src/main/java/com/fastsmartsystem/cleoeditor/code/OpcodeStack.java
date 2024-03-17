package com.fastsmartsystem.cleoeditor.code;

import com.fastsmartsystem.cleoeditor.code.OpcodeLine;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;

import java.util.ArrayList;
import java.util.Iterator;

public class OpcodeStack {
    public ArrayList<OpcodeLine> opcodes = new ArrayList<>();

    public OpcodeLine getOpcode(int index) {
        if(index == opcodes.size()) {
            opcodes.add(new OpcodeLine());
        }
        opcodes.get(index).using = true;
        return opcodes.get(index);
    }

    public OpcodeLine getOpcodeByLine(int line) {
        for(OpcodeLine op : opcodes) {
            if(op.line == line) {
                return op;
            }
        }
        return null;
    }

    public void invalidate() {
        for(OpcodeLine op : opcodes) {
            op.using = false;
        }
    }

    public void clean() {
        Iterator<OpcodeLine> it = opcodes.iterator();
        while(it.hasNext()) {
            if(!it.next().using) {
                it.remove();
            }
        }
    }
}
