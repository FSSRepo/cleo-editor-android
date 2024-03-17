package com.fastsmartsystem.cleoeditor.views;

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
import com.github.ahmadaghazadeh.editor.processor.language.CLEOLanguage;

import java.util.ArrayList;

public class ParameterAdapter extends ArrayAdapter<ParameterItem> {
    LayoutInflater inflater;

    public ParameterAdapter(Context context, ArrayList<ParameterItem> parameters){
        super(context, R.layout.opcode_parameter_item, parameters);
        inflater = LayoutInflater.from(context);
    }

    public String getSourceName(int source) {
        switch (source) {
            case 0:
                return getContext().getString(R.string.global_var);
            case 1:
                return "Literal";
            case 2:
                return getContext().getString(R.string.local_var);
            case -1:
            case 3:
                return getContext().getString(R.string.any_var);
        }
        return "Unknown";
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.opcode_parameter_item, null);
        }
        final ParameterItem item = getItem(position);
        TextView paramName = convertView.findViewById(R.id.parameter_name);
        TextView paramExpected = convertView.findViewById(R.id.param_exp);
        TextView paramFound = convertView.findViewById(R.id.param_found);
        ImageView ivDirection = convertView.findViewById(R.id.parameter_direction);
        if(item.type_expected == -1 && item.source_expected == -1) { // parameter not required
            ivDirection.setImageResource(R.drawable.ic_error);
            paramName.setText(item.name);
            paramExpected.setText(getContext().getString(R.string.expected_param, getContext().getString(R.string.undefined), getContext().getString(R.string.undefined)));
            paramFound.setText(getContext().getString(R.string.found_param, CLEOLanguage.types[item.type_found],getSourceName(item.source_found)));
            return convertView;
        } else if(item.type_found == -1 && item.source_found == -1) { // parameter found required
            ivDirection.setImageResource(R.drawable.ic_error);
            paramName.setText(getContext().getString(R.string.undefined));
            paramExpected.setText(getContext().getString(R.string.expected_param, CLEOLanguage.types[item.type_expected], getSourceName(item.source_expected)));
            paramFound.setText(getContext().getString(R.string.found_param, getContext().getString(R.string.undefined), getContext().getString(R.string.undefined)));
            return convertView;
        }
        paramName.setText(item.name);
        if(item.output) {
            ivDirection.setImageResource(R.drawable.ic_parameter_out);
        } else { // opcode not required
            ivDirection.setImageResource(R.drawable.ic_parameter_in);
        }
        paramExpected.setText(getContext().getString(R.string.expected_param, CLEOLanguage.types[item.type_expected], getSourceName(item.source_expected)));
        paramFound.setText(getContext().getString(R.string.found_param, CLEOLanguage.types[item.type_found], getSourceName(item.source_found)));
        return convertView;
    }
}
