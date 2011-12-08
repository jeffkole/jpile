package com.opower.persistence.jpile.infile;

import java.util.Date;

/**
 * A chaining interface for adding data to a row. Implementations handle encoding objects for use in an infile stream.
 * Each append method also inserts necessary field separators, as needed.
 *
 * @author s-m
 * @since 1.0
 */
public interface InfileRow {

    /**
     * Provides an empty row by clearing and returning this row.
     *
     * @return empty row
     */
    InfileRow newRow();

    /**
     * Adds a single byte to this row before returning said row.
     *
     * @param b to add
     * @return this row
     */
    InfileRow append(byte b);

    /**
     * Adds a byte array to this row before returning said row.
     *
     * @param bytes to add
     * @return this row
     */
    InfileRow append(byte[] bytes);

    /**
     * Adds a String to this row before returning said row. Implementations are responsible for handling encoding.
     *
     * @param s to add
     * @return this row
     */
    InfileRow append(String s);

    /**
     * Adds a date to this row before returning said row. Dates should be formatted in the MySQL date format (yyyy-MM-dd).
     *
     * @param d to add
     * @return this row
     */
    InfileRow append(Date d);

    /**
     * Adds a boolean to this row before returning said row.
     */
    InfileRow append(Boolean b);

    /**
     * Adds an arbitrary object to this row before returning said row. Equivalent of calling
     * <code>this.append(o.toString());</code>
     *
     * @param o to add
     * @return this row
     */
    InfileRow append(Object o);

    /**
     * Adds the null sequence <code>\N</code> to the row to represent a null field.
     *
     * @return this row
     */
    InfileRow appendNull();

    /**
     * Adds a String to this row where all instances of the tab character ('\t') have been replaced by a comma (',').
     * This is necessary because MySQL's infile format uses tab as a value delimiter.
     *
     * @param s to add
     * @return this row
     */
    InfileRow appendEscaped(String s);
}
