/*
 * CLEO SCript Java
 * FSSRepo 2026
 */

package com.fastsmartsystem.cleo;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The footer Sanny Builder 4 appends after the code of a compiled script.
 *
 * Layout (read back from the reader of sanny.exe and from a script Sanny compiled):
 * <pre>
 *   code
 *   sections...      scanned from the end of the code; an unknown byte is skipped
 *   u32 codeLength   offset where the footer starts
 *   "__SBFTR\0"      signature (the last 8 bytes of the file)
 * </pre>
 * Sections (each starts with a NUL terminated tag):
 * <pre>
 *   "E\0"                      2 bytes, marks the footer as written by Sanny 4 (the code length is exact)
 *   "VAR\0" u32 n, n x { cstring name, i32 variable index }   global variable names used by the project
 *   "HEX\0" u32 n, n x 8 bytes     (skipped here)
 *   "AAA\0"                    4 bytes, flag (skipped)
 *   "FLAG\0" u32 flags         5 + 4 bytes; bit 12 says that a SRC section is present
 *   "SRC\0" u32 length, bytes  source text of the script
 * </pre>
 */
public class ScriptFooter
{
	public static final String SIGNATURE = "__SBFTR";

	/** Size of the code, i.e. where the footer starts. */
	public int codeLength;
	/** True when the "E" section is present. */
	public boolean extended;
	/** Value of the FLAG section; -1 if there is none. */
	public int flags = -1;
	/** Embedded source text (SRC section) or null. */
	public String source;
	/** Global variable names (without '$') to variable index, in file order. */
	public LinkedHashMap<String, Integer> variables = new LinkedHashMap<>();

	/** Parses the footer of a compiled script; null if the file has none. */
	public static ScriptFooter parse(byte[] d) {
		int size = d.length;
		if(size <= 12 || !SIGNATURE.equals(cstring(d, size - 8, 8)) || d[size - 1] != 0) {
			return null;
		}
		ScriptFooter f = new ScriptFooter();
		f.codeLength = i32(d, size - 12);
		int end = size - 12;
		if(f.codeLength < 0 || f.codeLength > end) {
			return null;
		}
		int p = f.codeLength;
		while(p < end) {
			String tag = cstring(d, p, end - p);
			if(tag == null) {
				break; // no terminating NUL
			}
			if(tag.equals("E")) {
				f.extended = true;
				p += 2;
			} else if(tag.equals("AAA")) {
				p += 4;
			} else if(tag.equals("HEX")) {
				if(p + 8 > end) {
					break;
				}
				p += 8 + i32(d, p + 4) * 8;
			} else if(tag.equals("VAR")) {
				if(p + 8 > end) {
					break;
				}
				int n = i32(d, p + 4);
				p += 8;
				for(int i = 0; i < n && p < end; i++) {
					String name = cstring(d, p, end - p);
					if(name == null || p + name.length() + 5 > end) {
						return f;
					}
					p += name.length() + 1;
					f.variables.put(name, Math.abs(i32(d, p)));
					p += 4;
				}
			} else if(tag.equals("FLAG")) {
				if(p + 9 > end) {
					break;
				}
				f.flags = i32(d, p + 5);
				p += 9;
			} else if(tag.equals("SRC")) {
				if(p + 8 > end) {
					break;
				}
				int len = i32(d, p + 4);
				if(len < 0 || p + 8 + len > end) {
					break;
				}
				if(f.flags >= 0 && (f.flags & (1 << 12)) != 0) {
					f.source = new String(d, p + 8, len, StandardCharsets.ISO_8859_1);
				}
				p += 8 + len;
			} else {
				p++;
			}
		}
		return f;
	}

	/** Bytes of a minimal footer: "E", the variable names and the trailer. */
	public static byte[] encode(int codeLength, Map<String, Integer> variables) {
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		FileStreamWriter w = new FileStreamWriter(out);
		w.writeString("E");
		w.writeByte(0);
		w.writeString("VAR");
		w.writeByte(0);
		w.writeInt(variables.size());
		for(Map.Entry<String, Integer> e : variables.entrySet()) {
			w.writeString(e.getKey());
			w.writeByte(0);
			w.writeInt(e.getValue());
		}
		w.writeInt(codeLength);
		w.writeString(SIGNATURE);
		w.writeByte(0);
		return out.toByteArray();
	}

	private static int i32(byte[] d, int p) {
		return (d[p] & 0xFF) | (d[p + 1] & 0xFF) << 8 | (d[p + 2] & 0xFF) << 16 | (d[p + 3] & 0xFF) << 24;
	}

	/** NUL terminated string at p, at most max bytes; null if no NUL is found in that range. */
	private static String cstring(byte[] d, int p, int max) {
		for(int n = 0; n < max && p + n < d.length; n++) {
			if(d[p + n] == 0) {
				return new String(d, p, n, StandardCharsets.ISO_8859_1);
			}
		}
		return null;
	}
}
