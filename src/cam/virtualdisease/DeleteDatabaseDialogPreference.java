package cam.virtualdisease;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

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
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            File root = Environment.getExternalStorageDirectory();
            File dir = new File(root, "VirtualDisease");

            if (!dir.exists())
            {
                if (!dir.mkdir())
                {
                    System.out.println("Cannot create storage directory on SD Card.");
                    ((Activity)getContext()).finish();
                }
            }

            File diseaseFile = new File(dir, Constants.DISEASE_FILE);
            try
            {
                diseaseFile.delete();
                if (!diseaseFile.exists() && !diseaseFile.createNewFile())
                {
                    System.out.println("Cannot create disease database on SD Card.");
                    ((Activity)getContext()).finish();
                }
            }
            catch (IOException ex)
            {
                System.out.println("Cannot create disease database on SD Card.");
                ((Activity)getContext()).finish();
            }

            File logFile = new File(dir, Constants.MEASUREMENTS_FILE);
            try
            {
                logFile.delete();
                if (!logFile.exists() && !logFile.createNewFile())
                {
                    System.out.println("Cannot create disease log on SD Card.");
                    ((Activity)getContext()).finish();
                }
            }
            catch (IOException ex)
            {
                System.out.println("Cannot create disease log on SD Card.");
                ((Activity)getContext()).finish();
            }

            File extraDiseaseFile = new File(dir, Constants.EXTRA_DISEASE_FILE);
            try
            {
                extraDiseaseFile.delete();
                if (!extraDiseaseFile.exists() && !extraDiseaseFile.createNewFile())
                {
                    System.out.println("Cannot create extra disease database on SD Card.");
                    ((Activity)getContext()).finish();
                }
            }
            catch (IOException e)
            {
                System.out.println("Cannot create extra disease database on SD Card.");
                ((Activity)getContext()).finish();
            }
        }
        else
        {
            System.out.println("Cannot read/write on SD Card.");
            ((Activity)getContext()).finish();
        }

        // TODO empty variables

        Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
    }

}