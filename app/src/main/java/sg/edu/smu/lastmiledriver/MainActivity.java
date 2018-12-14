package sg.edu.smu.lastmiledriver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;

public class MainActivity extends AppCompatActivity {
    private TextView status;
    private TextView station;
    private TextView stationID;
    private TextView pn;
    private TextView node;
    private TextView con;
    private Button button;
    private Location location;
    private String TAG = null;
    private String plateNum = "";
    private StompClient mStompClient;
    private double longi;
    private double lati;
    private int stationId;
    private String stationT = "";
    final Handler handler = new Handler();

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

        mStompClient = Stomp.over(WebSocket.class, "ws://35.247.175.250:8080/last-mile-app/gs-guide-websocket/websocket");
        stationID = findViewById(R.id.stationID);
        button = findViewById(R.id.button);
        status = findViewById(R.id.status);
        node = findViewById(R.id.node);
        con = findViewById(R.id.contacts);
        station = findViewById(R.id.station);

        pn = findViewById(R.id.platenum);
        pn.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                plateNum = "" + pn.getText();
                if (plateNum.equals("SBS997A") || plateNum.equals("SBS899B") || plateNum.equals("SBS888A")){
                    stationT = "Dhoby Ghaut";
                    stationID.setText("1");
                } else if (plateNum.equals("SBS895B") || plateNum.equals("SBS896B") || plateNum.equals("SBS889A")){
                    stationT = "Expo";
                    stationID.setText("2");
                } else if (plateNum.equals("SBS998A") || plateNum.equals("SBS898B")){
                    stationT = "Jurong East";
                    stationID.setText("3");
                } else if (plateNum.equals("SBS999A") || plateNum.equals("SBS897B")){
                    stationT = "Boon Keng";
                    stationID.setText("4");
                }
                station.setText(stationT);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        node.setText("");
        con.setText("");

        updateLoc();
        status.setText("Waiting at station");
        button.setVisibility(View.INVISIBLE);

        getNodes();
    }

    public String getNodes() {
        Gson gson = new Gson();

        mStompClient.connect();
        mStompClient.topic("/topic/dispatch").subscribe(message -> {
            JSONArray jsonArr = null;
            ArrayList<Map> mapList = new ArrayList<Map>();
            double capacity;
            double numOfOnBoard;
            String toCheck;
            LinkedTreeMap<Object,Object> contacts;
            boolean boo = false;
            try {
                jsonArr = new JSONArray(message.getPayload());
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);

                    Map<String,Object> map = new HashMap<String,Object>();
                    map = (Map<String,Object>) gson.fromJson(jsonObj.toString(), map.getClass());
                    capacity = Double.parseDouble(""+map.get("capacity"));
                    numOfOnBoard = Double.parseDouble(""+map.get("numOfOnboard"));
                    toCheck = ""+map.get("plateNum");
                    Object contact = map.get("assignedPassengers");
                    contacts = (LinkedTreeMap) contact;

                    if (toCheck.equals(plateNum)) {
                        Object[] nodes = contacts.keySet().toArray();
                        String show = "";
                        String conShow = "";
                        for (int j = 0; j < nodes.length; j++) {
                            Object n = nodes[j];
                            ArrayList<Double> al = (ArrayList)contacts.get(n);

                            if (j != nodes.length-1) {
                                show += n.toString() + " , ";
                            }else{
                                show += n.toString();
                            }

                            for (int k = 0; k < al.size(); k++) {
                                if (k != al.size()-1) {
                                    conShow += Math.round(al.get(k)) + " , ";
                                }else{
                                    conShow += Math.round(al.get(k));
                                }
                            }

                            if (j != nodes.length-1) {
                                conShow += " , ";
                            }
                        }
                        node.setText(show);
                        con.setText(conShow);
                    }
                    if (numOfOnBoard >= capacity){
                        if (toCheck.equals(plateNum)){
                            boo = true;
                        }
                    }
                    mapList.add(map);
                }

                if (boo) {
                    mStompClient.disconnect();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button.setVisibility(View.VISIBLE);
                            status.setText("Waiting at station");
                            button.setText("Your car is ready to dispatch");
                        }
                    });

                    String start = "dispatch?plateNum=" + plateNum;
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            PerformBackgroundTask bd = (PerformBackgroundTask) new PerformBackgroundTask(new AsyncResponse() {
                                @Override
                                public void processFinish(String toPrint) {
                                    status.setText("Driving");
                                    button.setText("Back to Station");

                                    changeStatus();
                                }
                            }, start).execute("Dispatch");
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        });

        mStompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.d(TAG, "Stomp connection opened");
                    break;

                case ERROR:
                    Log.e(TAG, "Error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    Log.d(TAG, "Stomp connection closed");
                    break;
            }
        });
        return "dispatch";
    }

    public void updateLoc() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            public void run() { handler.post(new Runnable() {
                public void run() {
                    try {
                        LocationManager locationManager;
                        LocationListener locationListener;
                        final String contextService = Context.LOCATION_SERVICE;
                        String provider;
                        locationManager = (LocationManager) getSystemService(contextService);
                        Criteria criteria = new Criteria();
                        criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
                        criteria.setAltitudeRequired(false);
                        criteria.setBearingRequired(false);
                        criteria.setCostAllowed(true);
                        criteria.setPowerRequirement(Criteria.POWER_LOW);
                        provider = locationManager.getBestProvider(criteria, true);

                        if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                                lati = location.getLatitude();
                                longi = location.getLongitude();
                                Log.e("android_lat", String.valueOf(lati));
                                Log.e("android_lon", String.valueOf(longi));
                                String loc = "update?plateNum=" + plateNum + "&longitude=" + longi + "&latitude=" + lati;
                                UpdateLoc bd = (UpdateLoc) new UpdateLoc( new AsyncResponse() {
                                    @Override
                                    public void processFinish(String str) {
                                    }
                                }, loc).execute("Update location");
                            }
                        };
                        locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }}
            });
            }
        };
        timer.schedule(doAsynchronousTask, 5000, 5000);
    }

    public void changeStatus() {
        mStompClient.connect();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String back = "return?plateNum=" + plateNum;
                PerformBackgroundTask bd = (PerformBackgroundTask) new PerformBackgroundTask( new AsyncResponse() {
                    @Override
                    public void processFinish(String toPrint) {
                        status.setText("Waiting at station");
                        button.setVisibility(View.INVISIBLE);
                        node.setText("");
                        con.setText("");
                    }
                }, back).execute("Trip finished");
            }
        });

    }
}