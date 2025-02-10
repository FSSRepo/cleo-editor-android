package com.github.ahmadaghazadeh.editor.adapter;

public class Symbol {
    private String showText;
    private String writeText;
    private int position;

    public Symbol(String showText, String writeText, int position) {
        this.showText = showText;
        this.writeText = writeText;
        this.position = position;
    }

    public String getShowText() {
        return showText;
    }

    public String getWriteText() {
        return writeText;
    }

    public int getPosition() {
        return position;
    }
}
