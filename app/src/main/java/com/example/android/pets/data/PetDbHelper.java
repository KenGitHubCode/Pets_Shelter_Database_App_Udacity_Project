package com.example.android.pets.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static com.example.android.pets.data.PetContract.PetEntry.COLUMN_PET_BREED;
import static com.example.android.pets.data.PetContract.PetEntry.COLUMN_PET_GENDER;
import static com.example.android.pets.data.PetContract.PetEntry.COLUMN_PET_NAME;
import static com.example.android.pets.data.PetContract.PetEntry.COLUMN_PET_WEIGHT;
import static com.example.android.pets.data.PetContract.PetEntry.TABLE_NAME;
import static com.example.android.pets.data.PetContract.PetEntry._ID;

/**
 * Created by Ken Muckey on 7/29/2018.
 */
public class PetDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "shelter.db";
    public static final int DATABASE_VERSION = 1;
    public static final String COMMA_SEP = ",";
    public static final String TEXT_TYPE = "TEXT";
    public static final String INT_TYPE = "INT";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS" + " " + TABLE_NAME;

    public static final String LOG_TAG = PetDbHelper.class.getName();

    //constructor
    public PetDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_PET_NAME + " " + TEXT_TYPE + " NOT NULL " + COMMA_SEP +
                        COLUMN_PET_BREED + " " + TEXT_TYPE + COMMA_SEP +
                        COLUMN_PET_GENDER + " " + INT_TYPE + " NOT NULL " + COMMA_SEP +
                        COLUMN_PET_WEIGHT + " " + INT_TYPE + " NOT NULL DEFAULT 0 " +
                        " )";

        db.execSQL(SQL_CREATE_ENTRIES);
    }

    /**
     * Upon Upgrade, deletes entries and runs onCreate again
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
