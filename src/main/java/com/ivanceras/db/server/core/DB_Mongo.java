package com.ivanceras.db.server.core;

import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBDecoder;
import com.mongodb.DBEncoder;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.MapReduceOutput;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.ivanceras.commons.conf.DBConfig;
import com.ivanceras.db.api.Aggregate;
import com.ivanceras.db.api.ColumnPair;
import com.ivanceras.db.api.DAO_DataSet;
import com.ivanceras.db.api.DB_Odbms;
import com.ivanceras.db.api.IDatabase;
import com.ivanceras.db.api.JoinPair;
import com.ivanceras.db.api.ModelDef;
import com.ivanceras.db.api.Query;
import com.ivanceras.db.api.SchemaTable;
import com.ivanceras.db.api.TreeRecursion;
import com.ivanceras.db.model.ModelMetaData;
import com.ivanceras.db.shared.DAO;
import com.ivanceras.db.shared.Filter;
import com.ivanceras.db.shared.Order;
import com.ivanceras.db.shared.exception.DBConnectionException;
import com.ivanceras.db.shared.exception.DatabaseException;
import com.ivanceras.fluent.sql.SQL;

public class DB_Mongo extends DB_Odbms implements IDatabase{

	DB db = null;

	public DB_Mongo(DBConfig config, String overwriteFile) throws DBConnectionException, DatabaseException {
		super(config, overwriteFile);
		Mongo mongo;
		try {
			int portNo = Integer.parseInt(dbPort);
			mongo = new Mongo(dbHost , portNo );
			db = mongo.getDB( dbName );
			boolean auth = true;
//			auth = db.authenticate(dbUser, dbPassword.toCharArray());
			if(auth){
				System.out.println("Connected to a MongoDB at "+dbHost+"/"+dbPort+"/"+dbName);
				init();
			}
			else{
				throw new DBConnectionException("Unable to connect database to "+dbHost+"/"+dbPort+"/"+dbName);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		} 
	}

	protected void init(){

	}

	@Override
	public void beginTransaction() throws DatabaseException {
		// TODO Auto-generated method stub

	}

	@Override
	public void commitTransaction() throws DatabaseException {
		// TODO Auto-generated method stub

	}

	@Override
	public void rollbackTransaction() throws DatabaseException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isTransacted() throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

//	@Override
//	public DAO[] select(ModelMetaData meta, ModelDef model,
//			JoinPair[] joinPairs, Filter[] filters, Integer offset,
//			Integer limit, Boolean doCount, Order[] orders,
//			Boolean selectAllColumns, Boolean distinct,
//			String[] distincColumns, Boolean keysOnly, TreeRecursion tree,
//			Aggregate[] aggr, String[] groupedColumns) throws DatabaseException {
//		String collectionName = model.getNamespace()+"."+model.getModelName();
//		DBCollection collection = db.getCollection(collectionName);
//		
//		String[] primaryAttributes = model.getPrimaryAttributes();
//		DBObject primaryIndex = new BasicDBObject();
//		for(String pri : primaryAttributes){
//			primaryIndex.put(pri, 1);
//		}
//		collection.ensureIndex(primaryIndex);
//		
//		BasicDBObject queryFilter = getProcessFilter(filters);
//		DBCursor cursor = collection.find(queryFilter);
//		String[] attributes = model.getAttributes();
//		
//		List<DAO> daoList = new ArrayList<DAO>();
//		
//		while(cursor.hasNext()){
//			DBObject dbObject = cursor.next();
//			DAO dao = new DAO(model.getModelName());
//			for(String att : attributes){
//				Object value = dbObject.get(att);
//				dao.set_Value(att, value);
//			}
//			daoList.add(dao);
//		}
//		return daoList.toArray(new DAO[daoList.size()]);
//	}

	private BasicDBObject getProcessFilter(Filter... filters) {
		BasicDBObject query = new BasicDBObject();
		for(Filter f : filters){
			String attribute = f.attribute;
			String operator = f.operator;
			Object filterValue = f.value;
			if(operator.equals(Filter.EQUAL)){
				query.put(attribute, filterValue);
			}
			else{
				String operatorEquiv = getOperatorEquiv(operator);
				BasicDBObject complexQuery = new BasicDBObject();
				complexQuery.put(operatorEquiv, filterValue);
				query.put(attribute, complexQuery);
			}
//			Filter[] childFilters = f.getFilterList();
//			if(childFilters != null){
//				BasicDBObject childQueryFilter = getProcessFilter(childFilters);
//				
//			}
			
		}
		return query;
	}
	
	private String getOperatorEquiv(String operator){
		if(operator.equals(Filter.GREATER_THAN)){
			return "$gt";
		}
		else if(operator.equals(Filter.LESS_THAN)){
			return "$lt";
		}
		else if(operator.equals(Filter.GREATER_THAN_OR_EQUAL)){
			return "$gte";
		}
		else if(operator.equals(Filter.LESS_THAN_OR_EQUAL)){
			return "$lte";
		}
		else if(operator.equals(Filter.IN)){
			return "$in";
		}
		else if(operator.equals(Filter.NOT_EQUAL)){
			return "$ne";
		}
		else if(operator.equals(Filter.LIKE)){
			return "$regex";
		}
		return operator;
	}

	@Override
	public boolean createModel(ModelDef model) throws DatabaseException {
		return false;
	}

//	@Override
//	public DAO insert(DAO dao, ModelDef model) throws DatabaseException {
//		String collectionName = model.getNamespace()+"."+model.getModelName();
//		DBCollection collection = db.getCollection(collectionName);
//		BasicDBObject doc = new BasicDBObject();
//		String[] attributes = model.getAttributes();
//		for(String att: attributes){
//			Object value = dao.get_Value(att);
//			doc.put(att, value);
//		}
//		String generated = model.getGeneratedAttribute();
//		System.out.println("Generated "+generated);
//		if(generated != null){
//			doc.put(generated, getSequence(model));
//		}
//		System.out.println("doc: "+doc);
//		WriteResult result = collection.insert(doc);
//		if(result.getError() ==null){
//			return dao;
//		}
//		else{
//			throw new DatabaseException(result.getError());
//		}
//	}
	
	public long getSequence(ModelDef model){
		DBCollection collection = db.getCollection("appd.ad_sequence");
		BasicDBObject seqo = new BasicDBObject();
		seqo.put("name", model.getNamespace()+"."+model.getModelName());
		BasicDBObject exseqo = (BasicDBObject) collection.findOne(seqo);
		Long seqVal = null;
		if(exseqo != null){
			System.out.println("exseqo: "+exseqo);
			seqVal = exseqo.getLong("seq");
			seqVal = seqVal != null ? seqVal+1 : 1;
			exseqo.put("seq", seqVal);
			collection.save(exseqo);
			return seqVal;
		}
		else{
			seqo.put("seq", 1);
			collection.insert(seqo);
			return 1;
		}
	}
	
//	@Override
//	public DAO update(DAO dao, ModelDef model, Filter[] filters)
//			throws DatabaseException {
//		String collectionName = model.getNamespace()+"."+model.getModelName();
//		DBCollection collection = db.getCollection(collectionName);
//		
//		DBObject queryFilter = getProcessFilter(filters);
//		collection.findAndModify(queryFilter, fromDAO(dao));
//		DAO[] daoList = select(null, model, null, filters, null, 1, null, null, null, null, null, null, null, null);
//		if(daoList != null && daoList.length > 0){
//			return daoList[0];
//		}
//		else{
//			throw new DatabaseException("No records has been updated");
//		}
//	}
	
	
	
	private DBObject fromDAO(DAO dao){
		if(dao == null){
			return null;
		}
		DBObject dbObject = new BasicDBObject();
		dbObject.putAll(dao.getProperties());
		return dbObject;
	}
	
	private DAO fromDBObject(String modelName, DBObject dbObject){
		if(dbObject == null){
			return null;
		}
		DAO dao = new DAO(modelName);
		dao.setProperties((HashMap<String, Object>)(dbObject.toMap()));
		return dao;
	}

	@Override
	public int delete(ModelDef model, Filter[] filters)
			throws DatabaseException {
		String collectionName = model.getNamespace()+"."+model.getModelName();
		DBCollection collection = db.getCollection(collectionName);
		DBObject filter = getProcessFilter(filters);
		WriteResult result = collection.remove(filter);
		return result.getN();
	}

	@Override
	public int empty(ModelDef model, boolean forced) throws DatabaseException {
		return delete(model, null);
	}

	@Override
	public boolean drop(ModelDef model, boolean forced)
			throws DatabaseException {
		return empty(model, forced) > 0;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reset() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ModelDef getModelMetaData(String schema, String tableName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SchemaTable[] getTableNames(String schema, String tablePattern, String[] includedSchema)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean createSchema(String schema) throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean dropNamespace(String schema, boolean forced)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void debugSql(boolean b) {
		// TODO Auto-generated method stub

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

	public static void main(String[] args){
		Mongo m;
		try {
			m = new Mongo( "localhost" , 27017 );
			DB db = m.getDB( "ivanceras" );	
			String user = "lee";
			String passwd = "Xxie$cndhE";
			//			boolean auth = db.authenticate(user, passwd);
						Set<String> colls = db.getCollectionNames();

			DB db2 = m.getDB("quotesdb");
			DBCollection collection = db2.getCollection("books.quotesCollection");
			BasicDBObject document = new BasicDBObject();
			collection.insert(document);

			document.put("id", 8888);
			document.put("quote", "Long range planning does not deal with future decisions, but with the future of present decisions.");
			document.put("author", "- Peter F. Drucker ");

						for (String s : colls) {
						    System.out.println("colls: "+s);
						}
			DBCollection coll = db.getCollection("ads.testCollectionNoNulls");
			System.out.println("From "+coll.getName());
			BasicDBObject doc = new BasicDBObject();

			doc.put("name", "MongoDB");
			doc.put("last", null);
			doc.put("type", "database");
			doc.put("count", 1);
			
			BasicDBObject info = new BasicDBObject();
			info.put("x", 203);
			info.put("y", 102);
			doc.put("info", info);
			coll.insert(doc);
			DBCursor myDoc = coll.find();
			while(myDoc.hasNext()){
				System.out.println(myDoc.next());
			}
			
			
			DBCollection mrcoll = collection;
			String map = "function() { key = 8888" +
	                   "emit( key, { count: 1, sum: 1 } );";
			
			String reduce = "function( quotes, values ) { var n = { count: 0, sum: 0};" +
	                   " for ( var i = 0; i < values.length; i ++ ) { n.sum += values[i].sum; " + 
	                   " n.count += values[i].count; } return n; }";
			
			 String outputCollection = "function( sum, value ) { value.avg = value.sum / value.count; return value; }";
			 
			OutputType type = OutputType.INLINE;
			DBObject query = new BasicDBObject();
			query.put("id", 8888);
			MapReduceCommand mrcommand = new MapReduceCommand(mrcoll, map, reduce, outputCollection, type, query );
			
			MapReduceOutput output = collection.mapReduce(mrcommand);
			System.out.println("MapReduceOutput: "+output.getOutputCollection());


		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean setForeignConstraint(ModelDef model)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DAO[] select(ModelMetaData meta, Query query)
			throws DatabaseException {
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
		// TODO Auto-generated method stub
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
	public SQL buildSQL(ModelMetaData meta, Query query, boolean useCursor) {
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
	public DAO insert(DAO dao, ModelMetaData meta, ModelDef model, Query query)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void search(Query query, String keyword) {
		// TODO Auto-generated method stub
		
	}

	//	private static void basicQuery(){
	//		 BasicDBObject searchQuery = new BasicDBObject();
	//		 searchQuery.put("id", 8888);
	//		 DBCursor cursor = collection.find(searchQuery);
	//	}

}
