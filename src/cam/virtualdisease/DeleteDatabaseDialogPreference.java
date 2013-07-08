package cam.virtualdisease;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

public class DeleteDatabaseDialogPreference extends DialogPreference {

    public DeleteDatabaseDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            deleteDatabase();
        }
    }



    private void deleteDatabase() {
        // TODO deleteDatabase
        Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
    }

}