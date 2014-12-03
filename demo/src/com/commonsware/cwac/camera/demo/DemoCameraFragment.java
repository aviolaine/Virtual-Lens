/***
7  Copyright (c) 2013 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.camera.demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraHost;
import com.commonsware.cwac.camera.CameraUtils;
import com.commonsware.cwac.camera.SimpleCameraHost;
import com.commonsware.cwac.camera.PictureTransaction;

import java.util.ArrayList;
import java.util.List;

public class DemoCameraFragment extends CameraFragment implements
    OnSeekBarChangeListener {
  private static final String KEY_USE_FFC=
      "com.commonsware.cwac.camera.demo.USE_FFC";
  private MenuItem singleShotItem=null;
  private MenuItem autoFocusItem=null;
  private MenuItem takePictureItem=null;
  private MenuItem groundHeightItem=null;
  //private MenuItem flashItem=null;
  private MenuItem recordItem=null;
  private MenuItem stopRecordItem=null;
    private MenuItem standing=null;
 // private MenuItem mirrorFFC=null;
  private boolean singleShotProcessing=false;
  private SeekBar zoom=null;
  private long lastFaceToast=0L;
  String flashMode=null;
  int faceheight; int sensorH;
  int maxZoom;

  static DemoCameraFragment newInstance(boolean useFFC) {
    DemoCameraFragment f=new DemoCameraFragment();
    Bundle args=new Bundle();

    args.putBoolean(KEY_USE_FFC, useFFC);
    f.setArguments(args);

    return(f);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    setHasOptionsMenu(true);

    SimpleCameraHost.Builder builder=
        new SimpleCameraHost.Builder(new DemoCameraHost(getActivity()));

    setHost(builder.useFullBleedPreview(true).build());
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    View cameraView=
        super.onCreateView(inflater, container, savedInstanceState);
    View results=inflater.inflate(R.layout.fragment, container, false);

    ((ViewGroup)results.findViewById(R.id.camera)).addView(cameraView);
    zoom=(SeekBar)results.findViewById(R.id.zoom);
    zoom.setKeepScreenOn(true);

    setRecordingItemVisibility();

    return(results);
  }

  @Override
  public void onPause() {
    super.onPause();

    getActivity().invalidateOptionsMenu();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.camera, menu);

    takePictureItem=menu.findItem(R.id.camera);
    //singleShotItem=menu.findItem(R.id.single_shot);
//    singleShotItem.setChecked(getContract().isSingleShotMode());
    autoFocusItem=menu.findItem(R.id.autofocus);
      standing=menu.findItem(R.id.stand);
   // flashItem=menu.findItem(R.id.flash);
    //recordItem=menu.findItem(R.id.record);
    //stopRecordItem=menu.findItem(R.id.stop);
    //groundHeightItem=menu.findItem(R.id.ground_height);
   // mirrorFFC=menu.findItem(R.id.mirror_ffc);

    if (isRecording()) {
      recordItem.setVisible(false);
      stopRecordItem.setVisible(true);
      takePictureItem.setVisible(false);
    }

    setRecordingItemVisibility();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.camera:
        takeSimplePicture();

        return(true);


      /*case R.id.record:
        try {
          record();
          getActivity().invalidateOptionsMenu();
        }
        catch (Exception e) {
          Log.e(getClass().getSimpleName(),
                "Exception trying to record", e);
          Toast.makeText(getActivity(), e.getMessage(),
                         Toast.LENGTH_LONG).show();
        }

        return(true);

      case R.id.stop:
        try {
          stopRecording();
          getActivity().invalidateOptionsMenu();
        }
        catch (Exception e) {
          Log.e(getClass().getSimpleName(),
                "Exception trying to stop recording", e);
          Toast.makeText(getActivity(), e.getMessage(),
                         Toast.LENGTH_LONG).show();
        }

        return(true);

*/
        case R.id.stand:
            item.setChecked(!item.isChecked());
            return (true);

      case R.id.autofocus:
        autoFocus();

        return(true);
