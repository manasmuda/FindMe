package com.msm.findme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.model.Document;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private MarkerOptions locmo=new MarkerOptions();
    private Location curloc=new Location(LocationManager.GPS_PROVIDER);
    private LatLng curlatlng;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlacesClient placesClient;

    private MaterialSearchBar searchbox;
    private Button continuebutton;
    private ImageButton curlocbutton;

    private EditText titleedit;
    private EditText locationedit;
    private Button submitbutton;

    final AutocompleteSessionToken token=AutocompleteSessionToken.newInstance();

    private  List<String> suglist = new ArrayList<>();
    private String querystring;
    private Map<String,Object> curaddress=new HashMap<>();
    private Map<String,Object> locmap=new HashMap<>();

    private FirebaseFirestore fmfirestore;

    private String curraddressstring;

    private List<AutocompletePrediction> sugpredlist=new ArrayList<>();

    private List<AutocompletePrediction> autocompletePredictions;

    private int sflag=0;

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        searchbox=findViewById(R.id.searchbox);
        continuebutton=findViewById(R.id.continuebutton);
        curlocbutton=findViewById(R.id.curlocbutton);

        fmfirestore=FirebaseFirestore.getInstance();

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        Places.initialize(getApplicationContext(),"AIzaSyA95D3BPvrt-B38H3DAuQvTG_ZH47Sklrk");
        placesClient= Places.createClient(getApplicationContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onBackPressed() {
        if(searchbox.isSearchEnabled()){
            if(querystring.length()==0){
                searchbox.disableSearch();
            }
        }
        super.onBackPressed();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},100);
        }
        else{
            fetchLastLoc();
            searchboxlisteners();
            buttonlisteners();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100){
            if(grantResults[0]==PackageManager.PERMISSION_DENIED || grantResults[1]==PackageManager.PERMISSION_DENIED){
                MapsActivity.this.finish();
            }
            else {
                fetchLastLoc();
                searchboxlisteners();
                buttonlisteners();
            }
        }
    }
    public void mapfunctions(){
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.setMyLocationEnabled(true);
            mMap.setBuildingsEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.clear();
            mMap.addMarker(locmo.position(curlatlng));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curlatlng,18));
            getAddress(curlatlng.latitude,curlatlng.longitude );
           mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    mMap.clear();
                    curlatlng=latLng;
                    mMap.addMarker(locmo.position(latLng));
                    getAddress(latLng.latitude,latLng.longitude );
                }
            });
        }
    }

    public void fetchLastLoc(){
        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS_COMPONENTS);
            final FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            placeResponse.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
                @Override
                public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                    if (task.isSuccessful()){
                        FindCurrentPlaceResponse response = task.getResult();
                        if(response.getPlaceLikelihoods().size()>0){
                            curaddress=new HashMap<>();
                            curlatlng=response.getPlaceLikelihoods().get(0).getPlace().getLatLng();
                            curraddressstring=response.getPlaceLikelihoods().get(0).getPlace().getAddress();
                            for (AddressComponent addressComponent:response.getPlaceLikelihoods().get(0).getPlace().getAddressComponents().asList()){
                                curaddress.put(addressComponent.getTypes().get(0),addressComponent.getName());
                                //Log.i("ab", addressComponent.getName());
                            }
                            mapfunctions();
                        }
                        else{
                            Task<Location> task1 = fusedLocationProviderClient.getLastLocation();
                            task1.addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if(location!=null){
                                        curloc=location;
                                        curlatlng = new LatLng(curloc.getLatitude(),curloc.getLongitude());
                                        mapfunctions();
                                        getAddress(curlatlng.latitude,curlatlng.longitude);
                                    }
                                }
                            });

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
                                    getAddress(curlatlng.latitude,curlatlng.longitude);
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
            });
        }
    }

    public void getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);
            add = add + "\n" + obj.getCountryName();
            add = add + "\n" + obj.getCountryCode();
            add = add + "\n" + obj.getAdminArea();
            add = add + "\n" + obj.getPostalCode();
            add = add + "\n" + obj.getSubAdminArea();
            add = add + "\n" + obj.getLocality();
            add = add + "\n" + obj.getSubThoroughfare();

            curaddress=new HashMap<>();
            curaddress.put("country",obj.getCountryName());
            curaddress.put("country_code",obj.getCountryCode());
            curaddress.put("admin_area",obj.getAdminArea());
            curaddress.put("postal_code",obj.getPostalCode());
            curaddress.put("subadmin_area",obj.getSubAdminArea());
            curaddress.put("locality",obj.getLocality());
            curraddressstring=obj.getAddressLine(0);

            Toast.makeText(this, "Address=>" + add,Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void searchboxlisteners(){

        searchbox.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sflag=0;
                querystring=s.toString();
                if(querystring.length()>0) {
                    suglist = new ArrayList<>();
                    sugpredlist = new ArrayList<>();
                    FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder().setCountry("IN").setLocationBias(RectangularBounds.newInstance(new LatLng(curlatlng.latitude - 0.3, curlatlng.longitude - 0.3), new LatLng(curlatlng.latitude + 0.3, curlatlng.longitude + 0.3))).setSessionToken(token).setQuery(querystring).build();
                    placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                            if (task.isSuccessful()) {
                                FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                if (predictionsResponse != null) {
                                    autocompletePredictions = predictionsResponse.getAutocompletePredictions();
                                    for (int i = 0; i < autocompletePredictions.size(); i++) {
                                        AutocompletePrediction prediction = autocompletePredictions.get(i);
                                        if (!sugpredlist.contains(prediction)) {
                                            suglist.add(prediction.getFullText(null).toString());
                                            sugpredlist.add(prediction);
                                        }
                                    }
                                    FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder().setCountry("IN").setLocationBias(RectangularBounds.newInstance(new LatLng(curlatlng.latitude - 1.5, curlatlng.longitude - 1.5), new LatLng(curlatlng.latitude + 1.5, curlatlng.longitude + 1.5))).setSessionToken(token).setQuery(querystring).build();
                                    placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                                        @Override
                                        public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                            if (task.isSuccessful()) {
                                                FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                                if (predictionsResponse != null) {
                                                    autocompletePredictions = predictionsResponse.getAutocompletePredictions();
                                                    for (int i = 0; i < autocompletePredictions.size(); i++) {
                                                        AutocompletePrediction prediction = autocompletePredictions.get(i);
                                                        if (!sugpredlist.contains(prediction)) {
                                                            suglist.add(prediction.getFullText(null).toString());
                                                            sugpredlist.add(prediction);
                                                        }
                                                    }
                                                    FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder().setCountry("IN").setLocationBias(RectangularBounds.newInstance(new LatLng(curlatlng.latitude , curlatlng.longitude), new LatLng(curlatlng.latitude , curlatlng.longitude ))).setTypeFilter(TypeFilter.REGIONS).setSessionToken(token).setQuery(querystring).build();
                                                    placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                                                            if (task.isSuccessful()) {
                                                                FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                                                                if (predictionsResponse != null) {
                                                                    autocompletePredictions = predictionsResponse.getAutocompletePredictions();
                                                                    for (int i = 0; i < autocompletePredictions.size(); i++) {
                                                                        AutocompletePrediction prediction = autocompletePredictions.get(i);
                                                                        if (!sugpredlist.contains(prediction)) {
                                                                            suglist.add(prediction.getFullText(null).toString());
                                                                            sugpredlist.add(prediction);
                                                                        }
                                                                    }
                                                                    if(sflag==0) {
                                                                        searchbox.updateLastSuggestions(suglist);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
                else {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            searchbox.clearSuggestions();
                            sflag=1;
                        }
                    }, 500);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchbox.setSuggestionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                final String tempplaceid=sugpredlist.get(position).getPlaceId();
                searchbox.setText(suglist.get(position));
                List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS,Place.Field.ADDRESS_COMPONENTS);
                FetchPlaceRequest request = FetchPlaceRequest.builder(tempplaceid, placeFields).build();
                placesClient.fetchPlace(request).addOnCompleteListener(new OnCompleteListener<FetchPlaceResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FetchPlaceResponse> task) {
                        if(task.isSuccessful()){
                            Place place=task.getResult().getPlace();
                            if(place!=null) {
                                try {
                                    curaddress=new HashMap<>();
                                    curraddressstring=place.getAddress();
                                    Toast.makeText(MapsActivity.this,place.getAddress(), Toast.LENGTH_LONG).show();
                                    for(AddressComponent addressComponent:place.getAddressComponents().asList()){
                                        curaddress.put(addressComponent.getTypes().get(0),addressComponent.getName());
                                        Log.i("abcde",addressComponent.getName()+",Type:"+addressComponent.getTypes().get(0));
                                    }
                                    curlatlng=place.getLatLng();
                                    mMap.clear();
                                    mMap.addMarker(locmo.position(curlatlng));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curlatlng, 18));
                                    //getAddress(curlatlng.latitude, curlatlng.longitude);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                });
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        searchbox.clearSuggestions();
                    }
                },1000);
            }


            @Override
            public void OnItemDeleteListener(int position, View v) {
                suglist.remove(position);
                sugpredlist.remove(position);
            }
        });
    }

    public void buttonlisteners(){
        curlocbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchbox.disableSearch();
                fetchLastLoc();
            }
        });

        continuebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog newlocdialog = new BottomSheetDialog(MapsActivity.this);
                View view=newlocdialog.getLayoutInflater().inflate(R.layout.newlocdialoglayout,null);
                titleedit=view.findViewById(R.id.titleedit);
                locationedit=view.findViewById(R.id.locationedit);
                submitbutton=view.findViewById(R.id.submitbutton);
                getAddress(curlatlng.latitude,curlatlng.longitude );
                locationedit.setText(curraddressstring);
                submitbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(titleedit.getText().toString().length()>0){
                            String rand=randomAlphaNumeric(15);
                            locmap=new HashMap<>();
                            locmap.put("title",titleedit.getText().toString() );
                            locmap.put("address",curraddressstring );
                            locmap.put("lat",curlatlng.latitude);
                            locmap.put("lon",curlatlng.longitude);
                            locmap.put("lid",rand );
                            fmfirestore.collection("locations").document(rand).set(locmap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        newlocdialog.dismiss();
                                        MapsActivity.this.finish();
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(MapsActivity.this,"Title is required",Toast.LENGTH_LONG).show();
                        }
                    }
                });
                newlocdialog.setContentView(view);
                newlocdialog.show();
            }
        });
    }
}
