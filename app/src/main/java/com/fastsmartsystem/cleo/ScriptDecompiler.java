/*
 * CLEO Script Java
 * FSSRepo 2026
 */

package com.fastsmartsystem.cleo;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Decompiles CLEO bytecode to text (SASCM.ini syntax).
 *
 * Argument types (the byte that precedes every parameter):
 * 0 end (variadic), 1 int32, 2 global var, 3 local var, 4 int8, 5 int16, 6 float32,
 * 7/8 global/local array, 9 text8, A/B global/local text8 var, C/D global/local text array,
 * E length-prefixed text, F text16, 10/11 local/global text16 var.
 *
 * Scripts compiled by Sanny Builder end with a footer (see {@link ScriptFooter}): the code stops there and the
 * global variable names stored in it are used; the ones the project does not define are declared in the output
 * with {$VAR name index} so the text compiles back to the same variables.
 */
public class ScriptDecompiler
{
	/** Decompiler options. */
	public static class Options {
		/** Extension written in the {$CLEO .cs} directive. */
		public String format = ".cs";
		/** Header with the compiler directive. */
		public boolean header = true;
		/** Use the Sanny Builder footer if the file has one. */
		public boolean useFooter = true;
		/** Appends '// address' to the end of every line. */
		public boolean showOffsets = false;
		/** Script name used for labels; null = the one of the first script_name (03A4) or "SCRIPT". */
		public String scriptName = null;
		/** Keep the comments of the opcode templates (the "; int" of "0A8E=3,%3d% = %1d% + %2d% ; int"). */
		public boolean templateComments = true;
	}

