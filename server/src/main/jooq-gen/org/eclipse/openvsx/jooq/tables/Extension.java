/*
 * This file is generated by jOOQ.
 */
package org.eclipse.openvsx.jooq.tables;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.openvsx.jooq.Indexes;
import org.eclipse.openvsx.jooq.Keys;
import org.eclipse.openvsx.jooq.Public;
import org.eclipse.openvsx.jooq.tables.records.ExtensionRecord;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function13;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row13;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Extension extends TableImpl<ExtensionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.extension</code>
     */
    public static final Extension EXTENSION = new Extension();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ExtensionRecord> getRecordType() {
        return ExtensionRecord.class;
    }

    /**
     * The column <code>public.extension.id</code>.
     */
    public final TableField<ExtensionRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.extension.average_rating</code>.
     */
    public final TableField<ExtensionRecord, Double> AVERAGE_RATING = createField(DSL.name("average_rating"), SQLDataType.DOUBLE, this, "");

    /**
     * The column <code>public.extension.download_count</code>.
     */
    public final TableField<ExtensionRecord, Integer> DOWNLOAD_COUNT = createField(DSL.name("download_count"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>public.extension.name</code>.
     */
    public final TableField<ExtensionRecord, String> NAME = createField(DSL.name("name"), SQLDataType.VARCHAR(255), this, "");

    /**
     * The column <code>public.extension.namespace_id</code>.
     */
    public final TableField<ExtensionRecord, Long> NAMESPACE_ID = createField(DSL.name("namespace_id"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>public.extension.public_id</code>.
     */
    public final TableField<ExtensionRecord, String> PUBLIC_ID = createField(DSL.name("public_id"), SQLDataType.VARCHAR(128), this, "");

    /**
     * The column <code>public.extension.active</code>.
     */
    public final TableField<ExtensionRecord, Boolean> ACTIVE = createField(DSL.name("active"), SQLDataType.BOOLEAN.nullable(false), this, "");

    /**
     * The column <code>public.extension.published_date</code>.
     */
    public final TableField<ExtensionRecord, LocalDateTime> PUBLISHED_DATE = createField(DSL.name("published_date"), SQLDataType.LOCALDATETIME(6), this, "");

    /**
     * The column <code>public.extension.last_updated_date</code>.
     */
    public final TableField<ExtensionRecord, LocalDateTime> LAST_UPDATED_DATE = createField(DSL.name("last_updated_date"), SQLDataType.LOCALDATETIME(6), this, "");

    /**
     * The column <code>public.extension.review_count</code>.
     */
    public final TableField<ExtensionRecord, Long> REVIEW_COUNT = createField(DSL.name("review_count"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>public.extension.deprecated</code>.
     */
    public final TableField<ExtensionRecord, Boolean> DEPRECATED = createField(DSL.name("deprecated"), SQLDataType.BOOLEAN.nullable(false), this, "");

    /**
     * The column <code>public.extension.replacement_id</code>.
     */
    public final TableField<ExtensionRecord, Long> REPLACEMENT_ID = createField(DSL.name("replacement_id"), SQLDataType.BIGINT, this, "");

    /**
     * The column <code>public.extension.downloadable</code>.
     */
    public final TableField<ExtensionRecord, Boolean> DOWNLOADABLE = createField(DSL.name("downloadable"), SQLDataType.BOOLEAN.nullable(false), this, "");

    private Extension(Name alias, Table<ExtensionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Extension(Name alias, Table<ExtensionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.extension</code> table reference
     */
    public Extension(String alias) {
        this(DSL.name(alias), EXTENSION);
    }

    /**
     * Create an aliased <code>public.extension</code> table reference
     */
    public Extension(Name alias) {
        this(alias, EXTENSION);
    }

    /**
     * Create a <code>public.extension</code> table reference
     */
    public Extension() {
        this(DSL.name("extension"), null);
    }

    public <O extends Record> Extension(Table<O> child, ForeignKey<O, ExtensionRecord> key) {
        super(child, key, EXTENSION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.EXTENSION__NAMESPACE_ID__IDX);
    }

    @Override
    public UniqueKey<ExtensionRecord> getPrimaryKey() {
        return Keys.EXTENSION_PKEY;
    }

    @Override
    public List<UniqueKey<ExtensionRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.UNIQUE_EXTENSION_PUBLIC_ID);
    }

    @Override
    public List<ForeignKey<ExtensionRecord, ?>> getReferences() {
        return Arrays.asList(Keys.EXTENSION__FK64IMD3NRJ67D50TPKJS94NGMN, Keys.EXTENSION__EXTENSION_REPLACEMENT_ID_FKEY);
    }

    private transient Namespace _namespace;
    private transient Extension _extension;

    /**
     * Get the implicit join path to the <code>public.namespace</code> table.
     */
    public Namespace namespace() {
        if (_namespace == null)
            _namespace = new Namespace(this, Keys.EXTENSION__FK64IMD3NRJ67D50TPKJS94NGMN);

        return _namespace;
    }

    /**
     * Get the implicit join path to the <code>public.extension</code> table.
     */
    public Extension extension() {
        if (_extension == null)
            _extension = new Extension(this, Keys.EXTENSION__EXTENSION_REPLACEMENT_ID_FKEY);

        return _extension;
    }

    @Override
    public Extension as(String alias) {
        return new Extension(DSL.name(alias), this);
    }

    @Override
    public Extension as(Name alias) {
        return new Extension(alias, this);
    }

    @Override
    public Extension as(Table<?> alias) {
        return new Extension(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Extension rename(String name) {
        return new Extension(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Extension rename(Name name) {
        return new Extension(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Extension rename(Table<?> name) {
        return new Extension(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row13 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row13<Long, Double, Integer, String, Long, String, Boolean, LocalDateTime, LocalDateTime, Long, Boolean, Long, Boolean> fieldsRow() {
        return (Row13) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function13<? super Long, ? super Double, ? super Integer, ? super String, ? super Long, ? super String, ? super Boolean, ? super LocalDateTime, ? super LocalDateTime, ? super Long, ? super Boolean, ? super Long, ? super Boolean, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function13<? super Long, ? super Double, ? super Integer, ? super String, ? super Long, ? super String, ? super Boolean, ? super LocalDateTime, ? super LocalDateTime, ? super Long, ? super Boolean, ? super Long, ? super Boolean, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
