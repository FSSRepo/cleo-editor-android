/*
 * CLEO Script Java
 * FSSRepo 2024
 */

package com.fastsmartsystem.cleo;
import java.io.*;
import java.util.ArrayList;

public class IDECollector {
	public static class IDEItem {
		public int id;
		public String dff;
		public boolean global_var = false;
	}

	public ArrayList<IDEItem> items = new ArrayList<>();
	
	public void load(String path) {
		try {
            BufferedReader reader  = new BufferedReader(new FileReader(path));
            String line;
			boolean collect = false, global_var = false;
            while ((line = reader.readLine()) != null) {
				line = line.replaceAll("\\s|\t","");
				if(line.startsWith("#") || 
					line.length() == 0) {
					continue;
				}
				if(!collect) {
					global_var = line.startsWith("globals");
					collect = global_var || line.matches("weap|cars|peds");
				} else if(line.startsWith("end")) {
					collect = false;
					global_var = false;
					continue;
				} else {
					String[] parts = line.split(",");
					IDEItem ide_itm = new IDEItem();
					ide_itm.id = Integer.parseInt(parts[0]);
					ide_itm.dff = parts[1].replace(" ","").toUpperCase();
					ide_itm.global_var = global_var;
					items.add(ide_itm);
				}
            }
			reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public String getDefinitionById(int id, boolean global) {
		for(IDEItem ide : items) {
			if(ide.id == id && ide.global_var == global) {
				return ide.dff;
			}
		}
		return null;
	}
	
	public int getIdByDefinition(String def) {
		for(IDEItem ide : items) {
			if(ide.dff.startsWith(def)){
				return ide.id;
			}
		}
		return -1;
	}
}
