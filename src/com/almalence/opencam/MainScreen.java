/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
*/

package com.almalence.opencam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.opencam.billing.IabHelper;
import com.almalence.opencam.billing.IabResult;
import com.almalence.opencam.billing.Inventory;
import com.almalence.opencam.billing.Purchase;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.util.AppRater;
import com.almalence.opencam.util.Util;

/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

public class MainScreen extends Activity implements View.OnClickListener,
		View.OnTouchListener, SurfaceHolder.Callback, Camera.PictureCallback,
		Camera.AutoFocusCallback, Handler.Callback, Camera.ErrorCallback,
		Camera.PreviewCallback {
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<
	public static MainScreen thiz;
	public static Context mainContext;
	public static Handler H;

	private Object syncObject = new Object();

	private static final int MSG_RETURN_CAPTURED = -1;

	// public static boolean FramesShot = false;

	public static File ForceFilename = null;

	private static Camera camera = null;

	public static GUI guiManager = null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	private static GLLayer glView;

	public boolean mPausing = false;

	Bundle msavedInstanceState;
	// private. if necessary?!?!?
	public SurfaceHolder surfaceHolder;
	public SurfaceView preview;
	private OrientationEventListener orientListener;
	private boolean landscapeIsNormal = false;
	private boolean surfaceJustCreated = false;
	private boolean surfaceCreated = false;
	public byte[] pviewBuffer;

	// shared between activities
	public static int surfaceWidth, surfaceHeight;
	private static int imageWidth, imageHeight;
	public static int previewWidth, previewHeight;
	private static int saveImageWidth, saveImageHeight;
	public static PowerManager pm = null;

	private CountDownTimer ScreenTimer = null;
	private boolean isScreenTimerRunning = false;

	public static int CameraIndex = 0;
	private static boolean CameraMirrored = false;
	private static boolean wantLandscapePhoto = false;
	public static int orientationMain = 0;
	public static int orientationMainPrevious = 0;

	private SoundPlayer shutterPlayer = null;

	// Flags to know which camera feature supported at current device
	public boolean mEVSupported = false;
	public boolean mSceneModeSupported = false;
	public boolean mWBSupported = false;
	public boolean mFocusModeSupported = false;
	public boolean mFlashModeSupported = false;
	public boolean mISOSupported = false;
	public boolean mCameraChangeSupported = false;

	public static List<String> supportedSceneModes;
	public static List<String> supportedWBModes;
	public static List<String> supportedFocusModes;
	public static List<String> supportedFlashModes;
	public static List<String> supportedISOModes;

	// Common preferences
	public static String ImageSizeIdxPreference;
	public static boolean ShutterPreference = true;
	// public static boolean FullMediaRescan;
	public static final String SavePathPref = "savePathPref";

	public static String SaveToPath;
	public static boolean SaveInputPreference;
	public static String SaveToPreference;

	// Camera resolution variables and lists
	public static final int MIN_MPIX_SUPPORTED = 1280 * 960;
	// public static final int MIN_MPIX_PREVIEW = 600*400;

	public static int CapIdx;

	public static List<Long> ResolutionsMPixList;
	public static List<String> ResolutionsIdxesList;
	public static List<String> ResolutionsNamesList;

	public static List<Long> ResolutionsMPixListIC;
	public static List<String> ResolutionsIdxesListIC;
	public static List<String> ResolutionsNamesListIC;

	public static List<Long> ResolutionsMPixListVF;
	public static List<String> ResolutionsIdxesListVF;
	public static List<String> ResolutionsNamesListVF;

	public static final int FOCUS_STATE_IDLE = 0;
	public static final int FOCUS_STATE_FOCUSED = 1;
	public static final int FOCUS_STATE_FAIL = 3;
	public static final int FOCUS_STATE_FOCUSING = 4;

	public static final int CAPTURE_STATE_IDLE = 0;
	public static final int CAPTURE_STATE_CAPTURING = 1;

	private static int mFocusState = FOCUS_STATE_IDLE;
	private static int mCaptureState = CAPTURE_STATE_IDLE;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	public static boolean isCreating = false;
	public static boolean mApplicationStarted = false;
	public static long startTime = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startTime = System.currentTimeMillis();
		msavedInstanceState = savedInstanceState;
		mainContext = this.getBaseContext();
		H = new Handler(this);
		thiz = this;

		mApplicationStarted = false;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ensure landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// set to fullscreen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// set some common view here
		setContentView(R.layout.opencamera_main_layout);

		/**** Billing *****/
		createBillingHandler();
		/**** Billing *****/
		
		//application rating helper
		AppRater.app_launched(this);

		// set preview, on click listener and surface buffers
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		orientListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int orientation) {
				// figure landscape or portrait
				if (MainScreen.thiz.landscapeIsNormal) {
					// Log.e("MainScreen",
					// "landscapeIsNormal = true. Orientation " + orientation +
					// "+90");
					orientation += 90;
				}
				// else
				// Log.e("MainScreen", "landscapeIsNormal = false. Orientation "
				// + orientation);

				if ((orientation < 45)
						|| (orientation > 315 && orientation < 405)
						|| ((orientation > 135) && (orientation < 225))) {
					if (MainScreen.wantLandscapePhoto == true) {
						MainScreen.wantLandscapePhoto = false;
						// Log.e("MainScreen", "Orientation = " + orientation);
						// Log.e("MainScreen","Orientation Changed. wantLandscapePhoto = "
						// + String.valueOf(MainScreen.wantLandscapePhoto));
						//PluginManager.getInstance().onOrientationChanged(false);
					}
				} else {
					if (MainScreen.wantLandscapePhoto == false) {
						MainScreen.wantLandscapePhoto = true;
						// Log.e("MainScreen", "Orientation = " + orientation);
						// Log.e("MainScreen","Orientation Changed. wantLandscapePhoto = "
						// + String.valueOf(MainScreen.wantLandscapePhoto));
						//PluginManager.getInstance().onOrientationChanged(true);
					}
				}

				// orient properly for video
				if ((orientation > 135) && (orientation < 225))
					orientationMain = 270;
				else if ((orientation < 45) || (orientation > 315))
					orientationMain = 90;
				else if ((orientation < 325) && (orientation > 225))
					orientationMain = 0;
				else if ((orientation < 135) && (orientation > 45))
					orientationMain = 180;
				
				if(orientationMain != orientationMainPrevious)
				{
					orientationMainPrevious = orientationMain;
					PluginManager.getInstance().onOrientationChanged(orientationMain);
				}
			}
		};

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		// prevent power drain
		ScreenTimer = new CountDownTimer(180000, 180000) {
			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				preview.setKeepScreenOn(false);
				isScreenTimerRunning = false;
			}
		};
		ScreenTimer.start();
		isScreenTimerRunning = true;

		// Description
		// init gui manager
		guiManager = new AlmalenceGUI();
		guiManager.createInitialGUI();
		this.findViewById(R.id.mainLayout1).invalidate();
		this.findViewById(R.id.mainLayout1).requestLayout();
		guiManager.onCreate();

		// init plugin manager
		PluginManager.getInstance().onCreate();

		if (this.getIntent().getAction() != null) {
			if (this.getIntent().getAction()
					.equals(MediaStore.ACTION_IMAGE_CAPTURE)) {
				try {
					MainScreen.ForceFilename = new File(
							((Uri) this.getIntent().getExtras()
									.getParcelable(MediaStore.EXTRA_OUTPUT))
									.getPath());
					if (MainScreen.ForceFilename.getAbsolutePath().equals(
							"/scrapSpace")) {
						MainScreen.ForceFilename = new File(Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ "/mms/scrapSpace/.temp.jpg");
						new File(MainScreen.ForceFilename.getParent()).mkdirs();
					}
				} catch (Exception e) {
					MainScreen.ForceFilename = null;
				}
			} else {
				MainScreen.ForceFilename = null;
			}
		} else {
			MainScreen.ForceFilename = null;
		}
	}

	public void onPreferenceCreate(PreferenceFragment prefActivity) {
		CharSequence[] entries;
		CharSequence[] entryValues;

		if (ResolutionsIdxesList != null) {
			entries = ResolutionsNamesList
					.toArray(new CharSequence[ResolutionsNamesList.size()]);
			entryValues = ResolutionsIdxesList
					.toArray(new CharSequence[ResolutionsIdxesList.size()]);

			ListPreference lp = (ListPreference) prefActivity
					.findPreference("imageSizePrefCommon");
			lp.setEntries(entries);
			lp.setEntryValues(entryValues);

			// set currently selected image size
			int idx;
			for (idx = 0; idx < ResolutionsIdxesList.size(); ++idx) {
				if (Integer.parseInt(ResolutionsIdxesList.get(idx)) == CapIdx) {
					break;
				}
			}
			if (idx < ResolutionsIdxesList.size()) {
				lp.setValueIndex(idx);
			}
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				// @Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					int value = Integer.parseInt(newValue.toString());
					CapIdx = value;
					return true;
				}
			});
		}
	}

	public void queueGLEvent(final Runnable runnable) {
		final GLSurfaceView surfaceView = glView;

		if (surfaceView != null && runnable != null) {
			surfaceView.queueEvent(runnable);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		MainScreen.guiManager.onStart();
		PluginManager.getInstance().onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mApplicationStarted = false;
		MainScreen.guiManager.onStop();
		PluginManager.getInstance().onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		MainScreen.guiManager.onDestroy();
		PluginManager.getInstance().onDestroy();

		/**** Billing *****/
		destroyBillingHandler();
		/**** Billing *****/

		glView = null;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!isCreating)
			new CountDownTimer(50, 50) {
				public void onTick(long millisUntilFinished) {
				}

				public void onFinish() {
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.mainContext);
					CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
							: 1;
					ShutterPreference = prefs.getBoolean("shutterPrefCommon",
							false);
					ImageSizeIdxPreference = prefs.getString(
							"imageSizePrefCommon", "-1");
					// FullMediaRescan = prefs.getBoolean("mediaPref", true);
					SaveToPath = prefs.getString(SavePathPref, Environment
							.getExternalStorageDirectory().getAbsolutePath());
					SaveInputPreference = prefs.getBoolean("saveInputPref",
							false);
					SaveToPreference = prefs.getString("saveToPref", "0");

					MainScreen.guiManager.onResume();
					PluginManager.getInstance().onResume();
					MainScreen.thiz.mPausing = false;

					if (surfaceCreated && (camera == null)) {
						MainScreen.thiz.findViewById(R.id.mainLayout2)
								.setVisibility(View.VISIBLE);
						setupCamera(surfaceHolder);

						if (glView != null && camera != null)
							glView.onResume();

						PluginManager.getInstance().onGUICreate();
						MainScreen.guiManager.onGUICreate();
					}
					orientListener.enable();
				}
			}.start();

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources()
				.openRawResourceFd(R.raw.plugin_capture_tick));

		if (ScreenTimer != null) {
			if (isScreenTimerRunning)
				ScreenTimer.cancel();
			ScreenTimer.start();
			isScreenTimerRunning = true;
		}

		Log.e("Density", "" + getResources().getDisplayMetrics().toString());
	}

	@Override
	protected void onPause() {
		super.onPause();
		mApplicationStarted = false;

		MainScreen.guiManager.onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		// initiate full media rescan
		// if (FramesShot && FullMediaRescan)
		// {
		// // using MediaScannerConnection.scanFile(this, paths, null, null);
		// instead
		// sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
		// Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
		// FramesShot = false;
		// }

		if (ShutterPreference) {
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.mainContext.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		}

		this.mPausing = true;

		if (glView != null) {
			glView.onPause();
		}

		if (ScreenTimer != null) {
			if (isScreenTimerRunning)
				ScreenTimer.cancel();
			isScreenTimerRunning = false;
		}

		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}

		this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

		if (shutterPlayer != null) {
			shutterPlayer.release();
			shutterPlayer = null;
		}
	}

	public void PauseMain() {
		onPause();
	}

	public void StopMain() {
		onStop();
	}

	public void StartMain() {
		onStart();
	}

	public void ResumeMain() {
		onResume();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format,
			final int width, final int height) {

		if (!isCreating)
			new CountDownTimer(50, 50) {
				public void onTick(long millisUntilFinished) {

				}

				public void onFinish() {
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.mainContext);
					CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
							: 1;
					ShutterPreference = prefs.getBoolean("shutterPrefCommon",
							false);
					ImageSizeIdxPreference = prefs.getString(
							"imageSizePrefCommon", "-1");
					// FullMediaRescan = prefs.getBoolean("mediaPref", true);

					if (!MainScreen.thiz.mPausing && surfaceCreated
							&& (camera == null)) {
						surfaceWidth = width;
						surfaceHeight = height;
						MainScreen.thiz.findViewById(R.id.mainLayout2)
								.setVisibility(View.VISIBLE);
						setupCamera(holder);
						PluginManager.getInstance().onGUICreate();
						MainScreen.guiManager.onGUICreate();
					}
				}
			}.start();
		else {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
					: 1;
			ShutterPreference = prefs.getBoolean("shutterPrefCommon", false);
			ImageSizeIdxPreference = prefs.getString("imageSizePrefCommon",
					"-1");
			// FullMediaRescan = prefs.getBoolean("mediaPref", true);

			if (!MainScreen.thiz.mPausing && surfaceCreated && (camera == null)) {
				surfaceWidth = width;
				surfaceHeight = height;
			}
		}
	}

	public void delayedSetup() {
		isCreating = false;
		MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(
				View.VISIBLE);
		setupCamera(surfaceHolder);
		PluginManager.getInstance().onGUICreate();
		MainScreen.guiManager.onGUICreate();
	}

	@TargetApi(9)
	protected void openCameraFrontOrRear() {
		if (Camera.getNumberOfCameras() > 0) {
			camera = Camera.open(MainScreen.CameraIndex);
		}

		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(CameraIndex, cameraInfo);

		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			CameraMirrored = true;
		else
			CameraMirrored = false;
	}

	public void setupCamera(SurfaceHolder holder) {
		if (camera == null) {
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					openCameraFrontOrRear();
				} else {
					camera = Camera.open();
				}
			} catch (RuntimeException e) {
				camera = null;
			}

			if (camera == null) {
				// H.sendEmptyMessage(MSG_NO_CAMERA);
				return;
			}
		}

		PluginManager.getInstance().SelectDefaults();

		// screen rotation
		try {
			camera.setDisplayOrientation(90);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}

		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}

		PopulateCameraDimensions();
		ResolutionsMPixListIC = ResolutionsMPixList;
		ResolutionsIdxesListIC = ResolutionsIdxesList;
		ResolutionsNamesListIC = ResolutionsNamesList;

		PluginManager.getInstance().SelectImageDimension(); // updates SX, SY
															// values

		// ----- Select preview dimensions with ratio correspondent to full-size
		// image
		PluginManager.getInstance().SetCameraPreviewSize();

		guiManager.setupViewfinderPreviewSize();

		Camera.Parameters cp = camera.getParameters();
		Size previewSize = cp.getPreviewSize();

		if (PluginManager.getInstance().isGLSurfaceNeeded()) {
			if (glView == null) {
				glView = new GLLayer(MainScreen.mainContext);// (GLLayer)findViewById(R.id.SurfaceView02);
				glView.setLayoutParams(new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
				((RelativeLayout) findViewById(R.id.mainLayout2)).addView(
						glView, 1);
				glView.setZOrderMediaOverlay(true);
				glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			}
		} else {
			((RelativeLayout) findViewById(R.id.mainLayout2))
					.removeView(glView);
			glView = null;
		}

		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) preview
				.getLayoutParams();
		if (glView != null) {
			glView.setVisibility(View.VISIBLE);
			glView.setLayoutParams(lp);
		} else {
			if (glView != null)
				glView.setVisibility(View.GONE);
		}

		pviewBuffer = new byte[previewSize.width
				* previewSize.height
				* ImageFormat.getBitsPerPixel(camera.getParameters()
						.getPreviewFormat()) / 8];

		camera.setErrorCallback(MainScreen.thiz);

		supportedSceneModes = getSupportedSceneModes();
		supportedWBModes = getSupportedWhiteBalance();
		supportedFocusModes = getSupportedFocusModes();
		supportedFlashModes = getSupportedFlashModes();
		supportedISOModes = getSupportedISO();

		PluginManager.getInstance().SetCameraPictureSize();
		PluginManager.getInstance().SetupCameraParameters();
		cp = camera.getParameters();

		try {
			camera.setParameters(cp);
		} catch (RuntimeException e) {
			Log.e("CameraTest", "MainScreen.setupCamera unable setParameters "
					+ e.getMessage());
		}

		previewWidth = camera.getParameters().getPreviewSize().width;
		previewHeight = camera.getParameters().getPreviewSize().height;

		Util.initialize(mainContext);

		guiManager.onCameraCreate();
		PluginManager.getInstance().onCameraParametersSetup();

		// ----- Start preview and setup frame buffer if needed

		// ToDo: call camera release sequence from onPause somewhere ???
		new CountDownTimer(10, 10) {
			@Override
			public void onFinish() {
				try // exceptions sometimes happen here when resuming after
					// processing
				{
					camera.startPreview();
				} catch (RuntimeException e) {
					// MainScreen.H.sendEmptyMessage(MainScreen.MSG_NO_CAMERA);
					return;
				}

				camera.setPreviewCallbackWithBuffer(MainScreen.thiz);
				camera.addCallbackBuffer(pviewBuffer);

				PluginManager.getInstance().onCameraSetup();
				guiManager.onCameraSetup();
				MainScreen.mApplicationStarted = true;
			}

			@Override
			public void onTick(long millisUntilFinished) {
			}
		}.start();
	}

	public static void PopulateCameraDimensions() {
		ResolutionsMPixList = new ArrayList<Long>();
		ResolutionsIdxesList = new ArrayList<String>();
		ResolutionsNamesList = new ArrayList<String>();

		if (MainScreen.camera == null)
			return;
		Camera.Parameters cp = MainScreen.camera.getParameters();

		List<Camera.Size> cs;
		int MinMPIX = MIN_MPIX_SUPPORTED;
		cs = cp.getSupportedPictureSizes();

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;
		Size sHighest = cs.get(iHighestIndex);

		for (int ii = 0; ii < cs.size(); ++ii) {
			Size s = cs.get(ii);

			if ((long) s.width * s.height > (long) sHighest.width
					* sHighest.height) {
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) s.width * s.height < MinMPIX)
				continue;

			Long lmpix = (long) s.width * s.height;
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.width / s.height;

			// find good location in a list
			int loc;
			for (loc = 0; loc < ResolutionsMPixList.size(); ++loc)
				if (ResolutionsMPixList.get(loc) < lmpix)
					break;

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;
			if (Math.abs(ratio - 1) == 0)
				ri = 4;

			ResolutionsNamesList.add(loc,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			ResolutionsIdxesList.add(loc, String.format("%d", ii));
			ResolutionsMPixList.add(loc, lmpix);
		}

		if (ResolutionsNamesList.size() == 0) {
			Size s = cs.get(iHighestIndex);

			Long lmpix = (long) s.width * s.height;
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.width / s.height;

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;
			if (Math.abs(ratio - 1) == 0)
				ri = 4;

			ResolutionsNamesList.add(0,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			ResolutionsIdxesList.add(0, String.format("%d", 0));
			ResolutionsMPixList.add(0, lmpix);
		}

		return;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// ----- Find 'normal' orientation of the device

		Display display = ((WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if ((rotation == Surface.ROTATION_90)
				|| (rotation == Surface.ROTATION_270))
			landscapeIsNormal = true; // false; - if landscape view orientation
										// set for MainScreen
		else
			landscapeIsNormal = false;

		surfaceCreated = true;
		surfaceJustCreated = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceCreated = false;
		surfaceJustCreated = false;
	}

	@TargetApi(14)
	public boolean isFaceDetectionAvailable(Camera.Parameters params) {
		if (params.getMaxNumDetectedFaces() > 0)
			return true;
		else
			return false;
	}

	public Size getPreviewSize() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return null;

		if (camera != null)
			return camera.new Size(lp.width, lp.height);
		else
			return null;
	}

	public int getPreviewWidth() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return 0;

		return lp.width;

	}

	public int getPreviewHeight() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return 0;

		return lp.height;
	}

	@Override
	public void onError(int error, Camera camera) {
	}

	/*
	 * CAMERA parameters access functions
	 * 
	 * Camera.Parameters get/set Camera scene modes getSupported/set Camera
	 * white balance getSupported/set Camera focus modes getSupported/set Camera
	 * flash modes getSupported/set
	 * 
	 * For API14 Camera focus areas get/set Camera metering areas get/set
	 */
	public boolean isFrontCamera() {
		return CameraMirrored;
	}

	public Camera getCamera() {
		return camera;
	}

	public void setCamera(Camera cam) {
		camera = cam;
	}

	public Camera.Parameters getCameraParameters() {
		if (camera != null)
			return camera.getParameters();

		return null;
	}

	public void setCameraParameters(Camera.Parameters params) {
		if (params != null && camera != null)
			camera.setParameters(params);
	}

	public boolean isExposureCompensationSupported() {
		if (camera != null) {
			if (camera.getParameters().getMinExposureCompensation() == 0
					&& camera.getParameters().getMaxExposureCompensation() == 0)
				return false;
			else
				return true;
		} else
			return false;
	}

	public int getMinExposureCompensation() {
		if (camera != null)
			return camera.getParameters().getMinExposureCompensation();
		else
			return 0;
	}

	public int getMaxExposureCompensation() {
		if (camera != null)
			return camera.getParameters().getMaxExposureCompensation();
		else
			return 0;
	}

	public float getExposureCompensationStep() {
		if (camera != null)
			return camera.getParameters().getExposureCompensationStep();
		else
			return 0;
	}

	public float getExposureCompensation() {
		if (camera != null)
			return camera.getParameters().getExposureCompensation()
					* camera.getParameters().getExposureCompensationStep();
		else
			return 0;
	}

	public void resetExposureCompensation() {
		if (camera != null) {
			if (!isExposureCompensationSupported())
				return;
			Camera.Parameters params = camera.getParameters();
			params.setExposureCompensation(0);
			camera.setParameters(params);
		}
	}

	public boolean isSceneModeSupported() {
		List<String> supported_scene = getSupportedSceneModes();
		if (supported_scene != null && supported_scene.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedSceneModes() {
		if (camera != null)
			return camera.getParameters().getSupportedSceneModes();

		return null;
	}

	public boolean isWhiteBalanceSupported() {
		List<String> supported_wb = getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedWhiteBalance() {
		if (camera != null)
			return camera.getParameters().getSupportedWhiteBalance();

		return null;
	}

	public boolean isFocusModeSupported() {
		List<String> supported_focus = getSupportedFocusModes();
		if (supported_focus != null && supported_focus.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedFocusModes() {
		if (camera != null)
			return camera.getParameters().getSupportedFocusModes();

		return null;
	}

	public boolean isFlashModeSupported() {
		List<String> supported_flash = getSupportedFlashModes();
		if (supported_flash != null && supported_flash.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedFlashModes() {
		if (camera != null)
			return camera.getParameters().getSupportedFlashModes();

		return null;
	}

	public boolean isISOSupported() {
		List<String> supported_iso = getSupportedISO();
		if (supported_iso != null && supported_iso.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedISO() {
		if (camera != null) {
			Camera.Parameters camParams = MainScreen.camera.getParameters();
			String supportedIsoValues = camParams.get("iso-values");
			if (supportedIsoValues != "" && supportedIsoValues != null) {
				List<String> isoList = new ArrayList<String>();
				String delims = "[,]+";
				String[] ISOs = supportedIsoValues.split(delims);
				for (int i = 0; i < ISOs.length; i++)
					isoList.add(ISOs[i]);

				return isoList;
			}
		}

		return null;
	}

	public String getSceneMode() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null)
				return params.getSceneMode();
		}

		return null;
	}

	public String getWBMode() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null)
				return params.getWhiteBalance();
		}

		return null;
	}

	public String getFocusMode() {
		
		try {
			if (camera != null) {
				Camera.Parameters params = camera.getParameters();
				if (params != null)
					return params.getFocusMode();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("MainScreen", "getFocusMode exception: " + e.getMessage());
		}

		return null;
	}

	public String getFlashMode() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null)
				return params.getFlashMode();
		}

		return null;
	}

	public String getISOMode() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null)
				return params.get("iso");
		}

		return null;
	}

	public void setCameraSceneMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.setSceneMode(mode);
				camera.setParameters(params);
			}
		}
	}

	public void setCameraWhiteBalance(String mode) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.setWhiteBalance(mode);
				camera.setParameters(params);
			}
		}
	}

	public void setCameraFocusMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.setFocusMode(mode);
				camera.setParameters(params);
			}
		}
	}

	public void setCameraFlashMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.setFlashMode(mode);
				camera.setParameters(params);
			}
		}
	}

	public void setCameraISO(String mode) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.set("iso", mode);
				camera.setParameters(params);
			}
		}
	}

	public void setCameraExposureCompensation(int iEV) {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			if (params != null) {
				params.setExposureCompensation(iEV);
				camera.setParameters(params);
			}
		}
	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(String sceneMode) {
		return guiManager.getSceneIcon(sceneMode);
	}

	public int getWBIcon(String wb) {
		return guiManager.getWBIcon(wb);
	}

	public int getFocusIcon(String focusMode) {
		return guiManager.getFocusIcon(focusMode);
	}

	public int getFlashIcon(String flashMode) {
		return guiManager.getFlashIcon(flashMode);
	}

	public int getISOIcon(String isoMode) {
		return guiManager.getISOIcon(isoMode);
	}

	public void updateCameraFeatures() {
		mEVSupported = isExposureCompensationSupported();
		mSceneModeSupported = isSceneModeSupported();
		mWBSupported = isWhiteBalanceSupported();
		mFocusModeSupported = isFocusModeSupported();
		mFlashModeSupported = isFlashModeSupported();
		mISOSupported = isISOSupported();
	}

	public void setCameraFocusAreas(List<Area> focusAreas) {
		if (camera != null) {
			try {
				Camera.Parameters params = camera.getParameters();
				if (params != null) {
					params.setFocusAreas(focusAreas);
					camera.setParameters(params);
				}
			} catch (RuntimeException e) {
				Log.e("SetFocusArea", e.getMessage());
			}
		}
	}

	public void setCameraMeteringAreas(List<Area> meteringAreas) {
		if (camera != null) {
			try {
				Camera.Parameters params = camera.getParameters();
				if (params != null) {
					params.setMeteringAreas(meteringAreas);
					camera.setParameters(params);
				}
			} catch (RuntimeException e) {
				Log.e("SetMeteringArea", e.getMessage());
			}
		}
	}

	/*
	 * 
	 * CAMERA parameters access function ended
	 */

	// >>Description
	// section with user control procedures and main capture functions
	//
	// all events translated to PluginManager
	// Description<<

	public static boolean takePicture() {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null
					&& getFocusState() != MainScreen.FOCUS_STATE_FOCUSING) {
				MainScreen.mCaptureState = MainScreen.CAPTURE_STATE_CAPTURING;
				// Log.e("", "mFocusState = " + getFocusState());
				camera.takePicture(null, null, null, MainScreen.thiz);
				return true;
			}

			// Log.e("", "takePicture(). FocusState = FOCUS_STATE_FOCUSING ");
			return false;
		}
	}

	public static boolean autoFocus(Camera.AutoFocusCallback listener) {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null) {
				if (mCaptureState != CAPTURE_STATE_CAPTURING) {
					setFocusState(MainScreen.FOCUS_STATE_FOCUSING);
					camera.autoFocus(listener);
					return true;
				}
			}

			return false;
		}
	}

	public static boolean autoFocus() {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null) {
				if (mCaptureState != CAPTURE_STATE_CAPTURING) {
					String fm = thiz.getFocusMode();
					// Log.e("", "mCaptureState = " + mCaptureState);
					setFocusState(MainScreen.FOCUS_STATE_FOCUSING);
					camera.autoFocus(MainScreen.thiz);
					return true;
				}
			}

			// Log.e("", "autoFocus(). FocusState = CAPTURE_STATE_CAPTURING");

			return false;
		}
	}

	public static void cancelAutoFocus() {
		if (camera != null) {
			setFocusState(MainScreen.FOCUS_STATE_IDLE);
			camera.cancelAutoFocus();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!mApplicationStarted)
			return true;

		if (keyCode == KeyEvent.KEYCODE_MENU) {
			menuButtonPressed();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_CAMERA
				|| keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			MainScreen.guiManager.onHardwareShutterButtonPressed();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_FOCUS) {
			MainScreen.guiManager.onHardwareFocusButtonPressed();
			return true;
		}
		
		//check if volume button has some functions except Zoom-ing
		if ( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP )
		{
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			String defaultModeName = prefs.getString("volumeButtonPrefCommon", "0");
			if (defaultModeName.equals("0"))
			{
				MainScreen.guiManager.onHardwareFocusButtonPressed();
				MainScreen.guiManager.onHardwareShutterButtonPressed();
				return true;
			}
		}
		
		
		if (PluginManager.getInstance().onKeyDown(true, keyCode, event))
			return true;
		if (guiManager.onKeyDown(true, keyCode, event))
			return true;

		if (keyCode == KeyEvent.KEYCODE_BACK)
    	{
    		if (AppRater.showRateDialogIfNeeded(this))
    		{
    			return true;
    		}
    	}
		
		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}

	@Override
	public void onClick(View v) {
		if (mApplicationStarted)
			MainScreen.guiManager.onClick(v);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (mApplicationStarted)
			return MainScreen.guiManager.onTouch(view, event);
		return true;
	}

	public boolean onTouchSuper(View view, MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void onButtonClick(View v) {
		MainScreen.guiManager.onButtonClick(v);
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
		PluginManager.getInstance().onPictureTaken(paramArrayOfByte,
				paramCamera);
		MainScreen.mCaptureState = MainScreen.CAPTURE_STATE_IDLE;
	}

	@Override
	public void onAutoFocus(boolean focused, Camera paramCamera) {
		Log.e("", "onAutoFocus call");
		PluginManager.getInstance().onAutoFocus(focused, paramCamera);
		if (focused)
			setFocusState(MainScreen.FOCUS_STATE_FOCUSED);
		else
			setFocusState(MainScreen.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera) {
		PluginManager.getInstance().onPreviewFrame(data, paramCamera);
		camera.addCallbackBuffer(pviewBuffer);
	}

	// >>Description
	// message processor
	//
	// processing main events and calling active plugin procedures
	//
	// possible some additional plugin dependent events.
	//
	// Description<<
	@Override
	public boolean handleMessage(Message msg) {

		if (msg.what == MSG_RETURN_CAPTURED) {
			this.setResult(RESULT_OK);
			this.finish();
			return true;
		}
		PluginManager.getInstance().handleMessage(msg);

		return true;
	}

	public void menuButtonPressed() {
		PluginManager.getInstance().menuButtonPressed();
	}

	public void disableCameraParameter(GUI.CameraParameter iParam,
			boolean bDisable) {
		guiManager.disableCameraParameter(iParam, bDisable);
	}

	public void showOpenGLLayer() {
		if (glView != null && glView.getVisibility() == View.GONE) {
			glView.setVisibility(View.VISIBLE);
			glView.onResume();
		}
	}

	public void hideOpenGLLayer() {
		if (glView != null && glView.getVisibility() == View.VISIBLE) {
			glView.setVisibility(View.GONE);
			glView.onPause();
		}
	}

	public void PlayShutter(int sound) {
		if (!MainScreen.ShutterPreference) {
			MediaPlayer mediaPlayer = MediaPlayer
					.create(MainScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public void PlayShutter() {
		if (!MainScreen.ShutterPreference) {
			if (shutterPlayer != null)
				shutterPlayer.play();
		}
	}

	// set TRUE to mute and FALSE to unmute
	public void MuteShutter(boolean mute) {
		if (MainScreen.ShutterPreference) {
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.mainContext.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
		}
	}

	public static int getImageWidth() {
		return imageWidth;
	}

	public static void setImageWidth(int setImageWidth) {
		imageWidth = setImageWidth;
	}

	public static int getImageHeight() {
		return imageHeight;
	}

	public static void setImageHeight(int setImageHeight) {
		imageHeight = setImageHeight;
	}

	public static int getSaveImageWidth() {
		return saveImageWidth;
	}

	public static void setSaveImageWidth(int setSaveImageWidth) {
		saveImageWidth = setSaveImageWidth;
	}

	public static int getSaveImageHeight() {
		return saveImageHeight;
	}

	public static void setSaveImageHeight(int setSaveImageHeight) {
		saveImageHeight = setSaveImageHeight;
	}

	public static boolean getCameraMirrored() {
		return CameraMirrored;
	}

	public static void setCameraMirrored(boolean setCameraMirrored) {
		CameraMirrored = setCameraMirrored;
	}

	public static boolean getWantLandscapePhoto() {
		return wantLandscapePhoto;
	}

	public static void setWantLandscapePhoto(boolean setWantLandscapePhoto) {
		wantLandscapePhoto = setWantLandscapePhoto;
	}

	public static void setFocusState(int state) {
		if (state != MainScreen.FOCUS_STATE_IDLE
				&& state != MainScreen.FOCUS_STATE_FOCUSED
				&& state != MainScreen.FOCUS_STATE_FAIL)
			return;

		MainScreen.mFocusState = state;

		Message msg = new Message();
		msg.what = PluginManager.MSG_BROADCAST;
		msg.arg1 = PluginManager.MSG_FOCUS_STATE_CHANGED;
		H.sendMessage(msg);
	}

	public static int getFocusState() {
		return MainScreen.mFocusState;
	}

	/*******************************************************/
	/************************ Billing ************************/

	IabHelper mHelper;

	private boolean unlockAllPurchased = false;
	private boolean hdrPurchased = false;
	private boolean panoramaPurchased = false;
	private boolean objectRemovalBurstPurchased = false;
	private boolean groupShotPurchased = false;

	private void createBillingHandler() {
		if (isInstalled("com.almalence.hdr_plus"))
			hdrPurchased = true;
		if (isInstalled("com.almalence.panorama.smoothpanorama"))
			panoramaPurchased = true;
		if (isInstalled("com.almalence.pixfix"))
			hdrPurchased = true;

		String base64EncodedPublicKey = "market key here";
		// Create the helper, passing it our context and the public key to
		// verify signatures with
		Log.v("Main billing", "Creating IAB helper.");
		mHelper = new IabHelper(this, base64EncodedPublicKey);

		mHelper.enableDebugLogging(true);

		Log.v("Main billing", "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.v("Main billing", "Setup finished.");

				if (!result.isSuccess()) {
					Log.v("Main billing", "Problem setting up in-app billing: "
							+ result);
					return;
				}

				List<String> additionalSkuList = new ArrayList<String>();
				additionalSkuList.add("plugin_almalence_hdr");
				additionalSkuList.add("plugin_almalence_panorama");
				additionalSkuList.add("unlock_all_forever");
				additionalSkuList.add("plugin_almalence_moving_burst");
				additionalSkuList.add("plugin_almalence_groupshot");

				Log.v("Main billing", "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(true, additionalSkuList,
						mGotInventoryListener);
			}
		});
	}

	private void destroyBillingHandler() {
		try {
			if (mHelper != null)
				mHelper.dispose();
			mHelper = null;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing",
					"destroyBillingHandler exception: " + e.getMessage());
		}
	}

	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			if (inventory == null)
				return;

			if (inventory.hasPurchase("plugin_almalence_hdr")) {
				hdrPurchased = true;
			}
			if (inventory.hasPurchase("plugin_almalence_panorama")) {
				panoramaPurchased = true;
			}
			if (inventory.hasPurchase("unlock_all_forever")) {
				unlockAllPurchased = true;
			}
			if (inventory.hasPurchase("plugin_almalence_moving_burst")) {
				objectRemovalBurstPurchased = true;
			}
			if (inventory.hasPurchase("plugin_almalence_groupshot")) {
				groupShotPurchased = true;
			}
		}
	};

	private int HDR_REQUEST = 100;
	private int PANORAMA_REQUEST = 101;
	private int ALL_REQUEST = 102;
	private int OBJECTREM_BURST_REQUEST = 103;
	private int GROUPSHOT_REQUEST = 104;
	Preference hdrPref, panoramaPref, allPref, objectremovalPref,
			groupshotPref;

	public void onBillingPreferenceCreate(final PreferenceFragment prefActivity) {
		allPref = prefActivity.findPreference("purchaseAll");
		allPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				// generate payload to identify user....????
				String payload = "";
				try {
					mHelper.launchPurchaseFlow(MainScreen.thiz,
							"unlock_all_forever", ALL_REQUEST,
							mPreferencePurchaseFinishedListener, payload);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Main billing", "Purchase result " + e.getMessage());
					Toast.makeText(MainScreen.thiz,
							"Error during purchase " + e.getMessage(),
							Toast.LENGTH_LONG).show();
				}

				prefActivity.getActivity().finish();
				Preferences.closePrefs();
				return true;
			}
		});
		if (unlockAllPurchased) {
			allPref.setEnabled(false);
			allPref.setSummary(R.string.already_unlocked);

			hdrPurchased = true;
			panoramaPurchased = true;
			objectRemovalBurstPurchased = true;
			groupShotPurchased = true;
		}

		hdrPref = prefActivity.findPreference("hdrPurchase");
		hdrPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				// generate payload to identify user....????
				String payload = "";
				try {
					mHelper.launchPurchaseFlow(MainScreen.thiz,
							"plugin_almalence_hdr", HDR_REQUEST,
							mPreferencePurchaseFinishedListener, payload);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Main billing", "Purchase result " + e.getMessage());
					Toast.makeText(MainScreen.thiz,
							"Error during purchase " + e.getMessage(),
							Toast.LENGTH_LONG).show();
				}

				prefActivity.getActivity().finish();
				Preferences.closePrefs();
				return true;
			}
		});

		if (hdrPurchased) {
			hdrPref.setEnabled(false);
			hdrPref.setSummary(R.string.already_unlocked);
		}

		panoramaPref = prefActivity.findPreference("panoramaPurchase");
		panoramaPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						// generate payload to identify user....????
						String payload = "";
						try {
							mHelper.launchPurchaseFlow(MainScreen.thiz,
									"plugin_almalence_panorama",
									PANORAMA_REQUEST,
									mPreferencePurchaseFinishedListener,
									payload);
						} catch (Exception e) {
							e.printStackTrace();
							Log.e("Main billing",
									"Purchase result " + e.getMessage());
							Toast.makeText(MainScreen.thiz,
									"Error during purchase " + e.getMessage(),
									Toast.LENGTH_LONG).show();
						}

						prefActivity.getActivity().finish();
						Preferences.closePrefs();
						return true;
					}
				});
		if (panoramaPurchased) {
			panoramaPref.setEnabled(false);
			panoramaPref.setSummary(R.string.already_unlocked);
		}

		objectremovalPref = prefActivity.findPreference("movingPurchase");
		objectremovalPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						// generate payload to identify user....????
						String payload = "";
						try {
							mHelper.launchPurchaseFlow(MainScreen.thiz,
									"plugin_almalence_moving_burst",
									OBJECTREM_BURST_REQUEST,
									mPreferencePurchaseFinishedListener,
									payload);
						} catch (Exception e) {
							e.printStackTrace();
							Log.e("Main billing",
									"Purchase result " + e.getMessage());
							Toast.makeText(MainScreen.thiz,
									"Error during purchase " + e.getMessage(),
									Toast.LENGTH_LONG).show();
						}

						prefActivity.getActivity().finish();
						Preferences.closePrefs();
						return true;
					}
				});
		if (objectRemovalBurstPurchased) {
			objectremovalPref.setEnabled(false);
			objectremovalPref.setSummary(R.string.already_unlocked);
		}

		groupshotPref = prefActivity.findPreference("groupPurchase");
		groupshotPref
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference preference) {
						String payload = "";
						try {
							mHelper.launchPurchaseFlow(MainScreen.thiz,
									"plugin_almalence_groupshot",
									GROUPSHOT_REQUEST,
									mPreferencePurchaseFinishedListener,
									payload);
						} catch (Exception e) {
							e.printStackTrace();
							Log.e("Main billing",
									"Purchase result " + e.getMessage());
							Toast.makeText(MainScreen.thiz,
									"Error during purchase " + e.getMessage(),
									Toast.LENGTH_LONG).show();
						}

						prefActivity.getActivity().finish();
						Preferences.closePrefs();
						return true;
					}
				});
		if (groupShotPurchased) {
			groupshotPref.setEnabled(false);
			groupshotPref.setSummary(R.string.already_unlocked);
		}
	}

	public boolean showUnlock = false;
	// Callback for when purchase from preferences is finished
	IabHelper.OnIabPurchaseFinishedListener mPreferencePurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.v("Main billing", "Purchase finished: " + result
					+ ", purchase: " + purchase);
			if (result.isFailure()) {
				Log.v("Main billing", "Error purchasing: " + result);
				new CountDownTimer(100, 100) {
					public void onTick(long millisUntilFinished) {
					}

					public void onFinish() {
						showUnlock = true;
						Intent intent = new Intent(MainScreen.thiz,
								Preferences.class);
						startActivity(intent);
					}
				}.start();
				return;
			}

			Log.v("Main billing", "Purchase successful.");

			if (purchase.getSku().equals("plugin_almalence_hdr")) {
				Log.v("Main billing", "Purchase HDR.");

				hdrPurchased = true;
				hdrPref.setEnabled(false);
				hdrPref.setSummary(R.string.already_unlocked);
			}
			if (purchase.getSku().equals("plugin_almalence_panorama")) {
				Log.v("Main billing", "Purchase Panorama.");

				panoramaPurchased = true;
				panoramaPref.setEnabled(false);
				panoramaPref.setSummary(R.string.already_unlocked);
			}
			if (purchase.getSku().equals("unlock_all_forever")) {
				Log.v("Main billing", "Purchase all.");

				unlockAllPurchased = true;
				allPref.setEnabled(false);
				allPref.setSummary(R.string.already_unlocked);

				groupshotPref.setEnabled(false);
				groupshotPref.setSummary(R.string.already_unlocked);

				objectremovalPref.setEnabled(false);
				objectremovalPref.setSummary(R.string.already_unlocked);

				panoramaPref.setEnabled(false);
				panoramaPref.setSummary(R.string.already_unlocked);

				hdrPref.setEnabled(false);
				hdrPref.setSummary(R.string.already_unlocked);
			}
			if (purchase.getSku().equals("plugin_almalence_moving_burst")) {
				Log.v("Main billing", "Purchase object removal.");

				objectRemovalBurstPurchased = true;
				objectremovalPref.setEnabled(false);
				objectremovalPref.setSummary(R.string.already_unlocked);
			}
			if (purchase.getSku().equals("plugin_almalence_groupshot")) {
				Log.v("Main billing", "Purchase groupshot.");

				groupShotPurchased = true;
				groupshotPref.setEnabled(false);
				groupshotPref.setSummary(R.string.already_unlocked);
			}
		}
	};

	public void launchPurchase(String SKU, int requestID) {
		String payload = "";
		try {
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU, requestID,
					mPurchaseFinishedListener, payload);
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(this, "Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.v("Main billing", "Purchase finished: " + result
					+ ", purchase: " + purchase);
			if (result.isFailure()) {
				Log.v("Main billing", "Error purchasing: " + result);
				return;
			}

			Log.v("Main billing", "Purchase successful.");

			if (purchase.getSku().equals("plugin_almalence_hdr")) {
				Log.v("Main billing", "Purchase HDR.");
				hdrPurchased = true;
			}
			if (purchase.getSku().equals("plugin_almalence_panorama")) {
				Log.v("Main billing", "Purchase Panorama.");
				panoramaPurchased = true;
			}
			if (purchase.getSku().equals("unlock_all_forever")) {
				Log.v("Main billing", "Purchase unlock_all_forever.");
				unlockAllPurchased = true;
			}
			if (purchase.getSku().equals("plugin_almalence_moving_burst")) {
				Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
				objectRemovalBurstPurchased = true;
			}
			if (purchase.getSku().equals("plugin_almalence_groupshot")) {
				Log.v("Main billing", "Purchase plugin_almalence_groupshot.");
				groupShotPurchased = true;
			}

			showUnlock = true;
			Intent intent = new Intent(MainScreen.thiz, Preferences.class);
			startActivity(intent);
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("Main billing", "onActivityResult(" + requestCode + ","
				+ resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		} else {
			Log.v("Main billing", "onActivityResult handled by IABUtil.");
		}
	}

	// next methods used to store number of free launches.
	// using files to store this info

	// returns number of launches left
	public int getLeftLaunches(String modeID) {
		String dirPath = getFilesDir().getAbsolutePath() + File.separator
				+ modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists()) {
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}
		int left = ReadLaunches(projDir);
		return left;
	}

	// decrements number of launches left
	public void decrementLeftLaunches(String modeID) {
		String dirPath = getFilesDir().getAbsolutePath() + File.separator
				+ modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists()) {
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}

		int left = ReadLaunches(projDir);
		if (left > 0)
			WriteLaunches(projDir, left - 1);
	}

	// writes number of launches left into memory
	private void WriteLaunches(File projDir, int left) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(projDir + "/left");
			fos.write(left);
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// reads number of launches left from memory
	private int ReadLaunches(File projDir) {
		int left = 0;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(projDir + "/left");
			left = fis.read();
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return left;
	}

	public boolean checkLaunches(Mode mode) {
		// if mode free
		if (mode.SKU == null)
			return true;
		if (mode.SKU.isEmpty())
			return true;

		// if all unlocked
		if (unlockAllPurchased == true)
			return true;

		// if current mode unlocked
		if (mode.SKU.equals("plugin_almalence_hdr")) {
			if (hdrPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_panorama_augmented")) {
			if (panoramaPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_moving_burst")) {
			if (objectRemovalBurstPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_groupshot")) {
			if (groupShotPurchased == true)
				return true;
		}

		// if (!mode.purchased)
		{
			int launchesLeft = MainScreen.thiz.getLeftLaunches(mode.modeID);
			if (0 == launchesLeft)// no more launches left
			{
				// show appstore for this mode
				launchPurchase(mode.SKU, 100);
				return false;
			} else if ((10 == launchesLeft) || (20 == launchesLeft)
					|| (5 >= launchesLeft)) {
				// show appstore button and say that it cost money
				int id = MainScreen.thiz.getResources().getIdentifier(
						mode.modeName, "string",
						MainScreen.thiz.getPackageName());
				String modename = MainScreen.thiz.getResources().getString(id);

				Toast toast = Toast.makeText(this, modename + " "
						+ getResources().getString(R.string.Pref_Billing_Left)
						+ " " + launchesLeft, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}
		}
		return true;
	}

	private boolean isInstalled(String packageName) {
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}
	/************************ Billing ************************/
	/*******************************************************/
	
	
	//Application rater code
	public static void CallStoreFree(Activity act)
    {
    	try
    	{
        	Intent intent = new Intent(Intent.ACTION_VIEW);
       		intent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
	        act.startActivity(intent);
    	}
    	catch(ActivityNotFoundException e)
    	{
//    		// instruct user how to download manually
//    		AlertDialog ad = new AlertDialog.Builder(act)
//    			.setTitle(R.string.no_market_title)
//    			.setMessage(R.string.no_market_msg)
//    			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
//    			{
//    				public void onClick(DialogInterface dialog, int whichButton) {}
//    			})
//    			.create();
//    		
//    		ad.show();
    		
    		return;
    	}
    }
	
}