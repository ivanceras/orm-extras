/*******************************************************************************
 * Copyright by CMIL
 ******************************************************************************/
package com.ivanceras.db.server.core;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.ivanceras.commons.conf.DBConfig;
import com.ivanceras.commons.strings.CStringUtils;
import com.ivanceras.db.api.Aggregate;
import com.ivanceras.db.api.ColumnDataType;
import com.ivanceras.db.api.ColumnPair;
import com.ivanceras.db.api.DAO_DataSet;
import com.ivanceras.db.api.DB_Odbms;
import com.ivanceras.db.api.IDatabase;
import com.ivanceras.db.api.JoinPair;
import com.ivanceras.db.api.ModelDef;
import com.ivanceras.db.api.SchemaTable;
import com.ivanceras.db.api.TreeRecursion;
import com.ivanceras.db.model.ModelMetaData;
import com.ivanceras.db.shared.DAO;
import com.ivanceras.db.shared.Filter;
import com.ivanceras.db.shared.Order;
import com.ivanceras.db.shared.datatype.DataTypeGeneric;
import com.ivanceras.db.shared.exception.DataTypeException;
import com.ivanceras.db.shared.exception.DatabaseException;
import com.ivanceras.fluent.sql.SQL;

public class DB_BigTable extends DB_Odbms implements IDatabase{



	public DB_BigTable(DBConfig config)
			throws DatabaseException {
		super(config, null);
		init();
	}

	DatastoreService datastore = null;
	static long writeCalls = 0;
	static boolean countWrites = false;
	Transaction txn = null;
	private boolean isTransacted;

	private boolean debugSql = false;


	public void init(){
		datastore = DatastoreServiceFactory.getDatastoreService();
	}

	public void setDatastoreService(DatastoreService datastore){
		this.datastore = datastore;
	}

	@Override
	public boolean createModel(ModelDef model) throws DatabaseException {
		return false;
	}
	
	/**
	 * Get the parent entity of this model name, to be used in the hasOne field
	 * @param modelName
	 * @return
	 * @throws DatabaseException
	 */
	private String getParentModelName(String modelName) throws DatabaseException{
		Query q = new Query(modelName);
		String msg = "Entity must have at least one record to identify the parent entity";
		q.setKeysOnly();
		Iterator<Entity> result = datastore.prepare(q).asIterator(FetchOptions.Builder.withLimit(1));
		if(result.hasNext()){
			Entity entity = result.next();
			if(entity != null){
				Key parentKey = entity.getParent();
				if(parentKey != null){
					return parentKey.getKind();
				}
			}
			else{
				throw new DatabaseException(msg);
			}
		}
		else{
			throw new DatabaseException(msg);
		}
		return null;
	}
	
	/**
	 * Get the model names that refer this entity as parent, to be used in the hasMany field
	 * @param modelName
	 * @return
	 * @throws DatabaseException 
	 */
	private String[] getChildModelNames(String modelName) throws DatabaseException{
		if(entityRelationshipd == null){
			extractEntityRelation();
		}
		if(entityRelationshipd.containsKey(modelName)){
			Set<String> childEntities = entityRelationshipd.get(modelName);
			if(childEntities != null && childEntities.size() > 0){
				return childEntities.toArray(new String[childEntities.size()]);
			}
		}
		return null;
	}
	
	HashMap<String, Set<String>> entityRelationshipd = null;
	
	private void extractEntityRelation() throws DatabaseException{
		entityRelationshipd = new HashMap<String, Set<String>>();
		SchemaTable[] schTables = getTableNames(null, null, null);
		for(SchemaTable sch : schTables){
			String parentModel = getParentModelName(sch.getTableName());
			if(entityRelationshipd.containsKey(parentModel)){
				Set<String> child = entityRelationshipd.get(parentModel);
				child.add(sch.getTableName());
			}
			else{
				Set<String> emptySet = new HashSet<String>();
				entityRelationshipd.put(parentModel, emptySet);
			}
		}
	}


