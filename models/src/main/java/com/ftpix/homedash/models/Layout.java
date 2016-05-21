package com.ftpix.homedash.models;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "layouts")
public class Layout {

	// length of a grid unit, means a 2x2 module with 150 as widhth would be
	// 300x300px
	public static int GRID_UNIT_WIDTH = 100;
	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	@Expose
	private int id;

	@DatabaseField
	@Expose
	private String name;

	@DatabaseField(unique = true)
	@Expose
	private int maxGridWidth;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxGridWidth() {
		return maxGridWidth;
	}

	public void setMaxGridWidth(int maxGridWidth) {
		this.maxGridWidth = maxGridWidth;
	}

	public int getActualSize() {
		return GRID_UNIT_WIDTH * maxGridWidth;
	}

}
