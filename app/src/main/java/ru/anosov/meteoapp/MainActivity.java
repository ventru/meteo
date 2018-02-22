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
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bluetooth";

    //GUI
    private Button btButton;
    private TextView lightnessText;
    private TextView humidityText;
    private TextView tempText;
    private TextView cabText;
    private TextView statusLabel;
    private Button disconnectButton;
    private Button optionsButton;
    private Button sendGetButton;
    private TextView statusGetLabel;
    private TextView counterGetLabel;
    private TextView counterPostLabel;


    //Bluetooth
    private Handler h;
    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;		                                                            // Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    // SPP UUID сервиса
    private static String address = "98:D3:32:20:70:00";                                            // MAC-адрес Bluetooth модуля

    private int counterGet = 0;
    private int counterPost = 0;

    //Get
    private int minTemp = 18;
    private int maxTemp = 24;
    private int minHum = 40;
    private int maxHum = 60;
    private int minLight = 300;
    private int maxLight = 500;
    private String content = "nullable";

    //Post
    private String forPostReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize GUI
        btButton = findViewById(R.id.btButton);
        lightnessText = findViewById(R.id.LightnessText);
        humidityText = findViewById(R.id.HumidityText);
        tempText = findViewById(R.id.TempText);
        cabText = findViewById(R.id.CabText);
        statusLabel = findViewById(R.id.statusLabel);
        disconnectButton = findViewById(R.id.disconnectButton);
        optionsButton = findViewById(R.id.optionsButton);
        sendGetButton = findViewById(R.id.sendGetButton);
        statusGetLabel = findViewById(R.id.statusGetLabel);
        counterGetLabel = findViewById(R.id.counterGetLabel);
        counterPostLabel = findViewById(R.id.counterPostLabel);
        disconnectButton.setEnabled(false);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:													        // если приняли сообщение в Handler
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);												        // формируем строку
                        int startOfLineIndex = sb.indexOf("<") + 1;                                 //Находим начало строки
                        int endOfLineIndex = sb.indexOf(">") - 1;							    // определяем символы конца строки
                        if (endOfLineIndex > 0) { 						                            // если встречаем конец строки,
                            String sbprint = sb.substring(startOfLineIndex, endOfLineIndex);        // то извлекаем строку
                            sb.delete(0, sb.length());										        // и очищаем sb
                            counterGet++;
                            String[] data = parseData(sbprint);

                            lightnessText.setText("Lightness: " + data[1] + " lx");
                            tempText.setText("Temp: " + data[2] + " C°");
                            humidityText.setText("Humidity: " + data[3] + " %");
                            cabText.setText("Cab: " + data[4]);

                            forPostReady = buildPostString(data[2],data[3],data[1],data[4]);
                            //запуск отправки данных на сервер в отдельном потоке
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    new SendPost().execute();
                                }
                            });
                            t.start();
                            try {
                                t.join();
                                counterPost++;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                            analyzeData(data[2], data[3], data[1]);
                            Log.d(TAG, "Данные обработаны");
                            counterGetLabel.setText(String.valueOf(counterGet));
                            counterPostLabel.setText(String.valueOf(counterPost));
                        }
                        break;
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();		                                    // получаем локальный Bluetooth адаптер

        checkBTState();

        btButton.setOnClickListener(new View.OnClickListener() {		                            // определяем обработчик при нажатии на кнопку
            public void onClick(View v) {
                btButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                mConnectedThread.write("0 9000 ");	                                                // Отправляем клманду Bluetooth цифру 1
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                mConnectedThread.write("1 ");
            }
        });
    }

    public void archiveButtonClick(View view){
        btButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        mConnectedThread.write("1 ");
        Intent intent = new Intent(this, ArchiveActivity.class);
        startActivity(intent);
    }

    public void optionsButtonClick(View view){
        btButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        mConnectedThread.write("1 ");
        Intent intent = new Intent(this, OptionsActivity.class);
        startActivity(intent);
    }

    public void offSendingDataFromStation(){
        btButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        mConnectedThread.write("1 ");
    }

    public void analyzeData(String temp, String hum, String light){
        String resultAll = "", resultTemp = "", resultHum = "", resultLight = "";
        if (Integer.parseInt(temp) > maxTemp) resultTemp = "WARNING! (Выше нормы)The average temperature above the norm. Value:" + temp;
        if (Integer.parseInt(temp) < minTemp) resultTemp = "WARNING! (Ниже нормы)The average temperature is below the norm. Value: " + temp;
        if ((Integer.parseInt(temp) >= minTemp) && (Integer.parseInt(temp) <= maxTemp)) resultTemp = "(Нормы соблюдены)The temperature is normal. Value: " + temp;

        if (Integer.parseInt(hum) > maxHum) resultHum = "WARNING! (Выше нормы)The average humidity above the norm. Value:" + hum;
        if (Integer.parseInt(hum) < minHum) resultHum = "WARNING! (Ниже нормы)The average humidity is below the norm. Value: " + hum;
        if ((Integer.parseInt(hum) >= minHum) && (Integer.parseInt(hum) <= maxHum)) resultHum = "(Нормы соблюдены)The humidity is normal. Value: " + hum;

        if (Integer.parseInt(light) > maxLight) resultLight = "WARNING! (Выше нормы)The average light above the norm. Value:" + light;
        if (Integer.parseInt(light) < minLight) resultLight = "WARNING! (Ниже нормы)The average light is below the norm. Value: " + light;
        if ((Integer.parseInt(light) >= minLight) && (Integer.parseInt(light) <= maxLight)) resultLight = "(Нормы соблюдены)The light is normal. Value: " + light;

        if (minTemp == 0) statusLabel.setText("(Ошибка!)RESULTS IS EMPTY! CHECK THIS!");
        resultAll = resultTemp + ";\n" + resultHum + ";\n" + resultLight;
        if (minTemp != 0) statusLabel.setText(resultAll);
    }

    public void getNorms(View v){
        offSendingDataFromStation();
        sendGetButton.setEnabled(false);
        SendGet sendget = new SendGet();
        sendget.execute("http://83.146.108.92:4040/norms");
    }

    public String buildPostString(String temp, String hum, String light, String cab){
        String result = "temp=" + temp + "&" + "hum=" + hum + "&" + "light=" + light + "&" + "cab=" + cab;
        return result;
    }

    class SendGet extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... path) {
            try{
                btButton.setEnabled(true);
                content = getContent(path[0]);
            }
            catch (IOException ex){
                content = ex.getMessage();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String content){
            Date currentTime = Calendar.getInstance().getTime();
            statusGetLabel.setText("Synchronized! " + "Last date:" + currentTime + ",values: " + content);
            String[] contentParse;
            String delimeter = ",";
            contentParse = content.split(delimeter); //todo:NullPointerException at ru.anosov.meteoapp.MainActivity$SendGet.onPostExecute(MainActivity.java:237)
            sendGetButton.setEnabled(true);
            try {
                minTemp = Integer.valueOf(contentParse[0]);
                maxTemp = Integer.parseInt(contentParse[1]);
                minHum = Integer.parseInt(contentParse[2]);
                maxHum = Integer.parseInt(contentParse[3]);
                minLight = Integer.parseInt(contentParse[4]);
                maxLight = Integer.parseInt(contentParse[5]);
            }catch (NumberFormatException e){
                Toast.makeText(getApplicationContext(),"Error converting" + e.getMessage(),Toast.LENGTH_LONG);
            }
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

    class SendPost extends AsyncTask<Void, Void, Void >{
        String resultString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String myURL = "http://83.146.108.92:4040/app";
                String parammetrs = forPostReady;

                byte[] data = null;
                InputStream is = null;

                try {
                    URL url = new URL(myURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    conn.setRequestProperty("Content-Length", "" + Integer.toString(parammetrs.getBytes().length));
                    OutputStream os = conn.getOutputStream();
                    data = parammetrs.getBytes("UTF-8");
                    os.write(data);
                    data = null;

                    conn.connect();
                    int responseCode= conn.getResponseCode();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    if (responseCode == 200) {
                        is = conn.getInputStream();

                        byte[] buffer = new byte[8192]; // Такого вот размера буфер
                        // Далее, например, вот так читаем ответ
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        data = baos.toByteArray();
                        resultString = new String(data, "UTF-8");
                    } else {
                    }


                } catch (MalformedURLException e) {

                    //resultString = "MalformedURLException:" + e.getMessage();
                } catch (IOException e) {

                    //resultString = "IOException:" + e.getMessage();
                } catch (Exception e) {

                    //resultString = "Exception:" + e.getMessage();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if(resultString != null) {
                Toast toast = Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_SHORT);
                toast.show();
            }

        }
    }

    public String[] parseData(String data){
        String[] outputStrings;
        String delimeter = ";";
        outputStrings = data.split(delimeter);
        return outputStrings;
    }

    //BT
    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Соединяемся...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Создание Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth не поддерживается");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth включен...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);		// Получаем кол-во байт и само собщение в байтовый массив "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Отправляем в очередь сообщений Handler

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Данные для отправки: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Ошибка отправки данных: " + e.getMessage() + "...");

            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
