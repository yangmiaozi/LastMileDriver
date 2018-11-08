package sg.edu.smu.lastmiledriver;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

public class GetStation extends AsyncTask<String,Void,String> {
    private String station;

    public AsyncResponse delegate = null;

    public GetStation(AsyncResponse delegate){
        this.delegate = delegate;
    }

    @Override
    protected String doInBackground(String... params) {
        String type = params[0];
        String station_url = "";//url
        try {
            station = params[0];
            URL url = new URL(station_url);
            HttpURLConnection huc = (HttpURLConnection)url.openConnection();
            huc.setRequestMethod("POST");
            huc.setDoInput(true);
            huc.setDoOutput(true);
            OutputStream ops = huc.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ops,"UTF-8"));
            String post_data = URLEncoder.encode("station","UTF-8")+"="+URLEncoder.encode(station,"UTF-8");
            bw.write(post_data);
            bw.flush();
            bw.close();
            ops.close();
            InputStream is = huc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is,"iso-8859-1"));
            String result = "";
            String line;
            while((line = br.readLine())!=null){
                result += line;
            }
            br.close();
            is.close();
            huc.disconnect();
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        delegate.processFinish(result);
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

}