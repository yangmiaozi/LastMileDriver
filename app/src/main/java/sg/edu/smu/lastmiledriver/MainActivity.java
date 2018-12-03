package sg.edu.smu.lastmiledriver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.java_websocket.WebSocket;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import rx.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;


public class MainActivity extends AppCompatActivity {
    private TextView station;
    private TextView status;
    private Button button;
    private TextView mTextViewReplyFromServer;
    private EditText mEditTextSendMessage;
    private Location location;
    private String TAG = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View titleView = getWindow().findViewById(android.R.id.title);
        if (titleView != null) {
            ViewParent parent = titleView.getParent();
            if (parent != null && (parent instanceof View)) {
                View parentView = (View) parent;
                parentView.setBackgroundColor(Color.rgb(0x88, 0x33, 0x33));
            }
        }
        button = findViewById(R.id.button);
        status = findViewById(R.id.status);
        station = findViewById(R.id.station);

        status.setText("Waiting");
        String result = status.getText().toString();
        if (result.equals("Driving")) {
            button.setText("Change status to Waiting");
        } else {
            button.setText("Change status to Driving");
        }

        if (button.getText().toString().equals("Change status to Waiting")) { // trip finish, return to server
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PerformBackgroundTask bd = (PerformBackgroundTask) new PerformBackgroundTask(
                            new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                    scheduler();
                                }
                            }
                    ).execute("Trip finished");
                    status.setText("Waiting");
                    button.setText("Change status to Driving");
                    sendMessage(mEditTextSendMessage.getText().toString());
                }
            });
        } else { //bus leaves station
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    status.setText("Driving");
                    button.setText("Change status to Waiting");
                    sendMessage(mEditTextSendMessage.getText().toString());
                }
            });
        }

        //getLoc();

        StompClient mStompClient;
        mStompClient = Stomp.over(WebSocket.class, "ws://35.247.175.250:8080/gs-guide-websocket");
        mStompClient.connect();

        mStompClient.topic("/topic/dispatch").subscribe(topicMessage -> {Log.d(TAG, topicMessage.getPayload());});
        mStompClient.send("/topic/hello-msg-mapping", "My first STOMP message!").subscribe();

        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            System.out.println("opennnn");
                            break;
                        case ERROR:
                            System.out.println("opennnn");
                            break;
                        case CLOSED:
                            System.out.println("closeeeee");
                    }
                });
        mStompClient.disconnect();
    }

    public void getLoc() {
        LocationManager locationManager;
        LocationListener locationListener;
        final String contextService = Context.LOCATION_SERVICE;
        String provider;
        double lat;
        double lon;
        locationManager = (LocationManager) getSystemService(contextService);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = locationManager.getLastKnownLocation(provider);
        locationListener = new LocationListener() {
            public void onStatusChanged(String provider, int status, Bundle extras){
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled (String provider){
            }

            public void onLocationChanged (Location location){
                final Double lat = location.getLatitude();
                final Double lon = location.getLongitude();
                Log.e("android_lat", String.valueOf(lat));
                Log.e("android_lon", String.valueOf(lon));
            }
        };
        locationManager.requestLocationUpdates(provider, 3000, 0, locationListener);
    }

    public void getStation(){
        GetStation gs = (GetStation) new GetStation(
            new AsyncResponse() {
                @Override
                public void processFinish(String output) {
                    station.setText(output);
                }
            }
        ).execute("Get Station");
    }

    public void scheduler() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            getStation();
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 50000);
    }

    private void sendMessage(final String msg) {
        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
            try {

                Socket s = new Socket("35.247.175.250", 8080);
                OutputStream out = s.getOutputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String line = br.readLine();
                System.out.println("from server" + line);

                PrintWriter output = new PrintWriter(out);
                output.println(msg);
                output.flush();
                BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                final String st = input.readLine();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String s = mTextViewReplyFromServer.getText().toString();
                        if (st.trim().length() != 0) {
                            mTextViewReplyFromServer.setText(s + "\nFrom Server : " + st);
                        }
                    }
                });
                output.close();
                out.close();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            }
        });
        thread.start();
    }
}


