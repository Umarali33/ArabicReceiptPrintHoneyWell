package com.umarali.arabicdemohoneywell;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.honeywell.mobility.print.LinePrinter;
import com.honeywell.mobility.print.LinePrinterException;
import com.honeywell.mobility.print.PrintProgressEvent;
import com.honeywell.mobility.print.PrintProgressListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {
    EditText editText, macAddress, printerId;
    Button btn;
    private String jsonCmdAttribStr = null;
    private String base64LogoPng = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.editText);
        macAddress = findViewById(R.id.macAddress);
        printerId = findViewById(R.id.printerId);
        btn = findViewById(R.id.print);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readAssetFiles();
                PrintTask printTask = new PrintTask();
                printTask.execute(printerId.getText().toString(), macAddress.getText().toString());
            }
        });
    }

    private void readAssetFiles() {
        InputStream input = null;
        ByteArrayOutputStream output = null;
        AssetManager assetManager = getAssets();
        String[] files = {"printer_profiles.JSON", "honeywell_logo.bmp"};
        int fileIndex = 0;
        int initialBufferSize;

        try {
            for (String filename : files) {
                input = assetManager.open(filename);
                initialBufferSize = (fileIndex == 0) ? 8000 : 2500;
                output = new ByteArrayOutputStream(initialBufferSize);

                byte[] buf = new byte[1024];
                int len;
                while ((len = input.read(buf)) > 0) {
                    output.write(buf, 0, len);
                }
                input.close();
                input = null;

                output.flush();
                output.close();
                switch (fileIndex) {
                    case 0:
                        jsonCmdAttribStr = output.toString();
                        break;
                    case 1:
                        base64LogoPng = Base64.encodeToString(output.toByteArray(), Base64.DEFAULT);
                        break;
                }

                fileIndex++;
                output = null;
            }
        } catch (Exception ex) {
            editText.append("Error reading asset file: " + files[fileIndex]);
        } finally {
            try {
                if (input != null) {
                    input.close();
                    input = null;
                }

                if (output != null) {
                    output.close();
                    output = null;
                }
            } catch (IOException e) {
            }
        }
    }
    class PrintTask extends AsyncTask<String, Integer, String> {
        /**
         * This method runs on a background thread. The specified parameters
         * are the parameters passed to the execute method by the caller of
         * this task. This method can call publishProgress to publish updates
         * on the UI thread.
         */

        @Override
        protected String doInBackground(String... args) {
            String sPrinterID = args[0];
            String sPrinterAddr = args[1];
            doPrint(sPrinterID, sPrinterAddr);
            return null;
        }

        /**
         * Runs on the UI thread after publishProgress is invoked. The
         * specified values are the values passed to publishProgress.
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            // Access the values array.
            int progress = values[0];

            switch (progress) {
                case PrintProgressEvent.MessageTypes.CANCEL:
                    // You may display "Printing cancelled".
                    break;
                case PrintProgressEvent.MessageTypes.COMPLETE:
                    // You may display "Printing completed".
                    break;
                case PrintProgressEvent.MessageTypes.ENDDOC:
                    // You may display "End of document".
                    break;
                case PrintProgressEvent.MessageTypes.FINISHED:
                    // You may display "Printer connection closed".
                    break;
                case PrintProgressEvent.MessageTypes.STARTDOC:
                    // You may display "Start printing document".
                    break;
                default:
                    // You may display "Unknown progress message" or do nothing.
                    break;
            }
        }

        private void doPrint(String sPrinterID, String sPrinterAddr) {
            LinePrinter.ExtraSettings exSettings = new LinePrinter.ExtraSettings();
            exSettings.setContext(MainActivity.this);
            LinePrinter lp = null;
            try {
                // Creates a LinePrinter object with the specified
                // parameters. The URI string "bt://00:02:5B:00:02:78"
                // specifies to connect to the printer via Bluetooth
                // and the Bluetooth MAC address.


                File profiles = new File(getExternalFilesDir(null), "printer_profiles.JSON");
                //lp = new LinePrinter(profiles.getAbsolutePath(), sPrinterID,"bt://" + sPrinterAddr, exSettings);
                lp = new LinePrinter(jsonCmdAttribStr, sPrinterID,
                        "bt://" + sPrinterAddr, exSettings);

                lp.addPrintProgressListener(new PrintProgressListener() {
                    public void receivedStatus(PrintProgressEvent aEvent) {
                        // Publishes updates on the UI thread.
                        publishProgress(aEvent.getMessageType());
                    }
                });

                lp.connect();  // Connects to the printer

                lp.setBold(true);   // Sets bold font.
                lp.write("SALES ORDER - طلب المبيعات");
                lp.setBold(false);  // Returns to normal font.
                lp.newLine(2);
                lp.write(editText.getText().toString());
                lp.newLine(2);
                lp.write(" PRD. DESCRIPT.   PRC.  QTY.    NET.");
                lp.newLine(2);
                lp.writeLine(" 1501 Timer-Md1  13.15     1   13.15");
                lp.writeLine(" 1502 Timer-Md2  13.15     3   39.45");
                lp.writeLine(" 1503 Timer-Md3  13.15     1   13.15");
                lp.writeLine(" 1504 Timer-Md4  13.15     4   52.60");
                lp.writeLine(" 1505 Timer-Md5  13.15     5   65.75");
                lp.writeLine("                        ----  ------");
                lp.write("                  SUBTOTAL    15  197.25");
                lp.newLine(2);

                lp.writeLine("                        ----  ------");
                lp.write("               TOTAL SALES    15  197.15");
                lp.newLine(4);

            } catch (LinePrinterException ex) {
                // Handles LinePrinter exceptions
            } catch (Exception ex) {
                // Handles other exceptions
            } finally {
                if (lp != null) {
                    try {
                        lp.disconnect();  // Disconnects from the printer
                        lp.close();  // Releases resources
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }
}