	@Override
	public DAO[] select(ModelMetaData meta, com.ivanceras.db.api.Query query) throws DatabaseException {

		ModelDef model = query.getModel();
		JoinPair[] joinPairs = query.getJoinPairs();
		Filter[] filters = query.getFilters();
		Long offset = query.getOffset();
		Integer limit = query.getLimit();
		Order[] orders = query.getOrders();
		Boolean selectAllColumns = query.getSelectAllColumns();
		Boolean distinct = query.getDistinct();
		String[] distinctColumns = query.getDistinctColumns();
//		TreeRecursion tree = query.getTree();
		Aggregate[] aggr = query.getAggregate();
		String[] groupedColumns = query.getGroupedColumns();
		Boolean keysOnly = query.getKeysOnly();
		
		if(debugSql && orders != null){
			System.out.println(model.getModelName()+" Order[]: "+Arrays.asList(orders));
		}
		Key[] keys = selectKeys(model, joinPairs, filters, offset, limit, orders);
		DAO[] recordObjs = new DAO[keys.length];
		String[] attributes = model.getAttributes();
		String[] dataTypes = model.getDataTypes();
		for(int i = 0; i < keys.length; i++){
			if(keysOnly != null && keysOnly){
				recordObjs[i] = new DAO(model.getModelName());
				String[] primaryKeys = model.getPrimaryAttributes();
				for(String primary : primaryKeys){
					recordObjs[i].set_Value(primary, keys[i]);
				}
				if(primaryKeys.length > 1){
					throw new DatabaseException("Unable to retrieve via keys only when there are more than 1 primary keys");
				}
			}
			else{
				Key key = keys[i];
				recordObjs[i] = new DAO(model.getModelName());
				Entity entity;
				try {
					entity = datastore.get(key);
					if(attributes != null){
						for(int j = 0; j < attributes.length; j++){
							Object value = entity.getProperty(attributes[j]);
							value = getFromBigTableValue(value, dataTypes[j]);
							recordObjs[i].set_Value(attributes[j], value);
						}
					}
				} catch (EntityNotFoundException e) {
					e.printStackTrace();
					throw new DatabaseException("Entity not found! "+key);
				}
			}
		}
		return recordObjs;
	}



	@Override
	public int delete(ModelDef model, Filter[] filters)
	throws DatabaseException {
		String namespace = model.getNamespace();
		if(useNamespace() && namespace != null){
			NamespaceManager.set(namespace);
		}
		Key[] keys = selectKeys(model, null, filters, null, null, null);
		System.out.println("retrieving entity "+model.getModelName()+" for deletion with filter["+filters+"], got "+keys.length+" records... ");
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		for(int i = 0; i < keys.length; i++){
			datastore.delete(keys[i]);
		}
		return keys.length;
	}

	@Override
	public boolean drop(ModelDef model, boolean forced) throws DatabaseException {
		String namespace = model.getNamespace();
		if(useNamespace() && namespace != null){
			NamespaceManager.set(namespace);
		}
		empty(model, forced);
		return true;
	}

	@Override
	public int empty(ModelDef model, boolean forced) throws DatabaseException {
		return delete(model, null);
	}

	HashMap<String, Entity> entityList = new HashMap<String, Entity>();
	
	
//	@Override
//	public DAO insert(DAO dao, ModelDef model) throws DatabaseException {
//		String namespace = model.getNamespace();
//		if(useNamespace() && namespace != null){
//			NamespaceManager.set(namespace);
//		}
//		Entity entity = null;
//		if(model.getPrimaryOwner() !=null){
//			Key parentKey = createParentKey(dao, model);
//			entity = new Entity(model.getModelName(), parentKey);
//		}
//		else{
//			entity = new Entity(model.getModelName());
//		}
//		String[] attributes = model.getAttributes();
//		for(int i = 0; i < attributes.length; i++){
//			Object value = convertDaoValueToDB(dao, attributes[i]);
//			entity.setProperty(attributes[i], value);
//		}
//		Key key = datastore.put(entity);
//		System.out.println("Inserted "+dao.getModelName()+"_"+dao+"_");
//		System.out.println("\twith key "+key+" and parent "+key.getParent());
//		if(countWrites){
//			writeCalls++;
//		}
//		Long idValue = key.getId();
//		String genColumn = model.getGeneratedAttribute();
//		if(genColumn != null){//update the value of the generated column when there is a generated column defined!
//			if(dao.get_Value(genColumn) == null){//set only when it is null, otherwise it must have come from imports
//				Integer genValue = Integer.valueOf(idValue.toString());
//				dao.set_Value(genColumn, genValue);
//				entity.setProperty(genColumn, genValue);
//				datastore.put(entity);
//				if(countWrites){
//					writeCalls++;
//				}
//			}
//		}
//		return dao;
//	}


//	@Override
//	public DAO update(DAO dao, ModelDef model, Filter[] filters, String[] ignoreColumns)
//	throws DatabaseException {
//		String namespace = model.getNamespace();
//		if(useNamespace() && namespace != null){
//			NamespaceManager.set(namespace);
//		}
//		Key[] keys = selectKeys(model, null, filters, null, null, null);
//		System.out.println("retrieving entity "+model.getModelName()+" for update with filter["+filters+"], got "+keys.length+" records... ");
//		for(Filter f : filters){
//			System.out.println(f);
//		}
//		for(int i = 0; i < keys.length; i++){
//			Key key = keys[i];
//			System.out.println("retrieving entity "+model.getModelName()+"["+keys[i]+"]");
//			Entity entity = null;
//			try {
//				entity = datastore.get(key);
//				if(entity == null){
//					throw new DatabaseException("Unable to retrieve entity for updating "+key+" ["+keys[i]+"]");
//				}
//				String[] attributes = model.getAttributes();
//				for(int j = 0; j < attributes.length; j++){
//					Object value = convertDaoValueToDB(dao, attributes[j]);
//					entity.setProperty(attributes[j], value);
//				}
//				Key updatekey = datastore.put(entity);
//				Long idValue = updatekey.getId();
//				System.out.println("update idValue: "+idValue);
//				if(countWrites){
//					writeCalls++;
//				}
//			} catch (EntityNotFoundException e) {
//				e.printStackTrace();
//				throw new DatabaseException("Entity not found!");
//			}
//
//		}
//		return dao;
//	}