	private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+)([a-zA-Z]?)%");
	private static final Pattern SAFE_NAME = Pattern.compile("[^A-Za-z0-9_]");
	private static final Pattern VAR_NAME = Pattern.compile("[A-Za-z_]\\w*");

	private static final int T_END = 0x0, T_INT32 = 0x1, T_GVAR = 0x2, T_LVAR = 0x3, T_INT8 = 0x4, T_INT16 = 0x5,
		T_FLOAT = 0x6, T_GARRAY = 0x7, T_LARRAY = 0x8, T_STR8 = 0x9, T_GSTRVAR = 0xA, T_LSTRVAR = 0xB,
		T_GSTRARRAY = 0xC, T_LSTRARRAY = 0xD, T_STRLEN = 0xE, T_STR16 = 0xF, T_LSTR16VAR = 0x10, T_GSTR16VAR = 0x11;

	private final OpcodesLoader scm;
	private final IDECollector ide_collector;
	private final ArrayList<String> warnings = new ArrayList<>();
	private ScriptFooter footer;
	private final HashMap<Integer, String> footerNames = new HashMap<>();
	private final TreeMap<Integer, String> usedFooterNames = new TreeMap<>();

	private static class Arg {
		String text;
		boolean literal; // int8/int16/int32
		int value;
	}

	private static class Instr {
		int address;
		int id;          // includes the 0x8000 negation bit
		boolean invert;
		OpcodeInfo info;
		ArrayList<Arg> args = new ArrayList<>();
	}

	public ScriptDecompiler(OpcodesLoader scm, IDECollector ide) {
		this.scm = scm;
		this.ide_collector = ide;
	}

	/** Warnings of the last decompilation (unknown opcode, truncated data, ...). */
	public List<String> getWarnings() {
		return warnings;
	}

	/** Footer found by the last decompilation; null if there was none. */
	public ScriptFooter getFooter() {
		return footer;
	}

	/** Decompiles a compiled script; format is the extension of the file (".cs", ".csa", ".csi"). */
	public String decompile(InputStream is, String format) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] chunk = new byte[8192];
			int n;
			while ((n = is.read(chunk)) > 0) {
				bytes.write(chunk, 0, n);
			}
			Options opt = new Options();
			opt.format = format;
			return decompile(bytes.toByteArray(), opt);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return "// This file was decompiled using SASCM.ini\n{$CLEO " + format + "}\n\nDecompile Error:\n" + sw;
		} finally {
			try { is.close(); } catch (IOException ignored) {}
		}
	}

	public String decompile(byte[] data, Options opt) {
		warnings.clear();
		footerNames.clear();
		usedFooterNames.clear();
		footer = opt.useFooter ? ScriptFooter.parse(data) : null;
		if(footer != null) {
			for(Map.Entry<String, Integer> e : footer.variables.entrySet()) {
				// only names the compiler can read back, and not a plain number
				if(VAR_NAME.matcher(e.getKey()).matches() && !footerNames.containsKey(e.getValue() * 4)) {
					footerNames.put(e.getValue() * 4, e.getKey());
				}
			}
		}
		int end = footer != null ? footer.codeLength : data.length;
		boolean[] blocked = new boolean[1];
		ArrayList<Instr> code = decode(data, end, blocked);
		String script_name = opt.scriptName;
		if(script_name == null) {
			script_name = "SCRIPT";
			for(Instr in : code) {
				if(in.id == 0x03A4 && in.args.size() > 0) {
					script_name = SAFE_NAME.matcher(in.args.get(0).text.replace("'", "").replace("\"", "")).replaceAll("_");
					break;
				}
			}
		}
		// jump addresses: relative to the start of the script (the value is negative)
		HashSet<Integer> addresses = new HashSet<>();
		for(Instr in : code) {
			for(int i = 0; i < in.args.size(); i++) {
				Arg a = in.args.get(i);
				if(a.literal && in.info.kindOf(i) == 'p' && a.value <= 0) {
					a.text = "@" + script_name + "_" + (-a.value);
					addresses.add(-a.value);
				}
			}
		}
		StringBuilder out = new StringBuilder();
		if(opt.header) {
			out.append("// This file was decompiled using SASCM.ini\n{$CLEO ").append(opt.format).append("}\n\n");
		}
		for(Map.Entry<Integer, String> e : usedFooterNames.entrySet()) {
			out.append("{$VAR ").append(e.getValue()).append(' ').append(e.getKey() / 4).append("}\n");
		}
		if(blocked[0]) {
			out.append("// CLEO Script blocked\n\n");
		}
		for(Instr in : code) {
			if(addresses.contains(in.address)) {
				out.append("\n:").append(script_name).append("_").append(in.address).append("\n");
			}
			out.append(format(in, opt.templateComments));
			if(opt.showOffsets) {
				out.append(" // ").append(in.address);
			}
			out.append("\n");
		}
		for(String w : warnings) {
			out.append("// warning: ").append(w).append("\n");
		}
		return out.toString();
	}

	private String format(Instr in, boolean comments) {
		String template = in.info.template == null ? "" : in.info.template;
		if(comments) {
			template = template.replace(";", "//");
		} else {
			int c = template.indexOf(';');
			int d = template.indexOf("//");
			if(c < 0 || (d >= 0 && d < c)) {
				c = d;
			}
			template = c < 0 ? template : template.substring(0, c);
		}
		String line = (in.invert ? "not " : "") + template.replaceAll("\\s+$", "");
		Matcher m = PLACEHOLDER.matcher(line);
		StringBuffer sb = new StringBuffer();
		int used = in.info.templateParams();
		while(m.find()) {
			int n = Integer.parseInt(m.group(1)) - 1;
			String text = n < in.args.size() ? in.args.get(n).text : "?";
			m.appendReplacement(sb, Matcher.quoteReplacement(text));
		}
		m.appendTail(sb);
		// parameters the template does not name (variadic): appended at the end
		for(int i = used; i < in.args.size(); i++) {
			sb.append(' ').append(in.args.get(i).text);
		}
		return (String.format("%04X: %s", in.id, sb)).replaceAll("\\s+$", "");
	}

	private ArrayList<Instr> decode(byte[] data, int end, boolean[] blocked) {
		ArrayList<Instr> code = new ArrayList<>();
		Cursor cur = new Cursor(data, 0, end);
		boolean first_try_unblock = true;
		while(cur.pos + 2 <= end) {
			Instr in = new Instr();
			in.address = cur.pos;
			try {
				in.id = cur.ushort();
				in.invert = in.id > 0x7FFF;
				in.info = scm.ops.get(in.id & 0x7FFF);
				if(in.info == null) {
					Instr last = code.isEmpty() ? null : code.get(code.size() - 1);
					if(first_try_unblock && last != null && last.id == 0x0002 && last.args.size() == 1 && last.args.get(0).literal) {
						// junk after a jump: continue at its target once (protected scripts)
						int target = -last.args.get(0).value;
						if(target > cur.pos - 2 && target < end) {
							first_try_unblock = false;
							cur.pos = target;
							continue;
						}
					}
					warnings.add(String.format("%d: unknown opcode %04X, decompilation stopped", in.address, in.id));
					break;
				}
				readArguments(cur, in);
			} catch (ScriptFormatException e) {
				warnings.add(String.format("%d: %s (opcode %04X), decompilation stopped", in.address, e.getMessage(), in.id));
				break;
			}
			code.add(in);
			if(code.size() == 1 && in.id == 0x0002 && in.args.size() == 1 && in.args.get(0).literal && in.args.get(0).value < 0) {
				// a script that starts with a jump: the bytes in between are not code
				int target = -in.args.get(0).value;
				if(target > cur.pos && target < end) {
					blocked[0] = true;
					cur.pos = target;
				}
			}
		}
		return code;
	}

	private void readArguments(Cursor cur, Instr in) {
		OpcodeInfo info = in.info;
		boolean variadic = info.isVariadic();
		int count = variadic ? info.templateParams() : info.param_count;
		for(int j = 0; j < count; j++) {
			in.args.add(readArgument(cur, info.kindOf(j), in.id == 0x00D6));
		}
		if(variadic) {
			// variadic opcodes end with a type byte of 0
			while(true) {
				if(cur.peek() == T_END) {
					cur.pos++;
					break;
				}
				in.args.add(readArgument(cur, ' ', false));
			}
		}
	}

	private Arg readArgument(Cursor cur, char kind, boolean ifCount) {
		Arg arg = new Arg();
		if(kind == 'k') {
			// the only case without a type byte: raw 128-byte text (05B6 save_string_to_debug_file)
			arg.text = "\"" + cur.string(128) + "\"";
			return arg;
		}
		int type = cur.ubyte();
		switch(type) {
			case T_INT32:
			case T_INT8:
			case T_INT16:
				arg.value = type == T_INT32 ? cur.int32() : (type == T_INT8 ? cur.sbyte() : cur.sshort());
				arg.literal = true;
				arg.text = Integer.toString(arg.value);
				if(ifCount) {
					// number of conditions of an 'if': 0 = one, 1..7 = and, 21..27 = or (the compiler counts them again)
					arg.text = arg.value == 0 ? "" : (arg.value < 8 && arg.value > 0 ? "and" : (arg.value > 20 ? "or" : arg.text));
				} else if(kind == 'm' || kind == 'o') {
					String def = ide_collector.getDefinitionById(arg.value, false);
					if(def != null) {
						arg.text = "#" + def;
					}
				}
				break;
			case T_GVAR:
				arg.text = globalVar(cur.ushort());
				break;
			case T_LVAR:
				arg.text = cur.ushort() + "@";
				break;
			case T_FLOAT:
				arg.text = floatToText(cur.float32());
				break;
			case T_STR8:
				arg.text = "'" + cur.string(8) + "'";
				break;
			case T_STRLEN:
				arg.text = "\"" + cur.string(cur.ubyte()) + "\"";
				break;
			case T_STR16:
				arg.text = "'" + cur.string(16) + "'";
				break;
			case T_LSTR16VAR:
				arg.text = cur.ushort() + "@v";
				break;
			case T_GSTR16VAR:
				arg.text = "v$" + cur.ushort();
				break;
			case T_GSTRVAR:
				arg.text = "s$" + cur.ushort();
				break;
			case T_LSTRVAR:
				arg.text = cur.ushort() + "@s";
				break;
			case T_GARRAY:
			case T_LARRAY:
			case T_GSTRARRAY:
			case T_LSTRARRAY:
				arg.text = readArray(cur, type);
				break;
			default:
				throw new ScriptFormatException(String.format("unknown argument type 0x%02X", type), cur.pos - 1);
		}
		return arg;
	}

	/** base(index,size+type), e.g. $12(3@,25i) or 8@(2@,10f). Type: i int, f float, s text8, v text16. */
	private String readArray(Cursor cur, int type) {
		int offset = cur.ushort();
		int index = cur.ushort();
		int size = cur.ubyte();
		int flags = cur.ubyte();
		boolean global = type == T_GARRAY || type == T_GSTRARRAY;
		String base = global ? globalVar(offset) : offset + "@";
		String idx = (flags & 0x80) != 0 ? globalVar(index) : index + "@";
		char elem = "ifsv".charAt(flags & 3);
		return base + "(" + idx + "," + size + elem + ")";
	}

	private String globalVar(int offset) {
		String def = ide_collector.getDefinitionById(offset, true);
		String own = footerNames.get(offset);
		if(own != null && !("$" + own).equals(def)) {
			// a name the project does not define: declare it in the output ({$VAR name index})
			usedFooterNames.put(offset, own);
			return "$" + own;
		}
		return def != null ? def : "$" + (offset / 4);
	}

	/** Float in plain decimal notation (no exponent) that the compiler can read back. */
	static String floatToText(float f) {
		if(Float.isNaN(f) || Float.isInfinite(f)) {
			throw new ScriptFormatException("float cannot be represented: " + f, -1);
		}
		String s = new BigDecimal(Float.toString(f)).toPlainString();
		return s.indexOf('.') < 0 ? s + ".0" : s;
	}

	/** Little-endian reader with bounds checking. */
	private static class Cursor {
		final byte[] data;
		int pos;
		final int end;

		Cursor(byte[] data, int pos, int end) {
			this.data = data;
			this.pos = pos;
			this.end = end;
		}

		private void need(int n) {
			if(pos + n > end) {
				throw new ScriptFormatException("truncated data", pos);
			}
		}

		int peek() {
			need(1);
			return data[pos] & 0xFF;
		}

		int ubyte() {
			need(1);
			return data[pos++] & 0xFF;
		}

		int sbyte() {
			return (byte) ubyte();
		}

		int ushort() {
			need(2);
			int v = (data[pos] & 0xFF) | (data[pos + 1] & 0xFF) << 8;
			pos += 2;
			return v;
		}

		int sshort() {
			return (short) ushort();
		}

		int int32() {
			need(4);
			int v = (data[pos] & 0xFF) | (data[pos + 1] & 0xFF) << 8 | (data[pos + 2] & 0xFF) << 16 | (data[pos + 3] & 0xFF) << 24;
			pos += 4;
			return v;
		}

		float float32() {
			return Float.intBitsToFloat(int32());
		}

		/** Text of len bytes ending at the first 0 (or all of it if there is none). */
		String string(int len) {
			need(len);
			int n = 0;
			while(n < len && data[pos + n] != 0) {
				n++;
			}
			String s = new String(data, pos, n, StandardCharsets.ISO_8859_1);
			pos += len;
			return s;
		}
	}
}
