package cam.virtualdisease;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class PopulateDiseaseDatabaseDialogPreference extends DialogPreference {

        private String defaultdiseases = "BASELINE, MAC, CURRENTTIME, 0, 1284046941913, 1.0\nSARS, MAC, CURRENTTIME, 1800000, 3600000, 0.8\nFLU, MAC, CURRENTTIME, 36000000, 7200000, 0.4\nCOLD, MAC, CURRENTTIME, 7200000, 10800000, 0.2"; //TODO Change second parameter for the MAC of the device

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
                    FileOutputStream diseaseFileOutputStream = new FileOutputStream(diseaseFile);
                    OutputStreamWriter diseaseOutputStream = new OutputStreamWriter(diseaseFileOutputStream);

                    diseaseFileOutputStream.write(defaultdiseases.getBytes(), 0, defaultdiseases.length());
                    diseaseFileOutputStream.flush();
                    diseaseFileOutputStream.close();

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
            Toast.makeText(getContext(), "Populated", Toast.LENGTH_SHORT).show();
        }

}