	public Key[] selectKeys(ModelDef model, JoinPair[] joinPairs, Filter[] filters, Long offset, Integer limit, Order[] orders) throws DatabaseException {
		String namespace = model.getNamespace();
		if(useNamespace() && namespace != null){
			NamespaceManager.set(namespace);
		}
		if(joinPairs != null){
			throw new DatabaseException("BigTable has not supported this feature yet!");
		}
		else{
			Query q = new Query(model.getModelName());
			q.setKeysOnly();//we don't need the rest
			String[] attributes = model.getAttributes();
			String[] dataTypes = model.getDataTypes();
			if(filters != null){
				for(Filter f : filters){
					String[] filterColumn = f.attribute.split("\\.");
					String column = filterColumn[filterColumn.length-1];
					String dataType = dataTypes[CStringUtils.indexOf(attributes, column)];
					q.addFilter(f.attribute, getEquivOp(f.operator), getFilterValue(f.value, dataType));
				}
			}
			if(orders != null && orders.length > 0){
				for(Order order : orders){
					String orderColumn = order.getColumn();
					SortDirection sortDirection = order.isAscending() ? SortDirection.ASCENDING : SortDirection.DESCENDING;
					q.addSort(orderColumn, sortDirection);
				}
			}
			if(debugSql){
				System.out.println("Namespace: "+namespace+", Query: "+q.toString());
			}
			PreparedQuery pq = datastore.prepare(q);
			Iterator<Entity> results = null;
			if(limit != null && offset != null){
				results = pq.asIterator(FetchOptions.Builder.withLimit(limit).offset(offset.intValue()));
			}else{
				results = pq.asIterator();
			}
			List<Key> keys = new ArrayList<Key>();
			while(results.hasNext()){
				Entity ent = results.next();
				keys.add(ent.getKey());
			}
			return keys.toArray(new Key[keys.size()]);
		}
	}

	private Query.FilterOperator getEquivOp(String operator) throws DatabaseException{
		if(operator.equals(Filter.EQUAL)) return Query.FilterOperator.EQUAL;
		if(operator.equals(Filter.GREATER_THAN)) return Query.FilterOperator.GREATER_THAN;
		if(operator.equals(Filter.GREATER_THAN_OR_EQUAL)) return Query.FilterOperator.GREATER_THAN_OR_EQUAL;
		if(operator.equals(Filter.IN)) return Query.FilterOperator.IN;
		if(operator.equals(Filter.LESS_THAN)) return Query.FilterOperator.LESS_THAN;
		if(operator.equals(Filter.LESS_THAN_OR_EQUAL)) return Query.FilterOperator.LESS_THAN_OR_EQUAL;
		if(operator.equals(Filter.NOT_EQUAL)) return Query.FilterOperator.NOT_EQUAL;
		if(operator.equals(Filter.LIKE)) throw new DatabaseException("Can not use Like in DB_BigTable");
		return Query.FilterOperator.EQUAL;
	}


	@Override
	public void beginTransaction() throws DatabaseException {
		txn = datastore.beginTransaction();
		isTransacted = false;
	}

	@Override
	public void commitTransaction() throws DatabaseException {
		txn.commit();
		isTransacted = true;
	}

	@Override
	public void rollbackTransaction() throws DatabaseException {
		txn.rollback();
		isTransacted = true;
	}

