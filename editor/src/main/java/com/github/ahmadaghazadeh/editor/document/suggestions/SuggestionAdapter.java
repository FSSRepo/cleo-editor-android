/*
 * Copyright (C) 2018 Light Team Software
 *
 * This file is part of ModPE IDE.
 *
 * ModPE IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ModPE IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ahmadaghazadeh.editor.document.suggestions;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;


import com.github.ahmadaghazadeh.editor.R;
import com.github.ahmadaghazadeh.editor.processor.language.CLEOLanguage;

import java.util.ArrayList;

/**
 * @author Trần Lê Duy
 */
public class SuggestionAdapter extends ArrayAdapter<SuggestionItem> {

    private LayoutInflater inflater;
    private ArrayList<SuggestionItem> clone;
    private ArrayList<SuggestionItem> suggestion;

    @LayoutRes
    private int resourceID;

    private Filter suggestionFilter = new Filter() {

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            if (resultValue == null) {
                return "";
            }
            return ((SuggestionItem) resultValue).getName();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            suggestion.clear();
            if (constraint != null) {
                for (SuggestionItem item : clone) {
                    if (item.compareTo(constraint.toString()) == 0) {
                        suggestion.add(item);
                    }
                }
                filterResults.values = suggestion;
                filterResults.count = suggestion.size();
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<SuggestionItem> filteredList = (ArrayList<SuggestionItem>) results.values;
            clear();
            if (filteredList != null && filteredList.size() > 0) {
                addAll(filteredList);
            }
            notifyDataSetChanged();
        }
    };

    public SuggestionAdapter(@NonNull Context context,
                             @LayoutRes int resource) {
        super(context, resource, new ArrayList<>());
        inflater = LayoutInflater.from(context);
        clone = new ArrayList<>();
        suggestion = new ArrayList<>();
        resourceID = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(resourceID, null);
        }
        final SuggestionItem item = getItem(position);

        TextView suggestionTitle = convertView.findViewById(R.id.suggestion_title);
        TextView suggestionClass = convertView.findViewById(R.id.suggestion_class);
        ImageView suggestionImage = convertView.findViewById(R.id.suggestion_type);
        assert item != null;
        suggestionTitle.setText(item.getName());
        switch (item.getType()) {
            case SuggestionType.TYPE_CONSTANT: //Constant
                suggestionImage.setImageResource(R.drawable.ic_constant);
                suggestionClass.setText("");
                break;
            case SuggestionType.TYPE_VARIABLE: //Variable
                suggestionImage.setImageResource(R.drawable.ic_variable);
                suggestionClass.setText("");
                break;
            case SuggestionType.TYPE_MODIFIER: //Modifier
                suggestionImage.setImageResource(R.drawable.ic_model);
                suggestionClass.setText(" : int");
                break;
            case SuggestionType.TYPE_ADDRESS: //Address
                suggestionImage.setImageResource(R.drawable.ic_label);
                suggestionClass.setText(" : int");
                break;
        }
        if(item.class_type != -1) {
            suggestionClass.setText(" : "+ CLEOLanguage.types[item.class_type]);
        }
        return convertView;
    }

    public ArrayList<SuggestionItem> getAllItems() {
        return clone;
    }
    /*

    public void clearAllData() {
        super.clear();
        clone.clear();
    }

    public void addData(@NonNull Collection<? extends SuggestionItem> collection) {
        addAll(collection);
        clone.addAll(collection);
    }*/

    @NonNull
    @Override
    public Filter getFilter() {
        return suggestionFilter;
    }
}
