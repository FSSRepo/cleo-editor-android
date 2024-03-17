package com.fastsmartsystem.cleoeditor.code;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fastsmartsystem.cleoeditor.R;

import java.util.ArrayList;

public class CodeErrorAdapter extends ArrayAdapter<CodeError> {
    LayoutInflater inflater;

    public CodeErrorAdapter(Context context,ArrayList<CodeError> errors){
        super(context, R.layout.code_error_item, errors);
        inflater = LayoutInflater.from(context);
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.code_error_item, null);
        }
        final CodeError item = getItem(position);
        TextView suggestionTitle = convertView.findViewById(R.id.code_error_title);
        assert item != null;
        if(item.line != -1) {
            suggestionTitle.setText(getContext().getString(R.string.code_line, item.line, item.info, ""));
        } else {
            suggestionTitle.setText(item.info);
        }
        return convertView;
    }
}
