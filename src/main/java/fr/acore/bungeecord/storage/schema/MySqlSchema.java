package fr.acore.bungeecord.storage.schema;


import fr.acore.bungeecord.api.storage.column.Column;
import fr.acore.bungeecord.api.storage.column.IColumn;
import fr.acore.bungeecord.api.storage.column.foreign.ManyToOne;
import fr.acore.bungeecord.api.storage.column.foreign.OneToMany;
import fr.acore.bungeecord.api.storage.column.type.ColumnType;
import fr.acore.bungeecord.api.storage.database.IDatabase;
import fr.acore.bungeecord.api.storage.schema.ISchema;
import fr.acore.bungeecord.api.storage.table.ITable;
import fr.acore.bungeecord.api.storage.table.OneToManyCollection;
import fr.acore.bungeecord.api.storage.table.Table;
import fr.acore.bungeecord.api.storage.utils.CustomSize;
import fr.acore.bungeecord.storage.column.MySqlColumn;
import fr.acore.bungeecord.storage.database.MySqlDatabase;
import fr.acore.bungeecord.storage.table.MySqlTable;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlSchema implements ISchema<MySqlTable> {

	private MySqlDatabase database;
	private String name;
	private List<MySqlTable> tables;
	
	public MySqlSchema(MySqlDatabase database, String name) {
		this.database = database;
		this.name = name;
		this.tables = new ArrayList<>();
	}
	
	@Override
	public void load() {
		if(!database.isConnected()) return;
		try {
			PreparedStatement statement = getConnection().prepareStatement("CREATE SCHEMA " + name + ";");
			statement.execute();
			statement.close();
		} catch(Exception e) {}
		try {
			PreparedStatement statement = getConnection().prepareStatement("SHOW TABLES FROM " + name + ";");
			ResultSet result = statement.executeQuery();
			while(result.next()) {
				String tableName = result.getString("Tables_in_" + name);
				
				MySqlTable sqlTable = new MySqlTable(this, tableName);
				PreparedStatement statement2 = getConnection().prepareStatement("SHOW COLUMNS FROM " + tableName + " IN " + name + ";");
				ResultSet result2 = statement2.executeQuery();
				while(result2.next()) {
					String cName = result2.getString("Field");
					String cType = result2.getString("Type");
					sqlTable.addColumn(new MySqlColumn(sqlTable, cName, ColumnType.fromSqlType(cType), CustomSize.fromSqlType(cType)));
				}
				statement2.close();
				tables.add(sqlTable);
				
			}
			statement.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void save() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public IDatabase<?> getDataBase() {
		return database;
	}

	@Override
	public Connection getConnection() {
		return database.getConnection();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public List<MySqlTable> getTables() {
		return this.tables;
	}

	@Override
	public MySqlTable getTable(String name) {
		for(MySqlTable table : tables) {
			if(table.getName().equals(name)) return table;
		}
		return null;
	}

	@Override
	public void addTable(MySqlTable table) {
		this.tables.add(table);
	}

	@Override
	public void createTable(Class<?> tableClazz) {
		String tableName = tableClazz.getDeclaredAnnotation(Table.class) != null ? tableClazz.getDeclaredAnnotation(Table.class).name() : tableClazz.getSimpleName();
		MySqlTable sqlTable = new MySqlTable(this, tableName, tableClazz);		
		if(getTable(tableName) == null) {
			for(Field f : tableClazz.getDeclaredFields()) {
				if(f.getDeclaredAnnotation(Column.class) != null) {
					Column column = f.getDeclaredAnnotation(Column.class);
					ColumnType cType = ColumnType.valueOf(f.getType().getSimpleName().toUpperCase().equals("INT") ? "INTEGER" : f.getType().getSimpleName().toUpperCase());
					IColumn c = new MySqlColumn(sqlTable, column.columnName().isEmpty() ? f.getName() : column.columnName(), cType, cType.getDefaultSize());
					c.injectField(f);
					sqlTable.addColumn(c);
				}else if(f.getDeclaredAnnotation(ManyToOne.class) != null) {
					ManyToOne fkColumn = f.getDeclaredAnnotation(ManyToOne.class);
					IColumn c = new MySqlColumn(sqlTable, fkColumn.columnName().isEmpty() ? f.getName() : fkColumn.columnName(), fkColumn.type(), fkColumn.type().getDefaultSize());
					c.injectField(f);
					sqlTable.addColumn(c);
				}else if(f.getDeclaredAnnotation(OneToMany.class) != null) {
					OneToMany listForeign = f.getDeclaredAnnotation(OneToMany.class);
					sqlTable.addOneToManyCollection(new OneToManyCollection(f, listForeign));
				}
			}
		}
		
		createTable(sqlTable);
	}
	
	@Override
	public void createTable(MySqlTable table) {
		//Creation si table existe pas + ajouts
		if(getTable(table.getName()) == null) {
			try {
				PreparedStatement statement = getConnection().prepareStatement(table.toSql(true));
				statement.execute();
				statement.close();
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			addTable(table);
			
		}else {
			//Injection si existe
			if(table.isInjected()) {
				getTable(table.getName()).inject(table.getRessourceClass());
			}else {
				System.out.println("Injection impossible");
			}
			
		}
		
	}
	
	@Override
	public void drop() {
		for(ITable t : tables) {
			drop(t.getName());
		}
	}

	@Override
	public void drop(String tableName) {
		try {
			PreparedStatement statement = getDataBase().getConnection().prepareStatement("DROP TABLE IF EXISTS " + getName() + "." + tableName + ";");
			statement.execute();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.tables.remove(getTable(tableName));
	}

}
