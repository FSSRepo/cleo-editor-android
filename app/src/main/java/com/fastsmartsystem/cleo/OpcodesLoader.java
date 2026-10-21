/*
 * CLEO Script Java
 * FSSRepo 2026
 */

package com.fastsmartsystem.cleo;

import java.util.*;
import java.io.*;

public class OpcodesLoader
{
	public HashMap<Integer, OpcodeInfo> ops = new HashMap<Integer, OpcodeInfo>();
	private byte[] data;
	private int offset = 0;

	public OpcodesLoader(String path) {
        try (InputStream is = new FileInputStream(path)) {
			data = new byte[is.available()];
			is.read(data);

			int num_opcodes = readShort();
			for(int i = 0; i < num_opcodes; i++) {
				OpcodeInfo info = new OpcodeInfo();
				int id = readShort();
				int description_len = readShort();
				info.description = readString(description_len);
				int example_len = readByte() & 0xff;
				info.example = readString(example_len);
				int template_len = readByte() & 0xff;
				info.template = readString(template_len);
				info.param_count = readByte();
				info.attributes = readUShort();

				for(int j = 0; j < info.param_count; j++) {
					OpcodeArgument prm = new OpcodeArgument();
					int param_name = readByte();
					prm.name = readString(param_name);
					prm.output = readByte() == 1;
					prm.type = readByte();
					prm.source = readByte();
					prm.offset = readByte();
					info.params.add(prm);
				}
				ops.put(id, info);
			}
			data = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
		// start_new_script and run_external_script take a list of arguments that ends with a 0x00 byte
		markVariadic(0x004F);
		markVariadic(0x0913);
	}

	/** CLEO opcodes that are conditions (usable after 'if'); the .ini format does not say which ones they are. */
	private static final int[] CLEO_CONDITIONS = { 0x0A9A, 0x0AA2, 0x0AA4, 0x0AA9, 0x0AAB, 0x0AB0, 0x0AB6, 0x0ABD, 0x0ABE,
		0x0AC8, 0x0AD2, 0x0AD4, 0x0AD5, 0x0AD6, 0x0AD7, 0x0AD8, 0x0ADA, 0x0ADC, 0x0AE1, 0x0AE2, 0x0AE3, 0x0AE4, 0x0AE5,
		0x0AE6, 0x0AE7 };

	private static boolean isCleoCondition(int id) {
		for(int c : CLEO_CONDITIONS) {
			if(c == id) {
				return true;
			}
		}
		return false;
	}

	private void markVariadic(int id) {
		OpcodeInfo info = ops.get(id);
		if(info != null) {
			info.variadic = true;
		}
	}

	/**
	 * Adds the opcodes of a Sanny Builder style list (SASCM.ini: "0AB3=2,cleo_shared_var %1d% = %2d%") that the data file
	 * does not define, for example the CLEO opcodes. A parameter count of -1 marks a variadic opcode.
	 * Returns the number of opcodes added.
	 */
	public int loadIni(String path) {
		int added = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if(line.isEmpty() || line.startsWith(";") || line.startsWith("[")) {
					continue;
				}
				int eq = line.indexOf('=');
				int comma = line.indexOf(',');
				if(eq <= 0 || comma < eq) {
					continue; // not an opcode definition
				}
				int id;
				int count;
				try {
					id = Integer.parseInt(line.substring(0, eq).trim(), 16);
					count = Integer.parseInt(line.substring(eq + 1, comma).trim());
				} catch (NumberFormatException e) {
					continue;
				}
				if(ops.containsKey(id)) {
					continue;
				}
				OpcodeInfo info = new OpcodeInfo();
				info.param_count = count;
				info.template = line.substring(comma + 1);
				info.description = "";
				info.example = "";
				info.variadic = count == -1;
				if(isCleoCondition(id)) {
					info.attributes |= OpcodeInfo.is_condition;
				}
				int fixed = info.templateParams();
				int[] order = info.placeholders();
				for(int i = 0; i < order.length; i++) {
					OpcodeArgument arg = new OpcodeArgument();
					arg.name = "param" + (i + 1);
					arg.offset = (byte) order[i];
					info.params.add(arg);
				}
				if(count == -1) {
					info.param_count = fixed;
				}
				ops.put(id, info);
				added++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return added;
	}

	public String readString(int len){
		return cortarnombre(new String(readByteArray(len)));
	}

	private String cortarnombre(String str) {
		int indexOf = str.indexOf(0);
		return indexOf > 0 ? str.substring(0, indexOf) : str;
	}

	public int readUShort() {
		int val = ((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
		offset += 2;
		return val;
	}

	public short readShort() {
		short val = (short)((data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8);
		offset += 2;
		return val;
	}

	public byte[] readByteArray(int len) {
		byte[] bdata = new byte[len];
		for (int i = 0; i < len; i++) {
			bdata[i] = data[offset];
			offset++;
		}
		return bdata;
	}

	public byte readByte() {
		byte val = data[offset];
		offset++;
		return val;
	}

	public int readInt() {
		int val = (data[offset] & 0xFF) | (data[offset + 1] & 0xFF) << 8 | (data[offset + 2] & 0xFF) << 16 | (data[offset + 3] & 0xFF) << 24;
		offset += 4;
		return val;
	}
}
