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

package com.almalence.plugins.processing.groupshot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.util.Size;

/***
Implements group shot processing
***/
@SuppressWarnings("deprecation")
public class GroupShotProcessingPlugin extends PluginProcessing implements OnTaskCompleteListener,
																 Handler.Callback, OnClickListener
{
	private long sessionID=0;
	
	private static final int MSG_PROGRESS_BAR_INVISIBLE = 1;
    private static final int MSG_PROGRESS_BAR_VISIBLE = 2;
    private static final int MSG_LEAVING = 3;
	private static final int MSG_END_OF_LOADING = 4;
    private static final int MSG_SELECTOR_VISIBLE = 5;
    private static final int MSG_SELECTOR_INVISIBLE = 6;
    
	static final int img2lay = 8; // 16		// image-to-layout subsampling factor
	
	static public int nFrames;						// number of input images
	static public int imgWidthFD;
	static public int imgHeightFD;
	static public int layWidth; 
	static public int layHeight;
	
	static public int previewBmpRealWidth;
	static public int previewBmpRealHeight;
	
	static Bitmap PreviewBmpInitial;
	static Bitmap PreviewBmp;
	private int mDisplayWidth;
	private int mDisplayHeight;
	
	private int mDisplayOrientationCurrent;
	private int mDisplayOrientationOnStartProcessing;
	private boolean mCameraMirrored;

	static long SaveTimeSt, SaveTimeEn;
	static long JpegTimeSt, JpegTimeEn;
	static long Prev1TimeSt, Prev1TimeEn;
	static long Prev2TimeSt, Prev2TimeEn;
	static long ProcTimeSt, ProcTimeEn;	
	
	static int OutNV21 = 0;
	
	static int[] mPixelsforPreview = null;
	
	static int mBaseFrame = 0;  // temporary
    static public byte[] manualLayout;
    static public int[] mArraryofFaceIndex;

	static int[] crop = new int[5];			// crop parameters and base image are stored here
	
	static public String[] filesSavedNames;
	static public int nFilesSaved;
	
	private ImageView mImgView;
	private Button mSaveButton;
	
	private int[][] mChosenFace;
	
	private final Handler mHandler = new Handler(this);
    
    private ProgressBar mProgressBar;   
	private Gallery mGallery;
    private TextView textVeiw;
	
	private Seamless mSeamless;
	
	/*
     * Group shot testing start
     */
	public static final int MAX_GS_FRAMES = 8; // 8 - is the same as in almashot-seamless.cpp
    public static int compressed_frame[] = new int[MAX_GS_FRAMES];
    public static int compressed_frame_len[] = new int[MAX_GS_FRAMES];
    public static ArrayList<byte[]> mJpegBufferList = new ArrayList<byte []>();
    public static ArrayList<Bitmap> mInputBitmapList = new ArrayList<Bitmap>();
    ArrayList<ArrayList <Rect>> mFaceList;
    
    public static final int MAX_FACE_DETECTED = 20;
    public static final float FACE_CONFIDENCE_LEVEL = 0.4f;
	
    private final Object syncObject = new Object();
	

    public static boolean SaveInputPreference;
    
    private boolean postProcessingRun = false;
    
	public GroupShotProcessingPlugin()
	{
		super("com.almalence.plugins.groupshotprocessing", 0, 0, null, null, 0, null);
	}

	@Override
	public void onStart()
	{
		getPrefs();
	}
	
	@Override
	public void onStartProcessing(long SessionID) 
	{
		Message msg = new Message();
		msg.what = PluginManager.MSG_PROCESSING_BLOCK_UI;
		MainScreen.H.sendMessage(msg);	
		
		Message msg2 = new Message();
		msg2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
		msg2.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg2);
		
		MainScreen.guiManager.lockControls = true;
		
		sessionID=SessionID;
		
        getPrefs();
        
        Display display = ((WindowManager) MainScreen.thiz.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	mDisplayWidth = display.getHeight();
    	mDisplayHeight = display.getWidth();
    	
    	mDisplayOrientationOnStartProcessing = MainScreen.guiManager.getDisplayOrientation();
    	mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
    	mCameraMirrored = MainScreen.getCameraMirrored();
        
        if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
        {
        	imgWidthFD = Seamless.getInstance().getWidthForFaceDetection(MainScreen.getImageHeight(), MainScreen.getImageWidth());
        	imgHeightFD = Seamless.getInstance().getHeightForFaceDetection(MainScreen.getImageHeight(), MainScreen.getImageWidth());
        }
        else
        {
        	imgWidthFD = Seamless.getInstance().getWidthForFaceDetection(MainScreen.getImageWidth(), MainScreen.getImageHeight());
    		imgHeightFD = Seamless.getInstance().getHeightForFaceDetection(MainScreen.getImageWidth(), MainScreen.getImageHeight());
        }
        
     	try {     		
            int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
    		
    		if (imagesAmount==0)
    			imagesAmount=1;
    		
    		nFrames = imagesAmount;
    		
    		for (int i=1; i<=imagesAmount; i++)
    		{
    			byte[] in = SwapHeap.CopyFromHeap(
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i+Long.toString(sessionID))),
    	        		Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i+Long.toString(sessionID)))
    	        		);
    			
    			mJpegBufferList.add(i-1, in);
    		}
    		
    		PreviewBmp = decodeJPEGfromBuffer(mJpegBufferList.get(0));
    		if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
    		{
	    		Matrix matrix = new Matrix();
	    		matrix.postRotate(mCameraMirrored? 270 : 90);
	    		PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix, true);
    		}
    		
    		previewBmpRealWidth = PreviewBmp.getWidth();
    		previewBmpRealHeight = PreviewBmp.getHeight();
    		
    		if (SaveInputPreference)
    		{
    			try
    	        {
    	            File saveDir = PluginManager.getInstance().GetSaveDir();
    	
    	            for (int i = 0; i<imagesAmount; ++i)
    	            {
    			    	Calendar d = Calendar.getInstance();

    		            File file = new File(
    		            		saveDir, 
    		            		String.format("%04d-%02d-%02d_%02d-%02d-%02d_OPENCAM.jpg",
    		            		d.get(Calendar.YEAR),
    		            		d.get(Calendar.MONTH)+1,
    		            		d.get(Calendar.DAY_OF_MONTH),
    		            		d.get(Calendar.HOUR_OF_DAY),
    		            		d.get(Calendar.MINUTE),
    		            		d.get(Calendar.SECOND)));
    	                
    		            FileOutputStream os = new FileOutputStream(file);
    		            
    		            if (os != null)
    		            {
    		            	// ToDo: not enough memory error reporting
    			            os.write(mJpegBufferList.get(i));
    			            os.close();
    			        
    			            ExifInterface ei = new ExifInterface(file.getAbsolutePath());
    			            if (mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
    			            {
    				            ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + (mCameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270 : ExifInterface.ORIENTATION_ROTATE_90));
    			            }
    			            ei.saveAttributes();
    		            }
    	            }
    	        }
    	        catch (Exception e)
    	        {
    	        	e.printStackTrace();
    	        }
    		}
    		
    		loadingSeamless();

			int max = 0;
			for (int i = 0;i < mFaceList.size();i++) {
				if (max < mFaceList.get(i).size()) {
					max = mFaceList.get(i).size();
				}
			}
			mChosenFace = new int[mFaceList.size()][max];
			for (int i = 0;i < mFaceList.size();i++) {
				Arrays.fill(mChosenFace[i], i);
			}
 		} 
     	catch (Exception e) 
 		{
 			e.printStackTrace();
 		}
		
     	PluginManager.getInstance().addToSharedMem("resultfromshared"+Long.toString(sessionID), "false");
		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
		
		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageWidth()));
    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(MainScreen.getSaveImageHeight()));
	}    	
	
	private void getPrefs()
    {
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz.getBaseContext());
        SaveInputPreference = prefs.getBoolean("saveInputPrefObjectRemoval", false);
    }
	
	private void getFaceRects()
	{
    	mFaceList = new ArrayList<ArrayList <Rect>>(mJpegBufferList.size());
    	
	    Face[] mFaces = new Face[MAX_FACE_DETECTED];
		for (int i=0; i<MAX_FACE_DETECTED; ++i)
			mFaces[i] = new Face();	
	    
        for(int index = 0; index < mJpegBufferList.size(); index++)
        {
			Size srcSize;
			if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
	        	srcSize = new Size(MainScreen.getImageHeight(), MainScreen.getImageWidth());
	        else
	        	srcSize = new Size(MainScreen.getImageWidth(), MainScreen.getImageHeight());
			
			//Log.e("Seamless", "fd size: "+dstSize.getWidth()+"x"+dstSize.getHeight());
			
			int numberOfFacesDetected = AlmaShotSeamless.GetFaces(index, mFaces);
			
			int Scale;
			if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
				Scale = MainScreen.getImageHeight() / imgWidthFD;
			else
				Scale = MainScreen.getImageWidth() / imgWidthFD;
	
			ArrayList<Rect> rect = new ArrayList<Rect>();
			for(int i = 0; i < numberOfFacesDetected; i++)
			{
				Face face = mFaces[i];
	            PointF myMidPoint = new PointF();
	            face.getMidPoint(myMidPoint);
	            float myEyesDistance = face.eyesDistance();
				if(face.confidence() > FACE_CONFIDENCE_LEVEL)
				{
					Rect faceRect = new Rect((int)(myMidPoint.x - myEyesDistance) * Scale,
				               (int)(myMidPoint.y - myEyesDistance) * Scale,
				               (int)(myMidPoint.x + myEyesDistance) * Scale,
				               (int)(myMidPoint.y + myEyesDistance) * Scale);
					rect.add(faceRect);
				}
			}
			
        	mFaceList.add(rect);	
        }
	}
	
	   private void loadingSeamless() {
	    	mSeamless = Seamless.getInstance();
	        Size preview = new Size(PreviewBmp.getWidth(), PreviewBmp.getHeight());
	    	// ToDo: correctness of w/h here depends on orientation while taking image,
	        // only product of inputSize is used later - this is why code still works  
	        Size inputSize = new Size(MainScreen.getImageWidth(), MainScreen.getImageHeight());
	        Size fdSize = new Size(imgWidthFD, imgHeightFD);

	    	try {
		        // Note: DecodeJpegs doing free() to jpeg data!
	        	mSeamless.addInputFrames(mJpegBufferList, inputSize, fdSize, mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180, (mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270) ? false: mCameraMirrored);
	        	getFaceRects();	
		        
		        sortFaceList();		        

				mSeamless.initialize(mBaseFrame, mFaceList, preview);				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	return;
	    }
	    
	    private void sortFaceList()
	    {    	
	    	ArrayList<ArrayList <Rect>> newFaceList = new ArrayList<ArrayList <Rect>>(mJpegBufferList.size());
	    	for(int i = 0; i < mJpegBufferList.size(); i++)
	        {
	    		newFaceList.add(new ArrayList<Rect>());
	        }   	
	    	
	    	while(isAnyFaceInList())
	    	{
	    		ArrayList<Integer> bestCandidateList = populateBestCandidate();
	    		
	    		if(bestCandidateList.size() != 0)
		    		for(int frameIndex = 0; frameIndex < newFaceList.size(); frameIndex++)
			    	{
			    		int bestFaceIndex = bestCandidateList.get(frameIndex);
			    		if(bestFaceIndex == -1)
			    			continue;
			    		
			    		newFaceList.get(frameIndex).add(mFaceList.get(frameIndex).remove(bestFaceIndex));
			    	}
	    	}
	    	
	    	mFaceList.clear();
	    	for(ArrayList <Rect> faces : newFaceList)
	    		mFaceList.add(faces);    	
	    }

	    private boolean isAnyFaceInList()
	    {
	    	for(ArrayList<Rect> faceList : mFaceList)
	    	{
	    		if(faceList.size() > 0)
	    			return true;
	    	}
	    	
	    	return false;
	    }
	    
	    private ArrayList<Integer> populateBestCandidate()
	    {
	    	int baseFaceX, baseFaceY, baseIndex, presenceNumber;
	    	float allowedDistance, distance, maxDistance, minDistance;
	    	
	    	int bestPresenceNumber = 0;
	    	float bestMaxDistance = -1;
	    	
	    	int candidateIndex, currIndex;
	    	ArrayList<Integer> candidateList = new ArrayList<Integer>(mJpegBufferList.size());
	    	ArrayList<Integer> bestCandidateList = new ArrayList<Integer>(mJpegBufferList.size()); 
	    	
	    	int i = 0;
	    	for(ArrayList<Rect> faceFrame : mFaceList)
	    	{
	    		baseIndex = 0;
	    		if(faceFrame.size() > 0)
	    			for(Rect baseFace : faceFrame)
	    			{ 
	    				candidateList.clear();
	    				baseFaceX = baseFace.centerX();
	    				baseFaceY = baseFace.centerY();
	    				allowedDistance = getRadius(baseFace) * 0.75f;
	    				presenceNumber = mFaceList.size();
	    				
	    				maxDistance = -1;
	    				for(int j = 0; j < mFaceList.size(); j++)
	    				{
	    					candidateIndex = -1;
	    					minDistance = -1;
	    					
	    					if(j == i || mFaceList.get(j).size() == 0)
	    					{
	    						if(j == i)
	    							candidateIndex = baseIndex;
	    						
	    						candidateList.add(candidateIndex);
	    						presenceNumber--;
	    						continue;
	    					}    					
	    					
	    					currIndex = 0;
	    					for(Rect candidateFace : mFaceList.get(j))
	    					{
	    						distance = getDistance(baseFaceX, baseFaceY, candidateFace.centerX(), candidateFace.centerY());
	    						if(distance < allowedDistance && (distance < minDistance || minDistance == -1))
	    						{
	    							minDistance = distance;
	    							candidateIndex = currIndex;    							
	    						}    						
	    						currIndex++;
	    					}
	    					
	    					if(minDistance == -1)
	    						presenceNumber--;
	    					
	    					if(minDistance > maxDistance)
	    						maxDistance = minDistance;
	    					
	    					candidateList.add(candidateIndex);
	    				}
	    				
	    				if(presenceNumber > bestPresenceNumber)
	    				{
	    					bestPresenceNumber = presenceNumber;
	    					bestMaxDistance = maxDistance;
	    					
	    					bestCandidateList.clear();
	    					for(Integer index : candidateList)
	    						bestCandidateList.add(index);
	    				}
	    				else if(presenceNumber == bestPresenceNumber && (maxDistance < bestMaxDistance || bestMaxDistance == -1))
	    				{
	    					bestMaxDistance = maxDistance;
	    					
	    					bestCandidateList.clear();
	    					for(Integer index : candidateList)
	    						bestCandidateList.add(index);
	    				}
	    			
	    				baseIndex++;
	    			}
	    		i++;
	    	}
	    	
	    	return bestCandidateList;
	    }
	    
		private boolean checkDistance(float radius, float x, float y, int centerX, int centerY) {
			float distance = getSquareOfDistance((int)x, (int)y, centerX, centerY);
			if (distance < (radius * radius)) {
				return true;
			}
			return false;
		}
		
		private boolean checkFaceDistance(float radius, float x, float y, int centerX, int centerY) {
			float distance = getDistance((int)x, (int)y, centerX, centerY);
			if (distance < (radius * 0.75f)) {
				return true;
			}
			return false;
		}
		
		private float getRadius(Rect rect) {
			return (rect.width() + rect.height()) / 2;
		}
		
		private int getSquareOfDistance(int x, int y, int x0, int y0) {
			return (x - x0) * (x - x0) + (y - y0) * (y - y0);
		}
		
		private int getDistance(int x, int y, int x0, int y0) {
			return (int)Math.round(Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0)));
		}

		public Bitmap decodeJPEGfromBuffer(byte[] data) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);

			float widthScale = (float)options.outWidth / (float)mDisplayWidth;
			float heightScale = (float)options.outHeight / (float)mDisplayHeight;
			float scale = widthScale > heightScale ? widthScale : heightScale;
			float imageRatio = (float)options.outWidth / (float)options.outHeight;
			float displayRatio = (float)mDisplayWidth / (float)mDisplayHeight;
			
			Bitmap bitmap = null;

			if (scale >= 8) {
				options.inSampleSize = 8;
			} else if (scale >= 4) {
				options.inSampleSize = 4;
			} else if (scale >= 2) {
				options.inSampleSize = 2;
			} else {
				options.inSampleSize = 1;
			}

			options.inJustDecodeBounds = false;
			
			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			
			if (imageRatio > displayRatio) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, mDisplayWidth, (int)(mDisplayWidth / displayRatio), true);
			} else {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, (int)(mDisplayHeight * imageRatio), mDisplayHeight, true);
			}

			if(bitmap != tempBitmap)
				tempBitmap.recycle();
			return bitmap;
		}
		
		
		
