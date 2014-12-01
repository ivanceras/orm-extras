package com.ivanceras.db.api;

import com.ivanceras.db.shared.Filter;

/**
 * Provides a way to replicate Changes of Database tables used in the entire application system
 * The replicator will have to use changelog tables which list down the changes of the system.
 * The changelog table can be used as an interface of the app to write down the changes there.
 * An active replicator can perform a table scan of all tables in all of the connected systems then reconcile this changes.
 * Comparison of values will be per column or each row. 
 * A hash is computed by concatenating the string values of each row hashes that don't match will be logged into the changelog table
 * 
 * Primary keys and Unique indexes can be used for ordering records when synching, so we don't
 * require tables to have a specifi column.
 * 
 * @author lee
 *
 */
public class Replicator {
	
	boolean twoWaySync = true;
	
	int itemsPerPage; //the items to process per replication process
	int page; //keeps track of the offset of the page
	EntityManager master; // replication can be configure one way sync or two way sync
	EntityManager slave;
	
	/**
	 * Slave usually only contains information that only pertains to that user
	 * i.e, if this user is only using data for his own,
	 * filter will be like Filter(createdby, Filter.EQUAL, "jsmith");
	 * 
	 * If the data is shared by people within the same organization,
	 * then filter will be Filter(organization_id, Filter.EQUAL, "acme corp");
	 * 
	 * 
	 * TODO: records that is shared all though out the entire systems
	 * such as global categories, countries, flags etc.
	 */
	Filter[] filters;// the filters to be applied when synching tables.
	
	
	
	public Replicator(EntityManager master, EntityManager slave){
		this.master = master;
		this.slave = slave;
	}
	
	public void tableScanner(){
		
	}
	
	/**
	 * Use primary keys to get distinction of each record,
	 * If there is no primary keys use the unique indexes, else throw an error
	 * @param model
	 */
	public void scanTable(ModelDef model){
		String[] primaryKeys = model.getPrimaryAttributes();
		if(primaryKeys == null){
			System.err.println("Can not sync table "+model.getTableName()+", it has no primary keys");
		}
		if(primaryKeys != null && primaryKeys.length < 1){
			System.err.println("Can not sync table "+model.getTableName()+", it has no primary keys");
		}
		diffTable(model);
	}
	
	/**
	 * Use primary keys for distinction of records,
	 * 
	 * @param model
	 */
	private void diffTable(ModelDef model) {
		
	}

}
