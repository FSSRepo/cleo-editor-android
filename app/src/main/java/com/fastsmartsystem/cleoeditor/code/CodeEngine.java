package com.fastsmartsystem.cleoeditor.code;

import android.util.Log;
import android.widget.Toast;

import com.fastsmartsystem.cleo.IDECollector;
import com.fastsmartsystem.cleo.OpcodeArgument;
import com.fastsmartsystem.cleo.OpcodeInfo;
import com.fastsmartsystem.cleo.OpcodesLoader;
import com.fastsmartsystem.cleoeditor.MainActivity;
import com.fastsmartsystem.cleoeditor.R;
import com.github.ahmadaghazadeh.editor.OnTabClickListener;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionItem;
import com.github.ahmadaghazadeh.editor.document.suggestions.SuggestionType;
import com.github.ahmadaghazadeh.editor.processor.TextProcessor;
import com.github.ahmadaghazadeh.editor.widget.CodeEditor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEngine implements CodeEditor.ICodeEditorTextChange, OnTabClickListener {
    private TextProcessor processor;
    private CodeEditor editor;

    QueueEngineListener listener;

    private final ArrayList<CodeError> stack_errors = new ArrayList<>();
    public ArrayList<CodeError> errors = new ArrayList<>();
    private CodeSuggestions suggestions;
    private OpcodeStack opcode_stack;
    private MainActivity app;
    private int line_idx = 0;
    private LineStack line_stack;
    private OpcodesLoader scm;
    private OpcodeLine if_waiting;
    private static final String pattern_arguments = "[sv][$@]\\d+|\\$\\w+|\\#\\w+|\\@\\w+|\\s\\d+\\@|\\s\\d+\\.\\d+|-[\\d.]+|\\s\\d+|'(.*?)'|\"(.*?)\"";
    private static final String pattern_var = "\\d+\\@|[sv][$@]\\d+|\\$\\w+";
    public String format;
    public boolean running = false;

    private String latent_opcode;

    public CodeEngine(MainActivity app, CodeEditor editor, IDECollector ide, OpcodesLoader scm) {
        this.app = app;
        this.editor = editor;
        this.scm = scm;
        this.editor.setOnTextChange(this);
        this.editor.tab_listener = this;
        this.processor = editor.getTextProcessor();
        suggestions = new CodeSuggestions(processor);
        opcode_stack = new OpcodeStack();
        line_stack = new LineStack();
        for(IDECollector.IDEItem item : ide.items) {
            suggestions.add(new SuggestionItem(item.global_var ?
                    SuggestionType.TYPE_CONSTANT : SuggestionType.TYPE_MODIFIER, (!item.global_var ? "#" : "") + item.dff));
        }
        suggestions.finish();
    }

    public CodeEditor getEditor() {
        return editor;
    }

    public void enqueue(QueueEngineListener listener) {
        if(!running) {
            listener.Enqueue();
        } else {
            this.listener = listener;
        }
    }

    public boolean hasErrors() { return errors.size() > 0; }

    protected void updateUI() {
        editor.post(() -> {
            processor.errors.clear();
            for(CodeError err : errors) {
                if(err.line != -1) {
                    processor.errors.add(err.line - 1);
                }
            }
            app.updateMenuIcon();
        });
    }

    public int getCurrentLine() {
        if(processor.getSelectionStart() != processor.getSelectionEnd()) {
            return -1;
        }
        return editor.getLineForIndex(processor.getSelectionStart()) + 1;
    }

    public OpcodeLine getOpcodeLine() {
        int line_current = getCurrentLine();
        if(line_current != -1) {
            return opcode_stack.getOpcodeByLine(line_current);
        }
        return null;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            while(running) {
                if(line_stack.hasModification()) {
                    // process code
                    processCode();
                    // update interface
                    updateUI();
                }
                if(listener != null) {
                    listener.Enqueue();
                    listener = null;
                    running = false;
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    private void processCode() {
        format = null;
        String[] lines = editor.getText().replaceAll("//.*", "").split("\n");
        suggestions.invalidate();
        opcode_stack.invalidate();
        line_stack.invalidate();
        int opcode_num = 0;
        for (line_idx = 1; line_idx <= editor.getLineCount(); line_idx++) {
            CodeLine line_state = line_stack.getLine(line_idx - 1);
            assert line_state != null;
            line_state.modified = false;
            if(line_idx - 1 >= lines.length) {
                continue;
            }
            String line = lines[line_idx - 1];
            if(line.length() > 0) {
                if (line.matches("\\{\\$CLEO\\s\\.cs[ais]?\\}")) {
                    format = getFormat(line);
                }
                // check label -> code address
                else if (line.startsWith(":") && line.length() > 1) {
                    processLabel(line);
                }
                else if(line.matches("[0-9A-Fa-f]+\\:.*")) {
                    String[] parts = line.split(":");
                    OpcodeLine opcode_m = opcode_stack.getOpcode(opcode_num);
                    opcode_num++;
                    opcode_m.using = true;
                    opcode_m.id = checkOpcode(parts[0]);
                    opcode_m.line = line_idx;
                    opcode_m.invert = opcode_m.id > 0x7FFF;
                    opcode_m.info = scm.ops.get(opcode_m.invert ? (opcode_m.id - 0x8000) : opcode_m.id);
                    if(opcode_m.info == null) {
                        // Unknown opcode
                        stack_errors.add(new CodeError(app.getString(R.string.unknown_opcode_error, parts[0]), line_idx));
                        continue;
                    }
                    if(if_waiting != null) {
                        processIf(opcode_m.info);
                    }
                    if(line.contains("not ") && !opcode_m.invert) {
                        stack_errors.add(new CodeError(app.getString(R.string.no_inverted_opcode), line_idx));
                        continue;
                    }
                    String code_line = parts.length == 1 ? "" : parts[1].replace(opcode_m.invert ? "not " : "", "");
                    processArguments(opcode_m, code_line);
                } else {
                    stack_errors.add(new CodeError(app.getString(R.string.unknown_opcode_error, line), line_idx));
                }
            }
        }
        opcode_stack.clean();
        line_stack.clean();
        // check labels
        for(OpcodeLine op : opcode_stack.opcodes) {
            if(op.arguments.size() > 0) {
                for(CodeParam arg : op.arguments) {
                    if(arg.type == OpcodeInfo.label_type_param && !suggestions.existAddress(arg.value)) {
                        stack_errors.add(new CodeError(app.getString(R.string.address_unknown, arg.value), op.line));
                    }
                }
            }
        }
        if(format == null) {
            stack_errors.add(0, new CodeError(app.getString(R.string.no_cleo_format), -1));
        }
        errors.clear();
        errors.addAll(stack_errors);
        stack_errors.clear();
        suggestions.rewind();
    }

    private static String getFormat(String input) {
        Pattern regex = Pattern.compile("\\.cs[\\w]*");
        Matcher matcher = regex.matcher(input);
        return matcher.find() ? matcher.group() : "";
    }

    private void processLabel(String line) {
        String address = line.replace(":", "@");
        int founded = suggestions.exist(address);
        if(founded == -1) {
            suggestions.add(new SuggestionItem(SuggestionType.TYPE_ADDRESS, address));
        } else if(founded > 1) {
            stack_errors.add(new CodeError("'" + address + "' " + app.getString(R.string.address_already_defined), line_idx));
        }
    }

    private void processIf(OpcodeInfo info) {
        if((info.attributes & OpcodeInfo.is_condition) != 0) {
            if_waiting.num_conditions++;
        } else if((info.attributes & OpcodeInfo.is_static) == 0 || line_idx >= editor.getLineCount()) {
            int num_conditions = if_waiting.num_conditions;
            int logical_op = if_waiting.logical_op;
            if(logical_op == 0) {
                if(num_conditions == 0) {
                    stack_errors.add(new CodeError("if statement requires at least one condition", if_waiting.line));
                } else if(num_conditions > 1) {
                    stack_errors.add(new CodeError("too much conditions", if_waiting.line));
                }
            } else {
                if(num_conditions < 2) {
                    stack_errors.add(new CodeError("if statement requires at least two conditions", if_waiting.line));
                } else if(if_waiting.num_conditions > 8) {
                    stack_errors.add(new CodeError("only a maximum of 8 conditions is supported, found " + num_conditions, if_waiting.line));
                } else {
                    if_waiting.arguments.get(0).value = (logical_op == 1 ? num_conditions - 1 : num_conditions + 19) + "";
                }
            }
            if_waiting = null;
        }
    }

    private void processArguments(OpcodeLine current, String code_line) {
        Pattern pattern = Pattern.compile(pattern_arguments + (current.id == 0x00D6 ? "|\\s((and)|(or))": ""));
        Matcher matcher = pattern.matcher(code_line);
        current.arguments.clear();
        OpcodeInfo info = current.info;
        // collect arguments
        while (matcher.find()) {
            String argument = matcher.group().replace(" ","");
            if(current.id == 0x00D6) {
                if(argument.matches("and|or")) {
                    if_waiting = current;
                    if_waiting.num_conditions = 0;
                    if_waiting.logical_op = argument.startsWith("and") ? 1 : 2;
                    if_waiting.arguments.add(new CodeParam("0", OpcodeInfo.int_type_param,  -1));
                } else {
                    stack_errors.add(new CodeError(app.getString(R.string.invalid_logical) + " '"+argument+"'", line_idx));
                }
            } else if(argument.charAt(0) == '#') {
                current.arguments.add(new CodeParam(argument, OpcodeInfo.model_any_type_param, -1));
            } else if(argument.charAt(0) == '@') {
                current.arguments.add(new CodeParam(argument, OpcodeInfo.label_type_param, -1));
            } else if(argument.matches(pattern_var)) {
                int source = -1;
                if(argument.contains("$")) {
                    source = 0; // global
                } else if (argument.contains("@")) {
                    source = 2; // local
                }
                current.arguments.add(new CodeParam(argument, OpcodeInfo.any_type_param, source));
            } else if(checkIfInteger(argument)) {
                current.arguments.add(new CodeParam(argument, OpcodeInfo.int_type_param, 1)); // literal source
            } else if (argument.matches("\\d+\\.\\d+")) {
                current.arguments.add(new CodeParam(argument, OpcodeInfo.float_type_param, 1)); // literal source
            } else {
                current.arguments.add(new CodeParam(argument, OpcodeInfo.any_type_param, -1));
            }
        }
        if(current.id == 0x00D6) {
            if (current.arguments.size() == 0) {
                if_waiting = current;
                if_waiting.num_conditions = 0;
                if_waiting.logical_op = 0;
                if_waiting.arguments.add(new CodeParam("0", OpcodeInfo.int_type_param, -1));
            }
        }
        // validate and catalog arguments
        for(int i = 0; i < current.arguments.size(); i++) {
            CodeParam arg = current.arguments.get(i);
            if(i >= current.info.params.size()) {
                return;
            }
            OpcodeArgument prm = current.info.params.get(i);
            if(prm.output) {
                if(arg.type == OpcodeInfo.any_type_param && arg.source != -1 && suggestions.exist(arg.value) == -1) {
                    suggestions.add(new SuggestionItem(SuggestionType.TYPE_VARIABLE, arg.value, prm.type));
                }
            } else {
                if(arg.type == OpcodeInfo.model_any_type_param && !suggestions.isConstant(arg.value)) {
                    stack_errors.add(new CodeError(app.getString(R.string.modifier_unknown, arg.value), line_idx));
                }
            }
        }
        if(current.arguments.size() != info.param_count) {
            if(current.arguments.size() < info.param_count) {
                stack_errors.add(new CodeError(app.getString(R.string.missing_parameters, info.param_count, current.arguments.size()), line_idx));
            } else {
                stack_errors.add(new CodeError(app.getString(R.string.too_much_parameters, info.param_count, current.arguments.size()), line_idx));
            }
        }
    }
    private int checkOpcode(String part) {
        try {
            return Integer.decode("0x" + part);
        }catch (NumberFormatException e){
            return -1;
        }
    }

    private static boolean checkIfInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void onTextChange(String str) {
        int cur_line = getCurrentLine();
        if(cur_line != -1) {
            int start_idx = editor.getIndexForStartOfLine(cur_line - 1);
            int end_idx = editor.getIndexForEndOfLine(cur_line - 1);
            if((end_idx - start_idx) == 4 && checkOpcode(str.substring(start_idx, end_idx)) != -1) {
                latent_opcode = str.substring(start_idx, end_idx);
            }
            line_stack.getLine(getCurrentLine() - 1).modified = true;
        } else {
            line_stack.rewind();
        }
        app.saved_file = false;
    }

    @Override
    public String OnTabClick() {
        if(latent_opcode != null) {
            int opcode_id = checkOpcode(latent_opcode);
            if(scm.ops.containsKey(opcode_id)) {
                latent_opcode = null;
                return ": " + scm.ops.get(opcode_id).template;
            } else {
                Toast.makeText(app, app.getString(R.string.unknown_opcode_error, latent_opcode), Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }
}
