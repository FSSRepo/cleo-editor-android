/*
 * CLEO Script Java
 * FSSRepo 2024
 */

package com.fastsmartsystem.cleo;

import com.fastsmartsystem.cleo.compiler.OpcodeParameter;
import com.fastsmartsystem.cleo.compiler.OpcodeProcessed;

import java.util.*;
import java.io.*;
import java.util.regex.*;

public class ScriptCompiler
{
	private OpcodesLoader scm;
	private HashMap<Integer, String> labels = new HashMap<>();
	private ArrayList<OpcodeProcessed> opcodes_processed = new ArrayList<OpcodeProcessed>();
	private ArrayList<OpcodeCompiled> opcodes_compiled = new ArrayList<OpcodeCompiled>();
	private HashMap<String, Integer> labels_addresses = new HashMap<>();
	private IDECollector ide_collector;
	private OpcodeProcessed if_waiting = null;
	private int num_conditions = 0;
	private int logical_op = 0;
	public String error = "";
	public int line_idx = 0;
	private int size = 0;

	private class OpcodeCompiled {
		public int id = 0;
		public OpcodeParameter[] params;
	}

	public ScriptCompiler(OpcodesLoader scm, IDECollector ide) {
		this.scm = scm;
		this.ide_collector = ide;
	}

	public boolean hasError() {
		return !error.isEmpty();
	}

	public String compile(String text, OutputStream os) {
		try {
			if(!processText(text)) {
				return error;
			}

			// code to binary
			if(!processScript()) {
				return error;
			}

			// write file
			saveScript(os);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return "Compile Error:\n" + sw + "\nLine " + line_idx + " breaked";
		}
		return "Script Length: "+ size;
	}

	private boolean processScript() {
		size = 0;

		for(OpcodeProcessed op : opcodes_processed) {
			String label = labels.get(op.code_address);
			if(label != null) {
				labels_addresses.put(label, size);
			}

			OpcodeCompiled oc = new OpcodeCompiled();
			oc.id = op.id;
			size += 2; // opcode id
			oc.params = new OpcodeParameter[op.arguments.size()];
			for(int i = 0; i < op.arguments.size(); i ++) {
				size ++;
				OpcodeParameter param = new OpcodeParameter();
				String arg = op.arguments.get(i);
				if(arg.charAt(0) == '@') {
					size += 4;
					param.type = 0x1;
					param.label = true;
					param.value = arg;
				} else if(arg.startsWith("s$")) {
					param.type = 0xA;
					size += 2;
					param.value_integer = Integer.parseInt(arg.replace("s$",""));
				} else if(arg.startsWith("v$")) {
					param.type = 0x11;
					size += 2;
					param.value_integer = Integer.parseInt(arg.replace("v$",""));
				} else if(arg.endsWith("@s")) {
					param.type = 0xB;
					size += 2;
					param.value_integer = Integer.parseInt(arg.replace("@s",""));
				} else if(arg.endsWith("@v")) {
					param.type = 0x10;
					size += 2;
					param.value_integer = Integer.parseInt(arg.replace("@v",""));
				} else if(op.id == 0x00D6) {
					size += 1;
					param.type = 0x04;
					param.value_integer = Integer.parseInt(arg);
				} else if(checkFormat(arg, "\\d{1,3}@")) {
					param.type = 0x3;
					size += 2;
					param.value_integer = Integer.parseInt(arg.replace("@",""));
				} else if(arg.charAt(0) == '$') {
					param.type = 0x2;
					size += 2;
					int fd = ide_collector.getIdByDefinition(arg);
					if(fd != -1) {
						param.value_integer = fd;
					} else {
						String test = arg.replace("$", "");
						if(!checkIfInteger(test)) {
							error = "Line "+line_idx+": invalid global variable '"+arg+"'";
							return false;
						}
						param.value_integer =  Integer.parseInt(test);
					}
				} else if(checkIfInteger(arg)) {
					param.value_integer = Integer.parseInt(arg);
					param.type = (Math.abs(param.value_integer) < 128 ? 0x4 :
							(Math.abs(param.value_integer) > 32735 ? 0x1 :
									0x5));
					size += param.type == 0x4 ? 1 : (param.type == 0x5 ? 2 : 4);
				} else if(checkIfFloat(arg)) {
					param.type = 0x6;
					size += 4;
					param.value_float = Float.parseFloat(arg);
				} else if(checkFormat(arg, "'([^']*)'")) {
					param.value = arg.replace("'","");
					param.type = (byte)(param.value.length() > 8 ? 0xF : 0x9);
					size += (param.value.length() > 8 ? 16 : 8);
				} else if(checkFormat(arg, "\"([^']*)\"")) {
					param.value = arg.replace("\"","");
					param.type = 0xE;
					size += 1 + param.value.length();
				}
				oc.params[op.info.params.get(i).offset] = param;
			}
			opcodes_compiled.add(oc);
		}
		opcodes_processed.clear();
		return true;
	}

