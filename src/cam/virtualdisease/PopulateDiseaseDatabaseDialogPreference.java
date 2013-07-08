package cam.virtualdisease;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class PopulateDiseaseDatabaseDialogPreference extends DialogPreference {

        public PopulateDiseaseDatabaseDialogPreference (Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        protected void onDialogClosed(boolean positiveResult) {
            super.onDialogClosed(positiveResult);
            if (positiveResult) {
                populateDatabase();
            }
        }



        private void populateDatabase() {
            // TODO deleteDatabase
            Toast.makeText(getContext(), "Populated", Toast.LENGTH_SHORT).show();
        }

}
