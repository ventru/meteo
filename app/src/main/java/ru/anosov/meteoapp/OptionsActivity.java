package ru.anosov.meteoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import ru.anosov.meteoapp.synchronize.SyncDB;

public class OptionsActivity extends AppCompatActivity {

    private List<String> rangeDates;
    private List<String> buildDataFromStation;
    private int index = 0;

    private Button syncButton;
    private Button sendCabButton;
    private Button sendTimeButton;

    private EditText cabEditText;
    private EditText timeEditText;

    private TextView debugLabel;

    private String content = "nullable";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        syncButton = (Button) findViewById(R.id.syncButton);
        sendCabButton = (Button) findViewById(R.id.sendCabButton);
        sendTimeButton = (Button) findViewById(R.id.sendTimeButton);
        cabEditText = (EditText) findViewById(R.id.cabEditText);
        timeEditText = (EditText) findViewById(R.id.timeEditText);
        debugLabel = (TextView) findViewById(R.id.debugLabel);

    }

    //run GET
    public void syncButtonClick(View v){
        syncButton.setEnabled(false);
        SendGet sendGet = new SendGet();
        sendGet.execute("http://83.146.108.92:4040/sync");
    }

    class SendGet extends AsyncTask<String, Void, String> {

        //first execute
        @Override
        protected String doInBackground(String... path) {
            try{
                content = getContent(path[0]);
            }
            catch (IOException ex){
                content = ex.getMessage();
            }
            return content;
        }

        //if doInBackground job's done then run onPost
        @Override
        protected void onPostExecute(String content){
            SyncDB syncDB = new SyncDB(content);
            rangeDates = syncDB.searchLostDates();

            debugLabel.setText("");
            if (rangeDates!= null){
                for (int i = 0; i < rangeDates.size(); i++) {
                    debugLabel.setText(debugLabel.getText() + rangeDates.get(i));
                }
            }
//            for (int i = 0; i < rangeDates.size(); i++) {
//                sendDatesToStation(rangeDates.get(i));
//            }
        }

        private String getContent(String path) throws IOException {
            BufferedReader reader=null;
            try {
                URL url=new URL(path);
                HttpURLConnection c=(HttpURLConnection)url.openConnection();
                c.setRequestMethod("GET");
                c.setReadTimeout(300);
                c.connect();
                reader= new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder buf=new StringBuilder();
                String line=null;
                while ((line=reader.readLine()) != null) {
                    buf.append(line + "\n");
                }
                return(buf.toString());
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

}
