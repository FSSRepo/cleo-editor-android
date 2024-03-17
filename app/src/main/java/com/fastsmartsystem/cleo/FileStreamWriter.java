/*
 * CLEO Script Java
 * FSSRepo 2024
 */

package com.fastsmartsystem.cleo;
import java.io.*;

public class FileStreamWriter
{
	OutputStream out;

	public FileStreamWriter(OutputStream out) {
		try{
			this.out = out;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	private void write(byte[] data){
		try{
			out.write(data);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void writeFloatArray(float[] data) {
        for (int ofs = 0; ofs < data.length; ofs++) {
            writeFloat(data[ofs]);
        }
    }

	public void writeByteArray(byte[] data) {
        write(data);
    }

	public void writeShortArray(short[] data) {
        for (int ofs = 0; ofs < data.length; ofs++) {
            writeShort(data[ofs]);
        }
    }

    public void writeString(String text) {
        writeByteArray(text.getBytes());
    }

	public void writeStringFromSize(int size, String text) {
        byte[] data = new byte[size];
        for(short i = 0;i < size;i++){
			if(i < text.length()) {
				data[i] = (byte)text.charAt(i);
			}else{
				break;
			}
		}
		writeByteArray(data);
    }

    public void writeFloat(float val) {
        writeInt(Float.floatToIntBits(val));
    }

    public void writeInt(int values) {
		byte[] data = new byte[4];
        data[0] = (byte) (values & 0xFF);
        data[1] = (byte) ((values >> 8) & 0xFF);
        data[2] = (byte) ((values >> 16) & 0xFF);
        data[3] = (byte) ((values >> 24) & 0xFF);
        write(data);
    }

    public void writeShort(int values) {
		byte[] data = new byte[2];
        data[0] = (byte) (values & 0xFF);
        data[1] = (byte) ((values >> 8) & 0xFF);
        write(data);
    }

    public void writeByte(int values) {
        write(new byte[] {
				  (byte) (values & 0xFF)
			  });
    }

	public void finish(){
		try {
			out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
