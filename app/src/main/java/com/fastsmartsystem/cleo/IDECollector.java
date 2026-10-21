/*
 * CLEO Script Java
 * FSSRepo 2026
 */

package com.fastsmartsystem.cleo;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class IDECollector {
	public static class IDEItem {
		public int id;
		public String dff;
		public boolean global_var = false;
	}

	public ArrayList<IDEItem> items = new ArrayList<>();
	private HashMap<String, Integer> global_names = new HashMap<>();
	private HashMap<Integer, String> global_ids = new HashMap<>();
	
	public void collect(String path) {
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
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
				} else {
					String[] parts = line.split(",");
					IDEItem ide_itm = new IDEItem();
					ide_itm.id = Integer.parseInt(parts[0]);
					ide_itm.dff = parts[1].replace(" ","").toUpperCase();
					ide_itm.global_var = global_var;
					items.add(ide_itm);
					if(global_var) {
						if(!global_names.containsKey(ide_itm.dff)) {
							global_names.put(ide_itm.dff, ide_itm.id);
						}
						if(!global_ids.containsKey(ide_itm.id)) {
							global_ids.put(ide_itm.id, ide_itm.dff);
						}
					}
				}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
	 * Loads global variable names in Sanny Builder's CustomVariables.ini format ("2=PLAYER_CHAR" means $2 is $PLAYER_CHAR).
	 * Names keep their case; entries already defined are not replaced.
	 */
	public void loadVariables(String path) {
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				int eq = line.indexOf('=');
				if(line.isEmpty() || line.startsWith(";") || eq <= 0) {
					continue;
				}
				String name = "$" + line.substring(eq + 1).trim();
				int offset;
				try {
					offset = Integer.parseInt(line.substring(0, eq).trim()) * 4;
				} catch (NumberFormatException e) {
					continue;
				}
				if(!global_ids.containsKey(offset) && !global_names.containsKey(name)) {
					IDEItem item = new IDEItem();
					item.id = offset;
					item.dff = name;
					item.global_var = true;
					items.add(item);
					global_ids.put(offset, name);
					global_names.put(name, offset);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Offset of a global variable by its exact name (with the leading '$'); -1 if unknown. */
	public int getGlobalIdByName(String name) {
		Integer id = global_names.get(name);
		return id == null ? -1 : id;
	}

	public String getDefinitionById(int id, boolean global) {
		if(global) {
			return global_ids.get(id);
		}
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
