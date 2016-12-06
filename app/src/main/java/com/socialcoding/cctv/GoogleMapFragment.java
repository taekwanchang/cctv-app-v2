package com.socialcoding.cctv;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.socialcoding.models.EyeOfSeoulPermissions;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by darkgs on 2016-03-26.
 */
public class GoogleMapFragment extends Fragment
        implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    private MainActivity mainActivity;
    private GoogleMap mMap;
    protected View view;
    private Location currLocation;

    private static Marker reportMarker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        } else {
            view = inflater.inflate(R.layout.fragment_googlemap, container, false);
        }

        if(mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                    .findFragmentById(R.id._fragment_google_map);
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!(getActivity() instanceof MainActivity)) {
            throw new RuntimeException("Invalid activity.");
        }
        mainActivity = (MainActivity) getActivity();

        /* Only works in real phone
        if(mMap == null) {
            SupportMapFragment fragment = SupportMapFragment.newInstance();
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.map, fragment);
            fragmentTransaction.commit();
            fragment.getMapAsync(this);
        }
        */
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerDragListener(this);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // Set zoom level
        mMap.moveCamera(CameraUpdateFactory.zoomTo(16));

        // Add a marker in Seoul, Korea and move the camera
        LatLng cityHallSeoul = new LatLng(37.5667151,126.9781312);
        mMap.addMarker(new MarkerOptions().position(cityHallSeoul)
                .draggable(true).title("Marker in Seoul"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(cityHallSeoul));

        if(ContextCompat.checkSelfPermission(getActivity(),
                EyeOfSeoulPermissions.LOCATION_PERMISSION_STRING) == EyeOfSeoulPermissions.GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Double latitude = marker.getPosition().latitude;
        Double longitude = marker.getPosition().longitude;
        if(reportMarker != null) {
            currLocation = new Location("dummy");
            currLocation.setLatitude(latitude);
            currLocation.setLongitude(longitude);
            reportMarker.setTitle("위도 : " + Double.toString(latitude) + ", " +
                    "경도 : " + Double.toString(longitude));
            // reportMarker.setSnippet(getAddress()); // Too slow...
            marker.showInfoWindow();
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Double latitude = marker.getPosition().latitude;
        Double longitude = marker.getPosition().longitude;
        if(reportMarker != null) {
            reportMarker.setTitle("위도 : " + Double.toString(latitude) + ", " +
                    "경도 : " + Double.toString(longitude));
            reportMarker.setSnippet(getAddress());
            marker.showInfoWindow();
        }
    }

    public void moveCamera(LatLng latLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    public boolean moveCurrentPosition() {
        if(ContextCompat.checkSelfPermission(getActivity(),
                EyeOfSeoulPermissions.LOCATION_PERMISSION_STRING) == EyeOfSeoulPermissions.GRANTED) {
            if(MainActivity.client != null) {
                currLocation = LocationServices.FusedLocationApi
                        .getLastLocation(MainActivity.client);
                if (currLocation != null) {
                    LatLng currLatLng = new LatLng(currLocation.getLatitude(),
                            currLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(currLatLng));
                    getActivity().findViewById(R.id.bottom_bar_google_map_loading_text_view)
                            .setVisibility(View.INVISIBLE);
                    getActivity().findViewById(R.id.bottom_bar_google_map_ask_layout)
                            .setVisibility(View.VISIBLE);
                    if (reportMarker != null) {
                        reportMarker.remove();
                    }
                    reportMarker = mMap.addMarker(new MarkerOptions().position(currLatLng)
                            .draggable(true).visible(true)
                            .title("위도 : " + currLocation.getLatitude() + ", " +
                                    "경도 : " + currLocation.getLongitude())
                            .snippet(getAddress()));
                }
            }
        }
        return true;
    }

    public void removeMarker() {
        if(reportMarker != null) {
            reportMarker.remove();
        }
    }

    public String getAddress() {
        double lat = currLocation.getLatitude();
        double lng = currLocation.getLongitude();
        String addr = null;
        Geocoder geocoder = new Geocoder(getActivity(), Locale.KOREA);
        List<Address> address;
        try {
            address = geocoder.getFromLocation(lat, lng, 1);
            if (address != null && address.size() > 0) {
                addr = address.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MainActivity.address = addr;
        return addr;
    }

    public void onSearchButtonClick(String searchText) {
        new Thread(new SearchHttpHandler(searchText)).start();
    }

    public class SearchHttpHandler implements Runnable {
        String searchText;

        SearchHttpHandler(String searchText) {
            this.searchText = searchText;
        }

        public void run() {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://maps.googleapis.com/maps/api/place/textsearch/" +
                            "json" +
                            "?query=" + searchText +
                            "&key=" + "AIzaSyDdvgtPbH9CLWhN8VehbLMIa1Y0DRDZ4Dc")
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String result = response.body().string();
                System.out.println(result);
                JSONObject jsonObject = new JSONObject(result);
                Double lat = (Double) jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").get("lat");
                Double lng = (Double) jsonObject.getJSONArray("results").getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location").get("lng");
                final LatLng latLng = new LatLng(lat, lng);

                mainActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        ((GoogleMapFragment) mainActivity.googleMapFragment).moveCamera(latLng);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
