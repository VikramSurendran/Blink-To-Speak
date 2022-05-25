// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.examples.facemesh;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
// ContentResolver dependency
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutioncore.VideoInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import android.speech.tts.TextToSpeech;
import me.xdrop.fuzzywuzzy.FuzzySearch;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";

  private FaceMesh facemesh;
  // Run the pipeline and the model inference on GPU or CPU.
  private static final boolean RUN_ON_GPU = true;

  private enum InputSource {
    UNKNOWN,
    IMAGE,
    VIDEO,
    CAMERA,
  }
  private InputSource inputSource = InputSource.UNKNOWN;
  // Image demo UI and image loader components.
  private ActivityResultLauncher<Intent> imageGetter;
  private FaceMeshResultImageView imageView;
  // Video demo UI and video loader components.
  private VideoInput videoInput;
  private ActivityResultLauncher<Intent> videoGetter;
  // Live camera demo UI and camera components.
  private CameraInput cameraInput;

  private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

  private List<String> resultSequence = new ArrayList<>();
  Map<String,String> dict = new HashMap<>();
  TextToSpeech t1;
  int togg=0;
  int new_togg = 0;
  int camera_flag =0;
  int trigger = 0;
  int trigger_count = 0;
  int temp_var = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
//    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//      getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//    }
    setupStaticImageDemoUiComponents();
    setupLearnerUiComponents();
    setupVideoDemoUiComponents();
    setupLiveDemoUiComponents();
    setupExtraButtons();
    dict.put( "Start or Stop","OSO");
    dict.put( "Wrong","OCOCOCOCOCOCO");
    dict.put( "Yes","OCO");
    dict.put( "No","OCOCO");
    dict.put( "I'm Ok","OCOCOCO");
    dict.put( "I'm Not Ok","OLRCO");
    dict.put( "Call Guardian","OCOL");
    dict.put( "Call Doctor","OCOR");
    dict.put( "I Want to Sleep","OCOCOCOCO");
    dict.put( "Breathlessness","OLO");
    dict.put( "Water","OLRO");
    dict.put( "Toilet","OUO");
    dict.put( "Heartache","OCOUO");
    dict.put( "How are you?","OCOCOLOCOCO");
    dict.put( "Emergency","OWOWOWOWOWOW");
    dict.put( "Danger","OSOCOCOCOCOCO");
    dict.put( "I have a problem ","OWOWO");
    dict.put( "Transfer","OWOWOLURO");
    dict.put( "I Love You","OWOWOWO");
    dict.put( "I'm Sorry","OUOWO");
    dict.put("Thank you", "OWOVOCOCO");
    //dict.put("I need a hug", "OUODOCOCO"); //Doubt
    dict.put("Let's talk", "OWO");
    dict.put("Let's go out in the open","OCOUOWO");
    dict.put("I want to go home", "OLROWO");
    dict.put("I want to meet my pet","OLOLURO");
    dict.put("Congratulations", "OCOCOLUROCOCO");
    dict.put("I'm in pain","OWOCO");
    dict.put("Change position","OLUROWO");
    //dict.put("I feel like eating", "OUD2O"); //doubt
    dict.put("Entertainment","OLURO");
    dict.put("Electrical Appliance","OLUROCO");
    dict.put("Wipe","OVOVOWOWO");
    dict.put("Move", "ORO");
    dict.put("Massage","OWOCOCO");
    dict.put("Hold","OCOLUROWO");
    dict.put("Lift", "OCOCOUO");
    dict.put("Wash", "OVOCOWO");
    dict.put("Scratch", "OWOWOVOVO");
    dict.put("Change", "OWOWOCO");
    dict.put("Adjust", "OWOWOCOCO");
   // dict.put("Happy","OUDOWO"); //doubt
  }

  @Override
  protected void onResume() {
    super.onResume();
//    if (inputSource == InputSource.CAMERA) {
//      // Restarts the camera and the opengl surface rendering.
//      cameraInput = new CameraInput(this);
//      cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
//      glSurfaceView.post(this::startCamera);
//      glSurfaceView.setVisibility(View.VISIBLE);
//    } else if (inputSource == InputSource.VIDEO) {
//      videoInput.resume();
//    }

    if(inputSource == InputSource.CAMERA) {
      inputSource = InputSource.UNKNOWN;
      finish();
      overridePendingTransition(0, 0);
      startActivity(getIntent());
      overridePendingTransition(0, 0);
    }else if (inputSource == InputSource.VIDEO){
       videoInput.resume();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.setVisibility(View.GONE);
      cameraInput.close();
    } else if (inputSource == InputSource.VIDEO) {
      videoInput.pause();
    }
  }

  private Bitmap downscaleBitmap(Bitmap originalBitmap) {
    double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
    int width = imageView.getWidth();
    int height = imageView.getHeight();
    if (((double) imageView.getWidth() / imageView.getHeight()) > aspectRatio) {
      width = (int) (height * aspectRatio);
    } else {
      height = (int) (width / aspectRatio);
    }
    return Bitmap.createScaledBitmap(originalBitmap, width, height, false);
  }

  private Bitmap rotateBitmap(Bitmap inputBitmap, InputStream imageData) throws IOException {
    int orientation =
        new ExifInterface(imageData)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    if (orientation == ExifInterface.ORIENTATION_NORMAL) {
      return inputBitmap;
    }
    Matrix matrix = new Matrix();
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        matrix.postRotate(90);
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        matrix.postRotate(180);
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        matrix.postRotate(270);
        break;
      default:
        matrix.postRotate(0);
    }
    return Bitmap.createBitmap(
        inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
  }

  /** Sets up the UI components for the static image demo. */


  private void setupStaticImageDemoUiComponents() {
    // The Intent to access gallery and read images as bitmap.
    imageGetter =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Intent resultIntent = result.getData();
              if (resultIntent != null) {
                if (result.getResultCode() == RESULT_OK) {
                  Bitmap bitmap = null;
                  try {
                    bitmap =
                        downscaleBitmap(
                            MediaStore.Images.Media.getBitmap(
                                this.getContentResolver(), resultIntent.getData()));
                  } catch (IOException e) {
                    Log.e(TAG, "Bitmap reading error:" + e);
                  }
                  try {
                    InputStream imageData =
                        this.getContentResolver().openInputStream(resultIntent.getData());
                    bitmap = rotateBitmap(bitmap, imageData);
                  } catch (IOException e) {
                    Log.e(TAG, "Bitmap rotation error:" + e);
                  }
                  if (bitmap != null) {
                    facemesh.send(bitmap);
                  }
                }
              }
            });
