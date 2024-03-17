/*
 * CLEO Script Java
 * FSSRepo 2024
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
        try {
			InputStream is = new FileInputStream(path);
			data = new byte[is.available()];
			is.read(data);
			is.close();
			is = null;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
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
