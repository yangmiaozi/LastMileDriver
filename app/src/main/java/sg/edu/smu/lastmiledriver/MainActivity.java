package sg.edu.smu.lastmiledriver;

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private TextView station;
    private TextView status;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View titleView = getWindow().findViewById(android.R.id.title);
        if (titleView != null) {
            ViewParent parent = titleView.getParent();
            if (parent != null && (parent instanceof View)) {
                View parentView = (View)parent;
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
        }else{
            button.setText("Change status to Driving");
        }

        if(button.getText().toString().equals("Change status to Waiting")){ // trip finish, return to server
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
                }
            });
        }else{ //bus leaves station
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    status.setText("Driving");
                    button.setText("Change status to Waiting");
                }
            });
        }
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
}