//    Button loadImageButton = findViewById(R.id.button_load_picture);
//    loadImageButton.setOnClickListener(
//        v -> {
//          if (inputSource != InputSource.IMAGE) {
//            stopCurrentPipeline();
//            setupStaticImageModePipeline();
//          }
//          // Reads images from gallery.
//          Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
//          pickImageIntent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
//          imageGetter.launch(pickImageIntent);
//        });
    imageView = new FaceMeshResultImageView(this);
  }

  private void setupLearnerUiComponents() {
    // The Intent to access gallery and read images as bitmap.
    Button loadImageButton = findViewById(R.id.button_load_picture);
    loadImageButton.setOnClickListener(
            v -> {
              finish();
              overridePendingTransition(0, 0);
              startActivity(getIntent());
              overridePendingTransition(0, 0);
              Intent activity2Intent = new Intent(getApplicationContext(), Learner.class);
              startActivity(activity2Intent);
            });

  }

  /** Sets up core workflow for static image mode. */
  private void setupStaticImageModePipeline() {
    TextView tv = (TextView)findViewById(R.id.textView);
    this.inputSource = InputSource.IMAGE;
    // Initializes a new MediaPipe Face Mesh solution instance in the static image mode.
    facemesh =
        new FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(true)
                .setRefineLandmarks(true)
                .setRunOnGpu(RUN_ON_GPU)
                .build());

    // Connects MediaPipe Face Mesh solution to the user-defined FaceMeshResultImageView.
    facemesh.setResultListener(
        faceMeshResult -> {
          eyeLandmark(faceMeshResult, /*showPixelValues=*/ false, tv);
          imageView.setFaceMeshResult(faceMeshResult);
          runOnUiThread(() -> imageView.update());
        });
    facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    frameLayout.removeAllViewsInLayout();
    imageView.setImageDrawable(null);
    frameLayout.addView(imageView);
    imageView.setVisibility(View.VISIBLE);
  }

  /** Sets up the UI components for the video demo. */
  private void setupVideoDemoUiComponents() {
    // The Intent to access gallery and read a video file.
    TextView tv = (TextView)findViewById(R.id.textView);
    videoGetter =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
              Intent resultIntent = result.getData();
              if (resultIntent != null) {
                if (result.getResultCode() == RESULT_OK) {
                  glSurfaceView.post(
                      () ->
                          videoInput.start(
                              this,
                              resultIntent.getData(),
                              facemesh.getGlContext(),
                              glSurfaceView.getWidth(),
                              glSurfaceView.getHeight()));
                }
              }
            });
    Button loadVideoButton = findViewById(R.id.button_load_video);
