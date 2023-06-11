package cc.cornerstones.biz.datasource.share.constants;

/**
 * Data Column Type
 *
 * @author bbottong
 */
public enum DataColumnTypeEnum {
    /**
     * TINYINT(1)
     */
    BOOLEAN,
    /**
     * A very small integer. The signed range is -128 to 127. The unsigned range is 0 to 255.
     */
    TINYINT,
    /**
     * A small integer. The signed range is -32768 to 32767. The unsigned range is 0 to 65535.
     */
    SMALLINT,
    /**
     * A medium-size integer. The signed range is -8388608 to 8388607. The unsigned range is 0 to 16777215.
     */
    MEDIUMINT,
    /**
     * A normal-size integer. The signed range is -2147483648 to 2147483647. The unsigned range is 0 to 4294967295.
     */
    INT,
    /**
     * A large integer. The signed range is -9223372036854775808 to 9223372036854775807. The unsigned range is 0 to 18446744073709551615.
     */
    LONG,
    /**
     * DECIMAL(M,D)
     * M is the maximum number of digits (the precision). It has a range of 1 to 65.
     * D is the number of digits to the right of the decimal point (the scale). It has a range of 0 to 30 and must
     * be no larger than M.
     * If D is omitted, the default is 0. If M is omitted, the default is 10.
     */
    DECIMAL,
    /**
     * The DATE type is used for values with a date part but no time part.
     * MySQL retrieves and displays DATE values in 'YYYY-MM-DD' format. The supported range is '1000-01-01' to '9999-12-31'.
     */
    DATE,
    /**
     * The DATETIME type is used for values that contain both date and time parts.
     * MySQL retrieves and displays DATETIME values in 'YYYY-MM-DD hh:mm:ss' format.
     * The supported range is '1000-01-01 00:00:00' to '9999-12-31 23:59:59'.
     * A DATETIME value can include a trailing fractional seconds part in up to microseconds (6 digits) precision.
     * In particular, any fractional part in a value inserted into a DATETIME column is stored rather than discarded.
     * With the fractional part included, the format for these values is 'YYYY-MM-DD hh:mm:ss[.fraction]',
     * the range for DATETIME values is '1000-01-01 00:00:00.000000' to '9999-12-31 23:59:59.999999'.
     * The fractional part should always be separated from the rest of the time by a decimal point;
     * no other fractional seconds delimiter is recognized.
     */
    DATETIME,
    /**
     * The TIMESTAMP data type is used for values that contain both date and time parts.
     * TIMESTAMP has a range of '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC.
     * A TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision.
     * In particular, any fractional part in a value inserted into a TIMESTAMP column is stored rather than discarded.
     * With the fractional part included, the format for these values is 'YYYY-MM-DD hh:mm:ss[.fraction]',
     * the range for TIMESTAMP values is '1970-01-01 00:00:01.000000' to '2038-01-19 03:14:07.999999'.
     * The fractional part should always be separated from the rest of the time by a decimal point;
     * no other fractional seconds delimiter is recognized.
     * <p>
     * MySQL converts TIMESTAMP values from the current time zone to UTC for storage,
     * and back from UTC to the current time zone for retrieval. (This does not occur for other types such as DATETIME.)
     * By default, the current time zone for each connection is the server's time.
     * The time zone can be set on a per-connection basis.
     */
    TIMESTAMP,
    /**
     * A time.
     */
    TIME,
    /**
     * The YEAR type is a 1-byte type used to represent year values. I
     */
    YEAR,
    /**
     * The length of a CHAR column is fixed to the length that you declare when you create the table.
     * The length can be any value from 0 to 255.
     * When CHAR values are stored, they are right-padded with spaces to the specified length.
     */
    CHAR,
    /**
     * Values in VARCHAR columns are variable-length strings. The length can be specified as a value from 0 to 65,535.
     * The effective maximum length of a VARCHAR is subject to the maximum row size (65,535 bytes, which is shared among all columns) and the character set used.
     */
    VARCHAR,
    /**
     * TEXT values are treated as nonbinary strings (character strings).
     * They have a character set other than binary, and values are sorted and compared based on the collation of the character set.
     * The four TEXT types are TINYTEXT, TEXT, MEDIUMTEXT, and LONGTEXT.
     */
    TEXT,
    /**
     * BLOB values are treated as binary strings (byte strings). They have the binary character set and collation,
     * and comparison and sorting are based on the numeric values of the bytes in column values.
     * The four BLOB types are TINYBLOB, BLOB, MEDIUMBLOB, and LONGBLOB.
     * These differ only in the maximum length of the values they can hold.
     */
    BLOB,
    /**
     * MySQL supports a native JSON data type defined by RFC 7159 that enables efficient access to data in JSON
     * (JavaScript Object Notation) documents.
     */
    JSON;
}
