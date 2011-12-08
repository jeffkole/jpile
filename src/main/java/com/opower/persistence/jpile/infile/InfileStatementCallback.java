package com.opower.persistence.jpile.infile;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.google.common.base.Throwables;
import com.mchange.v2.c3p0.C3P0ProxyStatement;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.StatementCallback;

/**
 * Generic Spring callback for executing the 'LOAD DATA INFILE' pattern of streaming data in
 * batch to MySQL. This will not work on databases other than MySQL. It requires simply
 * an SQL statement to execute, and an input stream from which to read. Since infile loads
 * do not stop for exceptional inserts necessarily, the driver collects all of the issues in
 * a {@link SQLWarning}. Since these statements do not return anything else that is meaningful, we
 * return this warning object, or null if there were no issues.
 * <p>
 * This class depends not only on the MySQL Connector-J driver, but also on the C3P0 connection pool.
 * The latter wraps all statements in a proxy, so when using the connection pool you must use its API
 * to access the underlying MySQL statement. This class hides all of this tomfoolery behind a very
 * simple facade.
 * <p>
 * Instances of this class are safe for use by multiple threads.
 *
 * @author s-m
 * @see <a href="http://dev.mysql.com/doc/refman/5.1/en/load-data.html">LOAD DATA INFILE reference</a>
 * @see C3P0ProxyStatement#rawStatementOperation(java.lang.reflect.Method, Object, Object[])
 * @see com.mysql.jdbc.Statement#setLocalInfileInputStream(java.io.InputStream)
 * @since 1.0
 */
// CR MB:  I don't think this is thread safe, it has a stateful object as an instance variable (inputStream)
public class InfileStatementCallback implements StatementCallback<List<Exception>> {

    // Infile method name for reflection lookup.
    // This is the MySQL driver dependency. We don't load the class, but this is the name
    // of the method on the MySQL statement that we invoke via reflection.
    private static final String INFILE_MUTATOR_METHOD = "setLocalInfileInputStream";

    // SQL statement
    private String loadInfileSql;
    // Source of data.
    private InputStream inputStream;

    /**
     * Constructs a callback from a SQL statement and a data stream from which to read.
     *
     * @param loadInfileSql to execute
     * @param inputStream   from which to read
     */
    public InfileStatementCallback(String loadInfileSql, InputStream inputStream) {
        this.loadInfileSql = loadInfileSql;
        this.inputStream = inputStream;
    }

    @Override
    public List<Exception> doInStatement(Statement statement) throws SQLException, DataAccessException {
        try {
            if(statement instanceof C3P0ProxyStatement) {
                Method m = com.mysql.jdbc.Statement.class.getMethod(INFILE_MUTATOR_METHOD, new Class[]{InputStream.class});
                C3P0ProxyStatement proxyStatement = (C3P0ProxyStatement) statement;
                proxyStatement.rawStatementOperation(m, C3P0ProxyStatement.RAW_STATEMENT, new Object[]{this.inputStream});
            }
            else if(statement instanceof com.mysql.jdbc.Statement) {
                com.mysql.jdbc.Statement mysqlStatement = (com.mysql.jdbc.Statement) statement;
                mysqlStatement.setLocalInfileInputStream(this.inputStream);
            }
            statement.execute(loadInfileSql);
            return extractWarnings(statement.getWarnings());
        }
        catch(NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
        catch(IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
        catch(InvocationTargetException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Adds all of the warnings in the chain of a passed warning to a collection.
     *
     * @param warning result from a bulk load operation
     * @return list of warnings
     */
    private List<Exception> extractWarnings(SQLWarning warning) {
        List<Exception> warnings = new ArrayList<Exception>(1000);
        while(warning != null) {
            warnings.add(warning);
            warning = warning.getNextWarning();
        }

        return warnings;
    }
}
