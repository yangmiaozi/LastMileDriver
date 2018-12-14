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

/**
 * Created by Miaozi on 9/12/18.
 */

public class UpdateLoc extends AsyncTask<String,Void,String> {
    private String url = "";

    public AsyncResponse delegate = null;

    public UpdateLoc(AsyncResponse delegate, String url) {
        this.delegate = delegate;
        this.url = url;
    }

    @Override
    protected String doInBackground(String... params) {
        String result_url = "http://35.247.175.250:8080/last-mile-app/drivers/" + url;
        String post_data;
        try {
            URL url = new URL(result_url);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("POST");
            huc.setDoInput(true);
            huc.setDoOutput(true);
            OutputStream ops = huc.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(ops, "UTF-8"));
            post_data = "";
            bw.write(post_data);
            bw.flush();
            bw.close();
            ops.close();
            InputStream is = huc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String result = "";
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            is.close();
            huc.disconnect();
            result = sb.toString();
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
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}