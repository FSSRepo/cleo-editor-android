/*
 * CLEO SCript Java
 * FSSRepo 2026
 */

package com.fastsmartsystem.cleo;

/** Format error in a script (binary when decompiling, text when compiling). */
public class ScriptFormatException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/** Offset (decompiler) or line number (compiler); -1 if not applicable. */
	public final int position;

	public ScriptFormatException(String message, int position) {
		super(message);
		this.position = position;
	}
}
