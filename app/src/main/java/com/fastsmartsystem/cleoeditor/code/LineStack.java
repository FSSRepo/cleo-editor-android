package com.fastsmartsystem.cleoeditor.code;

import android.util.Log;

import com.github.ahmadaghazadeh.editor.processor.utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;

public class LineStack {
    final ArrayList<CodeLine> lines = new ArrayList<>();

    public CodeLine getLine(int index) {
        synchronized (lines) {
            if(index >= lines.size()) {
                lines.add(new CodeLine());
                return lines.get(lines.size() - 1);
            } else {
                lines.get(index).using = true;
                return lines.get(index);
            }
        }
    }

    public boolean hasModification() {
        for (CodeLine line : lines) {
            if(line.modified) {
                return true;
            }
        }
        return lines.size() == 0;
    }

    public void invalidate() {
        for(CodeLine line : lines) {
            line.using = false;
        }
    }

    public void rewind() {
        for(CodeLine line : lines) {
            line.modified = true;
        }
    }

    public void clean() {
        Iterator<CodeLine> it = lines.iterator();
        while(it.hasNext()) {
            if(!it.next().using) {
                it.remove();
            }
        }
    }
}
