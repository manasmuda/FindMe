package com.msm.findme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class directionsplace extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MarkerOptions locmo=new MarkerOptions();
    private Location curloc=new Location(LocationManager.GPS_PROVIDER);
    private LatLng curlatlng;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlacesClient placesClient;

    private FirebaseFirestore fmfirestore;

    private LatLng destlatlng;

    private String url;

    private int markercount=0;
    private int zoomno=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directionsplace);

        destlatlng=new LatLng(getIntent().getDoubleExtra("lat",0.0 ),getIntent().getDoubleExtra("lon",0.0 ));
        zoomno=getIntent().getIntExtra("zoom",10 );
        Toast.makeText(directionsplace.this,String.valueOf(getIntent().getIntExtra("zoom",10 )) ,Toast.LENGTH_LONG ).show();

        fmfirestore=FirebaseFirestore.getInstance();

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(directionsplace.this);
        Places.initialize(getApplicationContext(),"AIzaSyA95D3BPvrt-B38H3DAuQvTG_ZH47Sklrk");
        placesClient= Places.createClient(getApplicationContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);



    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(directionsplace.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }
        else{
            fetchLastLoc();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100){
            if(grantResults[0]==PackageManager.PERMISSION_DENIED || grantResults[1]==PackageManager.PERMISSION_DENIED){
                directionsplace.this.finish();
            }
            else {
                fetchLastLoc();
            }
        }
    }
    public void mapfunctions(){
        if (ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.setBuildingsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.clear();
            mMap.addMarker(locmo.position(curlatlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            mMap.addMarker(locmo.position(destlatlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curlatlng,(int)zoomno));

            url="https://maps.googleapis.com/maps/api/directions/json?origin="+curlatlng.latitude+","+curlatlng.longitude+"&destination="+destlatlng.latitude+","+destlatlng.longitude+"&sensor=false&mode=driving&key=AIzaSyCP_yQKKD9NyA5gehtFveMyGGbmJRXq98E";

            MyTask myTask=new MyTask();
            myTask.execute();
        }
    }

    public void fetchLastLoc(){
        if (ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS_COMPONENTS);
            final FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            placeResponse.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful()){
                        FindCurrentPlaceResponse response = task.getResult();
                        if(response.getPlaceLikelihoods().size()>0){
                            curlatlng=response.getPlaceLikelihoods().get(0).getPlace().getLatLng();
                            mapfunctions();
                        }
                        else{
                            if (ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(directionsplace.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                Task<Location> task1 = fusedLocationProviderClient.getLastLocation();
                                task1.addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if (location != null) {
                                            curloc = location;
                                            curlatlng = new LatLng(curloc.getLatitude(), curloc.getLongitude());
                                            mapfunctions();
                                        }
                                    }
                                });
                            }

                        }
                    } else {
                        Task<Location> task1 = fusedLocationProviderClient.getLastLocation();
                        task1.addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if(location!=null){
                                    curloc=location;
                                    curlatlng = new LatLng(curloc.getLatitude(),curloc.getLongitude());
                                    mapfunctions();
                                }
                            }
                        });
                        Exception exception = task.getException();
                        if (exception instanceof ApiException) {
                            ApiException apiException = (ApiException) exception;
                            Log.e("abcd", "Place not found: " + apiException.getStatusCode());

                        }
                    }
                }
            });
        }
    }

    class MyTask extends AsyncTask<Integer, Integer, List<List<HashMap<String,String>>>> {

        List<List<HashMap<String,String>>> response=new ArrayList<List<HashMap<String,String>>>();

        @Override
        protected List<List<HashMap<String,String>>> doInBackground(Integer... params) {

            try {
                response = getJSONObjectFromURL(url); // calls method to get JSON object

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e("avcds",response.toString());


            return response;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String,String>>> result) {

            ArrayList points=null;
            PolylineOptions polylineOptions=null;

            for (List<HashMap<String,String>> path:result){
                points=new ArrayList();
                polylineOptions=new PolylineOptions();
                for(HashMap<String,String> point:path){
                    double lat=Double.parseDouble(point.get("lat"));
                    double lon=Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }
                polylineOptions.addAll(points);
                polylineOptions.width(15);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }
            if(polylineOptions!=null){
                mMap.addPolyline(polylineOptions);
            }
            else {
                Toast.makeText(directionsplace.this,"Cannot find a route" ,Toast.LENGTH_LONG ).show();
            }

        }
        @Override
        protected void onPreExecute() {
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    public  List<List<HashMap<String,String>>> getJSONObjectFromURL(String urlString) throws IOException, JSONException {

        List<List<HashMap<String,String>>> path1=new ArrayList<List<HashMap<String,String>>>();

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

        JSONArray routesarray;
        JSONArray legarray;
        JSONArray steparray;   

        JSONObject result=new JSONObject(jsonString);

        try {
            routesarray=result.getJSONArray("routes");
            for (int i=0;i<routesarray.length();i++){
                legarray=((JSONObject)routesarray.getJSONObject(i)).getJSONArray("legs");
                List path=new ArrayList<HashMap<String,String>>();
                for(int j=0;j<legarray.length();j++){
                    steparray=((JSONObject)legarray.getJSONObject(i)).getJSONArray("steps");
                    for(int k=0;k<steparray.length();k++){
                        String polyline="";
                        polyline=(String) ((JSONObject)((JSONObject)steparray.get(k)).get("polyline")).get("points");

                        int index = 0, len = polyline.length();
                        int lat = 0, lng = 0;

                        while (index < len) {
                            int b, shift = 0, result1 = 0;
                            // Decode latitude
                            do {
                                b = polyline.charAt(index++) - 63;
                                result1 |= (b & 0x1f) << shift;
                                shift += 5;
                            } while (b >= 0x20);
                            int r = (result1 & 1);
                            int dlat = (r != 0 ? ~(result1 >> 1) : (result1 >> 1));
                            lat += dlat;

                            // Decode longitude
                            shift = 0;
                            result1 = 0;
                            do {
                                b = polyline.charAt(index++) - 63;
                                result1 |= (b & 0x1f) << shift;
                                shift += 5;
                            } while (b >= 0x20);
                            int dlng = ((result1 & 1) != 0 ? ~(result1 >> 1) : (result1 >> 1));
                            lng += dlng;
                            Double d_lat=((double) lat / 100000.0);
                            Double d_lng=((double) lng / 100000.0);

                            HashMap<String,String> hm=new HashMap<>();
                            hm.put("lat",String.valueOf(d_lat));
                            hm.put("lon",String.valueOf(d_lng));
                            path.add(hm);

                        }
                    }
                }
                path1.add(path);
            }
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        return path1;
    }

}