	@Override
	public boolean isTransacted() throws DatabaseException {
		return isTransacted;
	}

	@Override
	public boolean isClosed() {
		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}



	@Override
	public ModelDef getModelMetaData(String schema, String tableName) throws DatabaseException {
		boolean caseSensitive = true;
		System.err.println("\t\tRetrieving meta data from Google Big ass table "+tableName);
		getColumnDetails(schema, tableName);
		ModelDef model = new ModelDef();
		model.setNamespace(schema);
		model.setModelName(tableName);
		model.setCaseSensitive(caseSensitive);
		ColumnDataType columns = getColumnDetails(schema, tableName);
		model.setAttributes(columns.getColumns());
		model.setDataTypes(columns.getDataTypes());
		
		String hasOne = getParentModelName(tableName);
		String[] hasOnes = {hasOne};
		model.setHasOne(hasOnes);

		String[] hasMany = getChildModelNames(tableName);
		model.setHasMany(hasMany);
		
		System.out.println(model);
		return model;
	}

	protected ColumnDataType getColumnDetails(String schema, String tableName) throws DatabaseException{
		List<String> attributeList = new ArrayList<String>();
		List<String> dataTypeList = new ArrayList<String>();
		Query q = new Query(Query.PROPERTY_METADATA_KIND);
		q.setAncestor(KeyFactory.createKey(Query.KIND_METADATA_KIND, tableName));
		for (Entity e : datastore.prepare(q).asIterable()) {
			String modelName = e.getKey().getParent().getName();
			String attribute = e.getKey().getName();
			System.out.println("Property "+modelName+": "+attribute);

			String[] representations = representationsOf(datastore, tableName, attribute);
			String officialDataType = getOfficialDataType(representations);
			String genericDataType = getEquivalentGeneralDataType(officialDataType);
			attributeList.add(attribute);
			dataTypeList.add(genericDataType);
		}
		ColumnDataType columns = new ColumnDataType();
		columns.setColumns(attributeList.toArray(new String[attributeList.size()]));
		columns.setDataTypes(dataTypeList.toArray(new String[dataTypeList.size()]));
		return columns;
	}

	private String[] representationsOf(DatastoreService ds, String kind, String property) {
		Query q = new Query(Query.PROPERTY_METADATA_KIND);
		Key parent = KeyFactory.createKey(Query.KIND_METADATA_KIND, kind);
		Key ancestor = KeyFactory.createKey(parent, Query.PROPERTY_METADATA_KIND, property);
		q.setAncestor(ancestor);
		Entity propInfo = ds.prepare(q).asSingleEntity();
		Collection<String> representations = (Collection<String>) propInfo.getProperty("property_representation");

		String[] dataTypes = new String[representations.size()];
		int i = 0;
		for(String dt : representations){
			dataTypes[i++] = dt;
		}
		return dataTypes;
	}

	public String getOfficialDataType(String[] representations){
		String officialType = null;
		if(representations.length == 1){
			officialType = representations[0];
		}
		else{
			officialType = representations[0];
			for(String rep : representations){
				if(officialType != NULL){
					officialType = rep;
				}
			}
		}
		return officialType;
	}

	public static final String STRING = "STRING";
	public static final String INT64 = "INT64";
	public static final String DOUBLE = "DOUBLE";
	public static final String BOOLEAN = "BOOLEAN";
	public static final String USER = "USER";
	public static final String REFERENCE = "REFERENCE";
	public static final String POINT = "POINT";
	public static final String NULL = "NULL";

	public String getEquivalentGeneralDataType(String googledt){
		if(googledt.equals(STRING)){
			return DataTypeGeneric.STRING;
		}
		else if(googledt.equals(INT64)){
			return DataTypeGeneric.INTEGER;
		}
		else if(googledt.equals(DOUBLE)){
			return DataTypeGeneric.DOUBLE;
		}
		else if(googledt.equals(BOOLEAN)){
			return DataTypeGeneric.BOOLEAN;
		}
		else if(googledt.equals(NULL)){
			return DataTypeGeneric.STRING;
		}
		else{
			try {
				throw new DataTypeException("No Equivalent General Data type found for DB datatype["+googledt+"]");
			} catch (DataTypeException e) {
				e.printStackTrace();
			}
			return null;
		}
	}