//    loadVideoButton.setOnClickListener(
//        v -> {
//          clearButtonUtility();
//          stopCurrentPipeline();
//          setupStreamingModePipeline(InputSource.VIDEO);
//          // Reads video from gallery.
//          Intent pickVideoIntent = new Intent(Intent.ACTION_PICK);
//          pickVideoIntent.setDataAndType(MediaStore.Video.Media.INTERNAL_CONTENT_URI, "video/*");
//          videoGetter.launch(pickVideoIntent);
//          tv.setText("Processing...");
//
//        });
    loadVideoButton.setOnClickListener(
            v -> {
              if(new_togg==0){
//            if (inputSource == InputSource.CAMERA) {
//              return;
//            }
                clearButtonUtility();
                stopCurrentPipeline();
                camera_flag=1;
                setupStreamingModePipeline(InputSource.CAMERA);
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                  tv.setText("Please provide Camera Access to begin and if you have already done so click on 'Start Camera' to begin" );
                }else {
                  new_togg++;
                  //tv.setText("Capturing...");
                  loadVideoButton.setText("Stop Camera");
                }
              }else{
                loadVideoButton.setText("Start Camera");
                //stopCurrentPipeline();
                if (cameraInput != null) {
                  cameraInput.setNewFrameListener(null);
                  cameraInput.close();
                }
                if (videoInput != null) {
                  videoInput.setNewFrameListener(null);
                  videoInput.close();
                }
                if (glSurfaceView != null) {
                  glSurfaceView.setVisibility(View.GONE);
                }
                FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
                imageView.setVisibility(View.GONE);
                frameLayout.removeAllViewsInLayout();
                clearButtonUtility();
                //resultButtonUtility();
                new_togg=0;

              }

            });
  }

  /** Sets up the UI components for the live demo with camera input. */
  private void setupLiveDemoUiComponents() {
    Button startCameraButton = findViewById(R.id.button_start_camera);
    TextView tv = (TextView)findViewById(R.id.textView);
    startCameraButton.setOnClickListener(
        v -> {
          if(togg==0){
//            if (inputSource == InputSource.CAMERA) {
//              return;
//            }
            clearButtonUtility();
            stopCurrentPipeline();
            camera_flag=0;
            setupStreamingModePipeline(InputSource.CAMERA);
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                tv.setText("Please provide Camera Access to begin and if you have already done so click on 'Start Camera' to begin" );
            }else {
              togg++;
              tv.setText("Capturing...");
              startCameraButton.setText("Stop Capture");
            }
          }else{
            startCameraButton.setText("Start Capture");
            //stopCurrentPipeline();
            if (cameraInput != null) {
              cameraInput.setNewFrameListener(null);
              cameraInput.close();
            }
            if (videoInput != null) {
              videoInput.setNewFrameListener(null);
              videoInput.close();
            }
            if (glSurfaceView != null) {
              glSurfaceView.setVisibility(View.GONE);
            }
            FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
            imageView.setVisibility(View.GONE);
            frameLayout.removeAllViewsInLayout();
            resultButtonUtility();
            togg=0;

          }
        });
  }

  /** Sets up core workflow for streaming mode. */
  private void setupStreamingModePipeline(InputSource inputSource) {
    TextView tv = (TextView)findViewById(R.id.textView);
    this.inputSource = inputSource;
    // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
    facemesh =
        new FaceMesh(
            this,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(RUN_ON_GPU)
                .build());
    facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

    if (inputSource == InputSource.CAMERA) {
      cameraInput = new CameraInput(this);
      cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
    } else if (inputSource == InputSource.VIDEO) {
      videoInput = new VideoInput(this);
      //tv.setText("bingo");
      videoInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
    }

    // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
    glSurfaceView =
        new SolutionGlSurfaceView<>(this, facemesh.getGlContext(), facemesh.getGlMajorVersion());
    glSurfaceView.setSolutionResultRenderer(new FaceMeshResultGlRenderer());
    glSurfaceView.setRenderInputImage(true);
    facemesh.setResultListener(
        faceMeshResult -> {
          eyeLandmark(faceMeshResult, /*showPixelValues=*/ false, tv);
          glSurfaceView.setRenderData(faceMeshResult);
          glSurfaceView.requestRender();
        });

    // The runnable to start camera after the gl surface view is attached.
    // For video input source, videoInput.start() will be called when the video uri is available.
    if (inputSource == InputSource.CAMERA) {
      glSurfaceView.post(this::startCamera);
    }

    // Updates the preview layout.
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    imageView.setVisibility(View.GONE);
    frameLayout.removeAllViewsInLayout();
    frameLayout.addView(glSurfaceView);
    glSurfaceView.setVisibility(View.VISIBLE);
    frameLayout.requestLayout();
  }

  private void clearButtonUtility(){
    TextView tv = (TextView)findViewById(R.id.textView);
    resultSequence.clear();
    tv.setText("");
    FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
    imageView.setVisibility(View.GONE);
    frameLayout.removeAllViewsInLayout();
  }
  private void resultButtonUtility(){
    TextView tv = (TextView)findViewById(R.id.textView);
    if(resultSequence.size()<4){
      tv.setText("Sequence too short. Try again!");
    }else {
      String res = "";
      for (int i = 0; i < resultSequence.size() - 2; i++) {
        if (resultSequence.get(i).equals(resultSequence.get(i + 1))) {
          if (resultSequence.get(i + 1).equals(resultSequence.get(i + 2))) {
            res = res + resultSequence.get(i);
            i++;
          }
        }
      }
      int tc = 0;
      String res1 = "";
      for (int i = 0; i < res.length(); i++) {
        if (res.charAt(i) == 'C') {
          tc++;
        } else {
          if (tc > 10) {
            res1 = res1 + 'S';
            tc = 0;
          } else if (tc > 0) {
            res1 = res1 + 'C';
            tc = 0;
          }
          res1 = res1 + res.charAt(i);
        }
      }
      int l = res1.length();
      int j = 0;
      String fin = "";
      fin = fin + res1.charAt(j);
      for (int i = 1; i < l; i++) {
        if (res1.charAt(i) != res1.charAt(j)) {
          fin = fin + res1.charAt(i);
          j = i;
        }
      }
      int max = 0;
      String finkey = "";
      for (Map.Entry<String, String> e : dict.entrySet()) {
        //Log.i(TAG, fin + " " + e.getValue());
        int temp = FuzzySearch.ratio(fin, e.getValue());
        //Log.i(TAG, String.valueOf(temp));
        if (temp > max) {
          max = temp;
          finkey = e.getKey();
        }
      }
      tv.setText("Initial Triads:  " + res + "\nFinal Sequence: " + fin + "\nResult: " + finkey);
      t1.speak(finkey, TextToSpeech.QUEUE_FLUSH, null);
    }
  }
  private void setupExtraButtons() {
    Button clearButton = findViewById(R.id.clear_button);
    Button resultButton = findViewById(R.id.result_button);
    t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if(status != TextToSpeech.ERROR) {
          t1.setLanguage(Locale.UK);
        }
      }
    });

    clearButton.setOnClickListener(view -> {
      clearButtonUtility();
    }
    );

    resultButton.setOnClickListener(view -> {
      resultButtonUtility();
     }
    );

  }

  private void startCamera() {
    cameraInput.start(
        this,
        facemesh.getGlContext(),
        CameraInput.CameraFacing.FRONT,
        glSurfaceView.getWidth(),
        glSurfaceView.getHeight());
  }

  private void stopCurrentPipeline() {
    if (cameraInput != null) {
      cameraInput.setNewFrameListener(null);
      cameraInput.close();
    }
    if (videoInput != null) {
      videoInput.setNewFrameListener(null);
      videoInput.close();
    }
    if (glSurfaceView != null) {
      glSurfaceView.setVisibility(View.GONE);
    }
    if (facemesh != null) {
      facemesh.close();
    }
  }

  private void eyeLandmark(FaceMeshResult result, boolean showPixelValues, TextView tv) {
    if (result == null || result.multiFaceLandmarks().isEmpty()) {
      return;
    }

    //Log.i(TAG, String.format("No of tensors: %d", result.multiFaceLandmarks().size()));
    //Log.i(TAG, String.format("No of coords in face: %d", result.multiFaceLandmarks().get(0).getLandmarkList().size()));
    //Log.i(TAG, String.format("No of coords in eye: %d", result.multiFaceLandmarks().get(3).getLandmarkList().size()));
    //P.S: only one freaking tensor and has all 478  coords

    NormalizedLandmark leftTop1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(387);
    NormalizedLandmark leftTop2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(385);
    NormalizedLandmark leftBottom1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(373);
    NormalizedLandmark leftBottom2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(380);
    NormalizedLandmark leftHorizontal1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(263);
    NormalizedLandmark leftHorizontal2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(362);

    NormalizedLandmark rightTop1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(158);
    NormalizedLandmark rightTop2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(160);
    NormalizedLandmark rightBottom1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(153);
    NormalizedLandmark rightBottom2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(144);
    NormalizedLandmark rightHorizontal1 = result.multiFaceLandmarks().get(0).getLandmarkList().get(133);
    NormalizedLandmark rightHorizontal2 = result.multiFaceLandmarks().get(0).getLandmarkList().get(33);

    // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.

    if (showPixelValues) {
      int width = result.inputBitmap().getWidth();
      int height = result.inputBitmap().getHeight();
      Log.i( TAG, "Raw Distance:");

      double lh = calculateDistanceRaw(leftHorizontal1,leftHorizontal2,width,height);
      double rh = calculateDistanceRaw(rightHorizontal1,rightHorizontal2,width,height);
      double lv1 = calculateDistanceRaw(leftTop1,leftBottom1,width,height);
      double lv2 = calculateDistanceRaw(leftTop2,leftBottom2,width,height);
      double rv1 = calculateDistanceRaw(rightTop1,rightBottom1,width,height);
      double rv2 = calculateDistanceRaw(rightTop2,rightBottom2,width,height);
      double leftEyeRatio = (lv1+lv2)/ (2*lh);
      double rightEyeRatio = (rv1 + rv2)/ (2*rh);
      Log.i( TAG, String.format("Left ER: %f  Right ER: %f", leftEyeRatio,rightEyeRatio));
      if(leftEyeRatio < 0.1 && !(rightEyeRatio<0.1) || !(leftEyeRatio<0.1) && rightEyeRatio<0.1)
      {
        resultSequence.add("w");
        Log.i( TAG, resultSequence.toString());
      }
      else if((leftEyeRatio + rightEyeRatio)/2 < 0.1)
      {
        resultSequence.add("b");
        Log.i( TAG, resultSequence.toString());
      }else{
        resultSequence.add("o");
        Log.i( TAG, resultSequence.toString());
      }
    } else {
      if(camera_flag==0) {
        //Log.i( TAG, "Normalized Distance:");
        //Log.i( TAG, "Its not blank now!!");
        double lh = calculateDistanceNormalized(leftHorizontal1, leftHorizontal2);
        double rh = calculateDistanceNormalized(rightHorizontal1, rightHorizontal2);
        double lv1 = calculateDistanceNormalized(leftTop1, leftBottom1);
        double lv2 = calculateDistanceNormalized(leftTop2, leftBottom2);
        double rv1 = calculateDistanceNormalized(rightTop1, rightBottom1);
        double rv2 = calculateDistanceNormalized(rightTop2, rightBottom2);
        double leftEyeRatio = (lv1 + lv2) / (2 * lh);
        double rightEyeRatio = (rv1 + rv2) / (2 * rh);
        //Log.i( TAG, String.format("Left ER: %f  Right ER: %f", leftEyeRatio,rightEyeRatio));
        if (leftEyeRatio < 0.1 && !(rightEyeRatio < 0.1)) {
          resultSequence.add("W");
          // Log.i( TAG, resultSequence.toString());
        } else if (!(leftEyeRatio < 0.1) && rightEyeRatio < 0.1) {
          resultSequence.add("V");
        } else if ((leftEyeRatio + rightEyeRatio) / 2 < 0.1) {
          resultSequence.add("C");
          //Log.i( TAG, resultSequence.toString());
        } else {
          // eye
          NormalizedLandmark leftPupil = result.multiFaceLandmarks().get(0).getLandmarkList().get(473);
          NormalizedLandmark rightPupil = result.multiFaceLandmarks().get(0).getLandmarkList().get(468);
          double leftpupiltoCorner1 = calculateDistanceNormalized(leftPupil, leftTop1);
          double leftpupiltoCorner2 = calculateDistanceNormalized(leftPupil, leftTop2);
          double rightpupiltoCorner1 = calculateDistanceNormalized(rightPupil, rightTop1);
          double rightpupiltoCorner2 = calculateDistanceNormalized(rightPupil, rightTop2);
          //Log.i( TAG, String.format("LeftPupiltoCorner1: %f  LeftPupiltoCorner2: %f RightPupiltoCorner1: %f RightPupiltoCorner2: %f", leftpupiltoCorner1,leftpupiltoCorner2, rightpupiltoCorner1, rightpupiltoCorner2));

          // for finding up center to top corners and bottom corners and their sum
          //        double leftpupiltoBottom1 = calculateDistanceNormalized(leftPupil,leftBottom1);
          //        double leftpupiltoBottom2 = calculateDistanceNormalized(leftPupil,leftBottom2);
          //        double rightpupiltoBottom1 = calculateDistanceNormalized(rightPupil,rightBottom1);
          //        double rightpupiltoBottom2 = calculateDistanceNormalized(rightPupil,rightBottom2);
          //        double leftpupilUpper = leftpupiltoCorner1 + leftpupiltoCorner2;
          //        double leftpupilLower = leftpupiltoBottom1 + leftpupiltoBottom2;
          //        double rightpupilUpper = rightpupiltoCorner1 + rightpupiltoCorner2;
          //        double rightpupilLower = rightpupiltoBottom1 + rightpupiltoBottom2;
          //        Log.i( TAG, String.format("LeftPupiltoUpper: %f  LeftPupilLower: %f RightPupilUpper: %f RightPupilLower: %f", leftpupilUpper,leftpupilLower, rightpupilUpper, rightpupilLower));

          // for finding up pupil up to upper and pupil down to bottom
          //        NormalizedLandmark leftPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(475);
          //        NormalizedLandmark leftPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(477);
          //        NormalizedLandmark rightPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(470);
          //        NormalizedLandmark rightPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(472);
          //        double leftpupiluptoTop1 = calculateDistanceNormalized(leftPupilUp, leftTop1);
          //        double leftpupiluptoTop2 = calculateDistanceNormalized(leftPupilUp, leftTop2);
          //        double rightpupiluptoTop1 = calculateDistanceNormalized(rightPupilUp,rightTop1);
          //        double rightpupiluptoTop2 = calculateDistanceNormalized(rightPupilUp,rightTop2);
          //        double leftpupildowntoBottom1 = calculateDistanceNormalized(leftPupilDown,leftBottom1);
          //        double leftpupildowntoBottom2 = calculateDistanceNormalized(leftPupilDown,leftBottom2);
          //        double rightpupildowntoBottom1 = calculateDistanceNormalized(rightPupilDown,rightBottom1);
          //        double rightpupildowntoBottom2 = calculateDistanceNormalized(rightPupilDown,rightBottom2);
          //        double leftpupilupUpper = leftpupiluptoTop1 + leftpupiluptoTop2;
          //        double leftpupildownLower = leftpupildowntoBottom1 + leftpupildowntoBottom2;
          //        double rightpupilupUpper = rightpupiluptoTop1 + rightpupiluptoTop2;
          //        double rightpupildownLower = rightpupildowntoBottom1 + rightpupildowntoBottom2;
          //        Log.i( TAG, String.format("LeftPupilupUpper: %f  LeftPupildownLower: %f RightPupilupUpper: %f RightPupildownLower: %f", leftpupilupUpper,leftpupildownLower, rightpupilupUpper, rightpupildownLower));

          // for finding up using pupil up to eye contour center up and pupil down to eye contour down
          //        NormalizedLandmark leftPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(475);
          //        NormalizedLandmark leftPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(477);
          //        NormalizedLandmark rightPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(470);
          //        NormalizedLandmark rightPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(472);
          //        NormalizedLandmark leftTopCenter = result.multiFaceLandmarks().get(0).getLandmarkList().get(386);
          //        NormalizedLandmark leftBottomCenter = result.multiFaceLandmarks().get(0).getLandmarkList().get(374);
          //        NormalizedLandmark rightTopCenter = result.multiFaceLandmarks().get(0).getLandmarkList().get(159);
          //        NormalizedLandmark rightBottomCenter = result.multiFaceLandmarks().get(0).getLandmarkList().get(145);
          //        double leftpupiluptoTopCenter = calculateDistanceNormalized(leftPupilUp, leftTopCenter);
          //        double rightpupiluptoTopCenter = calculateDistanceNormalized(rightPupilUp,rightTopCenter);
          //        double leftpupildowntoBottomCenter = calculateDistanceNormalized(leftPupilDown,leftBottomCenter);
          //        double rightpupildowntoBottomCenter = calculateDistanceNormalized(rightPupilDown,rightBottomCenter);
          //        Log.i( TAG, String.format("LeftPupiluptoTopCenter: %f  LeftPupildowntoBottomCenter: %f RightPupiluptoTopCenter: %f RightPupildowntoBottomCenter: %f", leftpupiluptoTopCenter,leftpupildowntoBottomCenter, rightpupiluptoTopCenter, rightpupildowntoBottomCenter));

          // for finding up using pupil center to face up and down method 1
          //        NormalizedLandmark leftFaceUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(257);
          //        NormalizedLandmark leftFaceDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(253);
          //        NormalizedLandmark rightFaceUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(27);
          //        NormalizedLandmark rightFaceDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(23);
          //        double leftpupiltoFaceUp = calculateDistanceNormalized(leftPupil, leftFaceUp);
          //        double leftpupiltoFaceDown = calculateDistanceNormalized(leftPupil, leftFaceDown);
          //        double rightpupiltoFaceUp = calculateDistanceNormalized(rightPupil, rightFaceUp);
          //        double rightpupiltoFaceDown = calculateDistanceNormalized(rightPupil, rightFaceDown);
          //        Log.i( TAG, String.format("LeftPupiltoFaceUp: %f  LeftPupiltoFaceDown: %f RightPupiltoFaceUp: %f RightPupiltoFaceDown: %f", leftpupiltoFaceUp,leftpupiltoFaceDown, rightpupiltoFaceUp, rightpupiltoFaceDown));

          // for finding up pupil up to line and pupil down to line distance
          NormalizedLandmark leftPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(475);
          NormalizedLandmark leftPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(477);
          NormalizedLandmark rightPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(470);
          NormalizedLandmark rightPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(472);
          double leftpupilUptoLine = calculatePointDistance(leftPupilUp, leftHorizontal1, leftHorizontal2);
          double leftpupilDowntoLine = calculatePointDistance(leftPupilDown, leftHorizontal1, leftHorizontal2);
          double rightpupilUptoLine = calculatePointDistance(rightPupilUp, rightHorizontal1, rightHorizontal2);
          double rightpupilDowntoLine = calculatePointDistance(rightPupilDown, rightHorizontal1, rightHorizontal2);


          //Log.i( TAG, String.format("LeftPupilUptoLine: %f  LeftPupilDowntoLine: %f RightPupilUptoLine: %f RightPupilDowntoLine: %f", leftpupilUptoLine,leftpupilDowntoLine, rightpupilUptoLine, rightpupilDowntoLine));

          if (leftpupilUptoLine > (2 * leftpupilDowntoLine) && rightpupilUptoLine > (2 * rightpupilDowntoLine)) {
            resultSequence.add("U");
            //Log.i( TAG, resultSequence.toString());
          }
          //        else if(Math.abs(leftpupilDowntoLine-leftpupilUptoLine)<0.003 && Math.abs(rightpupilDowntoLine-rightpupilUptoLine)<0.003){
          //          resultSequence.add("d");
          //          //Log.i( TAG, resultSequence.toString());
          //        }
          else if (leftpupiltoCorner1 > (2 * leftpupiltoCorner2) && rightpupiltoCorner1 > (2 * rightpupiltoCorner2)) {
            resultSequence.add("L");
            //Log.i( TAG, resultSequence.toString());
          } else if (leftpupiltoCorner2 > (2 * leftpupiltoCorner1) && rightpupiltoCorner2 > (2 * rightpupiltoCorner1)) {
            resultSequence.add("R");
            //Log.i( TAG, resultSequence.toString());
          } else {
            resultSequence.add("O");
            //Log.i(TAG, resultSequence.toString());
          }
        }
        //tv.setText(resultSequence.toString());
      }else{
        // patient start camera auto fn trial
        double lh = calculateDistanceNormalized(leftHorizontal1, leftHorizontal2);
        double rh = calculateDistanceNormalized(rightHorizontal1, rightHorizontal2);
        double lv1 = calculateDistanceNormalized(leftTop1, leftBottom1);
        double lv2 = calculateDistanceNormalized(leftTop2, leftBottom2);
        double rv1 = calculateDistanceNormalized(rightTop1, rightBottom1);
        double rv2 = calculateDistanceNormalized(rightTop2, rightBottom2);
        double leftEyeRatio = (lv1 + lv2) / (2 * lh);
        double rightEyeRatio = (rv1 + rv2) / (2 * rh);
        //Log.i( TAG, String.format("Left ER: %f  Right ER: %f", leftEyeRatio,rightEyeRatio));
        if(trigger ==0){
          if(trigger_count > 20){
            trigger=1;
            trigger_count=0;
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                tv.setText("Begin the sequence");
              }
            });
            t1.speak("Begin", TextToSpeech.QUEUE_FLUSH, null);
          }else {
            if (leftEyeRatio < 0.1 && !(rightEyeRatio < 0.1)) {
              //resultSequence.add("W");
              trigger_count++;
            }else{
              trigger_count = 0;
            }
          }
        }else {
          // trigger = 1;
          if (leftEyeRatio < 0.1 && !(rightEyeRatio < 0.1)) {
            resultSequence.add("W");
            trigger_count++;
            if(trigger_count>20){
              trigger = 0;
              trigger_count = 0;
              String res = "";
              for (int i = 0; i < resultSequence.size() - 2; i++) {
                if (resultSequence.get(i).equals(resultSequence.get(i + 1))) {
                  if (resultSequence.get(i + 1).equals(resultSequence.get(i + 2))) {
                    res = res + resultSequence.get(i);
                    i++;
                  }
                }
              }
              int last = res.lastIndexOf('O');
              int first = res.indexOf('O');
              res = res.substring(first,last);
              //Log.i(TAG, res);
              int tc = 0;
              String res1 = "";
              for (int i = 0; i < res.length(); i++) {
                if (res.charAt(i) == 'C') {
                  tc++;
                } else {
                  if (tc > 10) {
                    res1 = res1 + 'S';
                    tc = 0;
                  } else if (tc > 0) {
                    res1 = res1 + 'C';
                    tc = 0;
                  }
                  res1 = res1 + res.charAt(i);
                }
              }
              int l = res1.length();
              int j = 0;
              String fin = "";
              fin = fin + res1.charAt(j);
              for (int i = 1; i < l; i++) {
                if (res1.charAt(i) != res1.charAt(j)) {
                  fin = fin + res1.charAt(i);
                  j = i;
                }
              }
              int max = 0;
              String finkey = "";
              for (Map.Entry<String, String> e : dict.entrySet()) {
                //Log.i(TAG, fin + " " + e.getValue());
                int temp = FuzzySearch.ratio(fin, e.getValue());
                //Log.i(TAG, String.valueOf(temp));
                if (temp > max) {
                  max = temp;
                  finkey = e.getKey();
                }
              }
              String finalRes = res;
              String finalFin = fin;
              String finalFinkey = finkey;
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  tv.setText("Initial Triads:  " + finalRes + "\nFinal Sequence: " + finalFin + "\nResult: " + finalFinkey);
                }
              });
              resultSequence.clear();
              t1.speak(finkey, TextToSpeech.QUEUE_FLUSH, null);
