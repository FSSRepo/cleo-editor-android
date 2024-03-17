package com.fastsmartsystem.cleoeditor.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fastsmartsystem.cleo.OpcodeArgument;
import com.fastsmartsystem.cleoeditor.R;
import com.fastsmartsystem.cleoeditor.code.CodeEngine;
import com.fastsmartsystem.cleoeditor.code.CodeErrorAdapter;
import com.fastsmartsystem.cleoeditor.code.CodeParam;
import com.fastsmartsystem.cleoeditor.code.OpcodeLine;
import com.github.ahmadaghazadeh.editor.processor.TextNotFoundException;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class CodeInspectorDialog extends BottomSheetDialogFragment {
    CodeEngine engine;

    public CodeInspectorDialog(CodeEngine engine) {
        this.engine = engine;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.code_inspector, container, false);
        OpcodeLine opcode = engine.getOpcodeLine();
        TextView tvCodeLine = view.findViewById(R.id.code_line);
        TextView tvCodeInfo = view.findViewById(R.id.tvCodeInfo);
        ImageView ivClose = view.findViewById(R.id.ivCloseInspector);
        ivClose.setOnClickListener((v) -> dismiss());
        LinearLayout layInfo = view.findViewById(R.id.opcodeInfo);
        LinearLayout lvParameters = view.findViewById(R.id.lvParameters);
        if(opcode != null) {
            if(opcode.info != null) {
                tvCodeLine.setText(getContext().getString(R.string.code_line, opcode.line, String.format("%04X", opcode.id), (opcode.invert ? getString(R.string.inverted_opcode) : "")));
                tvCodeInfo.setText(getString(R.string.opcode_info, opcode.info.description, (opcode.info.example.length() > 0 ? getString(R.string.opcode_example, String.format("%04X", opcode.id), opcode.info.example) : "")));
                if(opcode.info.param_count > 0) {
                    lvParameters.setVisibility(View.VISIBLE);
                    ArrayList<ParameterItem> parameters = new ArrayList<>();
                    int count = Math.max(opcode.info.param_count, opcode.arguments.size());
                    for(int i = 0; i < count; i++) {
                        CodeParam found = null;
                        OpcodeArgument expected = null;
                        String value = "";
                        if(i < opcode.arguments.size()) {
                            found = opcode.arguments.get(i);
                            if(opcode.id == 0x00D6) {
                                value = (opcode.logical_op == -1 ? 0 :
                                        opcode.logical_op == 1 ? opcode.num_conditions - 1 :
                                                opcode.num_conditions + 19) + "";
                            } else {
                                value = found.value;
                            }
                        }
                        if(i < opcode.info.params.size()) {
                            expected = opcode.info.params.get(i);
                        }
                        if(found != null && expected != null) {
                            parameters.add(new ParameterItem(value, expected.output, expected.type, found.type, expected.source, found.source));
                        } else if(expected != null) {
                            parameters.add(new ParameterItem(expected.name, false, expected.type, -1, expected.source, -1));
                        } else {
                            parameters.add(new ParameterItem(value, false, -1, found.type, -1, found.source));

                        }
                    }
                    ParameterAdapter adapter = new ParameterAdapter(getContext(), parameters);
                    final int adapterCount = adapter.getCount();
                    for (int i = 0; i < adapterCount; i++) {
                        View item = adapter.getView(i, null, null);
                        lvParameters.addView(item);
                    }
                } else {
                    lvParameters.setVisibility(View.GONE);
                }
                layInfo.setVisibility(View.VISIBLE);
            } else {
                tvCodeLine.setText(getContext().getString(R.string.code_line, opcode.line, String.format("%04X", opcode.id), (opcode.invert ? getString(R.string.inverted_opcode) : "")));
                tvCodeInfo.setText(getString(R.string.unknown_opcode_error, String.format("%04X", opcode.id)));
                lvParameters.setVisibility(View.GONE);
            }
        } else {
            tvCodeLine.setText(getContext().getString(R.string.code_line, engine.getCurrentLine(), "----", ""));
            layInfo.setVisibility(View.GONE);
        }
        TextView tvError = view.findViewById(R.id.txtErrorInspector);
        tvError.setText(engine.hasErrors() ? getString(R.string.errors) : getString(R.string.no_errors));
        ImageView ivError = view.findViewById(R.id.ivError);
        ivError.setImageResource(engine.hasErrors() ? R.drawable.ic_error : R.drawable.ic_no_error);
        LinearLayout lvErrors = view.findViewById(R.id.lvErrors);
        CodeErrorAdapter adp = new CodeErrorAdapter(getContext(), engine.errors);
        if(adp.getCount() > 15) {
            Toast.makeText(getContext(), getString(R.string.too_many_errors, adp.getCount()), Toast.LENGTH_SHORT).show();
        }
        final int adapterCount = Math.min(adp.getCount(), 15);
        for (int i = 0; i < adapterCount; i++) {
            final int pos = i;
            View item = adp.getView(i, null, null);
            item.setOnClickListener((v) -> {
                if(engine.errors.get(pos).line != -1) {
                    try {
                        engine.getEditor().gotoLine(engine.errors.get(pos).line);
                    } catch (TextNotFoundException e) {
                        Toast.makeText(getContext(), "Code Engine Failure", Toast.LENGTH_SHORT).show();
                    }
                }
                dismiss();
            });
            lvErrors.addView(item);
        }

        return view;
    }
}
