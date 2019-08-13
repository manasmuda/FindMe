package com.msm.findme;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView locationlistview;
    private MaterialSearchBar locationsearch;
    private ImageButton newlocbutton1;
    private ImageButton homebutton1;
    private ImageButton logoutbutton1;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private FirebaseFirestore fmfirestore;
    private FirebaseUser fmuser;

    private MyAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private ArrayList<Map<String,Object>> locationslist=new ArrayList<>();
    private ArrayList<Map<String,Object>> rlocationlist=new ArrayList<>();
    private List<String> distancelist=new ArrayList<>();
    private List<String> rdistancelist=new ArrayList<>();

    private Intent homeintent=new Intent();
    private Intent newlocintent=new Intent();

    private String url;
    private String origin="";
    private String destinations="";
    private String purl="";

    private ProgressDialog progressDialog;

    private String querystring="";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cartmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.cartbutton){
            Intent intent=new Intent();
            intent.setClass(HomeActivity.this,MembershipActivity.class );
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        progressDialog=new ProgressDialog(HomeActivity.this);
        progressDialog.setMessage("Loading");
        progressDialog.show();

        fmfirestore=FirebaseFirestore.getInstance();
        fmuser=FirebaseAuth.getInstance().getCurrentUser();

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(HomeActivity.this);
        if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }
        else{
            Task<Location> task1 = fusedLocationProviderClient.getLastLocation();
            task1.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(final Location location) {
                    if(location!=null){
                        fmfirestore.collection("locations").orderBy("title").addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if(!progressDialog.isShowing()){
                                    progressDialog.show();
                                }
                                origin=String.valueOf(location.getLatitude())+","+String.valueOf(location.getLongitude());
                                locationslist=new ArrayList<>();
                                destinations="";
                                for(QueryDocumentSnapshot queryDocumentSnapshot:queryDocumentSnapshots){
                                    locationslist.add(queryDocumentSnapshot.getData());
                                    destinations=destinations+queryDocumentSnapshot.get("lat").toString()+","+queryDocumentSnapshot.get("lon").toString()+"|";
                                }
                                destinations=destinations.substring(0,destinations.length()-1);
                                url="https://maps.googleapis.com/maps/api/distancematrix/json?origins="+origin+"&destinations="+destinations+"&sensor=false&mode=driving&key=AIzaSyCP_yQKKD9NyA5gehtFveMyGGbmJRXq98E";
                                MyTask myTask=new MyTask();
                                myTask.execute(url);

                            }
                        });
                    }
                }
            });
            Exception exception = task1.getException();
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e("abcd", "Place not found: " + apiException.getStatusCode());

            }
        }


        locationsearch=findViewById(R.id.locationsearch);
        locationlistview=findViewById(R.id.locationlistview);
        newlocbutton1=findViewById(R.id.newlocbutton1);
        homebutton1=findViewById(R.id.homebutton1);
        logoutbutton1=findViewById(R.id.logoutbutton1);

        layoutManager = new LinearLayoutManager(this);
        locationlistview.setLayoutManager(layoutManager);

        newlocintent.setClass(HomeActivity.this, MapsActivity.class);

        locationlistview.setHasFixedSize(true);


        newlocbutton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(newlocintent,12 );
            }
        });

        logoutbutton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent logoutintent =new Intent();
                logoutintent.setClass(HomeActivity.this,MainActivity.class );
                setResult(RESULT_OK,logoutintent);
                HomeActivity.this.finish();
            }
        });

        locationsearch.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                querystring=s.toString();
                AutoComplete(querystring);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView ltt;
            public TextView lat;
            public TextView ldt;
            public LinearLayout lil;

            public ViewHolder(View itemView) {
                super(itemView);
                ltt = (TextView) itemView.findViewById(R.id.loctitletext);
                lat = (TextView) itemView.findViewById(R.id.locaddresstext);
                ldt=(TextView) itemView.findViewById(R.id.locdisttext);
                lil=itemView.findViewById(R.id.listitemlayout);
            }

            public View getlayoyt(){
                return lil;
            }


        }

        private ArrayList<Map<String,Object>> loclist;
        private List<String> distlist;


        public MyAdapter(ArrayList<Map<String,Object>> locslist,List<String> distslist) {
            loclist=locslist;
            distlist=distslist;
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.listitem, parent, false);
            ViewHolder viewHolder = new ViewHolder(contactView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder viewHolder, final int position) {
            TextView lat1=viewHolder.lat;
            TextView ltt1=viewHolder.ltt;
            TextView ldt1=viewHolder.ldt;

            ltt1.setText(loclist.get(position).get("title").toString());
            lat1.setText(loclist.get(position).get("address").toString());
            ldt1.setText(distlist.get(position));
            viewHolder.getlayoyt().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(HomeActivity.this,String.valueOf(1800/(1+(int)Double.parseDouble(distlist.get(position).substring(0,distlist.get(position).length()-3)))) ,Toast.LENGTH_LONG ).show();
                    Intent intent=new Intent();
                    intent.setClass(HomeActivity.this,directionsplace.class);
                    intent.putExtra("lat",Double.parseDouble(loclist.get(position).get("lat").toString()));
                    intent.putExtra("lon",Double.parseDouble(loclist.get(position).get("lon").toString()));
                    intent.putExtra("zoom",(int)(1800/(1+(int)Double.parseDouble(distlist.get(position).substring(0,distlist.get(position).length()-3)))));
                    startActivity(intent);
                }
            });

        }
        @Override
        public int getItemCount() {
            return loclist.size();
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
    }

    class MyTask extends AsyncTask<String, Integer, List<String>> {

        List<String> response=new ArrayList<>();

        @Override
        protected List<String> doInBackground(String... params) {

            String tempurl=params[0];

            try {
                response = getListObjectFromURL(tempurl); // calls method to get JSON object

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("avcds",response.toString());


            return response;
        }
        @Override
        protected void onPostExecute(List<String> result) {
            distancelist=result;
            mAdapter=new MyAdapter(locationslist,result);
            locationlistview.setAdapter(mAdapter);
            progressDialog.dismiss();
        }
        @Override
        protected void onPreExecute() {
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    public List<String> getListObjectFromURL(String urlString) throws IOException {

        List<String> distancelist=new ArrayList<>();

        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

        char[] buffer = new char[1024];

        String jsonString = new String();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        jsonString = sb.toString();

        System.out.println("JSON: " + jsonString);
        urlConnection.disconnect();

        JSONObject jsonObject;
        JSONArray distancearray;
        try {
            jsonObject=new JSONObject(jsonString);
            distancearray=jsonObject.getJSONArray("rows").getJSONObject(0).getJSONArray("elements");
            for (int i=0;i<distancearray.length();i++){
                distancelist.add(distancearray.getJSONObject(i).getJSONObject("distance").get("text").toString());
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        return distancelist;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                HomeActivity.this.finish();
            } else {
                if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Task<Location> task1 = fusedLocationProviderClient.getLastLocation();
                    task1.addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                fmfirestore.collection("locations").addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                        if (!progressDialog.isShowing()) {
                                            progressDialog.show();
                                        }
                                        locationslist = new ArrayList<>();
                                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                                            locationslist.add(queryDocumentSnapshot.getData());
                                        }
                                        url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + origin + "&destinations=" + destinations + "&sensor=false&mode=driving&key=AIzaSyCP_yQKKD9NyA5gehtFveMyGGbmJRXq98E";
                                        MyTask myTask = new MyTask();
                                        myTask.execute();

                                    }
                                });
                            }
                        }
                    });
                    Exception exception = task1.getException();
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e("abcd", "Place not found: " + apiException.getStatusCode());

                    }
                }
            }
        }



    }

    public void AutoComplete(String query){

        rdistancelist=new ArrayList<>();
        rlocationlist=new ArrayList<>();

        for(int i=0;i<locationslist.size();i++){
            if(locationslist.get(i).get("title").toString().toUpperCase().contains(query.toUpperCase()) || query.length()==0){
                rlocationlist.add(locationslist.get(i));
                rdistancelist.add(distancelist.get(i));
            }
        }
        mAdapter=new MyAdapter(rlocationlist,rdistancelist);
        locationlistview.setAdapter(mAdapter);
    }

}