	private boolean processText(String text) throws Exception {
		String[] lines = text.split("\n");
		line_idx = 0;
		error = "";
		for (String line : lines) {
			line_idx++;
			if(line.length() == 0 ||
					line.startsWith("//") || line.startsWith("{")) {
				continue;
			}
			if(line.contains("//")) {
				line = line.substring(0, line.indexOf("//"));
			}
			// check label -> code address
			if(line.startsWith(":")) {
				labels.put(line_idx + 1, line.replace(':', '@'));
				continue;
			}
			OpcodeProcessed current = new OpcodeProcessed();
			String[] parts = line.split(":");
			current.id = Integer.decode("0x" + parts[0]);
			current.invert = current.id > 0x7FFF;
			current.code_address = line_idx;
			current.info = scm.ops.get(current.invert ? (current.id - 0x8000) : current.id);
			if(current.info == null) {
				// Unknown opcode
				error += "Line "+line_idx+": '"+ parts[0] +"' Unknown opcode";
				return false;
			}
			if(if_waiting != null) {
				if((current.info.attributes & OpcodeInfo.is_condition) != 0) {
					num_conditions++;
				} else if((current.info.attributes & OpcodeInfo.is_static) == 0 || line_idx >= lines.length) {
					if(logical_op == 0) {
						if(num_conditions == 0) {
							error = "Line "+ line_idx +": if statement requires at least one condition";
							return false;
						} else if(num_conditions > 1) {
							error = "Line "+ line_idx +": too much conditions";
							return false;
						}
					} else {
						if(num_conditions < 2) {
							error = "Line "+ line_idx +": if statement requires at least two conditions";
							return false;
						} else if(num_conditions > 8) {
							error = "Line "+ line_idx +": only a maximum of 8 conditions is supported, found " + num_conditions;
							return false;
						} else {
							if_waiting.arguments.remove(0);
							if_waiting.arguments.add((logical_op == 1 ? num_conditions - 1 : num_conditions + 19) + "");
						}
					}
				}
			}

			// collect arguments
			if(collectArguments(current, parts[1].replace(current.invert ? "not " : "", ""))) {
				opcodes_processed.add(current);
			} else {
				return false;
			}
		}
		return true;
	}

	private void saveScript(OutputStream os) {
		FileStreamWriter fwr = new FileStreamWriter(os);
		for(OpcodeCompiled oc : opcodes_compiled) {
			fwr.writeShort(oc.id);
			for(OpcodeParameter param : oc.params) {
				fwr.writeByte(param.type);
				if(param.label) {
					fwr.writeInt(-labels_addresses.get(param.value));
					continue;
				}
				switch(param.type) {
					case 0x1: // int value
						fwr.writeInt(param.value_integer);
						break;
					case 0x2: // global var
					case 0x3: // local var
					case 0xA: // global string 8 var
					case 0xB: // local string 8 var
					case 0x10: // global string 16 var
					case 0x11: // local string 16 var
					case 0x5: // short value
						fwr.writeShort(param.value_integer);
						break;
					case 0x6: // float value
						fwr.writeFloat(param.value_float);
						break;
					case 0x4: // byte value
						fwr.writeByte(param.value_integer);
						break;
					case 0x9: // string 8
						fwr.writeStringFromSize(8, param.value);
						break;
					case 0xF: // string 16
						fwr.writeStringFromSize(16, param.value);
						break;
					case 0xE: // string
						fwr.writeByte(param.value.length());
						fwr.writeString(param.value);
						break;
				}
			}
		}
		fwr.finish();
	}

	private static boolean checkFormat(String input, String pattern) {
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(input);
		return matcher.matches();
	}

	private static boolean checkIfInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean checkIfFloat(String input) {
		try {
			Float.parseFloat(input);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean collectArguments(OpcodeProcessed current, String code) {
		String pattern_arguments = "[sv][$@]\\d+|\\$\\w+|\\#\\w+|\\@\\w+|\\s\\d+\\@|\\s\\d+\\.\\d+|-[\\d.]+|\\s\\d+|'(.*?)'|\"(.*?)\"|\\s((and)|(or))";
		Pattern pattern = Pattern.compile(pattern_arguments);
		Matcher matcher = pattern.matcher(code);
		while (matcher.find()) {
			String argument = matcher.group().replace(" ","");
			if(argument.charAt(0) != '#') {
				if(current.id == 0x00D6) {
					if(argument.matches("and|or")) {
						if_waiting = current;
						num_conditions = 0;
						logical_op = argument.startsWith("and") ? 1 : 2;
						if_waiting.arguments.add("0");
					} else {
						error = "Line "+ line_idx +": invalid logical operator '"+argument+"'";
						return false;
					}
				} else {
					current.arguments.add(argument);
				}
			} else {
				int fd = ide_collector.getIdByDefinition(argument.substring(1));
				if(fd == -1) {
					error = "Line "+line_idx+": invalid definition '" + argument + "'";
					return false;
				}
				current.arguments.add(fd +"");
			}
		}
		if(current.id == 0x00D6) {
			if(current.arguments.size() == 0) {
				if_waiting = current;
				num_conditions = 0;
				logical_op = 0;
				if_waiting.arguments.add("0");
				return true;
			}
		}
		if(current.info.param_count != -1 && current.arguments.size() != current.info.param_count) {
			error = "Line "+line_idx+": missing parameters, expected "+current.info.param_count + ", given "+ current.arguments.size();
			return false;
		}
		return true;
	}
}
