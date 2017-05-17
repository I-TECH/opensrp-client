package org.ei.opensrp.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.ei.opensrp.Context;
import org.ei.opensrp.domain.UniqueId;
import org.ei.opensrp.path.db.UniqueIdType;
import org.ei.opensrp.path.domain.KipUniqueId;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UniqueIdRepository extends BaseRepository {
    private static final String TAG = UniqueIdRepository.class.getCanonicalName();
    private static final String UniqueIds_SQL = "CREATE TABLE unique_ids(_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,openmrs_id VARCHAR NOT NULL,status VARCHAR NULL, " +
            "used_by VARCHAR NULL,synced_by VARCHAR NULL,created_at DATETIME NULL,updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, unique_id_type INTEGER CHECK(unique_id_type IN (1,2,3)) )";
    public static final String UniqueIds_TABLE_NAME = "unique_ids";
    public static final String ID_COLUMN = "_id";
    public static final String OPENMRS_ID_COLUMN = "openmrs_id";
    public static final String STATUS_COLUMN = "status";
    private static final String USED_BY_COLUMN = "used_by";
    private static final String SYNCED_BY_COLUMN = "synced_by";
    public static final String CREATED_AT_COLUMN = "created_at";
    public static final String UPDATED_AT_COLUMN = "updated_at";
    public static final String UNIQUE_ID_TYPE_COLUMN = "unique_id_type";
    public static final String[] UniqueIds_TABLE_COLUMNS = {ID_COLUMN, OPENMRS_ID_COLUMN, STATUS_COLUMN, USED_BY_COLUMN, SYNCED_BY_COLUMN, CREATED_AT_COLUMN, UPDATED_AT_COLUMN, UNIQUE_ID_TYPE_COLUMN};

    public static String STATUS_USED = "used";
    public static String STATUS_NOT_USED = "not_used";
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public UniqueIdRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(UniqueIds_SQL);
    }

    public void add(KipUniqueId uniqueId) {
        SQLiteDatabase database = getPathRepository().getWritableDatabase();
        database.insert(UniqueIds_TABLE_NAME, null, createValuesFor(uniqueId));
        //database.close();
    }

    /**
     * inserts ids in bulk to the db in a transaction since normally, each time db.insert() is used, SQLite creates a transaction (and resulting journal file in the filesystem), which slows things down.
     *
     * @param ids
     */
    public void bulkInsertOpenmrsIds(List<String> ids, UniqueIdType uniqueIdType) {
        SQLiteDatabase database = getPathRepository().getWritableDatabase();

        try {
            String userName = Context.getInstance().allSharedPreferences().fetchRegisteredANM();

            database.beginTransaction();
            for (String id : ids) {
                ContentValues values = new ContentValues();
                values.put(OPENMRS_ID_COLUMN, id);
                values.put(STATUS_COLUMN, STATUS_NOT_USED);
                values.put(SYNCED_BY_COLUMN, userName);
                values.put(CREATED_AT_COLUMN, dateFormat.format(new Date()));
                values.put(UNIQUE_ID_TYPE_COLUMN, uniqueIdType.getValue());
                database.insert(UniqueIds_TABLE_NAME, null, values);
            }
            database.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            database.endTransaction();
        }
    }

    public Long countUnUsedIds() {
        long count = 0;
        Cursor cursor = null;
        try {
            cursor = getPathRepository().getWritableDatabase().rawQuery("SELECT COUNT (*) FROM " + UniqueIds_TABLE_NAME + " WHERE " + STATUS_COLUMN + "=?",
                    new String[]{String.valueOf(STATUS_NOT_USED)});
            if (null != cursor)
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    count = cursor.getInt(0);
                }

        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return count;
    }

    /**
     * get next available unique id
     *
     * @return
     */
    public KipUniqueId getNextUniqueId(UniqueIdType uniqueIdType) {
        KipUniqueId kipUniqueId = null;
        Cursor cursor = null;
        try {
            cursor = getPathRepository().getReadableDatabase().query(UniqueIds_TABLE_NAME, UniqueIds_TABLE_COLUMNS, STATUS_COLUMN + " = ? AND " + UNIQUE_ID_TYPE_COLUMN + " = ?", new String[]{STATUS_NOT_USED, String.valueOf(uniqueIdType.getValue())}, null, null, CREATED_AT_COLUMN + " ASC", "1");
            List<KipUniqueId> ids = readAll(cursor);
            kipUniqueId = ids.isEmpty() ? null : ids.get(0);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return kipUniqueId;
    }

    /**
     * mark and openmrsid as used
     *
     * @param openmrsId
     */
    public void close(String openmrsId) {
        String userName = Context.getInstance().allSharedPreferences().fetchRegisteredANM();
        if (!openmrsId.contains("-")) {
            openmrsId = formatId(openmrsId);
        }
        ContentValues values = new ContentValues();
        values.put(STATUS_COLUMN, STATUS_USED);
        values.put(USED_BY_COLUMN, userName);
        getPathRepository().getWritableDatabase().update(UniqueIds_TABLE_NAME, values, OPENMRS_ID_COLUMN + " = ?", new String[]{openmrsId});
    }

    private String formatId(String openmrsId) {
        int lastIndex = openmrsId.length() - 1;
        String tail = openmrsId.substring(lastIndex);
        return openmrsId.substring(0, lastIndex) + "-" + tail;
    }

    private ContentValues createValuesFor(KipUniqueId uniqueId) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, uniqueId.getId());
        values.put(OPENMRS_ID_COLUMN, uniqueId.getOpenmrsId());
        values.put(STATUS_COLUMN, uniqueId.getStatus());
        values.put(USED_BY_COLUMN, uniqueId.getUsedBy());
        values.put(CREATED_AT_COLUMN, dateFormat.format(uniqueId.getCreatedAt()));
        values.put(UNIQUE_ID_TYPE_COLUMN, uniqueId.getUniqueIdType().getValue());
        return values;
    }

    private List<KipUniqueId> readAll(Cursor cursor) {
        List<KipUniqueId> kipUniqueIds = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            cursor.moveToFirst();
            while (cursor.getCount() > 0 && !cursor.isAfterLast()) {
                KipUniqueId kipUniqueId =new KipUniqueId(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), new Date(cursor.getLong(5)), UniqueIdType.fromInteger(cursor.getInt(7)));
                kipUniqueIds.add(kipUniqueId);

                cursor.moveToNext();
            }
        }
        return kipUniqueIds;
    }


}
