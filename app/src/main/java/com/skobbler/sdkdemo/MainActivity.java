package com.skobbler.sdkdemo;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.format.Time;
import android.text.style.EasyEditSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.map.realreach.SKRealReachSettings;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.poitracker.SKPOITrackerManager;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.routing.SKRouteInfo;
import com.skobbler.ngx.routing.SKRouteJsonAnswer;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.routing.SKViaPoint;
import com.skobbler.ngx.sdktools.navigationui.SKToolsAdvicePlayer;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.versioning.SKVersioningManager;
import com.skobbler.sdkdemo.util.DemoUtils;

import java.util.Locale;

public class MainActivity extends Activity implements SKMapSurfaceListener,SKCurrentPositionListener,SKRouteListener,SKNavigationListener {

    /**
     * Surface view for displaying the map
     */
    private SKMapSurfaceView mapView;
    private    DemoApplication app;
    private SKCurrentPositionProvider currentPositionProvider;
    private SKPosition currentPosition;
    private boolean isStartPointBtnPressed=true,isEndPointBtnPressed=false,isViaPointSelected=false;
    private SKCoordinate startPoint,destinationPoint,viaPoint;

    int GREEN_PIN_ICON_ID=0;
    int RED_PIN_ICON_ID=1;
    int VIA_POINT_ICON_ID=4;

    TextToSpeech textToSpeechEngine;

    private enum MapAdvices {
        TEXT_TO_SPEECH, AUDIO_FILES
    }
    /**
     * the view that holds the map view
     */
    private SKMapViewHolder mapHolder;

    Button postionMe;

    Firebase myFirebaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DemoUtils.initializeLibrary(this);
        setContentView(R.layout.activity_main);
        app = (DemoApplication) getApplication();

        postionMe = (Button)findViewById(R.id.position_me_button);
        mapHolder = (SKMapViewHolder)findViewById(R.id.view_group_map);
        mapHolder.setMapSurfaceListener(this);

        Firebase.setAndroidContext(this);
        myFirebaseRef = new Firebase("https://vikashverma.firebaseio.com/");

        currentPositionProvider = new SKCurrentPositionProvider(this);
        currentPositionProvider.setCurrentPositionListener(this);
        currentPositionProvider.requestLocationUpdates(DemoUtils.hasGpsModule(this), DemoUtils.hasNetworkModule(this), false);


