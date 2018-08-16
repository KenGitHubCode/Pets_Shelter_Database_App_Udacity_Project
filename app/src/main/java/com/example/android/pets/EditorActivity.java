/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;


/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Global Variables
//    // To access our database, we instantiate our subclass of SQLiteOpenHelper
//    // and pass the context, which is the current activity.
//    PetDbHelper mDbHelper = new PetDbHelper(this);
    public static final String LOG_TAG = EditorActivity.class.getName();
    // 0 value is arbitrary as there is the only loader in this class
    private static final int URL_LOADER = 0;
    // URI used for edit entry, global for loader callback methods
    private Uri currentEntryUri;
    // Variable to track if the item has changed via OnTouchListener
    private boolean mItemHasChanged = false;

    // EditText field to enter the pet's name
    private EditText mNameEditText;
    // EditText field to enter the pet's breed
    private EditText mBreedEditText;
    // EditText field to enter the pet's weight
    private EditText mWeightEditText;
    // EditText field to enter the pet's gender
    private Spinner mGenderSpinner;
    //Gender of the pet. The possible values are:
    // 0 for unknown gender, 1 for male, 2 for female.
    private int mGender = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Use getIntent() and getData() to get the associated URI
        currentEntryUri = getIntent().getData();

        // Only when within the setOnItemClickListener should there be a URI attached to an intent
        // Check if uri is null and set title to "Add.."
        // else set the title to the "Edit..." text
        if (currentEntryUri == null) {
            setTitle(R.string.add_title_title);
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(R.string.edit_title_title);

            // Init the Cursor Loader
            getLoaderManager().initLoader(URL_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);
        setupSpinner();

        // setOnTouchListener for each edittext field to determine if changes were made
        // to facilitate showUnsavedChangesDialog - preventing users from losing data
        // when accidently clicking back/up
        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 0; // Unknown
            }
        });
    }

    /*
     * Pulls input from fields, assigns to content variable, then inserts into database
     */
    private void savePet() {

        //Pull trimmed data into variables
        String nameInput = mNameEditText.getText().toString().trim();
        String breedInput = mBreedEditText.getText().toString().trim();
        int genderInput = mGender;
        // Weight should default to zero if not entered.  isEmpth check performed to prevent errors
        int weightInput = 0;
        if (!mWeightEditText.getText().toString().trim().isEmpty()) {
            weightInput = Integer.parseInt(mWeightEditText.getText().toString().trim());
        }

        // Checks if all values are null or empty and returns to complete activity if so.
        // Assuming user pressed save on mistake, nothing will be saved in this case.
        if (nameInput.isEmpty() &&
                breedInput.isEmpty() &&
                genderInput == PetEntry.GENDER_UNKNOWN) {
            return;
        }

        // Create new map of values to input into the table
        ContentValues petValues = new ContentValues();
        petValues.put(PetEntry.COLUMN_PET_NAME, nameInput);
        petValues.put(PetEntry.COLUMN_PET_BREED, breedInput);
        petValues.put(PetEntry.COLUMN_PET_GENDER, genderInput);
        petValues.put(PetEntry.COLUMN_PET_WEIGHT, weightInput);

        // Check if uri is null and set title to "Add.."
        // else set the title to the "Edit..." text
        if (currentEntryUri == null) {
            // Insert a new pet into the provider, returning the content URI for the new pet.
            Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, petValues);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_action_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.action_insert_saved),
                        Toast.LENGTH_SHORT).show();
            }

        } else {

            int newInt = getContentResolver().update(currentEntryUri, petValues, null, null);

            // Show a toast message depending on whether or not the insertion was successful
            if (newInt == 1) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_update_success),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.action_insert_saved),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                savePet();
                //Exit activity, jumping back to previous activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
//                //Exit activity, jumping back to previous activity
//                finish();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // loader callback method onCreateLoader - make sure it is using a URI for one pet.
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, currentEntryUri,
                CatalogActivity.PROJECTION, null, null, null);
    }

    // loader callback method onLoadFinished to update the inputs with the data for the pet.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {


        if (data == null || !data.moveToFirst()) {
            return;
        }
//        // Ensure that cursor is at first entry
//        data.moveToFirst();

        // Create and assign variables for the data
        String cursorName = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_NAME));
        String cursorBreed = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_BREED));
        int cursorGender = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_GENDER));
        int cursorWeight = data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT));

        // Auto populate the Edit Text fields
        mNameEditText.setText(cursorName);
        Log.i(LOG_TAG, "cursor Name is " + cursorName);
        mBreedEditText.setText(cursorBreed);
        mWeightEditText.setText(Integer.toString(cursorWeight));

        // Gender is a dropdown spinner, so map the constant value from the database
        // into one of the dropdown options (0 is Unknown, 1 is Male, 2 is Female).
        // Then call setSelection() so that option is displayed on screen as the current selection.
        switch (cursorGender) {
            case PetEntry.GENDER_MALE:
                mGenderSpinner.setSelection(1);
                break;
            case PetEntry.GENDER_FEMALE:
                mGenderSpinner.setSelection(2);
                break;
            default:
                mGenderSpinner.setSelection(0);
                break;
        }
    }

    //loader callback method onLoaderReset, and clear the input fields.
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mNameEditText.setText("");
        mBreedEditText.setText("");
        mGenderSpinner.setSelection(0);
        mWeightEditText.setText("");
    }

    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the mPetHasChanged boolean to true.

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (currentEntryUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        int delInt = getContentResolver().delete(currentEntryUri, null, null);

        // Show a toast message depending on whether or not the getContentResolver().delete method was successful
        if (delInt == 1) {
            // If the int provided by .delete() ==1, then the deletion was successful
            Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the deletion was unsuccessful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                    Toast.LENGTH_SHORT).show();
        }
        //Exit activity, jumping back to previous activity
        finish();
    }

}