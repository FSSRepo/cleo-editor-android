package com.fastsmartsystem.cleoeditor.code;

import android.util.Log;

import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionAdapter;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.processor.TextProcessor;
import com.github.ahmadaghazadeh.editor.processor.utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;

public class CodeSuggestions {

    private final String[] default_constants = {
            "$CLEO",
            ".cs",
            ".csa",
            ".csi"
    };
    private ArrayList<SuggestionItem> suggestions;
    private int constants_index = -1;

    protected CodeSuggestions(TextProcessor processor) {
        suggestions = ((SuggestionAdapter)processor.getAdapter()).getAllItems();
        for(String constant : default_constants) {
            suggestions.add(new SuggestionItem(SuggestionType.TYPE_CONSTANT, constant));
        }
    }

    public void finish() {
        constants_index = suggestions.size();
    }

    public void add(SuggestionItem item) {
        suggestions.add(item);
    }

    public int exist(String name) {
        for(int i = constants_index; i < suggestions.size(); i++) {
            String comp_ =suggestions.get(i).getName();
            if(name.contains(comp_) &&
                    name.length() == comp_.length()) {
                suggestions.get(i).use_count++;
                return suggestions.get(i).use_count;
            }
        }
        return -1;
    }

    public boolean existAddress(String name) {
        for(int i = constants_index; i < suggestions.size(); i++) {
            String comp_ = suggestions.get(i).getName();
            if(suggestions.get(i).getType() == SuggestionType.TYPE_ADDRESS && name.contains(comp_) &&
                    name.length() == comp_.length()) {
                return true;
            }
        }
        return false;
    }

    public boolean isConstant(String name) {
        for(int i = default_constants.length; i < constants_index; i++) {
            if(name.contains(suggestions.get(i).getName())) {
                return true;
            }
        }
        return false;
    }

    public void invalidate() {
        for(int i = constants_index; i < suggestions.size(); i++) {
            suggestions.get(i).use_count = 0;
        }
    }

    protected void rewind() {
        Iterator<SuggestionItem> it = suggestions.iterator();
        while(it.hasNext()) {
            if(it.next().use_count == 0) {
                it.remove();
            }
        }
    }
}
