package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import static com.example.android.pets.data.PetContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.PetContract.PATH_PETS;

/**
 * Created by Ken Muckey on 8/2/2018.
 */

/**
 * {@link ContentProvider} for Pets app.
 */
public class PetProvider extends ContentProvider {

    // To access our database, we instantiate our subclass of SQLiteOpenHelper
    PetDbHelper mDbHelper;
    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the pets table
     */
    private static final int PETS = 100;
    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    private static final int PET_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS, PETS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS + "/#", PET_ID);

    }

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {

        // To access our database, we instantiate our subclass of SQLiteOpenHelper
        // and pass the context, which is the current activity.
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // For the PETS code, query the pets table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                Log.i(LOG_TAG, "Query is " + cursor);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification for cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a pet into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertPet(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        // Checks gender for: NULL and if valid input
        Integer myGender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if (myGender == null || !PetContract.PetEntry.isValidGender(myGender)) {
            throw new IllegalArgumentException("Gender value is invalid.");
        }

        // Checks weight for : NULL value is allowed (but the database will use a
        //  default of 0 if a value isn't provided). Also, must be a positive value.
        Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if (weight.equals(null) || weight < 0) {
            throw new IllegalArgumentException("Weight but be a positive number.");
        }

        // Input validation not needed for BREED. Any input, including null, is accepted.

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new pet with the given values
        long id = database.insert(PetContract.PetEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        } else {
            Log.i(LOG_TAG, "Pet has been inserted from PetProvider uri insertPet method with uri: " + uri);
        }

        // Notificaiton for URI if changed
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        Log.i(LOG_TAG, "Selection at start of updatePet" + selection);

        /**
         *  Input Validation / Sanity Check section
         */
        //Check if there are any values to update
        if (values.size() == 0) {
            return 0;
        }

        // Check if value exists and then perform input validation
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            // Check that the name is not null
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // Check if value exists and then perform input validation
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            // Checks gender for: NULL and if valid input
            Integer myGender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
            if (myGender == null || !PetContract.PetEntry.isValidGender(myGender)) {
                throw new IllegalArgumentException("Gender value is invalid.");
            }
        }

        // Check if value exists and then perform input validation
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            // Checks weight for : NULL value is allowed (but the database will use a
            //  default of 0 if a value isn't provided). Also, must be a positive value.
            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight.equals(null) || weight < 0) {
                throw new IllegalArgumentException("Weight but be a positive number.");
            }
        }

        // Input validation not needed for BREED. Any input, including null, is accepted.

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Update the new pet with the given values
        int rowsAdded = database.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsAdded != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsAdded;
    }

    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted = 0;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                rowsDeleted = 0;
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

        /**
         * Returns the MIME type of data for the content URI.
         */
        @Override
        public String getType (Uri uri){
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case PETS:
                    return PetContract.PetEntry.CONTENT_LIST_TYPE;
                case PET_ID:
                    return PetContract.PetEntry.CONTENT_ITEM_TYPE;
                default:
                    throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
            }
        }
    }