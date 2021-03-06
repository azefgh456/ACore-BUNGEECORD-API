package fr.acore.bungeecord.api.storage.factory;

import fr.acore.bungeecord.api.manager.IManager;
import fr.acore.bungeecord.api.storage.constraint.query.IQueryConstraint;
import fr.acore.bungeecord.api.storage.table.ITable;
import fr.acore.bungeecord.storage.constraint.QueryConstraint;

import java.util.List;

public interface IDataFactory<T, U extends IManager> {

	public ITable getTable();
	public U getManager();
	
	public void loadAll();
	public void saveAll();
	
	public T load(String contraint, Object... datas);
	public void save(T obj);
	
	public boolean contain(String constraint, Object... datas);
	
	public void insert(T data);
	public void delete(T data);
	public void update(T data);
	public List<T> select(QueryConstraint queryConstraint);
	public T selectFirst(QueryConstraint queryConstraint);
	
}