//              Log.i( TAG, "Result here");
//              Log.i( TAG, resultSequence.toString());
//              tv.setText("Works boop.....................................");
            }
            // Log.i( TAG, resultSequence.toString());
          } else if (!(leftEyeRatio < 0.1) && rightEyeRatio < 0.1) {
            resultSequence.add("V");
            trigger_count = 0;
          } else if ((leftEyeRatio + rightEyeRatio) / 2 < 0.1) {
            resultSequence.add("C");
            trigger_count = 0;
            //Log.i( TAG, resultSequence.toString());
          } else {
            // eye
            trigger_count = 0;
            NormalizedLandmark leftPupil = result.multiFaceLandmarks().get(0).getLandmarkList().get(473);
            NormalizedLandmark rightPupil = result.multiFaceLandmarks().get(0).getLandmarkList().get(468);
            double leftpupiltoCorner1 = calculateDistanceNormalized(leftPupil, leftTop1);
            double leftpupiltoCorner2 = calculateDistanceNormalized(leftPupil, leftTop2);
            double rightpupiltoCorner1 = calculateDistanceNormalized(rightPupil, rightTop1);
            double rightpupiltoCorner2 = calculateDistanceNormalized(rightPupil, rightTop2);
            //Log.i( TAG, String.format("LeftPupiltoCorner1: %f  LeftPupiltoCorner2: %f RightPupiltoCorner1: %f RightPupiltoCorner2: %f", leftpupiltoCorner1,leftpupiltoCorner2, rightpupiltoCorner1, rightpupiltoCorner2));

            // for finding up pupil up to line and pupil down to line distance
            NormalizedLandmark leftPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(475);
            NormalizedLandmark leftPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(477);
            NormalizedLandmark rightPupilUp = result.multiFaceLandmarks().get(0).getLandmarkList().get(470);
            NormalizedLandmark rightPupilDown = result.multiFaceLandmarks().get(0).getLandmarkList().get(472);
            double leftpupilUptoLine = calculatePointDistance(leftPupilUp, leftHorizontal1, leftHorizontal2);
            double leftpupilDowntoLine = calculatePointDistance(leftPupilDown, leftHorizontal1, leftHorizontal2);
            double rightpupilUptoLine = calculatePointDistance(rightPupilUp, rightHorizontal1, rightHorizontal2);
            double rightpupilDowntoLine = calculatePointDistance(rightPupilDown, rightHorizontal1, rightHorizontal2);


            //Log.i( TAG, String.format("LeftPupilUptoLine: %f  LeftPupilDowntoLine: %f RightPupilUptoLine: %f RightPupilDowntoLine: %f", leftpupilUptoLine,leftpupilDowntoLine, rightpupilUptoLine, rightpupilDowntoLine));

            if (leftpupilUptoLine > (2 * leftpupilDowntoLine) && rightpupilUptoLine > (2 * rightpupilDowntoLine)) {
              resultSequence.add("U");
              //Log.i( TAG, resultSequence.toString());
            }
            //        else if(Math.abs(leftpupilDowntoLine-leftpupilUptoLine)<0.003 && Math.abs(rightpupilDowntoLine-rightpupilUptoLine)<0.003){
            //          resultSequence.add("d");
            //          //Log.i( TAG, resultSequence.toString());
            //        }
            else if (leftpupiltoCorner1 > (2 * leftpupiltoCorner2) && rightpupiltoCorner1 > (2 * rightpupiltoCorner2)) {
              resultSequence.add("L");
              //Log.i( TAG, resultSequence.toString());
            } else if (leftpupiltoCorner2 > (2 * leftpupiltoCorner1) && rightpupiltoCorner2 > (2 * rightpupiltoCorner1)) {
              resultSequence.add("R");
              //Log.i( TAG, resultSequence.toString());
            } else {
              resultSequence.add("O");
              //Log.i(TAG, resultSequence.toString());
            }
          }
          //tv.setText(resultSequence.toString());
        }
      }
    }
  }

  public double calculateDistanceRaw( NormalizedLandmark a, NormalizedLandmark b, int w, int h){
    double x = ((b.getX()*w)-(a.getX()*h)) * ((b.getX()*w)-(a.getX()*h));
    double y = ((b.getY()*w)-(a.getY()*h)) * ((b.getY()*w)-(a.getY()*h));
    return Math.sqrt(x+y);
  }
  public double calculateDistanceNormalized( NormalizedLandmark a, NormalizedLandmark b){
    double x = (b.getX()-a.getX()) * (b.getX()-a.getX());
    double y = (b.getY()-a.getY()) * (b.getY()-a.getY());
    return Math.sqrt(x+y);
  }
  public double calculatePointDistance(NormalizedLandmark p, NormalizedLandmark l1, NormalizedLandmark l2) {
    double A = p.getX() - l1.getX(); // position of point rel one end of line
    double B = p.getY() - l1.getY();
    double C = l2.getX() - l1.getX(); // vector along line
    double D = l2.getY() - l1.getY();
    double E = -D; // orthogonal vector
    double dot = A * E + B * C;
    double len_sq = E * E + C * C;
    return (double) Math.abs(dot) / Math.sqrt(len_sq);
  }
}
