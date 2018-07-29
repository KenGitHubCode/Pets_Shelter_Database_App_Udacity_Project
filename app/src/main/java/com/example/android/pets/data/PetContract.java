package com.example.android.pets.data;

import android.provider.BaseColumns;

/**
 * Created by Ken Muckey on 7/29/2018.
 */
public final class PetContract {

    /*
    * Innner class for each table in the database
    * Implements BaseColumns class
     */
    public static abstract class petEntry implements BaseColumns {

        public static final String TABLE_NAME = "pets";
        public static final String COLUMN_ID = "_id";
        public static final String COLUMN_BREED = "breed";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_WEIGHT = "weight";

        /**
         * Possible values for specific headers listed above
         */
        public static final int GENDER_UNKNOWN = 0;
        public static final int GENDER_MALE = 1;
        public static final int GENDER_FEMALE = 2;

    }


}
