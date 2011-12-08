package com.opower.persistence.jpile.infile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Date;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A buffer used to collect data in MySQL's infile format. This buffer also maintains a separate row buffer
 * and implements methods to allow clients to clear and append various data types to said row. These methods insert
 * field and line separators as needed as well as provide proper formats for declaring date and null values.
 * <p>
 * When the current row is complete, it can be added to the infile buffer via {@link #addRowToInfile()}. If the row
 * does not fit into the infile buffer, none of its contents are added. To make room, clients should read the contents
 * of the infile buffer with {@link #asInputStream()} and then clear it. In general,clients should consider implementing
 * an {@link com.opower.persistence.jpile.loader.InfileObjectLoader} to manage infile buffers. That class provides higher
 * level interaction and
 * management of these buffers.
 * <p>
 * Instances of this class are not safe for use by multiple threads.
 *
 * @author Sean-Michael
 * @see <a href="http://dev.mysql.com/doc/refman/5.1/en/load-data.html">LOAD DATA INFILE reference</a>
 * @since 1.0
 */
public class InfileDataBuffer implements InfileRow {
    private static Logger logger = LoggerFactory.getLogger(InfileDataBuffer.class);

    // Defaults.
    /**
     * Default size in bytes of the infile buffer.
     */
    public static final int DEFAULT_INFILE_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
    /**
     * Default size in bytes of the row buffer.
     */
    public static final int DEFAULT_ROW_BUFFER_SIZE = 1024 * 2; // 2kB

    // Infile constants
    protected static final String MYSQL_NULL_STRING = "\\N";
    protected static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    // Utilities
    private final CharsetEncoder encoder;

    // Common byte sequences
    private final byte[] nullBytes;
    private final byte[] tabBytes;
    private final byte[] newlineBytes;

    // Buffers
    private final ByteBuffer infileBuffer;
    private final ByteBuffer rowBuffer;

    public InfileDataBuffer(Charset charset, int infileBufferSize, int rowBufferSize) {
        Preconditions.checkNotNull(charset, "No charset set for encoding.");
        this.encoder = charset.newEncoder();

        // This not using the encoder because that API is tedious just to encode a few strings.
        this.tabBytes = "\t".getBytes(charset);
        this.newlineBytes = "\n".getBytes(charset);
        this.nullBytes = MYSQL_NULL_STRING.getBytes(charset);

        // Make sure the row buffer is not larger than the infile buffer. If that were allowed you'd get into cases
        // where you would not be able to write rows to the infile buffer even if it were empty.
        Preconditions.checkState(rowBufferSize <= infileBufferSize,
                                 "Cannot create a row buffer larger than the infile buffer.");

        this.rowBuffer = ByteBuffer.allocate(rowBufferSize);
        this.infileBuffer = ByteBuffer.allocate(infileBufferSize);
    }

    public InfileDataBuffer(Charset charset) {
        this(charset, DEFAULT_INFILE_BUFFER_SIZE, DEFAULT_ROW_BUFFER_SIZE);
    }

    public InfileDataBuffer() {
        this(Charset.defaultCharset());
    }

    /**
     * Attempts to add the current row to the infile buffer. If there is insufficient room for the current row
     * and -- if there is any other data in the buffer -- a newline, then the row is not added and the method returns
     * <code>false</code>.
     *
     * @return <code>true</code> if the current row fits into the infile (and has been added)
     */
    public boolean addRowToInfile() {
        boolean addNewline = this.infileBuffer.position() > 0;
        if(this.infileBuffer.remaining() < (this.rowBuffer.position() + (addNewline ? this.newlineBytes.length : 0))) {
            return false;
        }
        if(addNewline) {
            this.infileBuffer.put(this.newlineBytes);
        }
        this.rowBuffer.flip();
        this.infileBuffer.put(this.rowBuffer);
        return true;
    }

    /**
     * Gets a view of the contents of the infile buffer as input stream. Once you are done reading, you <i>must</i>
     * clear or reset this buffer.
     *
     * @return buffer contents
     */
    // CR MB: Do we want to add status flags to this class to prevent undefined use?
    public InputStream asInputStream() {
        this.infileBuffer.flip();
        return new ByteArrayInputStream(this.infileBuffer.array(), 0, this.infileBuffer.limit());
    }

    /**
     * Resets this buffer, clearing both the current row and the infile buffer.
     */
    public void reset() {
        this.infileBuffer.clear();
        this.rowBuffer.clear();
    }

    /**
     * Clears the contents of the infile buffer, but maintains the state of the current row.
     */
    public void clear() {
        this.infileBuffer.clear();
    }

    /**
     * Appends an encoded tab ('\t') character if current row has any data in it. Otherwise, it does nothing.
     */
    private void appendTabIfNeeded() {
        if(this.rowBuffer.position() > 0) {
            this.rowBuffer.put(this.tabBytes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(byte b) {
        this.appendTabIfNeeded();
        this.rowBuffer.put(b);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(byte[] bytes) {
        this.appendTabIfNeeded();
        this.rowBuffer.put(bytes);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(String s) {
        if(s == null) {
            return this.appendNull();
        }
        this.appendTabIfNeeded();
        // MySQL interprets backslashes as an escape character.  We want it to treat a backslash as a backslash so
        // we escape it.
        String escapedStr = s.replace("\\", "\\\\");
        CoderResult result = this.encoder.encode(CharBuffer.wrap(escapedStr), this.rowBuffer, false);
        if(!result.isUnderflow()) {
            try {
                result.throwException();
            }
            catch(CharacterCodingException e) {
                throw new Error(e);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(Date d) {
        return (d == null) ? this.appendNull() : this.append(dateTimeFormatter.print(new DateTime(d.getTime())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(Boolean b) {
        return (b == null) ? this.appendNull() : this.append(b ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow append(Object o) {
        return (o == null) ? this.appendNull() : this.append(o.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow appendNull() {
        return this.append(this.nullBytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final InfileRow appendEscaped(String s) {
        return append(s.replace('\t', ','));
    }

    /**
     * Clears the current row and returns this buffer as row view.
     *
     * @return this
     */
    @Override
    public final InfileRow newRow() {
        this.rowBuffer.clear();
        return this;
    }
}