/************************************************
 * 		POST PROCESSING
 ************************************************/
		@Override
		public boolean isPostProcessingNeeded()
		{
			return true;
		}
		
		@Override
		public void onStartPostProcessing()
		{
			LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();
			postProcessingView = inflator.inflate(R.layout.plugin_processing_groupshot_postprocessing, null, false);
			
			mImgView = ((ImageView)postProcessingView.findViewById(R.id.groupshotImageHolder));
	        PreviewBmp = decodeJPEGfromBuffer(mJpegBufferList.get(0));
	        PreviewBmpInitial = Bitmap.createBitmap(PreviewBmp);
	        if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
    		{
	    		Matrix matrix = new Matrix();
	    		matrix.postRotate(mCameraMirrored? 270 : 90);
	    		PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix, true);
    		}
	        
	        Matrix matrix = new Matrix();
	        matrix.postRotate(mCameraMirrored? ((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)? 90 : 270) : 90);
    		PreviewBmpInitial = Bitmap.createBitmap(PreviewBmpInitial, 0, 0, PreviewBmpInitial.getWidth(), PreviewBmpInitial.getHeight(), matrix, true);
	        
	        mImgView.setImageBitmap(PreviewBmpInitial);
	        
	        textVeiw = ((TextView)postProcessingView.findViewById(R.id.groupshotTextView));
	        textVeiw.setText("Loading image ...");
			
			mHandler.sendEmptyMessage(MSG_END_OF_LOADING);
	        
		}
		
		
	    private void setupImageSelector() {
	        mGallery = (Gallery) postProcessingView.findViewById(R.id.groupshotGallery);
	        mGallery.setAdapter(new ImageAdapter(MainScreen.mainContext, mJpegBufferList, mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270, mCameraMirrored));  		
    		
	        mGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					mGallery.setVisibility(Gallery.INVISIBLE);
					mBaseFrame  = position;
					mSeamless.setBaseFrame(mBaseFrame);
		            new Thread(new Runnable() {
		                public void run() {
		                	mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
		                	updateBitmap();
		                    mHandler.post(new Runnable() {
		                        public void run() {
		                        	if (PreviewBmp != null) {
		                        		mImgView.setImageBitmap(PreviewBmp);
		                        	}
		                        }
		                    });
		                    
		                    // Probably this should be called from mSeamless ? 
		                    // mSeamless.fillLayoutwithStitchingflag(mFaceRect);
		                    mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
		                }
		            }).start();
				}
			});

	        mGallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});
	        
	        
	        new Thread(new Runnable() {
                public void run() {
                    mHandler.post(new Runnable() {
                        public void run() {
                        	RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mGallery.getLayoutParams();
    			    		int[] rules = lp.getRules();
    			    		if(mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
    			    		{    		
    			    			//rules[RelativeLayout.ALIGN_PARENT_BOTTOM] = 0;
    			    			lp.addRule(RelativeLayout.CENTER_VERTICAL);
                    			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    			
                    			mGallery.setLayoutParams(lp);
                    			mGallery.requestLayout();
                    			
                    			mGallery.setPivotX(mGallery.getHeight());
                    			mGallery.setRotation(90);
                    			
    			    		}
    			    		else
    			    		{
    			    			rules[RelativeLayout.ALIGN_PARENT_BOTTOM] = 1;
                    			rules[RelativeLayout.CENTER_VERTICAL] = 0;
                    			rules[RelativeLayout.ALIGN_PARENT_LEFT] = 0;
                    			
                    			mGallery.setLayoutParams(lp);
                    			mGallery.requestLayout();
                    			
                    			mGallery.setRotation(0);
    			    		}
                        }
                    });                   
                }
            }).start();
	    	return;
	    }
		
		public Bitmap decodeFullJPEGfromBuffer(byte[] data) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			options.inJustDecodeBounds = false;
			options.inSampleSize = 1;
			return BitmapFactory.decodeByteArray(data, 0, data.length, options);
		}
		
		private void drawFaceRectOnBitmap(Bitmap bitmap, ArrayList<Rect> faceRect) {
			float ratiox;
			float ratioy;
			if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
			{
				ratiox = (float)MainScreen.getImageHeight() / (float)bitmap.getWidth();
				ratioy = (float)MainScreen.getImageWidth() / (float)bitmap.getHeight();
			}
			else
			{
				ratiox = (float)MainScreen.getImageWidth() / (float)bitmap.getWidth();
				ratioy = (float)MainScreen.getImageHeight() / (float)bitmap.getHeight();
			}

			Paint paint = new Paint();
			paint.setStyle(Paint.Style.STROKE);
			paint.setColor(0xFF00AAEA);
			paint.setStrokeWidth(5);
			paint.setPathEffect(new DashPathEffect(new float[] {5,5},0));

			Canvas c = new Canvas(bitmap);

			for (Rect rect : faceRect) {
				float radius = getRadius(rect);
				c.drawCircle(rect.centerX() / ratiox, rect.centerY() / ratioy, radius / ((ratiox+ratioy)/2), paint);
			}

			return;
		}
		
		private boolean eventContainsFace(float x, float y, ArrayList<Rect> faceRect, View v) {
			float ratiox;
			float ratioy;
			if(mDisplayOrientationOnStartProcessing == 0 || mDisplayOrientationOnStartProcessing == 180)
			{
				ratiox = (float)MainScreen.getImageHeight() / (float)previewBmpRealWidth;
				ratioy = (float)MainScreen.getImageWidth() / (float)previewBmpRealHeight;
			}
			else
			{
				ratiox = (float)MainScreen.getImageWidth() / (float)previewBmpRealWidth;
				ratioy = (float)MainScreen.getImageHeight() / (float)previewBmpRealHeight;
			}

			if(mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
			{
				float x_tmp = x;
				float y_tmp = y;
				x = y_tmp;
				y = mDisplayHeight-1-x_tmp;
			}
			
			if(mDisplayOrientationOnStartProcessing != mDisplayOrientationCurrent)
			{				
				if(mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
				{
					float previewScaleFactor = (float)previewBmpRealHeight/v.getHeight();
					x = (float)(x - (mDisplayWidth - (float)v.getWidth()/previewScaleFactor)/2)*previewScaleFactor;
					y = (float)y*previewScaleFactor;
				}
				else
				{
					float previewScaleFactor = (float)previewBmpRealWidth/v.getWidth();
					x = (float)x*previewScaleFactor;
					y = (float)(y - (mDisplayWidth - (float)v.getHeight()/previewScaleFactor)/2)*previewScaleFactor;
				}
			}
			
			
			//Have to correct touch coordinates coz ImageView centered on the screen
			//and it's coordinate system not aligned with screen coordinate system.
			if((mDisplayHeight > v.getHeight() || mDisplayWidth > v.getWidth())
				&& (mDisplayOrientationOnStartProcessing == mDisplayOrientationCurrent))				
			{
				x = x - (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)? mDisplayWidth : mDisplayHeight) - previewBmpRealWidth)/2;
				y = y - (((mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)? mDisplayHeight : mDisplayWidth) - previewBmpRealHeight)/2;
			}
			
			
			int i = 0;
			for (Rect rect : faceRect) {
				Rect newRect = new Rect((int)(rect.left / ratiox),
						(int)(rect.top/ ratioy), 
						(int)(rect.right/ ratiox), 
						(int)(rect.bottom/ ratioy));
				float radius = getRadius(newRect);
				if (checkDistance(radius, x, y, newRect.centerX(), newRect.centerY()))
				{
					int newFrameIndex = mChosenFace[mBaseFrame][i] + 1;
					while(!checkFaceIsSuitable(newFrameIndex, i, radius, ratiox, ratioy, newRect))
						newFrameIndex++;

					mChosenFace[mBaseFrame][i] = newFrameIndex;
					mSeamless.changeFace(i, mChosenFace[mBaseFrame][i] % nFrames);
					return true;
				}
				i++;
			}
			return false;
		}
		
		private boolean checkFaceIsSuitable(int frameIndex, int faceIndex, float faceRadius, float ratioX, float ratioY, Rect currFace)
		{
			if(mFaceList.get(frameIndex % nFrames).size() <= faceIndex)
				return false;
			else
			{
				Rect candidateRect = mFaceList.get(frameIndex % nFrames).get(faceIndex);
				Rect newRect = new Rect((int)(candidateRect.left / ratioX),
						(int)(candidateRect.top/ ratioY), 
						(int)(candidateRect.right/ ratioX), 
						(int)(candidateRect.bottom/ ratioY));
				return checkFaceDistance(faceRadius, newRect.centerX(), newRect.centerY(), currFace.centerX(), currFace.centerY());				
			}
		}
	    
	    private void setupImageView() {
	    	PreviewBmp = PreviewBmp.copy(Config.ARGB_8888, true);
	        drawFaceRectOnBitmap(PreviewBmp, mFaceList.get(mBaseFrame));
	        if(mDisplayOrientationOnStartProcessing == 90 || mDisplayOrientationOnStartProcessing == 270)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(90);
	    		PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix, true);
			}
	        mImgView.setImageBitmap(PreviewBmp);
	        mImgView.setOnTouchListener(new View.OnTouchListener()
	        {
	        	@Override
				public boolean onTouch(final View v, final MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN)
					{
						if (mGallery.getVisibility() == Gallery.VISIBLE) {
							mGallery.setVisibility(Gallery.INVISIBLE);
							return false;
						}
						mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
				            new Thread(new Runnable() {
				                public void run() {
			                	synchronized (syncObject) {
				                	if (eventContainsFace(event.getX(), event.getY(), mFaceList.get(mBaseFrame), v) == true) {
				                	updateBitmap();
				                	// Update screen
				                    mHandler.post(new Runnable() {
				                        public void run() {
				                        	if (PreviewBmp != null) {
				                        		mImgView.setImageBitmap(PreviewBmp);
				                        	}
				                        }
				                    });
				                	}else {
				                		mHandler.sendEmptyMessage(MSG_SELECTOR_VISIBLE);
				                	}
				                    mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
			                	}
				                }
				            }).start();
						}
					return false;
				}
	        });
	    }
	    
	    private void setupProgress() {
	        mProgressBar = (ProgressBar)postProcessingView.findViewById(R.id.groupshotProgressBar);
	        mProgressBar.setVisibility(View.INVISIBLE);
	    	return;
	    }
	    
	    public void setupSaveButton() {
	    	// put save button on screen
	        mSaveButton = new Button(MainScreen.thiz);
	        mSaveButton .setBackgroundResource(R.drawable.button_save_background);
	        mSaveButton .setOnClickListener(this);
	        LayoutParams saveLayoutParams = new LayoutParams(
	        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)), 
	        		(int) (MainScreen.mainContext.getResources().getDimension(R.dimen.postprocessing_savebutton_size)));
	        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
	        saveLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        saveLayoutParams.setMargins(
	        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
	        		(int)(MainScreen.thiz.getResources().getDisplayMetrics().density * 8), 
	        		0, 
	        		0);
			((RelativeLayout)postProcessingView.findViewById(R.id.groupshotLayout)).addView(mSaveButton, saveLayoutParams);
			mSaveButton.setRotation(mDisplayOrientationCurrent);
	    }
	    
		@Override
		public boolean handleMessage(Message msg)
		{
	    	switch (msg.what)
	    	{
	    	case MSG_END_OF_LOADING:
				setupImageView();
				setupImageSelector();
				setupSaveButton();
				setupProgress();
				textVeiw.setVisibility(TextView.INVISIBLE);
				postProcessingRun = true;
	    		break;
	    	case MSG_PROGRESS_BAR_INVISIBLE:
	    		mProgressBar.setVisibility(View.INVISIBLE);
	    		break;
	    	case MSG_PROGRESS_BAR_VISIBLE:
	    		mProgressBar.setVisibility(View.VISIBLE);
	            break;
	    	case MSG_SELECTOR_VISIBLE:
	    		mGallery.setVisibility(View.VISIBLE);
	    		break;
	    	case MSG_SELECTOR_INVISIBLE:
	    		mGallery.setVisibility(View.GONE);
	            break; 
	    	case MSG_LEAVING:
	    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_POSTPROCESSING_FINISHED);
	    		if(mSeamless != null)
	    			mSeamless.release();
	    		mJpegBufferList.clear();
	    		
	    		Message msg2 = new Message();
	    		msg2.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
	    		msg2.what = PluginManager.MSG_BROADCAST;
	    		MainScreen.H.sendMessage(msg2);
	    		
	    		MainScreen.guiManager.lockControls = false;
	    		
	    		postProcessingRun = false;
	        	return false;
	        default:
	        	return true;
	    	}
			return true;
		}
		
		
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event)
		{
			if (keyCode == KeyEvent.KEYCODE_BACK && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.VISIBLE)
			{
				mHandler.sendEmptyMessage(MSG_LEAVING);
				return true;
			}
			
			return super.onKeyDown(keyCode, event);
		}

		
	    @Override
		public void onClick(View v) 
		{
	    	if (v == mSaveButton)
	    	{
	    		savePicture(MainScreen.mainContext);
	    		
	    		mHandler.sendEmptyMessage(MSG_LEAVING);
	    	}
		}
	    
	    public void onOrientationChanged(int orientation)
	    {	    	
	    	if(orientation != mDisplayOrientationCurrent)
	    	{
	    		mDisplayOrientationCurrent = orientation;
	    		if(postProcessingRun)
		    	new Thread(new Runnable() {
	                public void run() {
	                	mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_VISIBLE);
	                	updateBitmap();
	                    mHandler.post(new Runnable() {
	                        public void run() {
	                        	if (PreviewBmp != null) {
	                        		mImgView.setImageBitmap(PreviewBmp);
	                        		mSaveButton.setRotation(mDisplayOrientationCurrent);
	                        		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mGallery.getLayoutParams();
	                        		int[] rules = lp.getRules();
	                        		if(mDisplayOrientationCurrent == 90 || mDisplayOrientationCurrent == 180)
	                        		{
	                        			lp.addRule(RelativeLayout.CENTER_VERTICAL);
	                        			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

	                        			mGallery.setRotation(90);
	                        			mGallery.setPivotX(mGallery.getHeight());
	                        		}
	                        		else
	                        		{
	                        			rules[RelativeLayout.ALIGN_PARENT_BOTTOM] = 1;
	                        			rules[RelativeLayout.CENTER_VERTICAL] = 0;
	                        			rules[RelativeLayout.ALIGN_PARENT_LEFT] = 0;	                        			
	                        			
	                        			mGallery.setLayoutParams(lp);
	                        			mGallery.requestLayout();
	                        			
	                        			mGallery.setRotation(0);
	                        		}
	                        	}
	                        }
	                    });
	                    
	                    // Probably this should be called from mSeamless ? 
	                    // mSeamless.fillLayoutwithStitchingflag(mFaceRect);
	                    mHandler.sendEmptyMessage(MSG_PROGRESS_BAR_INVISIBLE);
	                }
	            }).start();
	    	}
	    }

		synchronized public void updateBitmap()
		{	
			PreviewBmp = mSeamless.getPreviewBitmap();
			PreviewBmp = PreviewBmp.copy(Config.ARGB_8888, true);
			drawFaceRectOnBitmap(PreviewBmp, mFaceList.get(mBaseFrame));
			
			if(mDisplayOrientationCurrent == 90 || mDisplayOrientationCurrent == 180)
			{
				Matrix matrix = new Matrix();
				matrix.postRotate(90);
	    		PreviewBmp = Bitmap.createBitmap(PreviewBmp, 0, 0, PreviewBmp.getWidth(), PreviewBmp.getHeight(), matrix, true);
			}
			
			return;
		}
		
		public void savePicture(Context context)
	    {
			byte[] result = mSeamless.processingSaveData();
			if (result == null)
			{
				Log.e("GroupShot", "Exception occured in processingSaveData. Picture not saved.");
				return;
			}
			int frame_len = result.length;
			int frame = SwapHeap.SwapToHeap(result);
    		//boolean wantLandscape = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("frameorientation1" + Long.toString(sessionID)));
    		//boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("framemirrored1" + Long.toString(sessionID)));
    		PluginManager.getInstance().addToSharedMem("resultframeformat1"+Long.toString(sessionID), "jpeg");
			PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(frame));
	    	PluginManager.getInstance().addToSharedMem("resultframelen1"+Long.toString(sessionID), String.valueOf(frame_len));
	    	
	    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(true));
	    	PluginManager.getInstance().addToSharedMem("resultframemirrored1" + String.valueOf(sessionID), String.valueOf(false));
			
			
			PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), String.valueOf(1));
			
			PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