        textToSpeechEngine = new TextToSpeech(MainActivity.this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) { if (status == TextToSpeech.SUCCESS) {
                        int result = textToSpeechEngine.setLanguage(Locale.ENGLISH);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result ==
                                TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(MainActivity.this,
                                    "This Language is not supported",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.text_to_speech_engine_not_initialized),
                                Toast.LENGTH_SHORT).show();
                    }
                    }
                });

        myFirebaseRef.child("mylocation").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // textView.setText(snapshot.getValue().toString());  //prints "Do you have data? You'll love Firebase."

                SKCoordinate skCordinate= new SKCoordinate(0,0);;
                try { String str=snapshot.getValue().toString();
                    String long_lat[]=str.split("/");
                    skCordinate.setLatitude(Double.parseDouble(long_lat[0]));
                    skCordinate.setLongitude(Double.parseDouble(long_lat[1]));
                    Log.v("TAG_VI","lati "+long_lat[0]+" long "+long_lat[1]);
                }catch (Exception e)
                {
                    Log.v("TAG_VI","exception");
                }


                final SKSearchResult place = SKReverseGeocoderManager
                        .getInstance().reverseGeocodePosition(skCordinate);
                SKAnnotation annotation=new SKAnnotation(11);
                annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_BLUE);
                annotation.setLocation(place.getLocation());
                annotation.setMininumZoomLevel(5);


                if(mapView!=null)
                {mapView.addAnnotation(annotation,
                        SKAnimationSettings.ANIMATION_NONE);
                    mapView.centerMapOnPosition(skCordinate);
                }

            }

            @Override public void onCancelled(FirebaseError error) { }

        });




    }

    @Override
    protected void onPause() {
        super.onPause();
        mapHolder.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapHolder.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.download_map:
                Intent intent=new Intent(MainActivity.this,ResourceDownloadsListActivity.class);
                startActivity(intent);
                Log.v("TAG_VI"," download started");
                break;
        }
        Log.v("TAG_VI"," download failed");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder skMapViewHolder) {
        mapView = mapHolder.getMapSurfaceView();

    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onActionPan() {

    }

    @Override
    public void onActionZoom() {

    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onDoubleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onRotateMap() {

    }

    @Override
    public void onLongPress(SKScreenPoint skScreenPoint) {
            SKCoordinate poiCoordinates = mapView.pointToCoordinate(skScreenPoint);
            final SKSearchResult place = SKReverseGeocoderManager
                    .getInstance().reverseGeocodePosition(poiCoordinates);


            boolean selectPoint = isStartPointBtnPressed || isEndPointBtnPressed || isViaPointSelected;
            if (poiCoordinates != null && place != null && selectPoint) {
                SKAnnotation annotation = new SKAnnotation(GREEN_PIN_ICON_ID);
                if (isStartPointBtnPressed) {
                    annotation.setUniqueID(GREEN_PIN_ICON_ID);
                    annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_GREEN);
                    startPoint = place.getLocation();
                    isEndPointBtnPressed=true;
                    isStartPointBtnPressed=false;

                } else if (isEndPointBtnPressed) {
                    annotation.setUniqueID(RED_PIN_ICON_ID);
                    annotation
                            .setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_RED);
                    destinationPoint = place.getLocation();
                    isStartPointBtnPressed=true;
                }

                annotation.setLocation(place.getLocation());
                annotation.setMininumZoomLevel(5);
                mapView.addAnnotation(annotation,
                        SKAnimationSettings.ANIMATION_NONE);
            }

    }

    @Override
    public void onInternetConnectionNeeded() {

    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {

    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {

    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {

    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI) {

    }

    @Override
    public void onCompassSelected() {

    }

    @Override
    public void onCurrentPositionSelected() {

    }

    @Override
    public void onObjectSelected(int i) {

    }

    @Override
    public void onInternationalisationCalled(int i) {

    }

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    @Override
    public void onScreenshotReady(Bitmap bitmap) {

    }

    //button position me
    public void currentLocation(View v){

        mapView.centerMapOnCurrentPosition();
        Toast.makeText(MainActivity.this, " "+mapView.getMapCenter().getLatitude()+" "+mapView.getMapCenter().getLongitude(), Toast.LENGTH_LONG).show();
        Log.v("TAG_VI","position  "+mapView.getMapCenter().getLatitude()+" "+mapView.getMapCenter().getLongitude());
        }

    public void startRouteCalculation(View v)
    {
        SKRouteSettings route = new SKRouteSettings();
        // set start and destination points
        if(startPoint==null)
            Toast.makeText(MainActivity.this, "Long press on Map to select start point", Toast.LENGTH_SHORT).show();
        else if(destinationPoint==null)
            Toast.makeText(MainActivity.this, "Long press on Map to select destination point", Toast.LENGTH_SHORT).show();
        else{
            route.setStartCoordinate(startPoint);
            route.setDestinationCoordinate(destinationPoint);
            // set the number of routes to be calculated
            route.setNoOfRoutes(1);
            // set the route mode
            route.setRouteMode(SKRouteSettings.SKRouteMode.CAR_FASTEST);
            // set whether the route should be shown on the map after it's computed
            route.setRouteExposed(true);
            // set the route listener to be notified of route calculation
            // events
            SKRouteManager.getInstance().setRouteListener(this);
            // pass the route to the calculation routine
            SKRouteManager.getInstance().calculateRoute(route);
        }

    }

    private void launchNavigation() {
        // get navigation settings object
        SKNavigationSettings navigationSettings = new SKNavigationSettings();
       // navigationSettings.setShowRealGPSPositions(true);
        navigationSettings.setNavigationType(SKNavigationSettings.SKNavigationType.SIMULATION);

        SKNavigationManager navigationManager = SKNavigationManager.getInstance();
        navigationManager.setMapView(mapView);

        navigationManager.setNavigationListener(this);
        navigationManager.startNavigation(navigationSettings);

    }

    @Override
    public void onCurrentPositionUpdate(SKPosition currentPosition) {
        this.currentPosition = currentPosition;
        SKPositionerManager.getInstance().reportNewGPSPosition(this.currentPosition);
//        String str=currentPosition.getCoordinate().getLatitude()+"/"+currentPosition.getCoordinate().getLongitude();
//        myFirebaseRef.child("mylocation").setValue(str);
        Log.v("TAG_VI"," Current position update");
    }

    @Override
    public void onAllRoutesCompleted() {
        setAdvicesAndStartNavigation(MapAdvices.AUDIO_FILES);
    }

    @Override
    public void onRouteCalculationCompleted(SKRouteInfo skRouteInfo) {

        Toast.makeText(MainActivity.this,"total distance "+skRouteInfo.getDistance()+ " Estimated time:"+skRouteInfo.getEstimatedTime(), Toast.LENGTH_SHORT).show();
       //

    }

    @Override
    public void onRouteCalculationFailed(SKRoutingErrorCode skRoutingErrorCode) {

    }

    @Override
    public void onServerLikeRouteCalculationCompleted(SKRouteJsonAnswer skRouteJsonAnswer) {

    }

    @Override
    public void onOnlineRouteComputationHanging(int i) {

    }

   /// SKNavigationListner

    @Override
    public void onDestinationReached() {
        SKNavigationManager.getInstance().stopNavigation();
        SKRouteManager.getInstance().clearCurrentRoute();
    }

    @Override
    public void onSignalNewAdviceWithInstruction(String s) {
        textToSpeechEngine.speak(s, TextToSpeech.QUEUE_ADD, null);
    }

    @Override
    public void onSignalNewAdviceWithAudioFiles(String[] instruction, boolean b) {
        SKToolsAdvicePlayer.getInstance().playAdvice(instruction, SKToolsAdvicePlayer.PRIORITY_NAVIGATION);

    }

    @Override
    public void onSpeedExceededWithAudioFiles(String[] strings, boolean b) {

    }

    @Override
    public void onSpeedExceededWithInstruction(String s, boolean b) {

    }

    @Override
    public void onUpdateNavigationState(SKNavigationState skNavigationState) {

    }

    @Override
    public void onReRoutingStarted() {

    }

    @Override
    public void onFreeDriveUpdated(String s, String s1, String s2, SKNavigationState.SKStreetType skStreetType, double v, double v1) {

    }

    @Override
    public void onViaPointReached(int i) {

    }

    @Override
    public void onVisualAdviceChanged(boolean b, boolean b1, SKNavigationState skNavigationState) {

    }

    @Override
    public void onTunnelEvent(boolean b) {

    }
    ///     SKnavigationListner end/////

    private void setAdvicesAndStartNavigation(MapAdvices currentMapAdvices) {
        final SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
        advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN);
        advisorSettings.setAdvisorConfigPath(app.getMapResourcesDirPath() + "/Advisor");
        advisorSettings.setResourcePath(app.getMapResourcesDirPath() + "/Advisor/Languages");
        advisorSettings.setAdvisorVoice("en");
        switch (currentMapAdvices) {
            case AUDIO_FILES:
                advisorSettings.setAdvisorType(SKAdvisorSettings.SKAdvisorType.AUDIO_FILES);
                break;
            case TEXT_TO_SPEECH:
                advisorSettings.setAdvisorType(SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH);
                break;
        }
        SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
        launchNavigation();

    }
}