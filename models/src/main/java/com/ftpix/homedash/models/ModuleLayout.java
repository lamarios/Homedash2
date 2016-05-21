package com.ftpix.homedash.models;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "module_layout")
public class ModuleLayout {

	public static final String SIZE_1x1 = "1x1", SIZE_2x1 = "2x1", SIZE_2x2 = "2x2";

	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	private int id;

	@DatabaseField(foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 1)
	private Layout layout;

	@DatabaseField(foreign = true, foreignAutoRefresh = true, maxForeignAutoRefreshLevel = 1)
	private Module module;

	@DatabaseField
	private int x = 0;

	@DatabaseField
	private int y = 0;

	@DatabaseField
	private String size;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Layout getLayout() {
		return layout;
	}

	public void setLayout(Layout layout) {
		this.layout = layout;
	}

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public String getSize() {
		return size;
	}

	public String getWidth() {
		return size.split("x")[0];
	}

	public String getHeight() {
		return size.split("x")[1];
	}

	public void setSize(String size) {
		this.size = size;
	}
	
	@Override
	public boolean equals(Object obj) {
		try{
			ModuleLayout other = (ModuleLayout) obj;
			return id == other.getId();
		}catch(Exception e){
			return false;
		}
	}
}
