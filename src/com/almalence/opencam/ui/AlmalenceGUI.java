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

package com.almalence.opencam.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.googsharing.Thumbnail;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.Mode;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginType;
import com.almalence.opencam.Preferences;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.Panel.OnPanelListener;
import com.almalence.opencam.util.Util;

/***
 * AlmalenceGUI is an instance of GUI class, implements current GUI
 ***/

public class AlmalenceGUI extends GUI implements
		SeekBar.OnSeekBarChangeListener, View.OnLongClickListener,
		View.OnClickListener {
	private SharedPreferences preferences;

	private final int INFO_ALL = 0;
	private final int INFO_NO = 1;
	private final int INFO_PARAMS = 2;
	private int infoSet = INFO_PARAMS;

	public enum ShutterButton {
		DEFAULT, RECORDER_START, RECORDER_STOP
	};

	public enum SettingsType {
		SCENE, WB, FOCUS, FLASH, ISO, CAMERA, EV, MORE
	};

	private OrientationEventListener orientListener;	

	// certain quick control visible
	private boolean quickControlsVisible = false;

	// Quick control customization variables
	private ElementAdapter quickControlAdapter;
	private List<View> quickControlChangeres;
	private View currentQuickView = null; // Current quick control to replace
	private boolean quickControlsChangeVisible = false; // If qc customization
														// layout is showing now

	// Settings layout
	private ElementAdapter settingsAdapter;
	private List<View> settingsViews;
	private boolean settingsControlsVisible = false; // If quick settings layout
														// is showing now

	// Mode selector layout
	private ElementAdapter modeAdapter;
	private List<View> modeViews;
	private boolean modeSelectorVisible = false; // If quick settings layout is
													// showing now

	// Assoc list for storing association between mode button and mode ID
	private Hashtable<View, String> buttonModeViewAssoc;

	// private SharePopup mSharePopup;
	private Thumbnail mThumbnail;
	private RotateImageView thumbnailView;

	private final static Integer icon_ev = R.drawable.gui_almalence_settings_exposure;
	private final static Integer icon_cam = R.drawable.gui_almalence_settings_changecamera;
	private final static Integer icon_settings = R.drawable.gui_almalence_settings_more_settings;

	// Android camera parameters constants
	private final static String sceneAuto = MainScreen.thiz.getResources()
			.getString(R.string.sceneAutoSystem);
	private final static String sceneAction = MainScreen.thiz.getResources()
			.getString(R.string.sceneActionSystem);
	private final static String scenePortrait = MainScreen.thiz.getResources()
			.getString(R.string.scenePortraitSystem);
	private final static String sceneLandscape = MainScreen.thiz.getResources()
			.getString(R.string.sceneLandscapeSystem);
	private final static String sceneNight = MainScreen.thiz.getResources()
			.getString(R.string.sceneNightSystem);
	private final static String sceneNightPortrait = MainScreen.thiz
			.getResources().getString(R.string.sceneNightPortraitSystem);
	private final static String sceneTheatre = MainScreen.thiz.getResources()
			.getString(R.string.sceneTheatreSystem);
	private final static String sceneBeach = MainScreen.thiz.getResources()
			.getString(R.string.sceneBeachSystem);
	private final static String sceneSnow = MainScreen.thiz.getResources()
			.getString(R.string.sceneSnowSystem);
	private final static String sceneSunset = MainScreen.thiz.getResources()
			.getString(R.string.sceneSunsetSystem);
	private final static String sceneSteadyPhoto = MainScreen.thiz
			.getResources().getString(R.string.sceneSteadyPhotoSystem);
	private final static String sceneFireworks = MainScreen.thiz.getResources()
			.getString(R.string.sceneFireworksSystem);
	private final static String sceneSports = MainScreen.thiz.getResources()
			.getString(R.string.sceneSportsSystem);
	private final static String sceneParty = MainScreen.thiz.getResources()
			.getString(R.string.scenePartySystem);
	private final static String sceneCandlelight = MainScreen.thiz
			.getResources().getString(R.string.sceneCandlelightSystem);
	private final static String sceneBarcode = MainScreen.thiz.getResources()
			.getString(R.string.sceneBarcodeSystem);
	private final static String sceneHDR = MainScreen.thiz.getResources()
			.getString(R.string.sceneHDRSystem);
	private final static String sceneAR = MainScreen.thiz.getResources()
			.getString(R.string.sceneARSystem);

	private final static String wbAuto = MainScreen.thiz.getResources()
			.getString(R.string.wbAutoSystem);
	private final static String wbIncandescent = MainScreen.thiz.getResources()
			.getString(R.string.wbIncandescentSystem);
	private final static String wbFluorescent = MainScreen.thiz.getResources()
			.getString(R.string.wbFluorescentSystem);
	private final static String wbWarmFluorescent = MainScreen.thiz
			.getResources().getString(R.string.wbWarmFluorescentSystem);
	private final static String wbDaylight = MainScreen.thiz.getResources()
			.getString(R.string.wbDaylightSystem);
	private final static String wbCloudyDaylight = MainScreen.thiz
			.getResources().getString(R.string.wbCloudyDaylightSystem);
	private final static String wbTwilight = MainScreen.thiz.getResources()
			.getString(R.string.wbTwilightSystem);
	private final static String wbShade = MainScreen.thiz.getResources()
			.getString(R.string.wbShadeSystem);

	private final static String focusAuto = MainScreen.thiz.getResources()
			.getString(R.string.focusAutoSystem);
	private final static String focusInfinity = MainScreen.thiz.getResources()
			.getString(R.string.focusInfinitySystem);
	private final static String focusNormal = MainScreen.thiz.getResources()
			.getString(R.string.focusNormalSystem);
	private final static String focusMacro = MainScreen.thiz.getResources()
			.getString(R.string.focusMacroSystem);
	private final static String focusFixed = MainScreen.thiz.getResources()
			.getString(R.string.focusFixedSystem);
	private final static String focusEdof = MainScreen.thiz.getResources()
			.getString(R.string.focusEdofSystem);
	private final static String focusContinuousVideo = MainScreen.thiz
			.getResources().getString(R.string.focusContinuousVideoSystem);
	private final static String focusContinuousPicture = MainScreen.thiz
			.getResources().getString(R.string.focusContinuousPictureSystem);

	private final static String flashAuto = MainScreen.thiz.getResources()
			.getString(R.string.flashAutoSystem);
	private final static String flashOn = MainScreen.thiz.getResources()
			.getString(R.string.flashOnSystem);
	private final static String flashOff = MainScreen.thiz.getResources()
			.getString(R.string.flashOffSystem);
	private final static String flashRedEye = MainScreen.thiz.getResources()
			.getString(R.string.flashRedEyeSystem);
	private final static String flashTorch = MainScreen.thiz.getResources()
			.getString(R.string.flashTorchSystem);

	private final static String isoAuto = MainScreen.thiz.getResources()
			.getString(R.string.isoAutoSystem);
	private final static String iso100 = MainScreen.thiz.getResources()
			.getString(R.string.iso100System);
	private final static String iso200 = MainScreen.thiz.getResources()
			.getString(R.string.iso200System);
	private final static String iso400 = MainScreen.thiz.getResources()
			.getString(R.string.iso400System);
	private final static String iso800 = MainScreen.thiz.getResources()
			.getString(R.string.iso800System);
	private final static String iso1600 = MainScreen.thiz.getResources()
			.getString(R.string.iso1600System);

	// Lists of icons for camera parameters (scene mode, flash mode, focus mode,
	// white balance, iso)
	private final static Map<String, Integer> icons_scene = new Hashtable<String, Integer>() {
		{
			put(sceneAuto, R.drawable.gui_almalence_settings_scene_auto);
			put(sceneAction, R.drawable.gui_almalence_settings_scene_action);
			put(scenePortrait, R.drawable.gui_almalence_settings_scene_portrait);
			put(sceneLandscape,
					R.drawable.gui_almalence_settings_scene_landscape);
			put(sceneNight, R.drawable.gui_almalence_settings_scene_night);
			put(sceneNightPortrait,
					R.drawable.gui_almalence_settings_scene_nightportrait);
			put(sceneTheatre, R.drawable.gui_almalence_settings_scene_theater);
			put(sceneBeach, R.drawable.gui_almalence_settings_scene_beach);
			put(sceneSnow, R.drawable.gui_almalence_settings_scene_snow);
			put(sceneSunset, R.drawable.gui_almalence_settings_scene_sunset);
			put(sceneSteadyPhoto,
					R.drawable.gui_almalence_settings_scene_steadyphoto);
			put(sceneFireworks,
					R.drawable.gui_almalence_settings_scene_fireworks);
			put(sceneSports, R.drawable.gui_almalence_settings_scene_sports);
			put(sceneParty, R.drawable.gui_almalence_settings_scene_party);
			put(sceneCandlelight,
					R.drawable.gui_almalence_settings_scene_candlelight);
			put(sceneBarcode, R.drawable.gui_almalence_settings_scene_barcode);
			put(sceneHDR, R.drawable.gui_almalence_settings_scene_hdr);
			put(sceneAR, R.drawable.gui_almalence_settings_scene_ar);
		}
	};

	private final static Map<String, Integer> icons_wb = new Hashtable<String, Integer>() {
		{
			put(wbAuto, R.drawable.gui_almalence_settings_wb_auto);
			put(wbIncandescent,
					R.drawable.gui_almalence_settings_wb_incandescent);
			put(wbFluorescent, R.drawable.gui_almalence_settings_wb_fluorescent);
			put(wbWarmFluorescent,
					R.drawable.gui_almalence_settings_wb_warmfluorescent);
			put(wbDaylight, R.drawable.gui_almalence_settings_wb_daylight);
			put(wbCloudyDaylight,
					R.drawable.gui_almalence_settings_wb_cloudydaylight);
			put(wbTwilight, R.drawable.gui_almalence_settings_wb_twilight);
			put(wbShade, R.drawable.gui_almalence_settings_wb_shade);
		}
	};

	private final static Map<String, Integer> icons_focus = new Hashtable<String, Integer>() {
		{
			put(focusAuto, R.drawable.gui_almalence_settings_focus_auto);
			put(focusInfinity, R.drawable.gui_almalence_settings_focus_infinity);
			put(focusNormal, R.drawable.gui_almalence_settings_focus_normal);
			put(focusMacro, R.drawable.gui_almalence_settings_focus_macro);
			put(focusFixed, R.drawable.gui_almalence_settings_focus_fixed);
			put(focusEdof, R.drawable.gui_almalence_settings_focus_edof);
			put(focusContinuousVideo,
					R.drawable.gui_almalence_settings_focus_continiuousvideo);
			put(focusContinuousPicture,
					R.drawable.gui_almalence_settings_focus_continiuouspicture);
		}
	};

	private final static Map<String, Integer> icons_flash = new Hashtable<String, Integer>() {
		{
			put(flashOff, R.drawable.gui_almalence_settings_flash_off);
			put(flashAuto, R.drawable.gui_almalence_settings_flash_auto);
			put(flashOn, R.drawable.gui_almalence_settings_flash_on);
			put(flashRedEye, R.drawable.gui_almalence_settings_flash_redeye);
			put(flashTorch, R.drawable.gui_almalence_settings_flash_torch);
		}
	};

	private final static Map<String, Integer> icons_iso = new Hashtable<String, Integer>() {
		{
			put(isoAuto, R.drawable.gui_almalence_settings_iso_auto);
			put(iso100, R.drawable.gui_almalence_settings_iso_100);
			put(iso200, R.drawable.gui_almalence_settings_iso_200);
			put(iso400, R.drawable.gui_almalence_settings_iso_400);
			put(iso800, R.drawable.gui_almalence_settings_iso_800);
			put(iso1600, R.drawable.gui_almalence_settings_iso_1600);
		}
	};

	// List of localized names for camera parameters values
	private final static Map<String, String> names_scene = new Hashtable<String, String>() {
		{
			put(sceneAuto,
					MainScreen.thiz.getResources()
							.getString(R.string.sceneAuto));
			put(sceneAction,
					MainScreen.thiz.getResources().getString(
							R.string.sceneAction));
			put(scenePortrait,
					MainScreen.thiz.getResources().getString(
							R.string.scenePortrait));
			put(sceneLandscape,
					MainScreen.thiz.getResources().getString(
							R.string.sceneLandscape));
			put(sceneNight,
					MainScreen.thiz.getResources().getString(
							R.string.sceneNight));
			put(sceneNightPortrait,
					MainScreen.thiz.getResources().getString(
							R.string.sceneNightPortrait));
			put(sceneTheatre,
					MainScreen.thiz.getResources().getString(
							R.string.sceneTheatre));
			put(sceneBeach,
					MainScreen.thiz.getResources().getString(
							R.string.sceneBeach));
			put(sceneSnow,
					MainScreen.thiz.getResources()
							.getString(R.string.sceneSnow));
			put(sceneSunset,
					MainScreen.thiz.getResources().getString(
							R.string.sceneSunset));
			put(sceneSteadyPhoto,
					MainScreen.thiz.getResources().getString(
							R.string.sceneSteadyPhoto));
			put(sceneFireworks,
					MainScreen.thiz.getResources().getString(
							R.string.sceneFireworks));
			put(sceneSports,
					MainScreen.thiz.getResources().getString(
							R.string.sceneSports));
			put(sceneParty,
					MainScreen.thiz.getResources().getString(
							R.string.sceneParty));
			put(sceneCandlelight,
					MainScreen.thiz.getResources().getString(
							R.string.sceneCandlelight));
			put(sceneBarcode,
					MainScreen.thiz.getResources().getString(
							R.string.sceneBarcode));
			put(sceneHDR,
					MainScreen.thiz.getResources().getString(R.string.sceneHDR));
			put(sceneAR,
					MainScreen.thiz.getResources().getString(R.string.sceneAR));
		}
	};

	private final static Map<String, String> names_wb = new Hashtable<String, String>() {
		{
			put(wbAuto,
					MainScreen.thiz.getResources().getString(R.string.wbAuto));
			put(wbIncandescent,
					MainScreen.thiz.getResources().getString(
							R.string.wbIncandescent));
			put(wbFluorescent,
					MainScreen.thiz.getResources().getString(
							R.string.wbFluorescent));
			put(wbWarmFluorescent,
					MainScreen.thiz.getResources().getString(
							R.string.wbWarmFluorescent));
			put(wbDaylight,
					MainScreen.thiz.getResources().getString(
							R.string.wbDaylight));
			put(wbCloudyDaylight,
					MainScreen.thiz.getResources().getString(
							R.string.wbCloudyDaylight));
			put(wbTwilight,
					MainScreen.thiz.getResources().getString(
							R.string.wbTwilight));
			put(wbShade,
					MainScreen.thiz.getResources().getString(R.string.wbShade));
		}
	};

	private final static Map<String, String> names_focus = new Hashtable<String, String>() {
		{
			put(focusAuto,
					MainScreen.thiz.getResources()
							.getString(R.string.focusAuto));
			put(focusInfinity,
					MainScreen.thiz.getResources().getString(
							R.string.focusInfinity));
			put(focusNormal,
					MainScreen.thiz.getResources().getString(
							R.string.focusNormal));
			put(focusMacro,
					MainScreen.thiz.getResources().getString(
							R.string.focusMacro));
			put(focusFixed,
					MainScreen.thiz.getResources().getString(
							R.string.focusFixed));
			put(focusEdof,
					MainScreen.thiz.getResources()
							.getString(R.string.focusEdof));
			put(focusContinuousVideo,
					MainScreen.thiz.getResources().getString(
							R.string.focusContinuousVideo));
			put(focusContinuousPicture, MainScreen.thiz.getResources()
					.getString(R.string.focusContinuousPicture));
		}
	};

	private final static Map<String, String> names_flash = new Hashtable<String, String>() {
		{
			put(flashOff,
					MainScreen.thiz.getResources().getString(R.string.flashOff));
			put(flashAuto,
					MainScreen.thiz.getResources()
							.getString(R.string.flashAuto));
			put(flashOn,
					MainScreen.thiz.getResources().getString(R.string.flashOn));
			put(flashRedEye,
					MainScreen.thiz.getResources().getString(
							R.string.flashRedEye));
			put(flashTorch,
					MainScreen.thiz.getResources().getString(
							R.string.flashTorch));
		}
	};

	private final static Map<String, String> names_iso = new Hashtable<String, String>() {
		{
			put(isoAuto,
					MainScreen.thiz.getResources().getString(R.string.isoAuto));
			put(iso100,
					MainScreen.thiz.getResources().getString(R.string.iso100));
			put(iso200,
					MainScreen.thiz.getResources().getString(R.string.iso200));
			put(iso400,
					MainScreen.thiz.getResources().getString(R.string.iso400));
			put(iso800,
					MainScreen.thiz.getResources().getString(R.string.iso800));
			put(iso1600,
					MainScreen.thiz.getResources().getString(R.string.iso1600));
		}
	};

	// Defining for top menu buttons (camera parameters settings)
	private final int MODE_EV = R.id.evButton;
	private final int MODE_SCENE = R.id.sceneButton;
	private final int MODE_WB = R.id.wbButton;
	private final int MODE_FOCUS = R.id.focusButton;
	private final int MODE_FLASH = R.id.flashButton;
	private final int MODE_ISO = R.id.isoButton;
	private final int MODE_CAM = R.id.camerachangeButton;

	private Map<Integer, View> topMenuButtons;
	private Map<String, View> topMenuPluginButtons; // Each plugin may have one
													// top menu (and appropriate
													// quick control) button

	// Current quick controls
	private View quickControl1 = null;
	private View quickControl2 = null;
	private View quickControl3 = null;
	private View quickControl4 = null;

	private ElementAdapter scenemodeAdapter;
	private ElementAdapter wbmodeAdapter;
	private ElementAdapter focusmodeAdapter;
	private ElementAdapter flashmodeAdapter;
	private ElementAdapter isoAdapter;

	private Map<String, View> SceneModeButtons;
	private Map<String, View> WBModeButtons;
	private Map<String, View> FocusModeButtons;
	private Map<String, View> FlashModeButtons;
	private Map<String, View> ISOButtons;

	// Camera settings values which is exist at current device
	private List<View> activeScene;
	private List<View> activeWB;
	private List<View> activeFocus;
	private List<View> activeFlash;
	private List<View> activeISO;

	private List<String> activeSceneNames;
	private List<String> activeWBNames;
	private List<String> activeFocusNames;
	private List<String> activeFlashNames;
	private List<String> activeISONames;

	private boolean isEVEnabled = true;
	private boolean isSceneEnabled = true;
	private boolean isWBEnabled = true;
	private boolean isFocusEnabled = true;
	private boolean isFlashEnabled = true;
	private boolean isIsoEnabled = true;
	private boolean isCameraChangeEnabled = true;

	// GUI Layout
	public View guiView;

	// Current camera parameters
	private int mEV = 0;
	private String mSceneMode = null;
	private String mFlashMode = null;
	private String mFocusMode = null;
	private String mWB = null;
	private String mISO = null;

	// Flags to know which camera feature supported at current device
	public boolean mEVSupported = false;
	public boolean mSceneModeSupported = false;
	public boolean mWBSupported = false;
	public boolean mFocusModeSupported = false;
	public boolean mFlashModeSupported = false;
	public boolean mISOSupported = false;
	public boolean mCameraChangeSupported = false;

	// Prefer sizes for plugin's controls in pixels for screens with density =
	// 1;
	private static float fScreenDensity;

	private static int iInfoViewMaxHeight;
	private static int iInfoViewMaxWidth;
	private static int iInfoViewHeight;

	private static int iCenterViewMaxHeight;
	private static int iCenterViewMaxWidth;

	// indicates if it's first launch - to show hint layer.
	private boolean isFirstLaunch = true;

	public AlmalenceGUI() {
		topMenuButtons = new Hashtable<Integer, View>();
		topMenuPluginButtons = new Hashtable<String, View>();

		scenemodeAdapter = new ElementAdapter();
		wbmodeAdapter = new ElementAdapter();
		focusmodeAdapter = new ElementAdapter();
		flashmodeAdapter = new ElementAdapter();
		isoAdapter = new ElementAdapter();

		SceneModeButtons = new Hashtable<String, View>();
		WBModeButtons = new Hashtable<String, View>();
		FocusModeButtons = new Hashtable<String, View>();
		FlashModeButtons = new Hashtable<String, View>();
		ISOButtons = new Hashtable<String, View>();

		activeScene = new ArrayList<View>();
		activeWB = new ArrayList<View>();
		activeFocus = new ArrayList<View>();
		activeFlash = new ArrayList<View>();
		activeISO = new ArrayList<View>();

		activeSceneNames = new ArrayList<String>();
		activeWBNames = new ArrayList<String>();
		activeFocusNames = new ArrayList<String>();
		activeFlashNames = new ArrayList<String>();
		activeISONames = new ArrayList<String>();

		settingsAdapter = new ElementAdapter();
		settingsViews = new ArrayList<View>();

		quickControlAdapter = new ElementAdapter();
		quickControlChangeres = new ArrayList<View>();

		modeAdapter = new ElementAdapter();
		modeViews = new ArrayList<View>();
		buttonModeViewAssoc = new Hashtable<View, String>();
	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(String sceneMode) {
		return icons_scene.get(sceneMode);
	}

	public int getWBIcon(String wb) {
		return icons_wb.get(wb);
	}

	public int getFocusIcon(String focusMode) {
		return icons_focus.get(focusMode);
	}

	public int getFlashIcon(String flashMode) {
		return icons_flash.get(flashMode);
	}

	public int getISOIcon(String isoMode) {
		return icons_iso.get(isoMode);
	}

	@Override
	public float getScreenDensity() {
		return fScreenDensity;
	}

	@Override
	public void onStart() {

		// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		fScreenDensity = metrics.density;

		iInfoViewMaxHeight = (int) (MainScreen.mainContext.getResources()
				.getInteger(R.integer.infoControlHeight) * fScreenDensity);
		iInfoViewMaxWidth = (int) (MainScreen.mainContext.getResources()
				.getInteger(R.integer.infoControlWidth) * fScreenDensity);

		iCenterViewMaxHeight = (int) (MainScreen.mainContext.getResources()
				.getInteger(R.integer.centerViewHeight) * fScreenDensity);
		iCenterViewMaxWidth = (int) (MainScreen.mainContext.getResources()
				.getInteger(R.integer.centerViewWidth) * fScreenDensity);

		// set orientation listener to rotate controls
		this.orientListener = new OrientationEventListener(
				MainScreen.mainContext) {
			@Override
			public void onOrientationChanged(int orientation) {
				if (orientation == ORIENTATION_UNKNOWN)
					return;

				final Display display = ((WindowManager) MainScreen.thiz
						.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();
				final int orientationProc = (display.getWidth() <= display
						.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
						: Configuration.ORIENTATION_LANDSCAPE;
				final int rotation = display.getRotation();

				boolean remapOrientation = (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
						|| (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
						|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
						|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);

				if (remapOrientation)
					orientation = (orientation - 90 + 360) % 360;

				AlmalenceGUI.mDeviceOrientation = Util.roundOrientation(
						orientation, AlmalenceGUI.mDeviceOrientation);

				((RotateImageView) topMenuButtons.get(MODE_EV))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_SCENE))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_WB))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_FOCUS))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_FLASH))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_ISO))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) topMenuButtons.get(MODE_CAM))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);

				Set<String> keys = topMenuPluginButtons.keySet();
				Iterator<String> it = keys.iterator();
				while (it.hasNext()) {
					String key = it.next();
					((RotateImageView) topMenuPluginButtons.get(key))
							.setOrientation(AlmalenceGUI.mDeviceOrientation);
				}

				((RotateImageView) guiView.findViewById(R.id.buttonGallery))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);
				((RotateImageView) guiView.findViewById(R.id.buttonSelectMode))
						.setOrientation(AlmalenceGUI.mDeviceOrientation);

				final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
						: AlmalenceGUI.mDeviceOrientation % 360 + 360;
				rotateSquareViews(degree, 250);

				((TextView) guiView.findViewById(R.id.blockingText))
						.setRotation(-AlmalenceGUI.mDeviceOrientation);

				AlmalenceGUI.mPreviousDeviceOrientation = AlmalenceGUI.mDeviceOrientation;
			}
		};

		// create merged image for select mode button
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		String defaultModeName = prefs.getString("defaultModeName", "");
		Mode mode = ConfigParser.getInstance().getMode(defaultModeName);
		Bitmap bm = null;
		Bitmap iconBase = BitmapFactory.decodeResource(
				MainScreen.mainContext.getResources(),
				R.drawable.gui_almalence_select_mode);
		Bitmap iconOverlay = BitmapFactory.decodeResource(
				MainScreen.mainContext.getResources(),
				MainScreen.thiz.getResources().getIdentifier(mode.icon,
						"drawable", MainScreen.thiz.getPackageName()));
		iconOverlay = Bitmap.createScaledBitmap(iconOverlay,
				(int) (iconBase.getWidth() / 1.8),
				(int) (iconBase.getWidth() / 1.8), false);

		bm = mergeImage(iconBase, iconOverlay);
		bm = Bitmap.createScaledBitmap(bm, (int) (MainScreen.mainContext
				.getResources().getDimension(R.dimen.paramsLayoutHeight)),
				(int) (MainScreen.mainContext.getResources()
						.getDimension(R.dimen.paramsLayoutHeight)), false);
		((RotateImageView) guiView.findViewById(R.id.buttonSelectMode))
				.setImageBitmap(bm);
	}

	@Override
	public void onStop() {
		removePluginViews();
	}

	@Override
	public void onPause() {
		if (quickControlsChangeVisible)
			closeQuickControlsSettings();
		orientListener.disable();
	}

	@Override
	public void onResume() {
		this.updateThumbnailButton();

		setShutterIcon(ShutterButton.DEFAULT);

		lockControls = false;

		orientListener.enable();

		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_EV, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_WB, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_ISO, false);
		disableCameraParameter(CameraParameter.CAMERA_PARAMETER_CAMERACHANGE, false);

		// if first launch - show layout with hints
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		if (true == prefs.contains("isFirstLaunch")) {
			isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);
		} else {
			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("isFirstLaunch", false);
			prefsEditor.commit();
			isFirstLaunch = true;
		}

		// show/hide hints
		if (!isFirstLaunch)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.GONE);
		else
			guiView.findViewById(R.id.hintLayout).setVisibility(View.VISIBLE);
	}

	@Override
	public void onDestroy() {

	}

	@Override
	public void createInitialGUI() {
		guiView = LayoutInflater.from(MainScreen.mainContext).inflate(
				R.layout.gui_almalence_layout, null);

		RotateImageView galleryButton = (RotateImageView) guiView
				.findViewById(R.id.buttonGallery);

		Bitmap bitmap = Bitmap.createBitmap(96, 96, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.BLACK);
		Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap,
				(int) (MainScreen.mainContext.getResources()
						.getDimension(R.dimen.mainButtonHeight)),
				(int) ((15.0f)));

		galleryButton.setImageBitmap(bm);
		// Add GUI Layout to main layout of OpenCamera
		((RelativeLayout) MainScreen.thiz.findViewById(R.id.mainLayout1))
				.addView(guiView);
	}

	// Create standard OpenCamera's buttons and theirs OnClickListener
	@Override
	public void onCreate() {
		// Get application preferences object
		preferences = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);

		guiView.findViewById(R.id.evButton).setOnTouchListener(MainScreen.thiz);
		guiView.findViewById(R.id.sceneButton).setOnTouchListener(
				MainScreen.thiz);
		guiView.findViewById(R.id.wbButton).setOnTouchListener(MainScreen.thiz);
		guiView.findViewById(R.id.focusButton).setOnTouchListener(
				MainScreen.thiz);
		guiView.findViewById(R.id.flashButton).setOnTouchListener(
				MainScreen.thiz);
		guiView.findViewById(R.id.isoButton)
				.setOnTouchListener(MainScreen.thiz);
		guiView.findViewById(R.id.camerachangeButton).setOnTouchListener(
				MainScreen.thiz);

		// Long clicks are needed to open quick controls customization layout
		guiView.findViewById(R.id.evButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.sceneButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.wbButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.focusButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.flashButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.isoButton).setOnLongClickListener(this);
		guiView.findViewById(R.id.camerachangeButton).setOnLongClickListener(
				this);

		// Get all top menu buttons
		topMenuButtons.put(MODE_EV, guiView.findViewById(R.id.evButton));
		topMenuButtons.put(MODE_SCENE, guiView.findViewById(R.id.sceneButton));
		topMenuButtons.put(MODE_WB, guiView.findViewById(R.id.wbButton));
		topMenuButtons.put(MODE_FOCUS, guiView.findViewById(R.id.focusButton));
		topMenuButtons.put(MODE_FLASH, guiView.findViewById(R.id.flashButton));
		topMenuButtons.put(MODE_ISO, guiView.findViewById(R.id.isoButton));
		topMenuButtons.put(MODE_CAM,
				guiView.findViewById(R.id.camerachangeButton));

		SceneModeButtons = initCameraParameterModeButtons(icons_scene,
				names_scene, SceneModeButtons, MODE_SCENE);
		WBModeButtons = initCameraParameterModeButtons(icons_wb, names_wb,
				WBModeButtons, MODE_WB);
		FocusModeButtons = initCameraParameterModeButtons(icons_focus,
				names_focus, FocusModeButtons, MODE_FOCUS);
		FlashModeButtons = initCameraParameterModeButtons(icons_flash,
				names_flash, FlashModeButtons, MODE_FLASH);
		ISOButtons = initCameraParameterModeButtons(icons_iso, names_iso,
				ISOButtons, MODE_ISO);

		// Create top menu buttons for plugins (each plugin may have only one
		// top menu button)
		createPluginTopMenuButtons();

		thumbnailView = (RotateImageView) guiView
				.findViewById(R.id.buttonGallery);

		((RelativeLayout) MainScreen.thiz.findViewById(R.id.mainLayout1))
				.setOnTouchListener(MainScreen.thiz);
		((LinearLayout) MainScreen.thiz.findViewById(R.id.evLayout))
				.setOnTouchListener(MainScreen.thiz);
	}

	private Map<String, View> initCameraParameterModeButtons(
			Map<String, Integer> icons_map, Map<String, String> names_map,
			Map<String, View> paramMap, final int mode) {
		paramMap.clear();
		Set<String> keys = icons_map.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			final String system_name = it.next();
			final String value_name = names_map.get(system_name);
			LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
			View paramMode = inflator.inflate(
					R.layout.gui_almalence_quick_control_grid_element, null,
					false);
			// set some mode icon
			((ImageView) paramMode.findViewById(R.id.imageView))
					.setImageResource(icons_map.get(system_name));
			((TextView) paramMode.findViewById(R.id.textView))
					.setText(value_name);

			paramMode.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					switch (mode) {
					case MODE_SCENE:
						setSceneMode(system_name);
						break;
					case MODE_WB:
						setWhiteBalance(system_name);
						break;
					case MODE_FOCUS:
						setFocusMode(system_name);
						break;
					case MODE_FLASH:
						setFlashMode(system_name);
						break;
					case MODE_ISO:
						setISO(system_name);
						break;
					}
					guiView.findViewById(R.id.topPanel).setVisibility(
							View.VISIBLE);
					quickControlsVisible = false;
				}
			});

			paramMap.put(system_name, paramMode);
		}

		return paramMap;
	}

	@Override
	public void onGUICreate() {
		if (MainScreen.thiz.findViewById(R.id.infoLayout).getVisibility() == View.VISIBLE)
			iInfoViewHeight = MainScreen.thiz.findViewById(R.id.infoLayout)
					.getHeight();
		// Recreate plugin views
		removePluginViews();
		createPluginViews();

		LinearLayout infoLayout = (LinearLayout) guiView
				.findViewById(R.id.infoLayout);
		RelativeLayout.LayoutParams infoParams = (RelativeLayout.LayoutParams) infoLayout
				.getLayoutParams();
		if (infoParams != null) {
			int width = infoParams.width;
			if (infoLayout.getChildCount() == 0)
				infoParams.rightMargin = -width;
			else
				infoParams.rightMargin = 0;
			infoLayout.setLayoutParams(infoParams);
			infoLayout.requestLayout();
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		infoSet = prefs.getInt("defaultInfoSet", 2);
		if (infoSet == 2 && !isAnyViewOnViewfinder()) {
			infoSet = 0;
			prefs.edit().putInt("defaultInfoSet", infoSet).commit();
		}
		setInfo(false, 0, 0, false);

		MainScreen.thiz.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlmalenceGUI.this.updateThumbnailButton();
			}
		});

		final View postProcessingLayout = guiView
				.findViewById(R.id.postprocessingLayout);
		final View topPanel = guiView.findViewById(R.id.topPanel);
		final View mainButtons = guiView.findViewById(R.id.mainButtons);
		final View qcLayout = guiView.findViewById(R.id.qcLayout);
		final View buttonsLayout = guiView.findViewById(R.id.buttonsLayout);
		final View hintLayout = guiView.findViewById(R.id.hintLayout);

		mainButtons.bringToFront();
		qcLayout.bringToFront();
		buttonsLayout.bringToFront();
		topPanel.bringToFront();
		postProcessingLayout.bringToFront();
		hintLayout.bringToFront();

		// boolean useGeoTaggingPrefExport =
		// prefs.getBoolean("useGeoTaggingPrefExport", false);
		// if (useGeoTaggingPrefExport)
		// {
		// Location l = MLocation.getLocation(MainScreen.mainContext);
		// if (l==null)
		// Toast.makeText(MainScreen.mainContext,
		// "Can't get location. Turn on \"use GPS satellites\" setting",
		// Toast.LENGTH_LONG).show();
		// }
	}

	@Override
	public void setupViewfinderPreviewSize() {
		Camera camera = MainScreen.thiz.getCamera();
		if (null == camera)
			return;
		Camera.Parameters cp = camera.getParameters();

		// ----- Adjust preview size to have proper aspect ratio
		Size previewSize = cp.getPreviewSize();

		float cameraAspect = (float) previewSize.width / previewSize.height;

		RelativeLayout ll = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.mainLayout1);

		int previewSurfaceWidth = ll.getWidth();
		int previewSurfaceHeight = ll.getHeight();
		float surfaceAspect = (float) previewSurfaceHeight
				/ previewSurfaceWidth;

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);

		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		int screen_height = metrics.heightPixels;

		lp.width = previewSurfaceWidth;
		lp.height = previewSurfaceHeight;
		if (Math.abs(surfaceAspect - cameraAspect) > 0.05d) {
			if (surfaceAspect > cameraAspect
					&& (Math.abs(1 - cameraAspect) > 0.05d)) {
				int paramsLayoutHeight = (int) MainScreen.thiz.getResources()
						.getDimension(R.dimen.paramsLayoutHeight);
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = (int) (screen_height - 2 * paramsLayoutHeight);
				lp.topMargin = (int) (paramsLayoutHeight);
			} else if (surfaceAspect > cameraAspect) {
				int paramsLayoutHeight = (int) MainScreen.thiz.getResources()
						.getDimension(R.dimen.paramsLayoutHeight);
				// if wide-screen - decrease width of surface
				lp.width = previewSurfaceWidth;

				lp.height = previewSurfaceWidth;
				lp.topMargin = (int) (paramsLayoutHeight);
			}
		}

		MainScreen.thiz.preview.setLayoutParams(lp);
		guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		guiView.findViewById(R.id.specialPluginsLayout).setLayoutParams(lp);

		// if (metrics.heightPixels != lp.height && (lp.height != lp.width))
		// {
		// // RelativeLayout.LayoutParams flp = (RelativeLayout.LayoutParams)
		// guiView
		// // .findViewById(R.id.fullscreenLayout).getLayoutParams();
		// guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		// guiView.findViewById(R.id.specialPluginsLayout)
		// .setLayoutParams(lp);
		// }
		// else if(lp.height == lp.width)
		// {
		// // int paramsLayoutHeight = (int) MainScreen.thiz.getResources()
		// // .getDimension(R.dimen.paramsLayoutHeight);
		// // RelativeLayout.LayoutParams flp = (RelativeLayout.LayoutParams)
		// guiView
		// // .findViewById(R.id.fullscreenLayout).getLayoutParams();
		// //flp.topMargin = paramsLayoutHeight;
		// guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		// guiView.findViewById(R.id.specialPluginsLayout)
		// .setLayoutParams(lp);
		// }
		// else
		// {
		// // RelativeLayout.LayoutParams flp = (RelativeLayout.LayoutParams)
		// guiView
		// // .findViewById(R.id.fullscreenLayout).getLayoutParams();
		// // flp.topMargin = 0;
		// guiView.findViewById(R.id.fullscreenLayout).setLayoutParams(lp);
		// guiView.findViewById(R.id.specialPluginsLayout)
		// .setLayoutParams(lp);
		// }
	}

	/*
	 * Each plugin may have only one top menu button Icon id and Title (plugin's
	 * members) is use to make design of button
	 */
	public void createPluginTopMenuButtons() {
		topMenuPluginButtons.clear();

		createPluginTopMenuButtons(PluginManager.getInstance()
				.getActivePlugins(PluginType.ViewFinder));
		createPluginTopMenuButtons(PluginManager.getInstance()
				.getActivePlugins(PluginType.Capture));
		createPluginTopMenuButtons(PluginManager.getInstance()
				.getActivePlugins(PluginType.Processing));
		createPluginTopMenuButtons(PluginManager.getInstance()
				.getActivePlugins(PluginType.Filter));
		createPluginTopMenuButtons(PluginManager.getInstance()
				.getActivePlugins(PluginType.Export));
	}

	public void createPluginTopMenuButtons(List<Plugin> Plugins) {
		if (Plugins.size() > 0) {
			for (int i = 0; i < Plugins.size(); i++) {
				Plugin Plugin = Plugins.get(i);

				if (Plugin == null || Plugin.getQuickControlIconID() <= 0)
					continue;

				LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
				ImageView qcView = (ImageView) inflator.inflate(
						R.layout.gui_almalence_quick_control_button,
						(ViewGroup) guiView.findViewById(R.id.paramsLayout),
						false);

				qcView.setOnTouchListener(MainScreen.thiz);
				qcView.setOnClickListener(this);
				qcView.setOnLongClickListener(this);

				topMenuPluginButtons.put(Plugin.getID(), qcView);
			}
		}
	}

	// onGUICreate called when main layout is rendered and size's variables is
	// available
	// @Override
	public void createPluginViews() {
		int iInfoControlsRemainingHeight = iInfoViewHeight;

		List<View> info_views = null;
		Map<View, Plugin.ViewfinderZone> plugin_views = null;

		List<Plugin> vfPlugins = PluginManager.getInstance().getActivePlugins(
				PluginType.ViewFinder);
		if (vfPlugins.size() > 0) {
			for (int i = 0; i < vfPlugins.size(); i++) {
				Plugin vfPlugin = vfPlugins.get(i);
				if (vfPlugin != null) {
					plugin_views = vfPlugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = vfPlugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++) {
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(
								infoView, viewLayoutParams, iInfoViewMaxWidth,
								iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height) {
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}

		// Always check whether view is suitable to remaining space
		List<Plugin> capturePlugins = PluginManager.getInstance()
				.getActivePlugins(PluginType.Capture);
		if (capturePlugins.size() > 0) {
			for (int i = 0; i < capturePlugins.size(); i++) {
				Plugin capturePlugin = capturePlugins.get(i);
				if (capturePlugin != null) {
					plugin_views = capturePlugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = capturePlugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++) {
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(
								infoView, viewLayoutParams, iInfoViewMaxWidth,
								iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height) {
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}

		List<Plugin> processingPlugins = PluginManager.getInstance()
				.getActivePlugins(PluginType.Processing);
		if (processingPlugins.size() > 0) {
			for (int i = 0; i < processingPlugins.size(); i++) {
				Plugin processingPlugin = processingPlugins.get(i);
				if (processingPlugin != null) {
					plugin_views = processingPlugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = processingPlugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++) {
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(
								infoView, viewLayoutParams, iInfoViewMaxWidth,
								iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height) {
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}

		List<Plugin> filterPlugins = PluginManager.getInstance()
				.getActivePlugins(PluginType.Filter);
		if (filterPlugins.size() > 0) {
			for (int i = 0; i < filterPlugins.size(); i++) {
				Plugin filterPlugin = filterPlugins.get(i);
				if (filterPlugin != null) {
					plugin_views = filterPlugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = filterPlugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++) {
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(
								infoView, viewLayoutParams, iInfoViewMaxWidth,
								iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height) {
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}

		List<Plugin> exportPlugins = PluginManager.getInstance()
				.getActivePlugins(PluginType.Export);
		if (exportPlugins.size() > 0) {
			for (int i = 0; i < exportPlugins.size(); i++) {
				Plugin exportPlugin = exportPlugins.get(i);
				if (exportPlugin != null) {
					plugin_views = exportPlugin.getPluginViews();
					addPluginViews(plugin_views);

					// Add info controls
					info_views = exportPlugin.getInfoViews();
					for (int j = 0; j < info_views.size(); j++) {
						View infoView = info_views.get(j);

						// Calculate appropriate size of added plugin's view
						android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) infoView
								.getLayoutParams();
						viewLayoutParams = this.getTunedLinearLayoutParams(
								infoView, viewLayoutParams, iInfoViewMaxWidth,
								iInfoViewMaxHeight);

						if (iInfoControlsRemainingHeight >= viewLayoutParams.height) {
							iInfoControlsRemainingHeight -= viewLayoutParams.height;
							this.addInfoView(infoView, viewLayoutParams);
						}
					}
				}
			}
		}
	}

	//
	private void initDefaultQuickControls() {
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();

		quickControl1 = inflator.inflate(
				R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);
		quickControl2 = inflator.inflate(
				R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);
		quickControl3 = inflator.inflate(
				R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);
		quickControl4 = inflator.inflate(
				R.layout.gui_almalence_invisible_button,
				(ViewGroup) guiView.findViewById(R.id.paramsLayout), false);

		quickControl1.setOnLongClickListener(this);
		quickControl2.setOnLongClickListener(this);
		quickControl3.setOnLongClickListener(this);
		quickControl4.setOnLongClickListener(this);

		quickControl1.setOnClickListener(this);
		quickControl2.setOnClickListener(this);
		quickControl3.setOnClickListener(this);
		quickControl4.setOnClickListener(this);
	}

	// Called when camera object created in MainScreen.
	// After camera creation it is possibly to obtain
	// all camera possibilities such as supported scene mode, flash mode and
	// etc.
	@Override
	public void onCameraCreate() {

		String defaultQuickControl1 = "";
		String defaultQuickControl2 = "";
		String defaultQuickControl3 = "";
		String defaultQuickControl4 = "";

		// Remove buttons from previous camera start
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);

		mEVSupported = false;
		mSceneModeSupported = false;
		mWBSupported = false;
		mFocusModeSupported = false;
		mFlashModeSupported = false;
		mISOSupported = false;
		mCameraChangeSupported = false;

		activeScene.clear();
		activeWB.clear();
		activeFocus.clear();
		activeFlash.clear();
		activeISO.clear();

		activeSceneNames.clear();
		activeWBNames.clear();
		activeFocusNames.clear();
		activeFlashNames.clear();
		activeISONames.clear();

		removeAllQuickViews();
		initDefaultQuickControls();

		createPluginTopMenuButtons();

		// Create Exposure compensation button and slider with supported values
		if (MainScreen.thiz.isExposureCompensationSupported()) {
			mEVSupported = true;
			defaultQuickControl1 = String.valueOf(MODE_EV);

			float ev_step = MainScreen.thiz.getExposureCompensationStep();

			int minValue = MainScreen.thiz.getMinExposureCompensation();
			int maxValue = MainScreen.thiz.getMaxExposureCompensation();

			SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
			if (evBar != null) {
				int initValue = preferences.getInt(sEvPref, 0);
				evBar.setMax(maxValue - minValue);
				evBar.setProgress(initValue + maxValue);

				TextView leftText = (TextView) guiView
						.findViewById(R.id.seekBarLeftText);
				TextView rightText = (TextView) guiView
						.findViewById(R.id.seekBarRightText);

				int minValueReal = Math.round(minValue * ev_step);
				int maxValueReal = Math.round(maxValue * ev_step);
				String minString = String.valueOf(minValueReal);
				String maxString = String.valueOf(maxValueReal);

				if (minValueReal > 0)
					minString = "+" + minString;
				if (maxValueReal > 0)
					maxString = "+" + maxString;

				leftText.setText(minString);
				rightText.setText(maxString);

				mEV = initValue;
				Camera.Parameters params = MainScreen.thiz
						.getCameraParameters();
				params.setExposureCompensation(mEV);
				Camera camera = MainScreen.thiz.getCamera();
				if (null != camera)
				{
					try {
						camera.setParameters(params);
					} catch (Exception e) {
						e.printStackTrace();
						Log.e("onCameraCreate", "Ev exception: " + e.getMessage());
					}
				}

				evBar.setOnSeekBarChangeListener(this);
			}

			RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_EV);
			but.setImageResource(icon_ev);
		} else
			mEVSupported = false;

		// Create Scene mode button and adding supported scene modes
		List<String> supported_scene = MainScreen.thiz.getSupportedSceneModes();
		if (supported_scene != null && supported_scene.size() > 0 && activeScene != null) {
			mSceneModeSupported = true;
			// defaultQuickControl2 = String.valueOf(MODE_SCENE);

			String initValue = preferences.getString(sSceneModePref,
					sDefaultValue);
			if (!supported_scene.contains(initValue)) {
				if (MainScreen.thiz.isFrontCamera())
					initValue = supported_scene.get(0);
				else
					initValue = "auto";
			}

			Set<String> keys = SceneModeButtons.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String scene_name = it.next();
				if (supported_scene.contains(scene_name)
						&& scene_name != sceneHDR && scene_name != sceneNight) {
					activeScene.add(SceneModeButtons.get(scene_name));
					activeSceneNames.add(scene_name);
				}
			}

			scenemodeAdapter.Elements = activeScene;
			GridView gridview = (GridView) guiView
					.findViewById(R.id.scenemodeGrid);
			gridview.setAdapter(scenemodeAdapter);

			setButtonSelected(SceneModeButtons, initValue);
			setCameraParameterValue(MODE_SCENE, initValue);

			if (icons_scene!=null)
			{
				RotateImageView but = (RotateImageView) topMenuButtons
					.get(MODE_SCENE);
				int icon_id = icons_scene.get(initValue);
				but.setImageResource(icon_id);
			}

			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			params.setSceneMode(mSceneMode);
			Camera camera = MainScreen.thiz.getCamera();
			if (null != camera)
			{
				try {
					camera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("onCameraCreate", "Scene mode exception: " + e.getMessage());
				}
			}
		} else {
			mSceneModeSupported = false;
			mSceneMode = null;
		}

		// Create White Balance mode button and adding supported white balances
		List<String> supported_wb = MainScreen.thiz.getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.size() > 0 && activeWB != null) {
			mWBSupported = true;

			Set<String> keys = WBModeButtons.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String wb_name = it.next();
				if (supported_wb.contains(wb_name)) {
					activeWB.add(WBModeButtons.get(wb_name));
					activeWBNames.add(wb_name);
				}
			}

			wbmodeAdapter.Elements = activeWB;
			GridView gridview = (GridView) guiView.findViewById(R.id.wbGrid);
			gridview.setNumColumns(activeWB.size() > 9 ? 4 : 3);
			gridview.setAdapter(wbmodeAdapter);

			String initValue = preferences
					.getString(sWBModePref, sDefaultValue);
			if (!supported_wb.contains(initValue)) {
				if (MainScreen.thiz.isFrontCamera())
					initValue = supported_wb.get(0);
				else
					initValue = sDefaultValue;
			}
			setButtonSelected(WBModeButtons, initValue);
			setCameraParameterValue(MODE_WB, initValue);

			if (icons_wb!=null)
			{
				RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
				int icon_id = icons_wb.get(initValue);
				but.setImageResource(icon_id);
			}

			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			params.setWhiteBalance(mWB);
			Camera camera = MainScreen.thiz.getCamera();
			if (null != camera)
			{
				try {
					camera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("onCameraCreate", "WB exception: " + e.getMessage());
				}
			}
		} else
			mWBSupported = false;

		// Create Focus mode button and adding supported focus modes
		List<String> supported_focus = MainScreen.thiz.getSupportedFocusModes();
		if (supported_focus != null && supported_focus.size() > 0 && activeFocus != null) {
			mFocusModeSupported = true;
			defaultQuickControl3 = String.valueOf(MODE_FOCUS);

			Set<String> keys = FocusModeButtons.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String focus_name = it.next();
				if (supported_focus.contains(focus_name)) {
					activeFocus.add(FocusModeButtons.get(focus_name));
					activeFocusNames.add(focus_name);
				}
			}

			focusmodeAdapter.Elements = activeFocus;
			GridView gridview = (GridView) guiView
					.findViewById(R.id.focusmodeGrid);
			gridview.setNumColumns(activeFocus.size() > 9 ? 4 : 3);
			gridview.setAdapter(focusmodeAdapter);

			String initValue = preferences.getString(MainScreen
					.getCameraMirrored() ? sRearFocusModePref
					: sFrontFocusModePref, sDefaultFocusValue);
			if (!supported_focus.contains(initValue)) {
				if (supported_focus.contains(sDefaultValue))
					initValue = sDefaultValue;
				else
					initValue = supported_focus.get(0);

				// preferences.edit().putString(MainScreen.getCameraMirrored()?
				// sRearFocusModePref : sFrontFocusModePref,
				// initValue).commit();
			}
			setButtonSelected(FocusModeButtons, initValue);
			setCameraParameterValue(MODE_FOCUS, initValue);

			if (icons_focus!=null)
			{
				RotateImageView but = (RotateImageView) topMenuButtons
						.get(MODE_FOCUS);
				int icon_id = icons_focus.get(initValue);
				but.setImageResource(icon_id);
			}

			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			params.setFocusMode(mFocusMode);
			Camera camera = MainScreen.thiz.getCamera();
			if (null != camera)
			{
				try {
					camera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("onCameraCreate", "focus exception: " + e.getMessage());
				}
			}
		} else
			mFocusModeSupported = false;

		// Create Flash mode button and adding supported flash modes
		List<String> supported_flash = MainScreen.thiz.getSupportedFlashModes();
		if (supported_flash != null && supported_flash.size() > 0 && activeFlash != null) {
			mFlashModeSupported = true;
			defaultQuickControl2 = String.valueOf(MODE_FLASH);

			Set<String> keys = FlashModeButtons.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String flash_name = it.next();
				if (supported_flash.contains(flash_name)) {
					activeFlash.add(FlashModeButtons.get(flash_name));
					activeFlashNames.add(flash_name);
				}
			}

			flashmodeAdapter.Elements = activeFlash;
			GridView gridview = (GridView) guiView
					.findViewById(R.id.flashmodeGrid);
			gridview.setNumColumns(activeFlash.size() > 9 ? 4 : 3);
			gridview.setAdapter(flashmodeAdapter);

			String initValue = preferences.getString(sFlashModePref,
					sDefaultValue);
			if (!supported_flash.contains(initValue)) {
				if (MainScreen.thiz.isFrontCamera())
					initValue = supported_flash.get(0);
				else
					initValue = sDefaultValue;
			}
			setButtonSelected(FlashModeButtons, initValue);
			setCameraParameterValue(MODE_FLASH, initValue);

			if (icons_flash!=null)
			{
				RotateImageView but = (RotateImageView) topMenuButtons
						.get(MODE_FLASH);
				int icon_id = icons_flash.get(initValue);
				but.setImageResource(icon_id);
			}

			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			params.setFlashMode(mFlashMode);
			Camera camera = MainScreen.thiz.getCamera();
			if (null != camera)
			{
				try {
					camera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("onCameraCreate", "Flash exception: " + e.getMessage());
				}
			}
		} else {
			mFlashModeSupported = false;
			mFlashMode = null;
		}

		// Create ISO button and adding supported ISOs
		List<String> supported_iso = MainScreen.thiz.getSupportedISO();
		if (supported_iso != null && supported_iso.size() > 0 && activeISO != null) {
			mISOSupported = true;

			Set<String> keys = ISOButtons.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()) {
				String iso_name = it.next();
				if (supported_iso.contains(iso_name)) {
					activeISO.add(ISOButtons.get(iso_name));
					activeISONames.add(iso_name);
				}
			}

			isoAdapter.Elements = activeISO;
			GridView gridview = (GridView) guiView.findViewById(R.id.isoGrid);
			gridview.setNumColumns(activeISO.size() > 9 ? 4 : 3);
			gridview.setAdapter(isoAdapter);

			String initValue = preferences.getString(sISOPref, sDefaultValue);
			if (!supported_iso.contains(initValue)) {
				if (MainScreen.thiz.isFrontCamera())
					initValue = supported_iso.get(0);
				else
					initValue = sDefaultValue;

				preferences.edit().putString(sISOPref, initValue).commit();
			}
			setButtonSelected(ISOButtons, initValue);
			setCameraParameterValue(MODE_ISO, initValue);

			if (icons_iso!=null)
			{
				RotateImageView but = (RotateImageView) topMenuButtons
						.get(MODE_ISO);
				int icon_id = icons_iso.get(initValue);
				but.setImageResource(icon_id);
			}

			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			params.set(isoParam, mISO);
			Camera camera = MainScreen.thiz.getCamera();
			if (null != camera)
			{
				try {
					camera.setParameters(params);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("onCameraCreate", "ISO exception: " + e.getMessage());
				}
			}
		} else {
			mISOSupported = false;
			mISO = null;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			addCameraChangeButton();
			defaultQuickControl4 = String.valueOf(MODE_CAM);
		} else
			mCameraChangeSupported = false;

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		String qc1 = prefs.getString("quickControlButton1",
				defaultQuickControl1);
		String qc2 = prefs.getString("quickControlButton2",
				defaultQuickControl2);
		String qc3 = prefs.getString("quickControlButton3",
				defaultQuickControl3);
		String qc4 = prefs.getString("quickControlButton4",
				defaultQuickControl4);

		quickControl1 = isCameraParameterSupported(qc1) ? getQuickControlButton(
				qc1, quickControl1) : getFreeQuickControlButton(qc1, qc2, qc3,
				qc4, quickControl1);
		quickControl2 = isCameraParameterSupported(qc2) ? getQuickControlButton(
				qc2, quickControl2) : getFreeQuickControlButton(qc1, qc2, qc3,
				qc4, quickControl2);
		quickControl3 = isCameraParameterSupported(qc3) ? getQuickControlButton(
				qc3, quickControl3) : getFreeQuickControlButton(qc1, qc2, qc3,
				qc4, quickControl3);
		quickControl4 = isCameraParameterSupported(qc4) ? getQuickControlButton(
				qc4, quickControl4) : getFreeQuickControlButton(qc1, qc2, qc3,
				qc4, quickControl4);

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl1);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl2);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl3);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl4);

		if (mEVSupported) {
			Message msg = new Message();
			msg.arg1 = PluginManager.MSG_EV_CHANGED;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}

		// Hide all opened menu
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		// create and fill drawing slider
		initSettingsMenu();

		Panel.OnPanelListener pListener = new OnPanelListener() {
			public void onPanelOpened(Panel panel) {
				settingsControlsVisible = true;

				if (modeSelectorVisible)
					hideModeList();
				if (quickControlsChangeVisible)
					closeQuickControlsSettings();
				if (isSecondaryMenusVisible())
					hideSecondaryMenus();

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_CONTROL_LOCKED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
			}

			public void onPanelClosed(Panel panel) {
				settingsControlsVisible = false;

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false,
						false);
			}
		};

		guiView.findViewById(R.id.topPanel).bringToFront();
		((Panel) guiView.findViewById(R.id.topPanel))
				.setOnPanelListener(pListener);
	}

	private boolean isCameraParameterSupported(String param) {
		if (param != "" && topMenuPluginButtons.containsKey(param))
			return true;
		else if (param != ""
				&& com.almalence.opencam.util.Util.isNumeric(param)) {
			int cameraParameter = Integer.valueOf(param);
			switch (cameraParameter) {
			case MODE_EV:
				if (mEVSupported)
					return true;
				else
					return false;
			case MODE_SCENE:
				if (mSceneModeSupported)
					return true;
				else
					return false;
			case MODE_WB:
				if (mWBSupported)
					return true;
				else
					return false;
			case MODE_FLASH:
				if (mFlashModeSupported)
					return true;
				else
					return false;
			case MODE_FOCUS:
				if (mFocusModeSupported)
					return true;
				else
					return false;
			case MODE_ISO:
				if (mISOSupported)
					return true;
				else
					return false;
			case MODE_CAM:
				if (mCameraChangeSupported)
					return true;
				else
					return false;
			}
		}

		return false;
	}

	public void disableCameraParameter(CameraParameter iParam, boolean bDisable) {
		View topMenuView = null;
		switch (iParam) {
		case CAMERA_PARAMETER_EV:
			topMenuView = topMenuButtons.get(MODE_EV);
			isEVEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_SCENE:
			topMenuView = topMenuButtons.get(MODE_SCENE);
			isSceneEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_WB:
			topMenuView = topMenuButtons.get(MODE_WB);
			isWBEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_FOCUS:
			topMenuView = topMenuButtons.get(MODE_FOCUS);
			isFocusEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_FLASH:
			topMenuView = topMenuButtons.get(MODE_FLASH);
			isFlashEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_ISO:
			topMenuView = topMenuButtons.get(MODE_ISO);
			isIsoEnabled = !bDisable;
			break;
		case CAMERA_PARAMETER_CAMERACHANGE:
			topMenuView = topMenuButtons.get(MODE_CAM);
			isCameraChangeEnabled = !bDisable;
			break;
		}

		if (topMenuView != null) {
			correctTopMenuButtonBackground(topMenuView, !bDisable);

			initSettingsMenu();
		}

	}

	private void correctTopMenuButtonBackground(View topMenuView,
			boolean isEnabled) {
		if (topMenuView != null) {
			if (!isEnabled) {
				((RotateImageView) topMenuView).setColorFilter(0x50FAFAFA,
						PorterDuff.Mode.DST_IN);
			} else {
				((RotateImageView) topMenuView).clearColorFilter();
			}
		}
	}

	private View getQuickControlButton(String qcID, View defaultView) {
		if (qcID != "" && topMenuPluginButtons.containsKey(qcID)) {
			Plugin plugin = PluginManager.getInstance().getPlugin(qcID);
			RotateImageView view = (RotateImageView) topMenuPluginButtons
					.get(qcID);
			LinearLayout paramsLayout = (LinearLayout) MainScreen.thiz
					.findViewById(R.id.paramsLayout);
			int reqWidth = paramsLayout.getWidth() / 4;
			int reqHeight = paramsLayout.getHeight();

			view.setImageResource(plugin.getQuickControlIconID());
			return view;
		} else if (qcID != ""
				&& topMenuButtons.containsKey(Integer.valueOf(qcID)))
			return topMenuButtons.get(Integer.valueOf(qcID));

		return defaultView;
	}

	// Method for finding a button for a top menu which not yet represented in
	// that top menu
	private View getFreeQuickControlButton(String qc1, String qc2, String qc3,
			String qc4, View emptyView) {
		Set<Integer> topMenuButtonsKeys = topMenuButtons.keySet();
		Iterator<Integer> topMenuButtonsIterator = topMenuButtonsKeys
				.iterator();

		Set<String> topMenuPluginButtonsKeys = topMenuPluginButtons.keySet();
		Iterator<String> topMenuPluginButtonsIterator = topMenuPluginButtonsKeys
				.iterator();

		// Searching for free button in the top menu buttons list (scene mode,
		// wb, focus, flash, camera switch)
		while (topMenuButtonsIterator.hasNext()) {
			int id1, id2, id3, id4;
			try {
				id1 = Integer.valueOf(qc1);
			} catch (NumberFormatException exp) {
				id1 = -1;
			}
			try {
				id2 = Integer.valueOf(qc2);
			} catch (NumberFormatException exp) {
				id2 = -1;
			}
			try {
				id3 = Integer.valueOf(qc3);
			} catch (NumberFormatException exp) {
				id3 = -1;
			}
			try {
				id4 = Integer.valueOf(qc4);
			} catch (NumberFormatException exp) {
				id4 = -1;
			}

			int buttonID = topMenuButtonsIterator.next();
			View topMenuButton = topMenuButtons.get(buttonID);

			if (topMenuButton != quickControl1
					&& topMenuButton != quickControl2
					&& topMenuButton != quickControl3
					&& topMenuButton != quickControl4 && buttonID != id1
					&& buttonID != id2 && buttonID != id3 && buttonID != id4
					&& isCameraParameterSupported(String.valueOf(buttonID)))
				return topMenuButton;
		}

		// If top menu buttons dosn't have a free button, search in plugin's
		// buttons list
		while (topMenuPluginButtonsIterator.hasNext()) {
			String buttonID = topMenuPluginButtonsIterator.next();
			View topMenuButton = topMenuPluginButtons.get(buttonID);

			if (topMenuButton != quickControl1
					&& topMenuButton != quickControl2
					&& topMenuButton != quickControl3
					&& topMenuButton != quickControl4 && buttonID != qc1
					&& buttonID != qc2 && buttonID != qc3 && buttonID != qc4)
				return topMenuButton;
		}

		// If no button is found create a empty button
		return emptyView;
	}

	@TargetApi(9)
	void addCameraChangeButton() {
		if (Camera.getNumberOfCameras() > 1) {
			mCameraChangeSupported = true;

			RotateImageView but = (RotateImageView) topMenuButtons
					.get(MODE_CAM);
			but.setImageResource(icon_cam);
		} else
			mCameraChangeSupported = false;
	}

	public void rotateSquareViews(final int degree, int duration) {
		if (AlmalenceGUI.mPreviousDeviceOrientation != AlmalenceGUI.mDeviceOrientation
				|| duration == 0) {
			// float mode_rotation = mode0.getRotation();
			int startDegree = AlmalenceGUI.mPreviousDeviceOrientation == 0 ? 0
					: 360 - AlmalenceGUI.mPreviousDeviceOrientation;
			int endDegree = AlmalenceGUI.mDeviceOrientation == 0 ? 0
					: 360 - AlmalenceGUI.mDeviceOrientation;

			int diff = endDegree - startDegree;
			// diff = diff >= 0 ? diff : 360 + diff; // make it in range [0,
			// 359]
			//
			// // Make it in range [-179, 180]. That's the shorted distance
			// between the
			// // two angles
			endDegree = diff > 180 ? endDegree - 360 : diff < -180
					&& endDegree == 0 ? 360 : endDegree;

			if (modeSelectorVisible)
				rotateViews(modeViews, startDegree, endDegree, duration);

			if (settingsViews.size() > 0)
			{
				int delay = ((Panel)guiView.findViewById(R.id.topPanel)).isOpen()?250:0;
				rotateViews(settingsViews, startDegree, endDegree, delay);
			}

			if (quickControlChangeres.size() > 0
					&& this.quickControlsChangeVisible)
				rotateViews(quickControlChangeres, startDegree, endDegree,
						duration);

			if (guiView.findViewById(R.id.scenemodeLayout).getVisibility() == View.VISIBLE)
				rotateViews(activeScene, startDegree, endDegree, duration);

			if (guiView.findViewById(R.id.wbLayout).getVisibility() == View.VISIBLE)
				rotateViews(activeWB, startDegree, endDegree, duration);

			if (guiView.findViewById(R.id.focusmodeLayout).getVisibility() == View.VISIBLE)
				rotateViews(activeFocus, startDegree, endDegree, duration);

			if (guiView.findViewById(R.id.flashmodeLayout).getVisibility() == View.VISIBLE)
				rotateViews(activeFlash, startDegree, endDegree, duration);

			if (guiView.findViewById(R.id.isoLayout).getVisibility() == View.VISIBLE)
				rotateViews(activeISO, startDegree, endDegree, duration);
		}
	}

	private void rotateViews(List<View> views, final float startDegree,
			final float endDegree, long duration) {
		for (int i = 0; i < views.size(); i++) {
			float start = startDegree;
			float end = endDegree;
			final View view = views.get(i);

			if (duration == 0) {
				view.setRotation(endDegree);
			} else {
				start = startDegree - view.getRotation();
				end = endDegree - view.getRotation();

				RotateAnimation animation = new RotateAnimation(start, end,
						view.getWidth() / 2, view.getHeight() / 2);

				animation.setDuration(duration);
				animation.setFillAfter(true);
				animation.setInterpolator(new DecelerateInterpolator());

				animation.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationEnd(Animation animation) {
						// TODO Auto-generated method stub
						view.clearAnimation();
						view.setRotation(endDegree);

					}

					@Override
					public void onAnimationRepeat(Animation animation) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onAnimationStart(Animation animation) {
						// TODO Auto-generated method stub

					}

				});

				view.startAnimation(animation);
			}
		}
	}

	/***************************************************************************************
	 * 
	 * addQuickSetting method
	 * 
	 * type: EV, SCENE, WB, FOCUS, FLASH, CAMERA or MORE
	 * 
	 * Common method for tune quick control's and quick setting view's icon and
	 * text
	 * 
	 * Quick controls and Quick settings views has a similar design but
	 * different behavior
	 * 
	 ****************************************************************************************/
	private void addQuickSetting(SettingsType type, boolean isQuickControl) {
		int icon_id = -1;
		CharSequence icon_text = "";
		boolean isEnabled = true;

		switch (type) {
		case SCENE:
			icon_id = icons_scene.get(mSceneMode);
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_scene);
			isEnabled = isSceneEnabled;
			break;
		case WB:
			icon_id = icons_wb.get(mWB);
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_wb);
			isEnabled = isWBEnabled;
			break;
		case FOCUS:
			icon_id = icons_focus.get(mFocusMode);
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_focus);
			isEnabled = isFocusEnabled;
			break;
		case FLASH:
			icon_id = icons_flash.get(mFlashMode);
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_flash);
			isEnabled = isFlashEnabled;
			break;
		case ISO:
			icon_id = icons_iso.get(mISO);
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_iso);
			isEnabled = isIsoEnabled;
			break;
		case CAMERA: {
			icon_id = icon_cam;
			if (preferences.getBoolean("useFrontCamera", false) == false)
				icon_text = MainScreen.thiz.getResources().getString(
						R.string.settings_mode_rear);
			else
				icon_text = MainScreen.thiz.getResources().getString(
						R.string.settings_mode_front);

			isEnabled = isCameraChangeEnabled;
		}
			break;
		case EV:
			icon_id = icon_ev;
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_exposure);
			isEnabled = isEVEnabled;
			break;
		case MORE:
			icon_id = icon_settings;
			icon_text = MainScreen.thiz.getResources().getString(
					R.string.settings_mode_moresettings);
			break;
		}

		// Get required size of button
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		View settingView = inflator.inflate(
				R.layout.gui_almalence_quick_control_grid_element, null, false);
		ImageView iconView = (ImageView) settingView
				.findViewById(R.id.imageView);
		iconView.setImageResource(icon_id);
		TextView textView = (TextView) settingView.findViewById(R.id.textView);
		textView.setText(icon_text);

		if (!isEnabled && !isQuickControl) {
			iconView.setColorFilter(MainScreen.mainContext.getResources()
					.getColor(R.color.buttonDisabled), PorterDuff.Mode.DST_IN);
			textView.setTextColor(MainScreen.mainContext.getResources()
					.getColor(R.color.textDisabled));
		}

		// Create onClickListener of right type
		switch (type) {
		case SCENE:
			if (isQuickControl)
				createQuickControlSceneOnClick(settingView);
			else
				createSettingSceneOnClick(settingView);
			break;
		case WB:
			if (isQuickControl)
				createQuickControlWBOnClick(settingView);
			else
				createSettingWBOnClick(settingView);
			break;
		case FOCUS:
			if (isQuickControl)
				createQuickControlFocusOnClick(settingView);
			else
				createSettingFocusOnClick(settingView);
			break;
		case FLASH:
			if (isQuickControl)
				createQuickControlFlashOnClick(settingView);
			else
				createSettingFlashOnClick(settingView);
			break;
		case ISO:
			if (isQuickControl)
				createQuickControlIsoOnClick(settingView);
			else
				createSettingIsoOnClick(settingView);
			break;
		case CAMERA:
			if (isQuickControl)
				createQuickControlCameraChangeOnClick(settingView);
			else
				createSettingCameraOnClick(settingView);
			break;
		case EV:
			if (isQuickControl)
				createQuickControlEVOnClick(settingView);
			else
				createSettingEVOnClick(settingView);
			break;
		case MORE:
			if (isQuickControl)
				return;
			else
				createSettingMoreOnClick(settingView);
			break;
		}

		if (isQuickControl)
			quickControlChangeres.add(settingView);
		else
			settingsViews.add(settingView);
	}

	private void addPluginQuickSetting(Plugin plugin, boolean isQuickControl) {
		int iconID = plugin.getQuickControlIconID();
		String title = plugin.getQuickControlTitle();

		if (iconID <= 0)
			return;

		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
		View qcView = inflator.inflate(
				R.layout.gui_almalence_quick_control_grid_element, null, false);
		((ImageView) qcView.findViewById(R.id.imageView))
				.setImageResource(iconID);
		((TextView) qcView.findViewById(R.id.textView)).setText(title);

		createPluginQuickControlOnClick(plugin, qcView, isQuickControl);

		if (isQuickControl)
			quickControlChangeres.add(qcView);
		else
			settingsViews.add(qcView);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initQuickControlsMenu(View currentView) {
		quickControlChangeres.clear();
		if (quickControlAdapter.Elements != null) {
			quickControlAdapter.Elements.clear();
			quickControlAdapter.notifyDataSetChanged();
		}

		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext()) {
			Integer id = it.next();
			switch (id) {
			case R.id.evButton:
				if (mEVSupported)
					addQuickSetting(SettingsType.EV, true);
				break;
			case R.id.sceneButton:
				if (mSceneModeSupported)
					addQuickSetting(SettingsType.SCENE, true);
				break;
			case R.id.wbButton:
				if (mWBSupported)
					addQuickSetting(SettingsType.WB, true);
				break;
			case R.id.focusButton:
				if (mFocusModeSupported)
					addQuickSetting(SettingsType.FOCUS, true);
				break;
			case R.id.flashButton:
				if (mFlashModeSupported)
					addQuickSetting(SettingsType.FLASH, true);
				break;
			case R.id.isoButton:
				if (mISOSupported)
					addQuickSetting(SettingsType.ISO, true);
				break;
			case R.id.camerachangeButton:
				if (mCameraChangeSupported)
					addQuickSetting(SettingsType.CAMERA, true);
				break;
			}
		}

		// Add quick conrols from plugins
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(
				PluginType.ViewFinder));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(
				PluginType.Capture));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(
				PluginType.Processing));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(
				PluginType.Filter));
		initPluginQuickControls(PluginManager.getInstance().getActivePlugins(
				PluginType.Export));

		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeight = (int) (width / 3 - 5 * metrics.density);

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(
				LayoutParams.WRAP_CONTENT, modeHeight);

		for (int i = 0; i < quickControlChangeres.size(); i++) {
			View setting = quickControlChangeres.get(i);
			setting.setLayoutParams(params);
		}

		quickControlAdapter.Elements = quickControlChangeres;
		quickControlAdapter.notifyDataSetChanged();
	}

	private void initPluginQuickControls(List<Plugin> Plugins) {
		if (Plugins.size() > 0) {
			for (int i = 0; i < Plugins.size(); i++) {
				Plugin Plugin = Plugins.get(i);

				if (Plugin == null)
					continue;

				addPluginQuickSetting(Plugin, true);
			}
		}
	}

	private void initPluginSettingsControls(List<Plugin> Plugins) {
		if (Plugins.size() > 0) {
			for (int i = 0; i < Plugins.size(); i++) {
				Plugin Plugin = Plugins.get(i);

				if (Plugin == null)
					continue;

				addPluginQuickSetting(Plugin, false);
			}
		}
	}

	private void createPluginQuickControlOnClick(final Plugin plugin,
			View view, boolean isQuickControl) {
		if (isQuickControl)
			view.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons
							.get(plugin.getID());
					LinearLayout paramsLayout = (LinearLayout) MainScreen.thiz
							.findViewById(R.id.paramsLayout);
					int reqWidth = paramsLayout.getWidth() / 4;
					int reqHeight = paramsLayout.getHeight();

					pluginButton.setImageResource(plugin
							.getQuickControlIconID());

					switchViews(currentQuickView, pluginButton, plugin.getID());
					recreateQuickControlsMenu();
					changeCurrentQuickControl(pluginButton);
					initQuickControlsMenu(currentQuickView);
					showQuickControlsSettings();
				}

			});
		else
			view.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					try 
					{
						plugin.onQuickControlClick();
	
						int icon_id = plugin.getQuickControlIconID();
						String title = plugin.getQuickControlTitle();
						Drawable icon = MainScreen.mainContext.getResources()
								.getDrawable(icon_id);
						((ImageView) v.findViewById(R.id.imageView))
								.setImageResource(icon_id);
						((TextView) v.findViewById(R.id.textView)).setText(title);
	
						RotateImageView pluginButton = (RotateImageView) topMenuPluginButtons
								.get(plugin.getID());
						pluginButton.setImageDrawable(icon);
	
						initSettingsMenu();
					} catch (Exception e) {
						e.printStackTrace();
						Log.e("Almalence GUI", "createPluginQuickControlOnClick exception" + e.getMessage());
					}
				}
			});
	}

	private void createQuickControlEVOnClick(View ev) {
		ev.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView ev = (RotateImageView) topMenuButtons
						.get(MODE_EV);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icon_ev);
				ev.setImageDrawable(icon);

				switchViews(currentQuickView, ev, String.valueOf(MODE_EV));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(ev);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlSceneOnClick(View scene) {
		scene.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView scene = (RotateImageView) topMenuButtons
						.get(MODE_SCENE);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icons_scene.get(mSceneMode));
				scene.setImageDrawable(icon);

				switchViews(currentQuickView, scene, String.valueOf(MODE_SCENE));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(scene);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlWBOnClick(View wb) {
		wb.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView wb = (RotateImageView) topMenuButtons
						.get(MODE_WB);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icons_wb.get(mWB));
				wb.setImageDrawable(icon);

				switchViews(currentQuickView, wb, String.valueOf(MODE_WB));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(wb);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlFocusOnClick(View focus) {
		focus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView focus = (RotateImageView) topMenuButtons
						.get(MODE_FOCUS);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icons_focus.get(mFocusMode));
				focus.setImageDrawable(icon);

				switchViews(currentQuickView, focus, String.valueOf(MODE_FOCUS));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(focus);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlFlashOnClick(View flash) {
		flash.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView flash = (RotateImageView) topMenuButtons
						.get(MODE_FLASH);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icons_flash.get(mFlashMode));
				flash.setImageDrawable(icon);

				switchViews(currentQuickView, flash, String.valueOf(MODE_FLASH));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(flash);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlIsoOnClick(View iso) {
		iso.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView iso = (RotateImageView) topMenuButtons
						.get(MODE_ISO);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icons_iso.get(mISO));
				iso.setImageDrawable(icon);

				switchViews(currentQuickView, iso, String.valueOf(MODE_ISO));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(iso);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	private void createQuickControlCameraChangeOnClick(View cameraChange) {
		cameraChange.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RotateImageView cam = (RotateImageView) topMenuButtons
						.get(MODE_CAM);
				Drawable icon = MainScreen.mainContext.getResources()
						.getDrawable(icon_cam);
				cam.setImageDrawable(icon);

				switchViews(currentQuickView, cam, String.valueOf(MODE_CAM));
				recreateQuickControlsMenu();
				changeCurrentQuickControl(cam);
				initQuickControlsMenu(currentQuickView);
				showQuickControlsSettings();
			}

		});
	}

	public void changeCurrentQuickControl(View newCurrent) {
		if (currentQuickView != null)
			currentQuickView.setBackgroundDrawable(MainScreen.mainContext
					.getResources().getDrawable(
							R.drawable.transparent_background));

		currentQuickView = newCurrent;
		newCurrent.setBackgroundDrawable(MainScreen.mainContext.getResources()
				.getDrawable(R.drawable.layout_border_qc_button));
		((RotateImageView) newCurrent).setBackgroundEnabled(true);
	}

	private void showQuickControlsSettings() {
		unselectPrimaryTopMenuButtons(-1);
		hideSecondaryMenus();

		GridView gridview = (GridView) guiView.findViewById(R.id.qcGrid);
		gridview.setAdapter(quickControlAdapter);

		// final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ?
		// AlmalenceGUI.mDeviceOrientation % 360
		// : AlmalenceGUI.mDeviceOrientation % 360 + 360;
		// rotateSquareViews(degree, 0);

		((RelativeLayout) guiView.findViewById(R.id.qcLayout))
				.setVisibility(View.VISIBLE);
		(guiView.findViewById(R.id.qcGrid)).setVisibility(View.VISIBLE);

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.setBackgroundDrawable(MainScreen.mainContext.getResources()
						.getDrawable(R.drawable.topmenu_background_qc));

		Set<Integer> topmenu_keys = topMenuButtons.keySet();
		Iterator<Integer> it = topmenu_keys.iterator();
		while (it.hasNext()) {
			int key = it.next();
			if (currentQuickView != topMenuButtons.get(key)) {
				((RotateImageView) topMenuButtons.get(key))
						.setBackgroundEnabled(true);
				topMenuButtons.get(key).setBackgroundDrawable(
						MainScreen.mainContext.getResources().getDrawable(
								R.drawable.transparent_background));
			}
		}

		quickControlsChangeVisible = true;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void closeQuickControlsSettings() {
		RelativeLayout gridview = (RelativeLayout) guiView
				.findViewById(R.id.qcLayout);
		gridview.setVisibility(View.INVISIBLE);
		(guiView.findViewById(R.id.qcGrid)).setVisibility(View.INVISIBLE);
		quickControlsChangeVisible = false;

		currentQuickView.setBackgroundDrawable(MainScreen.mainContext
				.getResources().getDrawable(R.drawable.transparent_background));
		currentQuickView = null;

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.setBackgroundDrawable(MainScreen.mainContext.getResources()
						.getDrawable(R.drawable.topmenu_background));

		correctTopMenuButtonBackground(MainScreen.thiz.findViewById(MODE_EV),
				isEVEnabled);
		correctTopMenuButtonBackground(
				MainScreen.thiz.findViewById(MODE_SCENE), isSceneEnabled);
		correctTopMenuButtonBackground(MainScreen.thiz.findViewById(MODE_WB),
				isWBEnabled);
		correctTopMenuButtonBackground(
				MainScreen.thiz.findViewById(MODE_FOCUS), isFocusEnabled);
		correctTopMenuButtonBackground(
				MainScreen.thiz.findViewById(MODE_FLASH), isFlashEnabled);
		correctTopMenuButtonBackground(MainScreen.thiz.findViewById(MODE_ISO),
				isIsoEnabled);
		correctTopMenuButtonBackground(MainScreen.thiz.findViewById(MODE_CAM),
				isCameraChangeEnabled);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);

		guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
	}

	/***************************************************************************************
	 * 
	 * QUICK CONTROLS CUSTOMIZATION METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * begin >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
	 ****************************************************************************************/
	private void initSettingsMenu() {
		// Clear view list to recreate all settings buttons
		settingsViews.clear();
		if (settingsAdapter.Elements != null) {
			settingsAdapter.Elements.clear();
			settingsAdapter.notifyDataSetChanged();
		}

		// Obtain all theoretical buttons we know
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext()) {
			// If such camera feature is supported then add a button to settings
			// menu
			Integer id = it.next();
			switch (id) {
			case R.id.evButton:
				if (mEVSupported)
					addQuickSetting(SettingsType.EV, false);
				break;
			case R.id.sceneButton:
				if (mSceneModeSupported)
					addQuickSetting(SettingsType.SCENE, false);
				break;
			case R.id.wbButton:
				if (mWBSupported)
					addQuickSetting(SettingsType.WB, false);
				break;
			case R.id.focusButton:
				if (mFocusModeSupported)
					addQuickSetting(SettingsType.FOCUS, false);
				break;
			case R.id.flashButton:
				if (mFlashModeSupported)
					addQuickSetting(SettingsType.FLASH, false);
				break;
			case R.id.isoButton:
				if (mISOSupported)
					addQuickSetting(SettingsType.ISO, false);
				break;
			case R.id.camerachangeButton:
				if (mCameraChangeSupported)
					addQuickSetting(SettingsType.CAMERA, false);
				break;
			}
		}

		// Add quick conrols from plugins
		initPluginSettingsControls(PluginManager.getInstance()
				.getActivePlugins(PluginType.ViewFinder));
		initPluginSettingsControls(PluginManager.getInstance()
				.getActivePlugins(PluginType.Capture));
		initPluginSettingsControls(PluginManager.getInstance()
				.getActivePlugins(PluginType.Processing));
		initPluginSettingsControls(PluginManager.getInstance()
				.getActivePlugins(PluginType.Filter));
		initPluginSettingsControls(PluginManager.getInstance()
				.getActivePlugins(PluginType.Export));

		// The very last control is always MORE SETTINGS
		addQuickSetting(SettingsType.MORE, false);

		settingsAdapter.Elements = settingsViews;

		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeight = (int) (width / 3 - 5 * metrics.density);

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(
				LayoutParams.WRAP_CONTENT, modeHeight);

		for (int i = 0; i < settingsViews.size(); i++) {
			View setting = settingsViews.get(i);
			setting.setLayoutParams(params);
		}

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		GridView gridview = (GridView) guiView.findViewById(R.id.settingsGrid);
		gridview.setAdapter(settingsAdapter);
		settingsAdapter.notifyDataSetChanged();
	}

	private void createSettingSceneOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if (!isSceneEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.BOTTOM,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				List<String> supported_scene = MainScreen.thiz
						.getSupportedSceneModes();
				if (supported_scene.size() > 0) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, false);
					showParams(MODE_SCENE);
				} else {
					String newSceneMode;
					ListIterator<String> it = supported_scene
							.listIterator(supported_scene.indexOf(mSceneMode));
					it.next();
					if (it.hasNext())
						newSceneMode = it.next();
					else
						newSceneMode = supported_scene.get(0);

					Drawable icon = MainScreen.mainContext.getResources()
							.getDrawable(icons_scene.get(newSceneMode));
					((ImageView) v.findViewById(R.id.imageView))
							.setImageDrawable(icon);
					setSceneMode(newSceneMode);
				}
			}
		});
	}

	private void createSettingWBOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isWBEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				List<String> supported_wb = MainScreen.thiz
						.getSupportedWhiteBalance();
				if (supported_wb.size() > 0) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, false);
					showParams(MODE_WB);
				} else {
					String newWBMode;
					ListIterator<String> it = supported_wb
							.listIterator(supported_wb.indexOf(mWB));
					it.next();
					if (it.hasNext())
						newWBMode = it.next();
					else
						newWBMode = supported_wb.get(0);

					((ImageView) v.findViewById(R.id.imageView))
							.setImageResource(icons_wb.get(newWBMode));
					setWhiteBalance(newWBMode);
				}
			}
		});
	}

	private void createSettingFocusOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isFocusEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				List<String> supported_focus = MainScreen.thiz
						.getSupportedFocusModes();
				if (supported_focus.size() > 0) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, false);
					showParams(MODE_FOCUS);
				} else {
					String newFocusMode;
					ListIterator<String> it = supported_focus
							.listIterator(supported_focus.indexOf(mFocusMode));
					it.next();
					if (it.hasNext())
						newFocusMode = it.next();
					else
						newFocusMode = supported_focus.get(0);

					Drawable icon = MainScreen.mainContext.getResources()
							.getDrawable(icons_focus.get(newFocusMode));
					((ImageView) v.findViewById(R.id.imageView))
							.setImageDrawable(icon);
					setFocusMode(newFocusMode);
				}
			}
		});
	}

	private void createSettingFlashOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isFlashEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				List<String> supported_flash = MainScreen.thiz
						.getSupportedFlashModes();
				if (supported_flash.size() > 0) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, false);
					showParams(MODE_FLASH);
				} else {
					String newFlashMode;
					ListIterator<String> it = supported_flash
							.listIterator(supported_flash.indexOf(mFlashMode));
					it.next();
					if (it.hasNext())
						newFlashMode = it.next();
					else
						newFlashMode = supported_flash.get(0);

					Drawable icon = MainScreen.mainContext.getResources()
							.getDrawable(icons_flash.get(newFlashMode));
					((ImageView) v.findViewById(R.id.imageView))
							.setImageDrawable(icon);
					setFlashMode(newFlashMode);
				}
			}
		});
	}

	private void createSettingIsoOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isIsoEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				List<String> supported_iso = MainScreen.thiz.getSupportedISO();
				if (supported_iso.size() > 0) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, false);
					showParams(MODE_ISO);
				} else {
					String newISO;
					ListIterator<String> it = supported_iso
							.listIterator(supported_iso.indexOf(mISO));
					it.next();
					if (it.hasNext())
						newISO = it.next();
					else
						newISO = supported_iso.get(0);

					Drawable icon = MainScreen.mainContext.getResources()
							.getDrawable(icons_iso.get(newISO));
					((ImageView) v.findViewById(R.id.imageView))
							.setImageDrawable(icon);
					setISO(newISO);
				}
			}
		});
	}

	private void createSettingCameraOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isCameraChangeEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				CameraSwitched(true);
			}

		});
	}

	private void createSettingEVOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!isEVEnabled) {
					showToast(
							null,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							MainScreen.thiz.getResources().getString(
									R.string.settings_not_available), true,
							false);
					return;
				}
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false,
						false);
				showParams(MODE_EV);
			}
		});
	}

	private void createSettingMoreOnClick(View settingView) {
		settingView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PluginManager.getInstance().onShowPreferences();
				Intent settingsActivity = new Intent(MainScreen.mainContext,
						Preferences.class);
				MainScreen.thiz.startActivity(settingsActivity);
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false,
						false);
			}
		});
	}

	/***************************************************************************************
	 * 
	 * SETTINGS MENU METHODS
	 * 
	 * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< end
	 ***************************************************************************************/

	private void CameraSwitched(boolean restart) {
		if (PluginManager.getInstance().getProcessingCounter() != 0)
			return;
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		boolean isFrontCamera = prefs.getBoolean("useFrontCamera", false);
		prefs.edit().putBoolean("useFrontCamera", !isFrontCamera).commit();

		if (restart == true) {
			MainScreen.thiz.PauseMain();
			MainScreen.thiz.ResumeMain();
		}
	}

	private void showModeList() {
		unselectPrimaryTopMenuButtons(-1);
		hideSecondaryMenus();

		initModeList();
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(MainScreen.thiz.getResources()
				.getDimension(R.dimen.gridModeImageSize)
				+ MainScreen.thiz.getResources().getDimension(
						R.dimen.gridModeTextLayoutSize));

		int modeHeight = modeHeightByDimen > modeHeightByWidth ? modeHeightByWidth
				: modeHeightByDimen;

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(
				LayoutParams.WRAP_CONTENT, modeHeight);

		for (int i = 0; i < modeViews.size(); i++) {
			View mode = modeViews.get(i);
			mode.setLayoutParams(params);
		}

		GridView gridview = (GridView) guiView.findViewById(R.id.modeGrid);
		gridview.setAdapter(modeAdapter);

		((RelativeLayout) guiView.findViewById(R.id.modeLayout))
				.setVisibility(View.VISIBLE);
		(guiView.findViewById(R.id.modeGrid)).setVisibility(View.VISIBLE);

		guiView.findViewById(R.id.modeLayout).bringToFront();

		modeSelectorVisible = true;

		final int degree = AlmalenceGUI.mDeviceOrientation >= 0 ? AlmalenceGUI.mDeviceOrientation % 360
				: AlmalenceGUI.mDeviceOrientation % 360 + 360;
		rotateSquareViews(degree, 0);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void hideModeList() {
		RelativeLayout gridview = (RelativeLayout) guiView
				.findViewById(R.id.modeLayout);

		Animation gone = AnimationUtils.loadAnimation(MainScreen.thiz,
				R.anim.gui_almalence_modelist_invisible);
		gone.setFillAfter(true);

		gridview.setAnimation(gone);
		(guiView.findViewById(R.id.modeGrid)).setAnimation(gone);

		gridview.setVisibility(View.GONE);
		(guiView.findViewById(R.id.modeGrid)).setVisibility(View.GONE);

		gone.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				guiView.findViewById(R.id.modeLayout).clearAnimation();
				guiView.findViewById(R.id.modeGrid).clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});

		modeSelectorVisible = false;

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	@Override
	public void onHardwareShutterButtonPressed() {
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		lockControls = true;
		PluginManager.getInstance().OnShutterClick();
	}

	@Override
	public void onHardwareFocusButtonPressed() {
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		// lockControls = true;
		PluginManager.getInstance().OnFocusButtonClick();
	}

	private void shutterButtonPressed() {
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);
		lockControls = true;
		PluginManager.getInstance().OnShutterClick();
	}

	private void infoSlide(boolean toLeft, float XtoVisible, float XtoInvisible) {
		if ((infoSet == 0 & !toLeft) && isAnyViewOnViewfinder())
			infoSet = 2;
		else if (infoSet == 0 && !isAnyViewOnViewfinder())
			infoSet = 1;
		else if (isAnyViewOnViewfinder())
			infoSet = (infoSet + 1 * (toLeft ? 1 : -1)) % 3;
		else
			infoSet = 0;
		setInfo(toLeft, XtoVisible, XtoInvisible, true);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		prefs.edit().putInt("defaultInfoSet", infoSet).commit();
	}

	private void setInfo(boolean toLeft, float XtoVisible, float XtoInvisible,
			boolean isAnimate) {
		int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout)
				.getWidth();
		int infozoneWidth = guiView.findViewById(R.id.infoLayout).getWidth();
		int screenWidth = pluginzoneWidth + infozoneWidth;

		AnimationSet rlinvisible = new AnimationSet(true);
		rlinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet rlvisible = new AnimationSet(true);
		rlvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrinvisible = new AnimationSet(true);
		lrinvisible.setInterpolator(new DecelerateInterpolator());

		AnimationSet lrvisible = new AnimationSet(true);
		lrvisible.setInterpolator(new DecelerateInterpolator());

		int duration_invisible = isAnimate ? com.almalence.opencam.util.Util
				.clamp(Math.abs(Math.round(((toLeft ? XtoVisible
						: (screenWidth - XtoVisible)) * 500) / screenWidth)),
						10, 500) : 0;
		int duration_visible = isAnimate ? com.almalence.opencam.util.Util
				.clamp(Math.abs(Math.round(((toLeft ? XtoInvisible
						: (screenWidth - XtoInvisible)) * 500) / screenWidth)),
						10, 500) : 0;

		Animation invisible_alpha = new AlphaAnimation(1, 0);
		invisible_alpha.setDuration(duration_invisible);
		invisible_alpha.setRepeatCount(0);

		Animation visible_alpha = new AlphaAnimation(0, 1);
		visible_alpha.setDuration(duration_visible);
		visible_alpha.setRepeatCount(0);

		Animation rlinvisible_translate = new TranslateAnimation(XtoInvisible,
				-screenWidth, 0, 0);
		rlinvisible_translate.setDuration(duration_invisible);
		rlinvisible_translate.setFillAfter(true);

		Animation lrinvisible_translate = new TranslateAnimation(XtoInvisible,
				screenWidth, 0, 0);
		lrinvisible_translate.setDuration(duration_invisible);
		lrinvisible_translate.setFillAfter(true);

		Animation rlvisible_translate = new TranslateAnimation(XtoVisible, 0,
				0, 0);
		rlvisible_translate.setDuration(duration_visible);
		rlvisible_translate.setFillAfter(true);

		Animation lrvisible_translate = new TranslateAnimation(XtoVisible, 0,
				0, 0);
		lrvisible_translate.setDuration(duration_visible);
		lrvisible_translate.setFillAfter(true);

		// Add animations to appropriate set
		rlinvisible.addAnimation(invisible_alpha);
		rlinvisible.addAnimation(rlinvisible_translate);

		// rlvisible.addAnimation(visible_alpha);
		rlvisible.addAnimation(rlvisible_translate);

		lrinvisible.addAnimation(invisible_alpha);
		lrinvisible.addAnimation(lrinvisible_translate);

		// lrvisible.addAnimation(visible_alpha);
		lrvisible.addAnimation(lrvisible_translate);

		switch (infoSet) {
		case INFO_ALL: {
			hideSecondaryMenus();
			unselectPrimaryTopMenuButtons(-1);

			if (guiView.findViewById(R.id.paramsLayout).getVisibility() == View.GONE) {
				if (isAnimate)
					guiView.findViewById(R.id.paramsLayout).startAnimation(
							toLeft ? rlvisible : lrvisible);
				guiView.findViewById(R.id.paramsLayout).setVisibility(
						View.VISIBLE);
				((Panel) guiView.findViewById(R.id.topPanel)).reorder(false,
						true);
			}

			if (guiView.findViewById(R.id.pluginsLayout).getVisibility() == View.GONE) {
				if (isAnimate)
					guiView.findViewById(R.id.pluginsLayout).startAnimation(
							toLeft ? rlvisible : lrvisible);
				guiView.findViewById(R.id.pluginsLayout).setVisibility(
						View.VISIBLE);

				if (isAnimate)
					guiView.findViewById(R.id.infoLayout).startAnimation(
							toLeft ? rlvisible : lrvisible);
				guiView.findViewById(R.id.infoLayout).setVisibility(
						View.VISIBLE);
			}

			if (guiView.findViewById(R.id.fullscreenLayout).getVisibility() == View.GONE) {
				if (isAnimate)
					guiView.findViewById(R.id.fullscreenLayout).startAnimation(
							toLeft ? rlvisible : lrvisible);
				guiView.findViewById(R.id.fullscreenLayout).setVisibility(
						View.VISIBLE);
			}
			break;
		}

		case INFO_NO: {
			hideSecondaryMenus();
			unselectPrimaryTopMenuButtons(-1);

			if (guiView.findViewById(R.id.paramsLayout).getVisibility() == View.VISIBLE) {
				if (isAnimate)
					guiView.findViewById(R.id.paramsLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.paramsLayout)
						.setVisibility(View.GONE);
				((Panel) guiView.findViewById(R.id.topPanel)).reorder(true,
						true);
			}
			if (guiView.findViewById(R.id.pluginsLayout).getVisibility() == View.VISIBLE) {
				if (isAnimate)
					guiView.findViewById(R.id.pluginsLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.pluginsLayout).setVisibility(
						View.GONE);

				if (isAnimate)
					guiView.findViewById(R.id.infoLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.infoLayout).setVisibility(View.GONE);
			}
			if (guiView.findViewById(R.id.fullscreenLayout).getVisibility() == View.VISIBLE) {
				if (isAnimate)
					guiView.findViewById(R.id.fullscreenLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.fullscreenLayout).setVisibility(
						View.GONE);
			}
			break;
		}

		case INFO_PARAMS: {
			hideSecondaryMenus();
			unselectPrimaryTopMenuButtons(-1);

			if (guiView.findViewById(R.id.paramsLayout).getVisibility() == View.GONE) {
				if (isAnimate)
					guiView.findViewById(R.id.paramsLayout).startAnimation(
							toLeft ? rlvisible : lrvisible);
				guiView.findViewById(R.id.paramsLayout).setVisibility(
						View.VISIBLE);
				((Panel) guiView.findViewById(R.id.topPanel)).reorder(false,
						true);
			}
			if (guiView.findViewById(R.id.pluginsLayout).getVisibility() == View.VISIBLE) {
				if (isAnimate)
					guiView.findViewById(R.id.pluginsLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.pluginsLayout).setVisibility(
						View.GONE);

				if (isAnimate)
					guiView.findViewById(R.id.infoLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.infoLayout).setVisibility(View.GONE);
			}
			if (guiView.findViewById(R.id.fullscreenLayout).getVisibility() == View.VISIBLE) {
				if (isAnimate)
					guiView.findViewById(R.id.fullscreenLayout).startAnimation(
							toLeft ? rlinvisible : lrinvisible);
				guiView.findViewById(R.id.fullscreenLayout).setVisibility(
						View.GONE);
			}
			break;
		}

		}

		rlinvisible.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				guiView.findViewById(R.id.paramsLayout).clearAnimation();
				guiView.findViewById(R.id.pluginsLayout).clearAnimation();
				guiView.findViewById(R.id.fullscreenLayout).clearAnimation();
				guiView.findViewById(R.id.infoLayout).clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});

		lrinvisible.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				guiView.findViewById(R.id.paramsLayout).clearAnimation();
				guiView.findViewById(R.id.pluginsLayout).clearAnimation();
				guiView.findViewById(R.id.fullscreenLayout).clearAnimation();
				guiView.findViewById(R.id.infoLayout).clearAnimation();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});

		// checks preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putInt("defaultInfoSet", infoSet);
		prefsEditor.commit();
	}

	// Method used by quick controls customization feature. Swaps current quick
	// control with selected from qc grid.
	private void switchViews(View currentView, View newView, String qcID) {
		if (currentView == newView)
			return;

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);

		int currentQCNumber = -1;
		String currentViewID = "";
		if (currentView == quickControl1) {
			currentViewID = pref.getString("quickControlButton1", "");
			currentQCNumber = 1;
		} else if (currentView == quickControl2) {
			currentViewID = pref.getString("quickControlButton2", "");
			currentQCNumber = 2;
		} else if (currentView == quickControl3) {
			currentViewID = pref.getString("quickControlButton3", "");
			currentQCNumber = 3;
		} else if (currentView == quickControl4) {
			currentViewID = pref.getString("quickControlButton4", "");
			currentQCNumber = 4;
		}

		if (newView == quickControl1) {
			quickControl1 = currentView;
			pref.edit().putString("quickControlButton1", currentViewID)
					.commit();
		} else if (newView == quickControl2) {
			quickControl2 = currentView;
			pref.edit().putString("quickControlButton2", currentViewID)
					.commit();
		} else if (newView == quickControl3) {
			quickControl3 = currentView;
			pref.edit().putString("quickControlButton3", currentViewID)
					.commit();
		} else if (newView == quickControl4) {
			quickControl4 = currentView;
			pref.edit().putString("quickControlButton4", currentViewID)
					.commit();
		} else {
			if (currentView.getParent() != null)
				((ViewGroup) currentView.getParent()).removeView(currentView);
		}

		switch (currentQCNumber) {
		case 1:
			quickControl1 = newView;
			pref.edit().putString("quickControlButton1", qcID).commit();
			break;
		case 2:
			quickControl2 = newView;
			pref.edit().putString("quickControlButton2", qcID).commit();
			break;
		case 3:
			quickControl3 = newView;
			pref.edit().putString("quickControlButton3", qcID).commit();
			break;
		case 4:
			quickControl4 = newView;
			pref.edit().putString("quickControlButton4", qcID).commit();
			break;
		}
	}

	private void recreateQuickControlsMenu() {
		removeAllViews(topMenuButtons);
		removeAllViews(topMenuPluginButtons);
		removeAllQuickViews();

		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl1);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl2);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl3);
		((LinearLayout) guiView.findViewById(R.id.paramsLayout))
				.addView(quickControl4);
	}

	private void removeAllQuickViews() {
		if (quickControl1 != null && quickControl1.getParent() != null)
			((ViewGroup) quickControl1.getParent()).removeView(quickControl1);
		if (quickControl2 != null && quickControl2.getParent() != null)
			((ViewGroup) quickControl2.getParent()).removeView(quickControl2);
		if (quickControl3 != null && quickControl3.getParent() != null)
			((ViewGroup) quickControl3.getParent()).removeView(quickControl3);
		if (quickControl4 != null && quickControl4.getParent() != null)
			((ViewGroup) quickControl4.getParent()).removeView(quickControl4);
	}

	private void removeAllViews(Map<?, View> buttons) {
		Collection<View> button_set = buttons.values();
		Iterator<View> it = button_set.iterator();
		while (it.hasNext()) {
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);
		}
	}

	private void removePluginViews() {
		List<View> pluginsView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.pluginsLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			pluginsView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < pluginsView.size(); j++) {
			View view = pluginsView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		List<View> fullscreenView = new ArrayList<View>();
		RelativeLayout fullscreenLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.fullscreenLayout);
		for (int i = 0; i < fullscreenLayout.getChildCount(); i++)
			fullscreenView.add(fullscreenLayout.getChildAt(i));

		for (int j = 0; j < fullscreenView.size(); j++) {
			View view = fullscreenView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			fullscreenLayout.removeView(view);
		}

		// List<View> specialPluginsView = new ArrayList<View>();
		// RelativeLayout specialPluginsLayout = (RelativeLayout)
		// MainScreen.thiz
		// .findViewById(R.id.specialPluginsLayout);
		// for (int i = 0; i < specialPluginsLayout.getChildCount(); i++)
		// specialPluginsView.add(specialPluginsLayout.getChildAt(i));
		//
		// for (int j = 0; j < specialPluginsView.size(); j++) {
		// View view = specialPluginsView.get(j);
		// if (view.getParent() != null)
		// ((ViewGroup) view.getParent()).removeView(view);
		//
		// specialPluginsLayout.removeView(view);
		// }

		List<View> infoView = new ArrayList<View>();
		LinearLayout infoLayout = (LinearLayout) MainScreen.thiz
				.findViewById(R.id.infoLayout);
		for (int i = 0; i < infoLayout.getChildCount(); i++)
			infoView.add(infoLayout.getChildAt(i));

		for (int j = 0; j < infoView.size(); j++) {
			View view = infoView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			infoLayout.removeView(view);
		}
	}

	public boolean onLongClick(View v) {
		if (quickControlsChangeVisible)
			return true;

		if (modeSelectorVisible)
			return true;

		changeCurrentQuickControl(v);

		initQuickControlsMenu(v);
		showQuickControlsSettings();
		guiView.findViewById(R.id.topPanel).setVisibility(View.GONE);
		return true;
	}

	@Override
	public void onClick(View v) {
		hideSecondaryMenus();
		if (!quickControlsChangeVisible) {
			if (topMenuPluginButtons.containsValue(v)) {
				Set<String> pluginIDset = topMenuPluginButtons.keySet();
				Iterator<String> it = pluginIDset.iterator();
				while (it.hasNext()) {
					String pluginID = it.next();
					if (v == topMenuPluginButtons.get(pluginID)) {
						Plugin plugin = PluginManager.getInstance().getPlugin(
								pluginID);
						plugin.onQuickControlClick();

						int icon_id = plugin.getQuickControlIconID();
						Drawable icon = MainScreen.mainContext.getResources()
								.getDrawable(icon_id);
						((RotateImageView) v).setImageDrawable(icon);

						initSettingsMenu();
						break;
					}
				}
			}
			return;
		}

		changeCurrentQuickControl(v);

		initQuickControlsMenu(v);
		showQuickControlsSettings();
	}

	@Override
	public void onButtonClick(View button) {
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.INVISIBLE);

		int id = button.getId();
		if (lockControls && ((R.id.buttonShutter != id)))
			return;

		// 1. if quick settings slider visible - lock everything
		// 2. if modes visible - allow only selectmode button
		// 3. if change quick controls visible - allow only OK button
		// if (settingsControlsVisible && ((R.id.buttonSelectMode != id) ||
		// (R.id.buttonShutter != id) || (R.id.buttonGallery != id) ))
		// hideModeList();
		//
		if (settingsControlsVisible || quickControlsChangeVisible
				|| (modeSelectorVisible && (R.id.buttonSelectMode != id))) {
			if (quickControlsChangeVisible) {// if change control visible and
												// quick control button pressed
				if ((button != quickControl1) && (button != quickControl2)
						&& (button != quickControl3)
						&& (button != quickControl4))
					closeQuickControlsSettings();
			}
			if (settingsControlsVisible) {
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false,
						true);
				return;
			}
			if (modeSelectorVisible) {
				hideModeList();
				return;
			}
		}

		switch (id) {
		// BOTTOM BUTTONS - Modes, Shutter
		case R.id.buttonSelectMode: {
			if (quickControlsChangeVisible || settingsControlsVisible)
				break;

			if (!modeSelectorVisible)
				showModeList();
			else
				hideModeList();
		}
			break;

		case R.id.buttonShutter: {
			if (quickControlsChangeVisible || settingsControlsVisible)
				break;

			shutterButtonPressed();
		}
			break;

		case R.id.buttonGallery: {
			if (quickControlsChangeVisible || settingsControlsVisible)
				break;

			openGallery();
			break;
		}

		// TOP MENU BUTTONS - Scene mode, white balance, focus mode, flash mode,
		// settings
		case R.id.evButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			if (!isEVEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			LinearLayout Layout = (LinearLayout) guiView
					.findViewById(R.id.evLayout);
			if (Layout.getVisibility() == View.GONE) {
				unselectPrimaryTopMenuButtons(MODE_EV);
				hideSecondaryMenus();
				showParams(MODE_EV);
				quickControlsVisible = true;
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
		}
			break;
		case R.id.sceneButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			if (!isSceneEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			RelativeLayout Layout = (RelativeLayout) guiView
					.findViewById(R.id.scenemodeLayout);
			if (Layout.getVisibility() == View.GONE) {
				quickControlsVisible = true;
				unselectPrimaryTopMenuButtons(MODE_SCENE);
				hideSecondaryMenus();
				showParams(MODE_SCENE);
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
		}
			break;
		case R.id.wbButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			if (!isWBEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			RelativeLayout Layout = (RelativeLayout) guiView
					.findViewById(R.id.wbLayout);
			if (Layout.getVisibility() == View.GONE) {
				quickControlsVisible = true;
				unselectPrimaryTopMenuButtons(MODE_WB);
				hideSecondaryMenus();
				showParams(MODE_WB);
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
		}
			break;
		case R.id.focusButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			if (!isFocusEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			RelativeLayout Layout = (RelativeLayout) guiView
					.findViewById(R.id.focusmodeLayout);
			if (Layout.getVisibility() == View.GONE) {
				quickControlsVisible = true;
				unselectPrimaryTopMenuButtons(MODE_FOCUS);
				hideSecondaryMenus();
				showParams(MODE_FOCUS);
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
		}
			break;
		case R.id.flashButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			// hideSecondaryMenus();
			if (!isFlashEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			RelativeLayout Layout = (RelativeLayout) guiView
					.findViewById(R.id.flashmodeLayout);
			if (Layout.getVisibility() == View.GONE) {
				quickControlsVisible = true;
				unselectPrimaryTopMenuButtons(MODE_FLASH);
				hideSecondaryMenus();
				showParams(MODE_FLASH);
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
			// // switch flash parameters by touch
			// List<String> supported_flash = activeFlashNames;
			//
			// int idx = supported_flash.indexOf(mFlashMode);
			// idx++;
			// if (supported_flash.size() <= idx)
			// idx = 0;
			//
			// String newFlashMode = supported_flash.get(idx);
			// setFlashMode(newFlashMode);
		}
			break;
		case R.id.isoButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			if (!isIsoEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			RelativeLayout Layout = (RelativeLayout) guiView
					.findViewById(R.id.isoLayout);
			if (Layout.getVisibility() == View.GONE) {
				quickControlsVisible = true;
				unselectPrimaryTopMenuButtons(MODE_ISO);
				hideSecondaryMenus();
				showParams(MODE_ISO);
			} else {
				quickControlsVisible = false;
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
			}
		}
			break;
		case R.id.camerachangeButton: {
			if (quickControlsChangeVisible) {
				changeCurrentQuickControl(button);
				initQuickControlsMenu(button);
				showQuickControlsSettings();
				break;
			}

			unselectPrimaryTopMenuButtons(-1);
			hideSecondaryMenus();

			if (!isCameraChangeEnabled) {
				showToast(
						null,
						Toast.LENGTH_SHORT,
						Gravity.CENTER,
						MainScreen.thiz.getResources().getString(
								R.string.settings_not_available), true, false);
				break;
			}

			CameraSwitched(true);
		}
			break;

		// EXPOSURE COMPENSATION BUTTONS (-\+)
		case R.id.evMinusButton: {
			SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
			if (evBar != null) {
				int minValue = MainScreen.thiz.getMinExposureCompensation();
				int step = 1;
				int currProgress = evBar.getProgress();
				int iEv = currProgress - step;
				if (iEv < 0)
					iEv = 0;
				Camera camera = MainScreen.thiz.getCamera();
				if (null != camera) {
					Camera.Parameters params = camera.getParameters();
					params.setExposureCompensation(iEv + minValue);
					camera.setParameters(params);
				}

				preferences
						.edit()
						.putInt(sEvPref,
								Math.round((iEv + minValue)
										* MainScreen.thiz
												.getExposureCompensationStep()))
						.commit();

				evBar.setProgress(iEv);
			}
		}
			break;
		case R.id.evPlusButton: {
			SeekBar evBar = (SeekBar) guiView.findViewById(R.id.evSeekBar);
			if (evBar != null) {
				int minValue = MainScreen.thiz.getMinExposureCompensation();
				int maxValue = MainScreen.thiz.getMaxExposureCompensation();

				int step = 1;

				int currProgress = evBar.getProgress();
				int iEv = currProgress + step;
				if (iEv > maxValue - minValue)
					iEv = maxValue - minValue;
				Camera camera = MainScreen.thiz.getCamera();
				if (null != camera) {
					Camera.Parameters params = camera.getParameters();
					params.setExposureCompensation(iEv + minValue);
					camera.setParameters(params);
				}

				preferences
						.edit()
						.putInt(sEvPref,
								Math.round((iEv + minValue)
										* MainScreen.thiz
												.getExposureCompensationStep()))
						.commit();

				evBar.setProgress(iEv);
			}
		}
			break;
		}
		this.initSettingsMenu();
	}

	private void setSceneMode(String newMode) {
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null) {
			params.setSceneMode(newMode);
			MainScreen.thiz.setCameraParameters(params);
			mSceneMode = newMode;

			// After change scene mode it may be changed other stuff such as
			// flash, wb, focus mode.
			// Need to get this information and update state of current
			// parameters.
			params = MainScreen.thiz.getCameraParameters();
			String sceneNew = params.getSceneMode();
			String wbNew = params.getWhiteBalance();
			String flashNew = params.getFlashMode();
			String focusNew = params.getFocusMode();
			String isoNew = params.get(isoParam);

			// Save new params value
			mWB = wbNew;
			mFocusMode = focusNew;
			mFlashMode = flashNew;
			mISO = isoNew;

			// Set appropriate params buttons pressed
			setButtonSelected(SceneModeButtons, mSceneMode);
			setButtonSelected(WBModeButtons, mWB);
			setButtonSelected(FocusModeButtons, mFocusMode);
			setButtonSelected(FlashModeButtons, mFlashMode);
			setButtonSelected(ISOButtons, mISO);

			// Update icons for other camera parameter buttons
			RotateImageView but = null;
			int icon_id = -1;
			if (mWB != null) {
				but = (RotateImageView) topMenuButtons.get(MODE_WB);
				icon_id = icons_wb.get(mWB);
				but.setImageResource(icon_id);
				preferences.edit().putString(sWBModePref, mWB).commit();

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_WB_CHANGED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
			}
			if (mFocusMode != null) {
				but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
				icon_id = icons_focus.get(mFocusMode);
				but.setImageResource(icon_id);
				preferences
						.edit()
						.putString(
								MainScreen.getCameraMirrored() ? sRearFocusModePref
										: sFrontFocusModePref, mFocusMode)
						.commit();

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_FOCUS_CHANGED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
			}
			if (mFlashMode != null) {
				but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
				icon_id = icons_flash.get(mFlashMode);
				but.setImageResource(icon_id);
				preferences.edit().putString(sFlashModePref, mFlashMode)
						.commit();

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_FLASH_CHANGED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
			}
			if (mISO != null) {
				but = (RotateImageView) topMenuButtons.get(MODE_ISO);
				icon_id = icons_iso.get(mISO);
				but.setImageResource(icon_id);
				preferences.edit().putString(sISOPref, mISO).commit();

				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_ISO_CHANGED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
			}
			preferences.edit().putString(sSceneModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_SCENE);
		int icon_id = icons_scene.get(mSceneMode);
		but.setImageResource(icon_id);

		initSettingsMenu();
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_SCENE_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void setWhiteBalance(String newMode) {
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null) {
			if ((mSceneMode != sceneAuto || mWB != newMode)
					&& MainScreen.thiz.getSupportedSceneModes() != null) {
				setSceneMode(sceneAuto);
				params = MainScreen.thiz.getCameraParameters();
			}

			params = MainScreen.thiz.getCameraParameters();
			params.setWhiteBalance(newMode);
			MainScreen.thiz.setCameraParameters(params);
			mWB = newMode;
			setButtonSelected(WBModeButtons, mWB);

			preferences.edit().putString(sWBModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_WB);
		int icon_id = icons_wb.get(mWB);
		but.setImageResource(icon_id);

		initSettingsMenu();
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_WB_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void setFocusMode(String newMode) {
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null) {
			if (mSceneMode != sceneAuto && mFocusMode != focusAuto) {
				if (MainScreen.thiz.mSceneModeSupported)
					setSceneMode(sceneAuto);
				params = MainScreen.thiz.getCameraParameters();
			}

			params = MainScreen.thiz.getCameraParameters();
			params.setFocusMode(newMode);
			MainScreen.thiz.setCameraParameters(params);
			mFocusMode = newMode;
			setButtonSelected(FocusModeButtons, mFocusMode);

			preferences
					.edit()
					.putString(
							MainScreen.getCameraMirrored() ? sRearFocusModePref
									: sFrontFocusModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FOCUS);
		int icon_id = icons_focus.get(mFocusMode);
		but.setImageResource(icon_id);

		initSettingsMenu();
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_FOCUS_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void setFlashMode(String newMode) {
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null) {
			if (mSceneMode != sceneAuto && mFlashMode != flashAuto) {
				setSceneMode(sceneAuto);
				params = MainScreen.thiz.getCameraParameters();
			}

			params = MainScreen.thiz.getCameraParameters();
			params.setFlashMode(newMode);
			MainScreen.thiz.setCameraParameters(params);
			mFlashMode = newMode;
			setButtonSelected(FlashModeButtons, mFlashMode);

			preferences.edit().putString(sFlashModePref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_FLASH);
		int icon_id = icons_flash.get(mFlashMode);
		but.setImageResource(icon_id);

		initSettingsMenu();
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_FLASH_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void setISO(String newMode) {
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params != null) {
			if ((mSceneMode != sceneAuto || mISO != newMode)
					&& MainScreen.thiz.getSupportedSceneModes() != null) {
				setSceneMode(sceneAuto);
				params = MainScreen.thiz.getCameraParameters();
			}

			params.set(isoParam, newMode);
			MainScreen.thiz.setCameraParameters(params);
			mISO = newMode;
			setButtonSelected(ISOButtons, mISO);

			preferences.edit().putString(sISOPref, newMode).commit();
		}

		RotateImageView but = (RotateImageView) topMenuButtons.get(MODE_ISO);
		int icon_id = icons_iso.get(mISO);
		but.setImageResource(icon_id);

		initSettingsMenu();
		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_ISO_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	// Hide all pop-up layouts
	private void unselectPrimaryTopMenuButtons(int iTopMenuButtonSelected) {
		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext()) {
			Integer it_button = it.next();
			(topMenuButtons.get(it_button)).setPressed(false);
			(topMenuButtons.get(it_button)).setSelected(false);
		}

		if ((iTopMenuButtonSelected > -1)
				&& topMenuButtons.containsKey(iTopMenuButtonSelected)) {
			RotateImageView pressed_button = (RotateImageView) topMenuButtons
					.get(iTopMenuButtonSelected);
			pressed_button.setPressed(false);
			pressed_button.setSelected(true);
		}
	}

	private void topMenuButtonPressed(int iTopMenuButtonPressed) {

		Set<Integer> keys = topMenuButtons.keySet();
		Iterator<Integer> it = keys.iterator();
		while (it.hasNext()) {
			Integer it_button = it.next();
			if (isTopMenuButtonEnabled(it_button))
				(topMenuButtons.get(it_button))
						.setBackgroundDrawable(MainScreen.mainContext
								.getResources().getDrawable(
										R.drawable.transparent_background));
		}

		if ((iTopMenuButtonPressed > -1 && isTopMenuButtonEnabled(iTopMenuButtonPressed))
				&& topMenuButtons.containsKey(iTopMenuButtonPressed)) {
			RotateImageView pressed_button = (RotateImageView) topMenuButtons
					.get(iTopMenuButtonPressed);
			// pressed_button.setBackgroundEnabled(true);
			pressed_button
					.setBackgroundDrawable(MainScreen.mainContext
							.getResources()
							.getDrawable(
									R.drawable.almalence_gui_button_background_pressed));

		}
	}

	public boolean isTopMenuButtonEnabled(int iTopMenuButtonKey) {
		boolean isEnabled = false;
		switch (iTopMenuButtonKey) {
		case MODE_SCENE:
			if (isSceneEnabled)
				isEnabled = true;
			break;
		case MODE_WB:
			if (isWBEnabled)
				isEnabled = true;
			break;
		case MODE_FOCUS:
			if (isFocusEnabled)
				isEnabled = true;
			break;
		case MODE_FLASH:
			if (isFlashEnabled)
				isEnabled = true;
			break;
		case MODE_EV:
			if (isEVEnabled)
				isEnabled = true;
			break;
		case MODE_ISO:
			if (isIsoEnabled)
				isEnabled = true;
			break;
		case MODE_CAM:
			if (isCameraChangeEnabled)
				isEnabled = true;
			break;
		}

		return isEnabled;
	}

	// Hide all pop-up layouts
	@Override
	public void hideSecondaryMenus() {
		if (!isSecondaryMenusVisible())
			return;

		guiView.findViewById(R.id.evLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.scenemodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.wbLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.focusmodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.flashmodeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.isoLayout).setVisibility(View.GONE);

		guiView.findViewById(R.id.modeLayout).setVisibility(View.GONE);
		guiView.findViewById(R.id.vfLayout).setVisibility(View.GONE);

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	public boolean isSecondaryMenusVisible() {
		if (guiView.findViewById(R.id.evLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.scenemodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.wbLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.focusmodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.flashmodeLayout).getVisibility() == View.VISIBLE
				|| guiView.findViewById(R.id.isoLayout).getVisibility() == View.VISIBLE)
			return true;
		return false;
	}

	// Decide what layout to show when some main's parameters button is clicked
	private void showParams(int iButton) {
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		int width = metrics.widthPixels;
		int modeHeightByWidth = (int) (width / 3 - 5 * metrics.density);
		int modeHeightByDimen = Math.round(MainScreen.thiz.getResources()
				.getDimension(R.dimen.gridImageSize)
				+ MainScreen.thiz.getResources().getDimension(
						R.dimen.gridTextLayoutSize));

		int modeHeight = modeHeightByDimen > modeHeightByWidth ? modeHeightByWidth
				: modeHeightByDimen;

		AbsListView.LayoutParams params = new AbsListView.LayoutParams(
				LayoutParams.WRAP_CONTENT, modeHeight);

		List<View> views = null;
		switch (iButton) {
		case MODE_SCENE:
			views = activeScene;
			break;
		case MODE_WB:
			views = activeWB;
			break;
		case MODE_FOCUS:
			views = activeFocus;
			break;
		case MODE_FLASH:
			views = activeFlash;
			break;
		case MODE_ISO:
			views = activeISO;
			break;
		}

		if (views != null) {
			for (int i = 0; i < views.size(); i++) {
				View param = views.get(i);
				param.setLayoutParams(params);
			}
		}

		switch (iButton) {
		case MODE_EV:
			guiView.findViewById(R.id.evLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_SCENE:
			guiView.findViewById(R.id.scenemodeLayout).setVisibility(
					View.VISIBLE);
			break;
		case MODE_WB:
			guiView.findViewById(R.id.wbLayout).setVisibility(View.VISIBLE);
			break;
		case MODE_FOCUS:
			guiView.findViewById(R.id.focusmodeLayout).setVisibility(
					View.VISIBLE);
			break;
		case MODE_FLASH:
			guiView.findViewById(R.id.flashmodeLayout).setVisibility(
					View.VISIBLE);
			break;
		case MODE_ISO:
			guiView.findViewById(R.id.isoLayout).setVisibility(View.VISIBLE);
			break;
		}

		quickControlsVisible = true;

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	private void setButtonSelected(Map<String, View> buttonsList,
			String mode_name) {
		Set<String> keys = buttonsList.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String it_button = it.next();
			((RelativeLayout) buttonsList.get(it_button)).setPressed(false);
		}

		if ((mode_name != null) && buttonsList.containsKey(mode_name)) {
			RelativeLayout pressed_button = (RelativeLayout) buttonsList
					.get(mode_name);
			pressed_button.setPressed(false);
		}
	}

	private void setCameraParameterValue(int iParameter, String sValue) {
		switch (iParameter) {
		case MODE_SCENE:
			mSceneMode = sValue;
			break;
		case MODE_WB:
			mWB = sValue;
			break;
		case MODE_FOCUS:
			mFocusMode = sValue;
			break;
		case MODE_FLASH:
			mFlashMode = sValue;
			break;
		case MODE_ISO:
			mISO = sValue;
			break;
		}
	}

	/************************************************************************************
	 * 
	 * Methods for adding Viewfinder plugin's controls and informational
	 * controls from other plugins
	 * 
	 ***********************************************************************************/
	private void addInfoControl(View info_control) {
		// Calculate appropriate size of added plugin's view
		android.widget.LinearLayout.LayoutParams viewLayoutParams = (android.widget.LinearLayout.LayoutParams) info_control
				.getLayoutParams();
		viewLayoutParams = this.getTunedLinearLayoutParams(info_control,
				viewLayoutParams, iInfoViewMaxWidth, iInfoViewMaxHeight);

		((LinearLayout) guiView.findViewById(R.id.infoLayout)).addView(
				info_control, viewLayoutParams);
	}

	// Public interface for all plugins
	// Automatically decide where to put view and correct view's size if
	// necessary
	@Override
	protected void addPluginViews(Map<View, Plugin.ViewfinderZone> views_map) {
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext()) {
			
			try {
				View view = it.next();
				Plugin.ViewfinderZone desire_zone = views_map.get(view);
	
				android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
						.getLayoutParams();
				viewLayoutParams = this.getTunedPluginLayoutParams(view,
						desire_zone, viewLayoutParams);
	
				if (viewLayoutParams == null) // No free space on plugin's layout
					return;
	
				view.setLayoutParams(viewLayoutParams);
				if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
						|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
					((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout))
							.addView(view, 0,
									(ViewGroup.LayoutParams) viewLayoutParams);
				else
					((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
							.addView(view, viewLayoutParams);
			}
			catch (Exception e) {
				e.printStackTrace();
				Log.e("Almalence GUI", "addPluginViews exception: " + e.getMessage());
			}
		}
	}

	@Override
	public void addViewQuick(View view, Plugin.ViewfinderZone desire_zone) {
		android.widget.RelativeLayout.LayoutParams viewLayoutParams = (android.widget.RelativeLayout.LayoutParams) view
				.getLayoutParams();
		viewLayoutParams = this.getTunedPluginLayoutParams(view, desire_zone,
				viewLayoutParams);

		if (viewLayoutParams == null) // No free space on plugin's layout
			return;

		view.setLayoutParams(viewLayoutParams);
		if (desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_FULLSCREEN
				|| desire_zone == Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER)
			((RelativeLayout) guiView.findViewById(R.id.fullscreenLayout))
					.addView(view, 0, (ViewGroup.LayoutParams) viewLayoutParams);
		else
			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
					.addView(view, viewLayoutParams);
	}

	@Override
	protected void removePluginViews(Map<View, Plugin.ViewfinderZone> views_map) {
		Set<View> view_set = views_map.keySet();
		Iterator<View> it = view_set.iterator();
		while (it.hasNext()) {
			View view = it.next();
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
					.removeView(view);
		}
	}

	@Override
	public void removeViewQuick(View view) {
		if (view == null)
			return;
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);

		((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
				.removeView(view);
	}

	/* Private section for adding plugin's views */

	// INFO VIEW SECTION
	@Override
	protected void addInfoView(View view,
			android.widget.LinearLayout.LayoutParams viewLayoutParams) {
		if (((LinearLayout) guiView.findViewById(R.id.infoLayout))
				.getChildCount() != 0)
			viewLayoutParams.topMargin = 4;

		((LinearLayout) guiView.findViewById(R.id.infoLayout)).addView(view,
				viewLayoutParams);
	}

	@Override
	protected void removeInfoView(View view) {
		if (view.getParent() != null)
			((ViewGroup) view.getParent()).removeView(view);
		((LinearLayout) guiView.findViewById(R.id.infoLayout)).removeView(view);
	}

	protected boolean isAnyViewOnViewfinder() {
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.pluginsLayout);
		LinearLayout infoLayout = (LinearLayout) MainScreen.thiz
				.findViewById(R.id.infoLayout);
		RelativeLayout fullScreenLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.fullscreenLayout);

		return pluginsLayout.getChildCount() > 0
				|| infoLayout.getChildCount() > 0
				|| fullScreenLayout.getChildCount() > 0;
	}

	// selected mode - to use in onClick
	public Mode tmpActiveMode;

	// controls if info about new mode shown or not. to prevent from double info
	// private boolean modeChangedShown=false;
	private void initModeList() {
		modeViews.clear();
		buttonModeViewAssoc.clear();

		List<Mode> hash = ConfigParser.getInstance().getList();
		Iterator<Mode> it = hash.iterator();

		while (it.hasNext()) {
			Mode tmp = it.next();

			LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
			View mode = inflator.inflate(
					R.layout.gui_almalence_select_mode_grid_element, null,
					false);
			// set some mode icon
			((ImageView) mode.findViewById(R.id.modeImage))
					.setImageResource(MainScreen.thiz.getResources()
							.getIdentifier(tmp.icon, "drawable",
									MainScreen.thiz.getPackageName()));

			int id = MainScreen.thiz.getResources().getIdentifier(tmp.modeName,
					"string", MainScreen.thiz.getPackageName());
			String modename = MainScreen.thiz.getResources().getString(id);

			((TextView) mode.findViewById(R.id.modeText)).setText(modename);
			mode.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					hideModeList();

					// get mode associated with pressed button
					String key = buttonModeViewAssoc.get(v);
					Mode mode = ConfigParser.getInstance().getMode(key);
					// if selected the same mode - do not reinitialize camera
					// and other objects.
					if (PluginManager.getInstance().getActiveModeID() == mode.modeID)
						return;

					tmpActiveMode = mode;

					if (!MainScreen.thiz.checkLaunches(tmpActiveMode))
						return;

					new CountDownTimer(100, 100) {
						public void onTick(long millisUntilFinished) {
						}

						public void onFinish() {
							PluginManager.getInstance().switchMode(
									tmpActiveMode);
						}
					}.start();

					// set modes icon inside mode selection icon
					Bitmap bm = null;
					Bitmap iconBase = BitmapFactory.decodeResource(
							MainScreen.mainContext.getResources(),
							R.drawable.gui_almalence_select_mode);
					int id = MainScreen.thiz.getResources().getIdentifier(
							mode.icon, "drawable",
							MainScreen.thiz.getPackageName());
					Bitmap iconOverlay = BitmapFactory.decodeResource(
							MainScreen.mainContext.getResources(),
							MainScreen.thiz.getResources().getIdentifier(
									mode.icon, "drawable",
									MainScreen.thiz.getPackageName()));
					iconOverlay = Bitmap.createScaledBitmap(iconOverlay,
							(int) (iconBase.getWidth() / 1.8),
							(int) (iconBase.getWidth() / 1.8), false);

					bm = mergeImage(iconBase, iconOverlay);
					bm = Bitmap
							.createScaledBitmap(
									bm,
									(int) (MainScreen.mainContext
											.getResources()
											.getDimension(R.dimen.mainButtonHeightSelect)),
									(int) (MainScreen.mainContext
											.getResources()
											.getDimension(R.dimen.mainButtonHeightSelect)),
									false);
					((RotateImageView) guiView
							.findViewById(R.id.buttonSelectMode))
							.setImageBitmap(bm);

					int rid = MainScreen.thiz.getResources().getIdentifier(
							tmpActiveMode.howtoText, "string",
							MainScreen.thiz.getPackageName());
					String howto = "";
					if (rid != 0)
						howto = MainScreen.thiz.getResources().getString(rid);
					// show toast on mode changed
					showToast(
							v,
							Toast.LENGTH_SHORT,
							Gravity.CENTER,
							((TextView) v.findViewById(R.id.modeText))
									.getText()
									+ " "
									+ MainScreen.thiz.getResources().getString(
											R.string.almalence_gui_selected)
									+ (tmpActiveMode.howtoText.isEmpty() ? ""
											: "\n") + howto, false, true);
				}
			});
			buttonModeViewAssoc.put(mode, tmp.modeID);
			modeViews.add(mode);

			if (PluginManager.getInstance().getActiveModeID() == tmp.modeID) {
				mode.findViewById(R.id.modeImage).setBackgroundResource(
						R.drawable.thumbnail_background_selected_inner);
			}
		}

		modeAdapter.Elements = modeViews;
	}

	public void showToast(final View v, final int showLength,
			final int gravity, final String toastText,
			final boolean withBackground, final boolean startOffset) {
		MainScreen.thiz.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final RelativeLayout modeLayout = (RelativeLayout) guiView
						.findViewById(R.id.changeModeToast);

				DisplayMetrics metrics = new DisplayMetrics();
				MainScreen.thiz.getWindowManager().getDefaultDisplay()
						.getMetrics(metrics);
				int screen_height = metrics.heightPixels;
				int screen_width = metrics.widthPixels;

				RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) modeLayout
						.getLayoutParams();
				int[] rules = lp.getRules();
				if (gravity == Gravity.CENTER
						|| (mDeviceOrientation != 0 && mDeviceOrientation != 360))
					lp.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
				else {
					rules[RelativeLayout.CENTER_IN_PARENT] = 0;
					lp.addRule(RelativeLayout.CENTER_HORIZONTAL, 1);

					View shutter = guiView.findViewById(R.id.buttonShutter);
					int shutter_height = shutter.getHeight()
							+ shutter.getPaddingBottom();
					lp.setMargins(
							0,
							(int) (screen_height
									- MainScreen.thiz.getResources()
											.getDimension(
													R.dimen.paramsLayoutHeight) - shutter_height),
							0, shutter_height);
				}

				if (withBackground)
					modeLayout.setBackgroundDrawable(MainScreen.thiz
							.getResources().getDrawable(
									R.drawable.almalence_gui_toast_background));
				else
					modeLayout.setBackgroundDrawable(null);
				RotateImageView imgView = (RotateImageView) modeLayout
						.findViewById(R.id.selectModeIcon);
				TextView text = (TextView) modeLayout
						.findViewById(R.id.selectModeText);

				if (v != null) {
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView
							.getLayoutParams();
					pm.width = (int) MainScreen.thiz.getResources()
							.getDimension(R.dimen.mainButtonHeight);
					pm.height = (int) MainScreen.thiz.getResources()
							.getDimension(R.dimen.mainButtonHeight);
					imgView.setImageDrawable(((ImageView) v
							.findViewById(R.id.modeImage)).getDrawable());
				} else {
					RelativeLayout.LayoutParams pm = (RelativeLayout.LayoutParams) imgView
							.getLayoutParams();
					pm.width = 0;
					pm.height = 0;
				}
				text.setText("  " + toastText);
				text.setTextSize(16);
				text.setTextColor(Color.WHITE);

				text.setMaxWidth((int) Math.round(screen_width * 0.7));

				Animation visible_alpha = new AlphaAnimation(0, 1);
				visible_alpha.setStartOffset(startOffset ? 2000 : 0);
				visible_alpha.setDuration(1000);
				visible_alpha.setRepeatCount(0);

				final Animation invisible_alpha = new AlphaAnimation(1, 0);
				invisible_alpha
						.setStartOffset(showLength == Toast.LENGTH_SHORT ? 1000
								: 3000);
				invisible_alpha.setDuration(1000);
				invisible_alpha.setRepeatCount(0);

				visible_alpha.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						modeLayout.startAnimation(invisible_alpha);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationStart(Animation animation) {
					}
				});

				invisible_alpha.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						modeLayout.setVisibility(View.GONE);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationStart(Animation animation) {
					}
				});

				modeLayout.setRotation(-mDeviceOrientation);
				modeLayout.setVisibility(View.VISIBLE);
				modeLayout.bringToFront();
				modeLayout.startAnimation(visible_alpha);
			}
		});
	}

	// helper function to draw one bitmap on another
	private Bitmap mergeImage(Bitmap base, Bitmap overlay) {
		int adWDelta = (int) (base.getWidth() - overlay.getWidth()) / 2;
		int adHDelta = (int) (base.getHeight() - overlay.getHeight()) / 2;

		Bitmap mBitmap = Bitmap.createBitmap(base.getWidth(), base.getHeight(),
				Config.ARGB_8888);
		Canvas canvas = new Canvas(mBitmap);
		canvas.drawBitmap(base, 0, 0, null);
		canvas.drawBitmap(overlay, adWDelta, adHDelta, null);

		return mBitmap;
	}

	// Supplementary methods to find suitable size for plugin's view
	// For LINEARLAYOUT
	private android.widget.RelativeLayout.LayoutParams getTunedRelativeLayoutParams(
			View view, android.widget.RelativeLayout.LayoutParams currParams,
			int goodWidth, int goodHeight) {
		int viewHeight, viewWidth;

		if (currParams != null) {
			viewHeight = currParams.height;
			viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else {
			currParams = new android.widget.RelativeLayout.LayoutParams(
					goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	// For RELATIVELAYOUT
	private android.widget.LinearLayout.LayoutParams getTunedLinearLayoutParams(
			View view, android.widget.LinearLayout.LayoutParams currParams,
			int goodWidth, int goodHeight) {
		if (currParams != null) {
			int viewHeight = currParams.height;
			int viewWidth = currParams.width;

			if ((viewHeight > goodHeight || viewHeight <= 0) && goodHeight > 0)
				viewHeight = goodHeight;

			if ((viewWidth > goodWidth || viewWidth <= 0) && goodWidth > 0)
				viewWidth = goodWidth;

			currParams.width = viewWidth;
			currParams.height = viewHeight;

			view.setLayoutParams(currParams);
		} else {
			currParams = new android.widget.LinearLayout.LayoutParams(
					goodWidth, goodHeight);
			view.setLayoutParams(currParams);
		}

		return currParams;
	}

	/************************************************************************************************
	 * >>>>>>>>>
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * >>>>>>>>> BEGIN
	 ***********************************************************************************************/

	protected Plugin.ViewfinderZone zones[] = {
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_TOP_RIGHT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_RIGHT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_RIGHT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT,
			Plugin.ViewfinderZone.VIEWFINDER_ZONE_CENTER_LEFT };

	protected android.widget.RelativeLayout.LayoutParams getTunedPluginLayoutParams(
			View view, Plugin.ViewfinderZone desire_zone,
			android.widget.RelativeLayout.LayoutParams currParams) {
		int left = 0, right = 0, top = 0, bottom = 0;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.pluginsLayout);

		if (currParams == null)
			currParams = new android.widget.RelativeLayout.LayoutParams(
					getMinPluginViewWidth(), getMinPluginViewHeight());

		switch (desire_zone) {
		case VIEWFINDER_ZONE_TOP_LEFT: {
			left = 0;
			right = currParams.width;
			top = 0;
			bottom = currParams.height;

			currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT: {
			left = pluginLayout.getWidth() - currParams.width;
			right = pluginLayout.getWidth();
			top = 0;
			bottom = currParams.height;

			currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT: {
			left = 0;
			right = currParams.width;
			top = pluginLayout.getHeight() / 2 - currParams.height / 2;
			bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

			currParams.addRule(RelativeLayout.CENTER_VERTICAL);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT: {
			left = pluginLayout.getWidth() - currParams.width;
			right = pluginLayout.getWidth();
			top = pluginLayout.getHeight() / 2 - currParams.height / 2;
			bottom = pluginLayout.getHeight() / 2 + currParams.height / 2;

			currParams.addRule(RelativeLayout.CENTER_VERTICAL);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT: {
			left = 0;
			right = currParams.width;
			top = pluginLayout.getHeight() - currParams.height;
			bottom = pluginLayout.getHeight();

			currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT: {
			left = pluginLayout.getWidth() - currParams.width;
			right = pluginLayout.getWidth();
			top = pluginLayout.getHeight() - currParams.height;
			bottom = pluginLayout.getHeight();

			currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		}
			break;
		case VIEWFINDER_ZONE_FULLSCREEN: {
			currParams.width = MainScreen.thiz.getPreviewSize() != null ? MainScreen.thiz
					.getPreviewSize().width : 0;
			currParams.height = MainScreen.thiz.getPreviewSize() != null ? MainScreen.thiz
					.getPreviewSize().height : 0;
			currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			// if(currParams.width != currParams.height)
			// {
			// currParams.addRule(RelativeLayout.CENTER_IN_PARENT, 1);
			// }
			// else
			// {
			// currParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
			// }

			return currParams;
		}
		case VIEWFINDER_ZONE_CENTER: {
			if (currParams.width > iCenterViewMaxWidth)
				currParams.width = iCenterViewMaxWidth;

			if (currParams.height > iCenterViewMaxHeight)
				currParams.height = iCenterViewMaxHeight;

			currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			// if(currParams.width != currParams.height)
			// {
			// currParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			// }
			// else
			// {
			// currParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
			// int[] rules = currParams.getRules();
			// rules[RelativeLayout.CENTER_IN_PARENT] = 0;
			// }

			return currParams;
		}
		}

		return findFreeSpaceOnLayout(new Rect(left, top, right, bottom),
				currParams, desire_zone);
	}

	protected android.widget.RelativeLayout.LayoutParams findFreeSpaceOnLayout(
			Rect currRect,
			android.widget.RelativeLayout.LayoutParams currParams,
			Plugin.ViewfinderZone currZone) {
		boolean isFree = true;
		Rect childRect;

		// int child_left, child_right, child_top, child_bottom;
		View pluginLayout = MainScreen.thiz.findViewById(R.id.pluginsLayout);

		// Looking in all zones at clockwise direction for free place
		for (int j = Plugin.ViewfinderZone.getInt(currZone), counter = 0; j < zones.length; j++, counter++) {
			isFree = true;
			// Check intersections with already added views
			for (int i = 0; i < ((ViewGroup) pluginLayout).getChildCount(); ++i) {
				View nextChild = ((ViewGroup) pluginLayout).getChildAt(i);

				currRect = getPluginViewRectInZone(currParams, zones[j]);
				childRect = getPluginViewRect(nextChild);

				if (currRect.intersect(childRect)) {
					isFree = false;
					break;
				}
			}

			if (isFree) // Free zone has found
			{
				if (currZone == zones[j]) // Current zone is free
					return currParams;
				else {
					int rules[] = currParams.getRules();
					for (int i = 0; i < rules.length; i++)
						currParams.addRule(i, 0);

					switch (zones[j]) {
					case VIEWFINDER_ZONE_TOP_LEFT: {
						currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					}
						break;
					case VIEWFINDER_ZONE_TOP_RIGHT: {
						currParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					}
						break;
					case VIEWFINDER_ZONE_CENTER_LEFT: {
						currParams.addRule(RelativeLayout.CENTER_VERTICAL);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					}
						break;
					case VIEWFINDER_ZONE_CENTER_RIGHT: {
						currParams.addRule(RelativeLayout.CENTER_VERTICAL);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					}
						break;
					case VIEWFINDER_ZONE_BOTTOM_LEFT: {
						currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
					}
						break;
					case VIEWFINDER_ZONE_BOTTOM_RIGHT: {
						currParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						currParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					}
						break;
					default:
						break;
					}

					return currParams;
				}
			} else {
				if (counter == zones.length - 1) // Already looked in all zones
													// and they are all full
					return null;

				if (j == zones.length - 1) // If we started not from a first
											// zone (top-left)
					j = -1;
			}
		}

		return null;
	}

	protected Rect getPluginViewRectInZone(
			RelativeLayout.LayoutParams currParams, Plugin.ViewfinderZone zone) {
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.pluginsLayout);

		int viewWidth = currParams.width;
		int viewHeight = currParams.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		switch (zone) {
		case VIEWFINDER_ZONE_TOP_LEFT: {
			left = 0;
			right = viewWidth;
			top = 0;
			bottom = viewHeight;
		}
			break;
		case VIEWFINDER_ZONE_TOP_RIGHT: {
			left = layoutWidth - viewWidth;
			right = layoutWidth;
			top = 0;
			bottom = viewHeight;
		}
			break;
		case VIEWFINDER_ZONE_CENTER_LEFT: {
			left = 0;
			right = viewWidth;
			top = layoutHeight / 2 - viewHeight / 2;
			bottom = layoutHeight / 2 + viewHeight / 2;
		}
			break;
		case VIEWFINDER_ZONE_CENTER_RIGHT: {
			left = layoutWidth - viewWidth;
			right = layoutWidth;
			top = layoutHeight / 2 - viewHeight / 2;
			bottom = layoutHeight / 2 + viewHeight / 2;
		}
			break;
		case VIEWFINDER_ZONE_BOTTOM_LEFT: {
			left = 0;
			right = viewWidth;
			top = layoutHeight - viewHeight;
			bottom = layoutHeight;
		}
			break;
		case VIEWFINDER_ZONE_BOTTOM_RIGHT: {
			left = layoutWidth - viewWidth;
			right = layoutWidth;
			top = layoutHeight - viewHeight;
			bottom = layoutHeight;
		}
			break;
		default:
			break;
		}

		return new Rect(left, top, right, bottom);
	}

	protected Rect getPluginViewRect(View view) {
		int left = -1, right = -1, top = -1, bottom = -1;

		RelativeLayout pluginLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.pluginsLayout);
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view
				.getLayoutParams();
		int rules[] = lp.getRules();

		int viewWidth = lp.width;
		int viewHeight = lp.height;

		int layoutWidth = pluginLayout.getWidth();
		int layoutHeight = pluginLayout.getHeight();

		// Get X coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_LEFT] != 0) {
			left = 0;
			right = viewWidth;
		} else if (rules[RelativeLayout.ALIGN_PARENT_RIGHT] != 0) {
			left = layoutWidth - viewWidth;
			right = layoutWidth;
		}

		// Get Y coordinates
		if (rules[RelativeLayout.ALIGN_PARENT_TOP] != 0) {
			top = 0;
			bottom = viewHeight;
		} else if (rules[RelativeLayout.ALIGN_PARENT_BOTTOM] != 0) {
			top = layoutHeight - viewHeight;
			bottom = layoutHeight;
		} else if (rules[RelativeLayout.CENTER_VERTICAL] != 0) {
			top = layoutHeight / 2 - viewHeight / 2;
			bottom = layoutHeight / 2 + viewHeight / 2;
		}

		return new Rect(left, top, right, bottom);
	}

	// Supplementary methods for getting appropriate sizes for plugin's views
	@Override
	public int getMaxPluginViewHeight() {
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
				.getHeight() / 3;
	}

	@Override
	public int getMaxPluginViewWidth() {
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
				.getWidth() / 2;
	}

	@Override
	public int getMinPluginViewHeight() {
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
				.getHeight() / 12;
	}

	@Override
	public int getMinPluginViewWidth() {
		return ((RelativeLayout) guiView.findViewById(R.id.pluginsLayout))
				.getWidth() / 4;
	}

	/************************************************************************************************
	 * <<<<<<<<<<
	 * 
	 * DEFINITION OF PLACE ON LAYOUT FOR PLUGIN'S VIEWS
	 * 
	 * <<<<<<<<<< END
	 ***********************************************************************************************/

	private static float X = 0;

	private static float Xprev = 0;

	private static float Xoffset = 0;

	private static float XtoLeftInvisible = 0;
	private static float XtoLeftVisible = 0;

	private static float XtoRightInvisible = 0;
	private static float XtoRightVisible = 0;

	private MotionEvent prevEvent;
	private MotionEvent downEvent;
	private boolean scrolling = false;

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE) {
			if (event.getAction() == MotionEvent.ACTION_UP)
				guiView.findViewById(R.id.hintLayout).setVisibility(
						View.INVISIBLE);
			return true;
		}

		if (view == (LinearLayout) guiView.findViewById(R.id.evLayout)
				|| lockControls)
			return true;

		// to possibly slide-out top panel
		if (view == MainScreen.thiz.preview
				|| view == (View) MainScreen.thiz
						.findViewById(R.id.mainLayout1))
			((Panel) guiView.findViewById(R.id.topPanel)).touchListener
					.onTouch(view, event);

		else if (view.getParent() == (View) MainScreen.thiz
				.findViewById(R.id.paramsLayout) && !quickControlsChangeVisible) {

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				downEvent = MotionEvent.obtain(event);
				prevEvent = MotionEvent.obtain(event);
				scrolling = false;

				Set<Integer> keys = topMenuButtons.keySet();
				Iterator<Integer> it = keys.iterator();
				Integer pressed_button = -1;
				while (it.hasNext()) {
					Integer it_button = it.next();
					View v = topMenuButtons.get(it_button);
					if (v == view) {
						pressed_button = it_button;
						break;
					}
				}
				topMenuButtonPressed(pressed_button);

				return false;
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				topMenuButtonPressed(-1);
				if (scrolling == true)
					((Panel) guiView.findViewById(R.id.topPanel)).touchListener
							.onTouch(view, event);
				scrolling = false;
				if (prevEvent == null || downEvent == null)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_DOWN)
					return false;
				if (prevEvent.getAction() == MotionEvent.ACTION_MOVE) {
					if ((event.getY() - downEvent.getY()) < 50)
						return false;
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE
					&& scrolling == false) {
				if (downEvent == null)
					return false;
				if ((event.getY() - downEvent.getY()) < 50)
					return false;
				else {
					scrolling = true;
					((Panel) guiView.findViewById(R.id.topPanel)).touchListener
							.onTouch(view, downEvent);
				}
			}
			((Panel) guiView.findViewById(R.id.topPanel)).touchListener
					.onTouch(view, event);
		}

		// to allow quickControl's to process onClick, onLongClick
		if (view.getParent() == (View) MainScreen.thiz
				.findViewById(R.id.paramsLayout)) {
			return false;
		}

		boolean isMenuOpened = false;
		if (quickControlsChangeVisible || modeSelectorVisible
				|| settingsControlsVisible || isSecondaryMenusVisible())
			isMenuOpened = true;

		if (quickControlsChangeVisible
				&& view.getParent() != (View) MainScreen.thiz
						.findViewById(R.id.paramsLayout))
			closeQuickControlsSettings();

		if (modeSelectorVisible)
			hideModeList();

		hideSecondaryMenus();
		unselectPrimaryTopMenuButtons(-1);

		if (settingsControlsVisible)
			return true;
		else if (!isMenuOpened)
			// call onTouch of active vf and capture plugins
			PluginManager.getInstance().onTouch(view, event);

		RelativeLayout pluginLayout = (RelativeLayout) guiView
				.findViewById(R.id.pluginsLayout);
		RelativeLayout fullscreenLayout = (RelativeLayout) guiView
				.findViewById(R.id.fullscreenLayout);
		LinearLayout paramsLayout = (LinearLayout) guiView
				.findViewById(R.id.paramsLayout);
		LinearLayout infoLayout = (LinearLayout) guiView
				.findViewById(R.id.infoLayout);
		// OnTouch listener to show info and sliding grids
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			X = event.getX();
			Xoffset = X;
			Xprev = X;

			pluginLayout.clearAnimation();
			fullscreenLayout.clearAnimation();
			paramsLayout.clearAnimation();
			infoLayout.clearAnimation();

			Set<Integer> keys = topMenuButtons.keySet();
			Iterator<Integer> it = keys.iterator();
			Integer pressed_button = -1;
			while (it.hasNext()) {
				Integer it_button = it.next();
				View v = topMenuButtons.get(it_button);
				if (v == view) {
					pressed_button = it_button;
					break;
				}
			}
			topMenuButtonPressed(pressed_button);

			return true;
		}
		case MotionEvent.ACTION_UP: {
			float difX = event.getX();
			if ((X > difX) && (X - difX > 100)) {
				Log.i("AlamlenceGUI", "Move left");
				sliderLeftEvent();
				return true;
			} else if (X < difX && (difX - X > 100)) {
				Log.i("AlamlenceGUI", "Move right");
				sliderRightEvent();
				return true;
			}

			pluginLayout.clearAnimation();
			fullscreenLayout.clearAnimation();
			paramsLayout.clearAnimation();
			infoLayout.clearAnimation();

			topMenuButtonPressed(-1);

			break;
		}
		case MotionEvent.ACTION_MOVE: {
			int pluginzoneWidth = guiView.findViewById(R.id.pluginsLayout)
					.getWidth();
			int infozoneWidth = guiView.findViewById(R.id.infoLayout)
					.getWidth();
			int screenWidth = pluginzoneWidth + infozoneWidth;

			float difX = event.getX();

			Animation in_animation;
			Animation out_animation;
			Animation reverseout_animation;
			boolean toLeft;
			if (difX > Xprev) {
				out_animation = new TranslateAnimation(Xprev - Xoffset, difX
						- Xoffset, 0, 0);
				out_animation.setDuration(10);
				out_animation.setInterpolator(new LinearInterpolator());
				out_animation.setFillAfter(true);

				in_animation = new TranslateAnimation(Xprev - Xoffset
						- screenWidth, difX - Xoffset - screenWidth, 0, 0);
				in_animation.setDuration(10);
				in_animation.setInterpolator(new LinearInterpolator());
				in_animation.setFillAfter(true);

				reverseout_animation = new TranslateAnimation(difX
						+ (screenWidth - Xoffset), Xprev
						+ (screenWidth - Xoffset), 0, 0);
				reverseout_animation.setDuration(10);
				reverseout_animation.setInterpolator(new LinearInterpolator());
				reverseout_animation.setFillAfter(true);

				toLeft = false;

				XtoRightInvisible = difX - Xoffset;
				XtoRightVisible = difX - Xoffset - screenWidth;
			} else {
				out_animation = new TranslateAnimation(difX - Xoffset, Xprev
						- Xoffset, 0, 0);
				out_animation.setDuration(10);
				out_animation.setInterpolator(new LinearInterpolator());
				out_animation.setFillAfter(true);

				in_animation = new TranslateAnimation(screenWidth
						+ (Xprev - Xoffset), screenWidth + (difX - Xoffset), 0,
						0);
				in_animation.setDuration(10);
				in_animation.setInterpolator(new LinearInterpolator());
				in_animation.setFillAfter(true);

				reverseout_animation = new TranslateAnimation(Xprev - Xoffset
						- screenWidth, difX - Xoffset - screenWidth, 0, 0);
				reverseout_animation.setDuration(10);
				reverseout_animation.setInterpolator(new LinearInterpolator());
				reverseout_animation.setFillAfter(true);

				toLeft = true;

				XtoLeftInvisible = Xprev - Xoffset;
				XtoLeftVisible = screenWidth + (difX - Xoffset);
			}

			switch (infoSet) {
			case INFO_ALL: {
				pluginLayout.startAnimation(out_animation);
				fullscreenLayout.startAnimation(out_animation);
				infoLayout.startAnimation(out_animation);
				if ((difX < X) || !isAnyViewOnViewfinder())
					paramsLayout.startAnimation(out_animation);
			}
				break;
			case INFO_NO: {
				if ((toLeft && difX < X) || (!toLeft && difX > X))
					paramsLayout.startAnimation(in_animation);
				else
					paramsLayout.startAnimation(reverseout_animation);
				if (!toLeft && isAnyViewOnViewfinder()) {
					pluginLayout.startAnimation(in_animation);
					fullscreenLayout.startAnimation(in_animation);
					infoLayout.startAnimation(in_animation);
				} else if (toLeft && difX > X && isAnyViewOnViewfinder()) {
					pluginLayout.startAnimation(reverseout_animation);
					fullscreenLayout.startAnimation(reverseout_animation);
					infoLayout.startAnimation(reverseout_animation);
				}
			}
				break;
			case INFO_PARAMS: {
				if (difX > X)
					paramsLayout.startAnimation(out_animation);
				if (toLeft) {
					pluginLayout.startAnimation(in_animation);
					fullscreenLayout.startAnimation(in_animation);
					infoLayout.startAnimation(in_animation);
				} else if (difX < X) {
					pluginLayout.startAnimation(reverseout_animation);
					fullscreenLayout.startAnimation(reverseout_animation);
					infoLayout.startAnimation(reverseout_animation);
				}
			}
				break;
			}

			Xprev = Math.round(difX);

		}
			break;
		}
		return false;
	}

	private int getRelativeLeft(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft()
					+ getRelativeLeft((View) myView.getParent());
	}

	private int getRelativeTop(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + getRelativeTop((View) myView.getParent());
	}

	// @Override
	// public void autoFocus() {
	// MainScreen.thiz.autoFocus(MainScreen.thiz);
	// }

	// @Override
	// public void onAutoFocus(boolean focused, Camera paramCamera) {
	// PluginManager.getInstance().onAutoFocus(focused, paramCamera);
	// }

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		Camera camera = MainScreen.thiz.getCamera();
		if (null == camera)
			return;
		int iEv = progress - MainScreen.thiz.getMaxExposureCompensation();
		Camera.Parameters params = camera.getParameters();
		params.setExposureCompensation(iEv);
		camera.setParameters(params);

		preferences.edit().putInt(sEvPref, iEv).commit();

		mEV = iEv;

		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_EV_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	// Slider events handler

	private void sliderLeftEvent() {
		infoSlide(true, XtoLeftVisible, XtoLeftInvisible);
	}

	private void sliderRightEvent() {
		infoSlide(false, XtoRightVisible, XtoRightInvisible);
	}

	// shutter icons setter
	public void setShutterIcon(ShutterButton id) {
		if (id == ShutterButton.DEFAULT)
			((RotateImageView) guiView.findViewById(R.id.buttonShutter))
					.setImageResource(R.drawable.button_shutter);
		else if (id == ShutterButton.RECORDER_START)
			((RotateImageView) guiView.findViewById(R.id.buttonShutter))
					.setImageResource(R.drawable.button_shutter);
		else if (id == ShutterButton.RECORDER_STOP)
			((RotateImageView) guiView.findViewById(R.id.buttonShutter))
					.setImageResource(R.drawable.gui_almalence_shutter_pressed);
	}

	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event) {
		// hide hint screen
		if (guiView.findViewById(R.id.hintLayout).getVisibility() == View.VISIBLE)
			guiView.findViewById(R.id.hintLayout).setVisibility(View.INVISIBLE);

		int res = 0;
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (quickControlsChangeVisible) {
				closeQuickControlsSettings();
				res++;
				guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
			} else if (settingsControlsVisible) {
				((Panel) guiView.findViewById(R.id.topPanel)).setOpen(false,
						true);
				res++;
			} else if (modeSelectorVisible) {
				hideModeList();
				res++;
			} else if (quickControlsVisible) {
				unselectPrimaryTopMenuButtons(-1);
				hideSecondaryMenus();
				res++;
				guiView.findViewById(R.id.topPanel).setVisibility(View.VISIBLE);
				quickControlsVisible = false;
			}
		}

		if (keyCode == KeyEvent.KEYCODE_CAMERA /*
												 * || keyCode ==
												 * KeyEvent.KEYCODE_DPAD_CENTER
												 */) {
			if (settingsControlsVisible || quickControlsChangeVisible
					|| modeSelectorVisible) {
				if (quickControlsChangeVisible)
					closeQuickControlsSettings();
				if (settingsControlsVisible) {
					((Panel) guiView.findViewById(R.id.topPanel)).setOpen(
							false, true);
					return false;
				}
				if (modeSelectorVisible) {
					hideModeList();
					return false;
				}
			}
			shutterButtonPressed();
			return true;
		}

		// check if back button pressed and processing is in progress
		if (res == 0)
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (PluginManager.getInstance().getProcessingCounter() != 0) {
					// splash screen about processing
					AlertDialog.Builder builder = new AlertDialog.Builder(
							MainScreen.thiz)
							.setTitle("Processing...")
							.setMessage(
									MainScreen.thiz.getResources().getString(
											R.string.processing_not_finished))
							.setPositiveButton("Ok",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											// continue with delete
											dialog.cancel();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}

		return (res > 0 ? true : false);
	}

	private void openGallery() {
		if (mThumbnail == null)
			return;

		Uri uri = this.mThumbnail.getUri();

		Message message = new Message();
		message.arg1 = PluginManager.MSG_STOP_CAPTURE;
		message.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(message);

		if (!isUriValid(uri, MainScreen.thiz.getContentResolver())) {
			Log.e("AlmalenceGUI", "Uri invalid. uri=" + uri);
			return;
		}

		try {
			MainScreen.thiz.startActivity(new Intent(
					"com.android.camera.action.REVIEW", uri));
		} catch (ActivityNotFoundException ex) {
			try {
				MainScreen.thiz.startActivity(new Intent(Intent.ACTION_VIEW,
						uri));
			} catch (ActivityNotFoundException e) {
				Log.e("AlmalenceGUI", "review image fail. uri=" + uri, e);
			}
		}
	}

	public static boolean isUriValid(Uri uri, ContentResolver resolver) {
		if (uri == null)
			return false;

		try {
			ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
			if (pfd == null) {
				Log.e("AlmalenceGUI", "Fail to open URI. URI=" + uri);
				return false;
			}
			pfd.close();
		} catch (IOException ex) {
			return false;
		}

		return true;
	}

	// called to set any indication when export plugin work finished.
	public void onExportFinished() {
		// stop animation
		if (processingAnim != null) {
			processingAnim.clearAnimation();
			processingAnim.setVisibility(View.GONE);
		}
		RelativeLayout rl = (RelativeLayout) guiView
				.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.VISIBLE) {
			rl.setVisibility(View.GONE);
		}

		updateThumbnailButton();
		thumbnailView.invalidate();

		if (0 != PluginManager.getInstance().getProcessingCounter()) {
			new CountDownTimer(500, 500) {
				public void onTick(long millisUntilFinished) {
				}

				public void onFinish() {
					startProcessingAnimation();
				}
			}.start();
		}
	}

	public void onPostProcessingStarted() {
		guiView.findViewById(R.id.buttonGallery).setEnabled(false);
		guiView.findViewById(R.id.buttonShutter).setEnabled(false);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);
		guiView.findViewById(R.id.postprocessingLayout).setVisibility(
				View.VISIBLE);
		guiView.findViewById(R.id.postprocessingLayout).bringToFront();
		List<Plugin> processingPlugins = PluginManager.getInstance()
				.getActivePlugins(PluginType.Processing);
		if (processingPlugins.size() > 0) {
			View postProcessingView = processingPlugins.get(0)
					.getPostProcessingView();
			if (postProcessingView != null)
				((RelativeLayout) guiView
						.findViewById(R.id.postprocessingLayout))
						.addView(postProcessingView);
		}
	}

	public void onPostProcessingFinished() {
		List<View> postprocessingView = new ArrayList<View>();
		RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.thiz
				.findViewById(R.id.postprocessingLayout);
		for (int i = 0; i < pluginsLayout.getChildCount(); i++)
			postprocessingView.add(pluginsLayout.getChildAt(i));

		for (int j = 0; j < postprocessingView.size(); j++) {
			View view = postprocessingView.get(j);
			if (view.getParent() != null)
				((ViewGroup) view.getParent()).removeView(view);

			pluginsLayout.removeView(view);
		}

		guiView.findViewById(R.id.postprocessingLayout)
				.setVisibility(View.GONE);
		guiView.findViewById(R.id.buttonGallery).setEnabled(true);
		guiView.findViewById(R.id.buttonShutter).setEnabled(true);
		guiView.findViewById(R.id.buttonSelectMode).setEnabled(true);

		updateThumbnailButton();
		thumbnailView.invalidate();
	}

	private void updateThumbnailButton() {
		this.mThumbnail = Thumbnail.getLastThumbnail(MainScreen.thiz
				.getContentResolver());

		if (this.mThumbnail != null) {
			final Bitmap bitmap = this.mThumbnail.getBitmap();

			if (bitmap != null) {
				if (bitmap.getHeight() > 0 && bitmap.getWidth() > 0) {
					System.gc();

					try {
						Bitmap bm = Thumbnail
								.getRoundedCornerBitmap(
										bitmap,
										(int) (MainScreen.mainContext
												.getResources()
												.getDimension(R.dimen.mainButtonHeight)),
										(int) ((15.0f)));

						thumbnailView.setImageBitmap(bm);
					} catch (Exception e) {
						Log.v("AlmalenceGUI", "Can't set thumbnail");
					}
				}
			}
		} else {
			try {
				Bitmap bitmap = Bitmap.createBitmap(96, 96, Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				canvas.drawColor(Color.BLACK);
				Bitmap bm = Thumbnail.getRoundedCornerBitmap(bitmap,
						(int) (MainScreen.mainContext.getResources()
								.getDimension(R.dimen.mainButtonHeight)),
						(int) ((15.0f)));
				thumbnailView.setImageBitmap(bm);
			} catch (Exception e) {
				Log.v("AlmalenceGUI", "Can't set thumbnail");
			}
		}
	}

	private RotateImageView processingAnim;

	public void startProcessingAnimation() {
		processingAnim = ((RotateImageView) guiView
				.findViewById(R.id.buttonGallery2));
		processingAnim.setVisibility(View.VISIBLE);

		AnimationSet lrinvisible = new AnimationSet(true);
		lrinvisible.setInterpolator(new DecelerateInterpolator());

		int duration_invisible = 800;

		Animation invisible_alpha = new AlphaAnimation(1, 0);
		invisible_alpha.setDuration(duration_invisible);
		invisible_alpha.setRepeatCount(1000);

		int wid = thumbnailView.getWidth();

		Animation lrinvisible_translate = new TranslateAnimation(
				(int) (-thumbnailView.getHeight() / 2.9),
				(int) (thumbnailView.getHeight() / 2.2), 0, 0);
		lrinvisible_translate.setDuration(duration_invisible);
		lrinvisible_translate.setRepeatCount(1000);

		lrinvisible.addAnimation(invisible_alpha);
		lrinvisible.addAnimation(lrinvisible_translate);
		lrinvisible.setRepeatCount(1000);

		lrinvisible.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				processingAnim.clearAnimation();
				processingAnim.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});

		processingAnim.startAnimation(lrinvisible);
	}

	public void processingBlockUI() {
		RelativeLayout rl = (RelativeLayout) guiView
				.findViewById(R.id.blockingLayout);
		if (rl.getVisibility() == View.GONE) {
			rl.setVisibility(View.VISIBLE);
			rl.bringToFront();

			guiView.findViewById(R.id.buttonGallery).setEnabled(false);
			guiView.findViewById(R.id.buttonShutter).setEnabled(false);
			guiView.findViewById(R.id.buttonSelectMode).setEnabled(false);
		}
	}

	// capture indication - will play shutter icon opened/closed
	private boolean captureIndication = true;
	private boolean isIndicationOn = false;

	public void startContinuousCaptureIndication() {
		captureIndication = true;
		new CountDownTimer(200, 200) {
			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				if (captureIndication) {
					if (isIndicationOn) {
						((RotateImageView) guiView
								.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter);
						isIndicationOn = false;
					} else {
						((RotateImageView) guiView
								.findViewById(R.id.buttonShutter))
								.setImageResource(R.drawable.gui_almalence_shutter_pressed);
						isIndicationOn = true;
					}
					startContinuousCaptureIndication();
				}
			}
		}.start();
	}

	public void stopCaptureIndication() {
		captureIndication = false;
		((RotateImageView) guiView.findViewById(R.id.buttonShutter))
				.setImageResource(R.drawable.button_shutter);
	}

	public void showCaptureIndication() {
		new CountDownTimer(400, 200) {
			public void onTick(long millisUntilFinished) {
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setImageResource(R.drawable.gui_almalence_shutter_pressed);
			}

			public void onFinish() {
				((RotateImageView) guiView.findViewById(R.id.buttonShutter))
						.setImageResource(R.drawable.button_shutter);
			}
		}.start();
	}

	@Override
	public void onCameraSetup() {
	}

	@Override
	public void menuButtonPressed() {
	}

	@Override
	public void addMode(View mode) {
	}

	@Override
	public void SetModeSelected(View v) {
	}

	@Override
	public void hideModes() {
	}

	@Override
	public int getMaxModeViewWidth() {
		return -1;
	}

	@Override
	public int getMaxModeViewHeight() {
		return -1;
	}

	// @Override
	// public void autoFocus(){}
	// @Override
	// public void onAutoFocus(boolean focused, Camera paramCamera){}

	@Override
	@TargetApi(14)
	public void setFocusParameters() {
	}
}