	@Override
	public SchemaTable[] getTableNames(String owner, String tablePattern, String[] includedSchema)
	throws DatabaseException {
		Query q = new Query(Query.KIND_METADATA_KIND);
		System.err.println("Getting tables in BigTable");
		PreparedQuery result = datastore.prepare(q);
		List<SchemaTable> schemaTables = new ArrayList<SchemaTable>();
		for(Entity e : result.asIterable()){
			String namespace = e.getKey().getNamespace();
			String modelName = e.getKey().getName();
			System.out.println("entity: "+modelName);
			SchemaTable schtable = new SchemaTable(namespace, modelName);
			schemaTables.add(schtable);
		}
		return schemaTables.toArray(new SchemaTable[schemaTables.size()]);
	}



	protected Object convertDaoValueToDB(DAO dao, String column){
		if(dao == null) return null;
		Object value = dao.get_Value(column);
		if(value == null){
			return null;
		}
		if(value.getClass().equals(java.math.BigDecimal.class)){
			return ((BigDecimal)value).doubleValue();
		}
		else if(value.getClass().equals(java.math.BigInteger.class)){
			return ((BigInteger)value).longValue();
		}
		else if(value.getClass().equals(java.lang.String.class) && value != null && ((String)value).length() > 500){
			Text text = new Text((String)value);
			return text;
		}
		else{
			return value;
		}
	}
	public static Object getFilterValue(Object value, String dataType){
		if(value == null){
			return null;
		}
		if(dataType.equals("java.math.BigDecimal")){
			return ((BigDecimal)value).doubleValue();
		}
		else if(dataType.equals("java.math.BigInteger")){
			return ((BigDecimal)value).longValue();
		}
		else if(value.getClass().equals(java.lang.String.class) && value != null && ((String)value).length() > 500){
			Text text = new Text((String)value);
			return text;
		}
		else{
			return value;
		}
	}

	/**
	 * From the rdbms database to serializable DAO
	 * @param dao
	 * @param column
	 * @param value
	 * @return
	 */
	private static Object getFromBigTableValue(Object value, String dataType){
		if(value == null){
			return null;
		}
		if(dataType.equals(DataTypeGeneric.STRING) && value != null && value.getClass().equals(Text.class)){
			String strvalue = ((Text)value).getValue();
			return strvalue;
		}
		else if(value instanceof Long && dataType.equals(DataTypeGeneric.INTEGER)){
			return Integer.parseInt(value.toString());
		}
		else if(dataType.equals(DataTypeGeneric.INTEGER)){
			return value;
//			return Integer.valueOf(value.toString());//coud be long in the database
		}
		else if(dataType.equals(DataTypeGeneric.TIMESTAMP)){
			Date date = new Date();
			date.setTime(((Timestamp)value).getTime());
			return date;
		}
		else if(dataType.equals(DataTypeGeneric.BIGDECIMAL)){
			BigDecimal bdvalue = new BigDecimal((Double)value);
			return bdvalue;
		}
		else if(dataType.equals(DataTypeGeneric.BIGINTEGER)){
			return (BigInteger) value;
		}
		else if(dataType.equals(DataTypeGeneric.FLOAT)){
			return Float.parseFloat(value.toString());
		}
		else{
			return value;
		}
	}

	@Override
	public boolean createSchema(String schema) throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean dropNamespace(String schema, boolean forced) throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void debugSql(boolean b) {
		this.debugSql = b;
	}

	@Override
	public long writeToBlob(byte[] buf) throws DatabaseException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getBlob(long oid) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setForeignConstraint(ModelDef model)
	throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean useNamespace(){
		return true;
	}

	@Override
	public String[] getSubClasses(String tableName) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getParentClass(String tableName) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean renameModel(ModelDef model, String newName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long writeToBlob(String filename) throws DatabaseException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToOutputStream(Long blob_data, OutputStream out)
			throws DatabaseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public Map<String, String> getTableColumnComments(String tableName,
			String schema) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTableComment(String tableName, String schema)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DBConfig getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws DatabaseException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean prependTableName() {
		return false;
	}

	

	@Override
	public boolean existModel(ModelDef model) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setModelMetaDataDefinition(ModelMetaData metaData1)
			throws DatabaseException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModelMetaData getModelMetaDataDefinition() throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DAO update(DAO dao, ModelDef model, Filter[] filters)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SQL buildSQL(ModelMetaData meta, com.ivanceras.db.api.Query query,
			boolean useCursor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends DAO> T[] select(SQL sql,
			Map<String, ColumnPair> renamedColumns) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTableComment(String table) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DAO insert(DAO dao, ModelMetaData meta, ModelDef model,
			com.ivanceras.db.api.Query query) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void search(com.ivanceras.db.api.Query query, String keyword) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void correctDataTypes(DAO[] daoList, ModelDef model) {
		// TODO Auto-generated method stub
		
	}


}
