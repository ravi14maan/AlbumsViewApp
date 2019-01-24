package app.albums.android.com.albumsviewapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private String SHARED_PREF_NAME = "title";

    private ProgressDialog pDialog;
    private ListView lv;

    // URL to get albums JSON
    private static String url = "https://jsonplaceholder.typicode.com/albums/";

    ArrayList<HashMap<String, String>> albumList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albumList = new ArrayList<>();

        lv = (ListView) findViewById(R.id.list);

        new GetAlbums().execute();
    }

    /**
     * Method to check network( Internet ) connection.
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    /**
     * Async task class to get json data by making HTTP call
     */
    private class GetAlbums extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {

                    // Getting JSON Array node
                    JSONArray albums = new JSONArray(jsonStr);

                    // looping through All albums
                    for (int i = 0; i < albums.length(); i++) {
                        JSONObject c = albums.getJSONObject(i);

                        String userId = c.getString("userId");
                        String id = c.getString("id");
                        String title = c.getString("title");


                        // tmp hash map for single album
                        HashMap<String, String> album = new HashMap<>();

                        // adding each child node to HashMap key => value
                        album.put("userId", userId);
                        album.put("id", id);
                        album.put("title", title);

                        // adding album to album list
                        albumList.add(album);
                    }

                    /**
                     * Sorting the album title's alphabetically.
                     */
                    Collections.sort(albumList, new Comparator<HashMap<String, String>>() {
                        @Override
                        public int compare(HashMap<String, String> a, HashMap<String, String> b) {
                            String v1 = a.get("title");
                            String v2 = b.get("title");
                            return v1.compareTo(v2);
                        }
                    });

                    /**
                     * Storing the json array elements using SharedPreferences database(key->value pair).
                     */
                    SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor prefsEditor = mPrefs.edit();
                    Gson gson = new Gson();
                    String jsonTitle = gson.toJson(albumList);
                    prefsEditor.putString("title",jsonTitle);
                    prefsEditor.apply();

                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get AlbumsList from server. Check internet connection!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            /**
             * Retrieving the json array elements using SharedPreferences database(key->value pair).
             */
            if(!isOnline())
            {
                SharedPreferences preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
                String s = preferences.getString("title", "");
                albumList = new Gson().fromJson(s,new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType());
            }

            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, albumList,
                    R.layout.list_item, new String[]{
                    "title"}, new int[]{R.id.title});

            lv.setAdapter(adapter);
        }

    }
}