//			try
//	        {	
//				String[] filesSavedNames = new String[1];
//				
//				Calendar d = Calendar.getInstance();
//				File saveDir = PluginManager.getInstance().GetSaveDir();
//				String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d",
//	            		d.get(Calendar.YEAR),
//	            		d.get(Calendar.MONTH)+1,
//	            		d.get(Calendar.DAY_OF_MONTH),
//	            		d.get(Calendar.HOUR_OF_DAY),
//	            		d.get(Calendar.MINUTE),
//	            		d.get(Calendar.SECOND));
//				
//				fileFormat += "_" + PluginManager.getInstance().getActiveMode().modeName + ".jpg";
//				
//				File file = new File(
//	            		saveDir, 
//	            		fileFormat);				
//	            
//	            filesSavedNames[0] = file.toString();
//
//	            FileOutputStream os = new FileOutputStream(file);
//	            
//	            os.write(mSeamless.processingSaveData());
//	            
//	            os.close();
//
//	           	ContentValues values = new ContentValues(9);
//                values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
//                values.put(ImageColumns.DISPLAY_NAME, file.getName());
//                values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
//                values.put(ImageColumns.MIME_TYPE, "image/jpeg");
//                values.put(ImageColumns.ORIENTATION, 0);
//                values.put(ImageColumns.DATA, file.getAbsolutePath());
//                
//                MainScreen.thiz.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//                MediaScannerConnection.scanFile(MainScreen.thiz, filesSavedNames, null, null);
//	        }
//	        catch (Exception e)
//	        {
//
//	        	Toast toast = Toast.makeText(context, R.string.cannot_create_jpeg, Toast.LENGTH_LONG);
//	        	toast.show();
//	        	e.printStackTrace();
//	        }
			
	    }
}
