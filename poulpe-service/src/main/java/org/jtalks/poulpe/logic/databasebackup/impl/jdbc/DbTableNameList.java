package org.jtalks.poulpe.logic.databasebackup.impl.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.jtalks.poulpe.logic.databasebackup.impl.dto.TableForeignKey;

import com.google.common.collect.Lists;

/**
 * The class represents a list of tables which a database has. While providing the list of database tables class also
 * resolves tables dependencies problems which can occur when the dump file is being generated.
 * 
 * When the dump file is being generated it contains CREATE TABLE statements for each table from the database.
 * Unfortunately it's not possible to create tables (run CREATE TABLE statements) in free order because of relationships
 * (via foreign keys) between tables. So tables should be created in the order when table under creation is not
 * dependent on tables which are not created yet.
 * 
 * To resolve this the class before returning the list of database tables sorts it so table names will be arranged in
 * the order when a table in the list is independent on the tables below. When this is ready we can export tables data
 * from the top of the list to its bottom.
 * 
 * @author Evgeny Surovtsev
 * 
 */
public final class DbTableNameList {
    /**
     * Disable creation objects of the class.
     */
    private DbTableNameList() {
    }

    /**
     * Returns the list of all database table names which database contains from given Data source via JDBC.
     * 
     * @param dataSource
     *            A data source which will be used to connect to the database.
     * @return a List of Strings where every String instance represents a table name from the database.
     * @throws SQLException
     *             is thrown if there is an error during collaborating with the database.
     */
    public static List<String> getPlainList(final DataSource dataSource) throws SQLException {
        Connection connection = null;
        ResultSet tablesResultSet = null;
        List<String> tableNames = new ArrayList<String>();
        try {
            connection = dataSource.getConnection();
            DatabaseMetaData dbMetaData = connection.getMetaData();
            tablesResultSet = dbMetaData.getTables(null, null, null, new String[] { "TABLE" });
            while (tablesResultSet.next()) {
                tableNames.add(tablesResultSet.getString("TABLE_NAME"));
            }
        } finally {
            if (tablesResultSet != null) {
                tablesResultSet.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return tableNames;
    }

    /**
     * The method is used to resolve dependencies for given list of table names (i.e. sorts table names so less
     * dependent tables will be on top and more dependent tables will be on the bottom).
     * 
     * @param dataSource
     *            A data source which will be used to connect to the database.
     * @return A sorted list of table names where less dependent table are at a top of the list.
     * @throws SQLException
     *             Is thrown in case any errors during work with database occur.
     */
    public static List<String> getIndependentList(final DataSource dataSource) throws SQLException {
        final List<String> tableNames = getPlainList(dataSource);

        List<TableDependencies> tablesAndTheirDependencies = Lists.newArrayList();
        for (String tableName : tableNames) {
            TableDependencies tableDependencies = new TableDependencies(tableName);
            List<TableForeignKey> foreignKeyList = getForeignKeyList(dataSource, tableName);
            for (TableForeignKey foreignKey : foreignKeyList) {
                tableDependencies.addDependency(foreignKey.getPkTableName());
            }
            tablesAndTheirDependencies.add(tableDependencies);
        }
        Collections.sort(tablesAndTheirDependencies, new Comparator<TableDependencies>() {
            @Override
            public int compare(final TableDependencies o1, final TableDependencies o2) {
                Set<String> o1Dependencies = o1.getDependencies();
                Set<String> o2Dependencies = o2.getDependencies();

                if (o1Dependencies.contains(o2.getTableName()) && o2Dependencies.contains(o1.getTableName())) {
                    // cross dependency - this should never happen!
                    return 0;
                } else if (o1Dependencies.contains(o2.getTableName())
                        || (o1Dependencies.size() > 0 && o2Dependencies.size() == 0)) {
                    return 1;
                } else if (o2Dependencies.contains(o1.getTableName())
                        || (o1Dependencies.size() == 0 && o2Dependencies.size() > 0)) {
                    return -1;
                }

                return 0;
            }
        });

        List<String> tables = new ArrayList<String>();
        for (TableDependencies dependencies : tablesAndTheirDependencies) {
            tables.add(dependencies.getTableName());
        }
        return tables;
    }

    // TODO: remove the function
    public static List<TableForeignKey> getForeignKeyList(final DataSource dataSource, final String tableName)
            throws SQLException {

        List<TableForeignKey> tableForeignKeyList = new ArrayList<TableForeignKey>();
        Connection connection = null;
        ResultSet foreignKeys = null;
        try {
            connection = dataSource.getConnection();
            DatabaseMetaData dbMetaData = connection.getMetaData();
            foreignKeys = dbMetaData.getImportedKeys(null, null, tableName);
            while (foreignKeys.next()) {
                if (foreignKeys.getString("FK_NAME") != null) {
                    tableForeignKeyList.add(new TableForeignKey(
                            foreignKeys.getString("FK_NAME"),
                            foreignKeys.getString("FKCOLUMN_NAME"),
                            foreignKeys.getString("PKTABLE_NAME"),
                            foreignKeys.getString("PKCOLUMN_NAME")));
                }
            }
        } finally {
            if (foreignKeys != null) {
                foreignKeys.close();
            }
            if (connection != null) {
                connection.close();
            }
        }

        return tableForeignKeyList;
    }
}

/**
 * The class is used as a container for keeping information about database table dependencies.
 * 
 * @author Evgeny Surovtsev
 * 
 */
final class TableDependencies {
    /**
     * Constructor creates a new instance of Table Dependencies object for a givenTable Name.
     * 
     * @param tableName
     *            Specifies a Table Name which a Table Dependencies will be stored for.
     */
    TableDependencies(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Returns Database table name for the current Dependency Table object.
     * 
     * @return Database Table Name
     */
    String getTableName() {
        return tableName;
    }

    /**
     * Returns Dependencies (table names) for the current Table object.
     * 
     * @return Set of the table names which the current table is dependent on.
     */
    Set<String> getDependencies() {
        return new HashSet<String>(dependencies);
    }

    /**
     * Adds a new table name which the current table is dependent on.
     * 
     * @param dependency
     *            Table name which the current table is dependent on.
     */
    void addDependency(final String dependency) {
        dependencies.add(dependency);
    }

    private final String tableName;
    private final Set<String> dependencies = new HashSet<String>();
}
