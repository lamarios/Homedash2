package com.ftpix.homedash.models;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "pages")
public class Page {
	@DatabaseField(generatedId = true, allowGeneratedIdInsert = true)
	@Expose
	private int id;

	@DatabaseField
	@Expose
	private String name;

	@ForeignCollectionField(eager = false, maxEagerLevel = 0)
	public ForeignCollection<Module> modules;

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

	public ForeignCollection<Module> getModules() {
		return modules;
	}

	public void setModules(ForeignCollection<Module> modules) {
		this.modules = modules;
	}
}