/*
      case R.id.single_shot:
        item.setChecked(!item.isChecked());
        getContract().setSingleShotMode(item.isChecked());

        return(true);

      case R.id.show_zoom:
        item.setChecked(!item.isChecked());
        zoom.setVisibility(item.isChecked() ? View.VISIBLE : View.GONE);

        return(true);

      case R.id.flash:
      case R.id.mirror_ffc:
        item.setChecked(!item.isChecked());

        return(true);*/
    }

    return(super.onOptionsItemSelected(item));
  }

  boolean isSingleShotProcessing() {
    return(singleShotProcessing);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress,
                                boolean fromUser) {
//    if (fromUser) {
//      zoom.setEnabled(false);
//      zoomTo(zoom.getProgress()).onComplete(new Runnable() {
//        @Override
//        public void run() {
//          zoom.setEnabled(true);
//        }
//      }).go();
//    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
    // ignore
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
    // ignore
  }

  void setRecordingItemVisibility() {
    if (zoom != null && recordItem != null) {
      if (getDisplayOrientation() != 0
          && getDisplayOrientation() != 180) {
        recordItem.setVisible(false);
      }
    }
  }

  Contract getContract() {
    return((Contract)getActivity());
  }

  void takeSimplePicture() {
    if (singleShotItem!=null && singleShotItem.isChecked()) {
      singleShotProcessing=true;
      takePictureItem.setEnabled(false);
    }

    PictureTransaction xact=new PictureTransaction(getHost());

  /*  if (flashItem!=null && flashItem.isChecked()) {
      xact.flashMode(flashMode);
    }*/

    takePicture(xact);
  }

  interface Contract {
    boolean isSingleShotMode();

    void setSingleShotMode(boolean mode);
  }

  class DemoCameraHost extends SimpleCameraHost implements
      Camera.FaceDetectionListener {
    boolean supportsFaces=false;
    public DemoCameraHost(Context _ctxt) {
      super(_ctxt);
    }

    @Override
    public boolean useFrontFacingCamera() {
      if (getArguments() == null) {
        return(false);
      }

      return(getArguments().getBoolean(KEY_USE_FFC));
    }

    @Override
    public boolean useSingleShotMode() {
      if (singleShotItem == null) {
        return(false);
      }

      return(singleShotItem.isChecked());
    }

    @Override
    public void saveImage(PictureTransaction xact, byte[] image) {
      if (useSingleShotMode()) {
        singleShotProcessing=false;

        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            takePictureItem.setEnabled(true);
          }
        });

        DisplayActivity.imageToShow=image;
        startActivity(new Intent(getActivity(), DisplayActivity.class));
      }
      else {
        super.saveImage(xact, image);
      }
    }

    @Override
    public void autoFocusAvailable() {
      if (autoFocusItem != null) {
        autoFocusItem.setEnabled(true);
        
        if (supportsFaces)
          startFaceDetection();
      }
    }

    @Override
    public void autoFocusUnavailable() {
      if (autoFocusItem != null) {
        stopFaceDetection();
        
        if (supportsFaces)
          autoFocusItem.setEnabled(false);
      }
    }

    @Override
    public void onCameraFail(CameraHost.FailureReason reason) {
      super.onCameraFail(reason);

      Toast.makeText(getActivity(),
                     "Sorry, but you cannot use the camera now!",
                     Toast.LENGTH_LONG).show();
    }

    @Override
    public Parameters adjustPreviewParameters(Parameters parameters) {
  /*    flashMode=
          CameraUtils.findBestFlashModeMatch(parameters,
                                             Camera.Parameters.FLASH_MODE_RED_EYE,
                                             Camera.Parameters.FLASH_MODE_AUTO,
                                             Camera.Parameters.FLASH_MODE_ON);
*/
      if (doesZoomReallyWork() && parameters.getMaxZoom() > 0) {
        zoom.setMax(parameters.getMaxZoom());
        maxZoom = parameters.getMaxZoom();
        zoom.setOnSeekBarChangeListener(DemoCameraFragment.this);
      }
      else {
        zoom.setEnabled(false);
      }

      if (parameters.getMaxNumDetectedFaces() > 0) {
        supportsFaces=true;
      }
      else {
        Toast.makeText(getActivity(),
                       "Face detection not available for this camera",
                       Toast.LENGTH_LONG).show();
      }

      return(super.adjustPreviewParameters(parameters));
    }

    @Override
    public void onFaceDetection(Face[] faces, Camera camera) {
      if (faces.length > 0) {
          List<Rect> faceRects;
          faceRects = new ArrayList<Rect>();

          for (int i = 0; i < faces.length; i++) {
              int left = faces[i].rect.left;
              int right = faces[i].rect.right;
              int top = faces[i].rect.top;
              int bottom = faces[i].rect.bottom;
              Rect uRect = new Rect(left, top, right, bottom);
              faceRects.add(uRect);
              faceheight = bottom - top;
          }
          long now = SystemClock.elapsedRealtime();


          int imgH = Resources.getSystem().getDisplayMetrics().heightPixels;
          int imgFaceH = faceheight;
          if(standing.isChecked())
          sensorH=1600;
          else sensorH=1000;
          int faceH = 200;
          float focalL = camera.getParameters().getFocalLength();
          //object height/object image height = face height/face image height
          float distance = focalL * faceH * imgH / (imgFaceH * sensorH); //we need to set calibration
          System.out.println(sensorH);

          if(distance<10)
          zoomTo((int)(distance*10)).go();

      }
//        else faceheight=1;
//        autoFocus();
    }

    @Override
    @TargetApi(16)
    public void onAutoFocus(boolean success, Camera camera) {
//        if (success){takePictureItem.setEnabled(true);}
        //camera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();

        int imgH = metrics.heightPixels;
        int imgFaceH=faceheight;
        if(standing.isChecked())
            sensorH=1600;
        else sensorH=1000;
        int faceH=200;
        float focalL = camera.getParameters().getFocalLength();
        //object height/object image height = face height/face image height
        float distance = focalL*faceH*imgH/(imgFaceH*sensorH); //we need to set calibration
        System.out.println(distance);

        for(int i=0;i<(int)(distance*2);i++) {
            if(i<100)
                zoomTo(i).go();
        }
    }
  }
}