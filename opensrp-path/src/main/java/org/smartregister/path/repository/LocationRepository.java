package org.smartregister.path.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.api.domain.Location;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationRepository extends BaseRepository {
    private static final String TAG = LocationRepository.class.getCanonicalName();
    private static final String LOCATIONS_SQL = "CREATE TABLE locations(_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, uuid VARCHAR NOT NULL, name VARCHAR NOT NULL, tag VARCHAR NOT NULL, parent_uuid VARCHAR NULL)";
    public static final String LOCATIONS_TABLE_NAME = "locations";
    public static final String ID_COLUMN = "_id";
    public static final String UUID_COLUMN = "uuid";
    public static final String NAME_COLUMN = "name";
    public static final String TAG_COLUMN = "tag";
    public static final String PARENT_UUID_COLUMN = "parent_uuid";
    public static final String[] LOCATIONS_TABLE_COLUMNS = {ID_COLUMN, UUID_COLUMN, NAME_COLUMN, TAG_COLUMN, PARENT_UUID_COLUMN};

    public LocationRepository(PathRepository pathRepository) {
        super(pathRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(LOCATIONS_SQL);
    }

    /**
     * inserts ids in bulk to the db in a transaction since normally, each time db.insert() is used, SQLite creates a transaction (and resulting journal file in the filesystem), which slows things down.
     *
     * @param locations
     */
    public void bulkInsertLocations(List<Location> locations) throws JSONException {
        SQLiteDatabase database = getWritableDatabase();

        try {

            database.beginTransaction();
            for (Location l : locations) {
                ContentValues values = new ContentValues();

                values.put(UUID_COLUMN, l.getLocationId());
                values.put(NAME_COLUMN, l.getName());

                Set<String> tags = l.getTags();
                if(tags != null && tags.size() > 0){
                    String tagz = "";
                    for(String s : tags){
                        tagz += s + ":";
                    }
                    values.put(TAG_COLUMN, StringUtils.removeEnd(tagz, ":"));
                }

                Location parent = l.getParentLocation();
                if(parent != null){
                    values.put(PARENT_UUID_COLUMN, parent.getLocationId());
                }

                database.insert(LOCATIONS_TABLE_NAME, null, values);
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
            cursor = getWritableDatabase().rawQuery("SELECT COUNT (*) FROM " + LOCATIONS_TABLE_NAME,
                    new String[]{});
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

    public Location getLocationByName(String name) {
        Location location = null;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(LOCATIONS_TABLE_NAME, LOCATIONS_TABLE_COLUMNS, NAME_COLUMN + " = ?", new String[]{name}, null, null, " 1 ASC", "1");
            List<Location> locations = readAll(cursor);
            location = locations.isEmpty() ? null : locations.get(0);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return location;
    }

    /**
     * @return
     */
    public List<Location> getLocationsByTag(String tag) {
        List<Location> locations = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(LOCATIONS_TABLE_NAME, LOCATIONS_TABLE_COLUMNS, TAG_COLUMN + " = ?", new String[]{ tag }, null, null, " 1 ASC");
            locations = readAll(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "LocationsByTag list: " + locations.toString());
        return locations;
    }

    /**
     * @return
     */
    public List<Location> getChildLocations(String parentUuid) {
        List<Location> locations = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(LOCATIONS_TABLE_NAME, LOCATIONS_TABLE_COLUMNS, PARENT_UUID_COLUMN + " = ?", new String[]{parentUuid}, null, null, " 1 ASC");
            locations = readAll(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "ChildLocations list: " + locations.toString());
        return locations;
    }

    private List<Location> readAll(Cursor cursor) {
        List<Location> locations = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            cursor.moveToFirst();
            while (cursor.getCount() > 0 && !cursor.isAfterLast()) {
                Location l = new Location();
                l.setLocationId(cursor.getString(1));
                l.setName(cursor.getString(2));

                String tagString = cursor.getString(3);
                String[] tagsArray = tagString.split(":");
                Set<String> tags = new HashSet<>(Arrays.asList(tagsArray));
                l.setTags(tags);

                locations.add(l);

                cursor.moveToNext();
            }
        }
        return locations;
    }

    public List<Location> getAllLocations() {
        List<Location> locations = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(LOCATIONS_TABLE_NAME, LOCATIONS_TABLE_COLUMNS, null, null, null, null, " 1 ASC");
            locations = readAll(cursor);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "ChildLocations list: " + locations.toString());
        return locations;
    }
}
