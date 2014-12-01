package com.ivanceras.db.server.core;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.ivanceras.commons.conf.DBConfig;
import com.ivanceras.db.api.Aggregate;
import com.ivanceras.db.api.ApiUtils;
import com.ivanceras.db.api.ColumnDataType;
import com.ivanceras.db.api.DeclaredQuery;
import com.ivanceras.db.api.IDatabase;
import com.ivanceras.db.api.ModelDef;
import com.ivanceras.db.api.Query;
import com.ivanceras.db.api.SchemaTable;
import com.ivanceras.db.api.WindowFunction;
import com.ivanceras.db.model.ModelMetaData;
import com.ivanceras.db.shared.DAO;
import com.ivanceras.db.shared.exception.DBConnectionException;
import com.ivanceras.db.shared.exception.DatabaseException;
import com.ivanceras.fluent.sql.SQL;




public class DB_Oracle extends DB_Jdbc implements IDatabase{


	public DB_Oracle(DBConfig config, String overwriteFile)
			throws DatabaseException, DBConnectionException {
		super(config);
	}

	@Override
	String getDriverClass() {
		return "oracle.jdbc.OracleDriver";
	}

	@Override
	String getDBUrl() {
		return "jdbc:oracle:thin:@"+dbHost+":"+dbPort+"/"+dbName;
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
	protected String getAutoIncrementColumnConstraint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getTableSchema(String tableName) throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getUniqueKeys(String schema, String tableName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getRealTableName(String schema, String tableName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ColumnDataType getColumnDetails(String schema, String tableName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public SchemaTable[] getTableNames(String schema, String tablePattern, String[] includedSchema)
//			throws DatabaseException {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	protected String getAutoIncrementColumn(String realTableName)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getDBElementName(ModelDef model, String element) {
		return ApiUtils.getDBElementName(model, element);
	}

	@Override
	protected boolean appendReturningColumnClause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getEquivalentJavaObject(Object record, ModelDef model,
			String column) {
		return null;
	}

	@Override
	protected String forceKeyword() {
		return "CASCADE";
	}

	@Override
	protected boolean useSchema() {
		return true;
	}

	@Override
	protected String getStorageEngine() {
		return "";
	}

//	@Override
//	protected String buildTreeRecursiveSelectStatement(ModelDef model,
//			TreeRecursion tree) {
//		// TODO Auto-generated method stub
//		return null;
//	}

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
	public Query buildSubClassTableQuery(ModelDef model) {
		// TODO Auto-generated method stub
		return null;
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

//	@Override
//	public Map<String, String> getTableColumnComments(String tableName,
//			String schema) throws DatabaseException {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public String getTableComment(String tableName, String schema)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean caseSensitive() {
		// TODO Auto-generated method stub
		return false;
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
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected ForeignKey getExportedKeys(String arg0, String arg1)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ForeignKey getImportedKeys(String arg0, String arg1)
			throws DatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean schemaExist(String arg0) throws DatabaseException {
		// TODO Auto-generated method stub
		return false;
	}


//	@Override
//	public String getTableComment(String table) throws DatabaseException {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public void correctDataTypes(DAO[] daoList, ModelDef model) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object getEquivalentJavaObject(Object record) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SQL buildAggregateQuery(ModelMetaData meta,
			Aggregate[] aggregates, boolean doComma) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SQL buildDeclaredQuery(ModelMetaData meta,
			Map<String, DeclaredQuery> declaredQueries) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SQL buildWindowFunctions(ModelMetaData meta,
			List<WindowFunction> windowFunctions, boolean doComma) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Object getEquivalentDBObject(Object record) {
		// TODO Auto-generated method stub
		return null;
	}

}
