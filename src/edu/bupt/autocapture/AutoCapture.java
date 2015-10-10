package edu.bupt.autocapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class AutoCapture extends Activity implements SensorEventListener,OnClickListener,SurfaceHolder.Callback{
	
	private static String tag = "AutoCapture";
	private static final float NS2S = 1.0f / 1000000000.0f;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private static final int MEDIA_TYPE_IMAGE = 1;
	
	private static final String packageName = "edu.bupt.framelifting";
	private static final String className = "edu.bupt.framelifting.MainActivity";
	
	private SensorManager sensorManager;
	private Sensor gyroscopeSensor;
	
	private float timestamp;
	private float angle[] = new float[3];
	private ProgressBar progressBar;
	private int rotateProgress = 0;
	private int tempAngle;
	private int tmp_angle;
	private static int captureNum = 24;
	
	private float anglex, angley, anglez;
	private double rAngley;
	
	private Handler handler;
	private KalmanFilter kalmanFilter;
	
	private Camera mCamera;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private MediaRecorder mMediaRecorder;
	private Button bt_capture;
	
	private boolean isRecording;
	private boolean flag = false;
	
	private static String filePath;
	
	private Timer mTimer;
	private TimerTask mTimerTask;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_auto_capture);
		progressBar=(ProgressBar)findViewById(R.id.progressBar_record_progress);
		progressBar.setVisibility(View.VISIBLE);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		bt_capture=(Button) findViewById(R.id.start);
		bt_capture.setOnClickListener(this);
		mCamera=getCameraInstance();
		mSurfaceView=(SurfaceView) findViewById(R.id.SurfaceView);
		mSurfaceHolder=mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		kalmanFilter = new KalmanFilter();
		kalmanFilter.init();
		
//		new Thread(new Runnable(){
//		@Override
//		public void run() {
//			while(true){
//				if(flag){
//					kalmanFilter.inputData(angley);
//					rAngley = kalmanFilter.filter();
//	//				Log.i("估测数据y",Double.toString(rAngley));
//				}
//			}
//		}
//		
//	}).start();
				
//		showDialog();
		
		handler=new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what==1){
					progressBar.setProgress(rotateProgress);
				}
				if(msg.what==2){
					Toast.makeText(AutoCapture.this,"拍摄完成",Toast.LENGTH_SHORT).show();
					
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);            
					ComponentName cn = new ComponentName(packageName, className);            
					intent.setComponent(cn);
					intent.putExtra("filePath", filePath);
					startActivity(intent);
				}
			}
		};
		
	}
	
	public void init(){
		rotateProgress=0;
		angle[0]=0;
		angle[1]=0;
		angle[2]=0;
		progressBar.setProgress(rotateProgress);
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			if (timestamp != 0) {
				final float dT = (event.timestamp - timestamp) * NS2S;
				angle[0] += event.values[0] * dT;
				angle[1] += event.values[1] * dT;
				angle[2] += event.values[2] * dT;
				anglex = (float) Math.toDegrees(angle[0]);
				angley= (float) Math.toDegrees(angle[1]);
				tmp_angle = (int)angley;
//				Log.d(tag, "tmp_angle: " + tmp_angle);
				anglez = (float) Math.toDegrees(angle[2]);
				flag = true;
				
				if(rotateProgress < 100){
					rotateProgress = Math.abs((int)(((float)angley/360)*100));
					
					handler.sendEmptyMessage(1);
					
//					if(tmp_angle == tempAngle){
//						Log.d(tag, "tempAngle: " + tempAngle);
//						mCamera.autoFocus(mAFCallback);
//						tempAngle += 360/captureNum;
//					}
					
				}else{
					sensorManager.unregisterListener(this);
					handler.sendEmptyMessage(2);
					mMediaRecorder.stop();// stop the recording
					releaseMediaRecorder();// release the MediaRecorder object
//					mCamera.lock();// take camera access back from MediaRecorder
					bt_capture.setText("Capture");
					isRecording = false;
					flag = false;
					mTimer.cancel();
					init();
				}
			}
			timestamp = event.timestamp;
			
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if(holder.getSurface()==null){
			return;
		}
		mCamera.stopPreview();
		try {
			mCamera.setPreviewDisplay(holder);
			setCameraDisplayOrientation(this, 0, mCamera);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("Surface Changed", e.getMessage());
		}
		
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
		if(mCamera==null){
			mCamera=getCameraInstance();
		}
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("surfaceCreated", "error setting camera preview" + e.getMessage());
		}
		
	}


	private Camera getCameraInstance() {
		Camera c=null;
		try {
			c=Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("Open Camera", "failed");
		}
		return c;
	}
	
	private boolean prepareVideoRecorder(){
		if (mCamera == null) {
			mCamera=getCameraInstance();
		}
		mMediaRecorder=new MediaRecorder();
		// Step 1: Unlock and set camera to MediaRecorder
		
		mCamera.unlock();
		mMediaRecorder.setCamera(mCamera);
		// Step 2: Set sources
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher instead of setting format and encoding)
		mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
		// Step 4: Set output file
		mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
		// Step 5: Set the preview output
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
		 // Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Log.d("TAG", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		} catch (IOException e) {
			Log.d("TAG", "IOException preparing MediaRecorder: " + e.getMessage());
			e.printStackTrace();
			releaseMediaRecorder();
		}	
		return true;	
	}
	
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int rotationDegrees;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			rotationDegrees = (info.orientation + degrees) % 360;
			rotationDegrees = (360 - rotationDegrees) % 360; // compensate the mirror
		} else {
			rotationDegrees = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(rotationDegrees);
	}
	
	private void releaseMediaRecorder(){
		if(mMediaRecorder!=null){
			mMediaRecorder.reset();// clear recorder configuration
			mMediaRecorder.release();// release the recorder object
			mMediaRecorder=null;
			mCamera.lock();// lock camera for later use
		}
	}
	
	private void releaseCamera(){
		if(mCamera!=null){
			mCamera.release();// release the camera for other applications
			mCamera=null;
		}
	}
	private static File getOutputMediaFile(int type){
		File mediaStorageDir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+"RecordVideo");
		if(!mediaStorageDir.exists()){
			if(!mediaStorageDir.mkdirs()){
				Log.d("getOutputMediaFile", "failed to create directory");
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		if(type == MEDIA_TYPE_VIDEO){
			mediaFile = new File(mediaStorageDir.getPath() + 
					File.separator + "VID_" + timeStamp + ".mp4");
			filePath = mediaFile.getAbsolutePath();
		} else if(type == MEDIA_TYPE_IMAGE){
			mediaFile = new File(mediaStorageDir.getPath() + 
					File.separator + "PIC_" + timeStamp + ".jpg");
			filePath = mediaFile.getAbsolutePath();
		}else {
			return null;
		}
		return mediaFile;	
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releaseMediaRecorder();
		releaseCamera();
	}

	
	protected void onPause(){
		super.onPause();
		sensorManager.unregisterListener(this);
		flag=false;
		releaseMediaRecorder();// if you are using MediaRecorder, release it first
		releaseCamera();// release the camera immediately on pause event
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.start:
			if(isRecording){
				// stop recording and release camera
				mMediaRecorder.stop();// stop the recording
				releaseMediaRecorder();// release the MediaRecorder object
//				mCamera.lock();// take camera access back from MediaRecorder
				bt_capture.setText("Capture");
				mTimer.cancel();
				isRecording=false;
				sensorManager.unregisterListener(this);
				flag=false;
				init();
			}else{
				// initialize video camera
				if(prepareVideoRecorder()){
					mMediaRecorder.start();
					mTimer = new Timer();
					mTimerTask = new CameraTimerTask();
					mTimer.schedule(mTimerTask, 0, 3000);
					bt_capture.setText("Stop");
					isRecording = true;
					init();
					sensorManager.registerListener(this, gyroscopeSensor,
							SensorManager.SENSOR_DELAY_GAME);
				}else{
					releaseMediaRecorder();
				}
			}
		}
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	private PictureCallback mPicture = new PictureCallback(){
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			camera.stopPreview();
			new SaveImageTask().execute(data);
			camera.startPreview();
		}	
	};
	
	private AutoFocusCallback mAFCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if(success) {
				Log.d("AutoFocus", "success!");
					try {
//						camera.takePicture(null, null, mPicture);	
					} catch(Exception e) {
						e.printStackTrace();
					}
			}else {
				Log.d("AutoFocus", "failed!");
			}
		}	
	};
	
	public void showDialog() {
		AlertDialog dialog = null;
		AlertDialog.Builder builder = null;
		View view = LayoutInflater.from(AutoCapture.this).inflate(R.layout.dialog, null);
		final EditText et_savepath = (EditText) view.findViewById(R.id.et_dialog);
		builder = new AlertDialog.Builder(AutoCapture.this);
		builder.setTitle("Input the number of pictures：");
		builder.setView(view);
		builder.setPositiveButton("Confirm",new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				captureNum = Integer.parseInt(et_savepath.getText().toString());
				tempAngle = 360/captureNum;
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});

		dialog = builder.create();
		dialog.show();
	}
	
	private class SaveImageTask extends AsyncTask<byte[], Void, File> {
		@Override
		protected File doInBackground(byte[]... params) {
			return saveImage(params[0]);
		}
		
		@Override
		protected void onPostExecute(File result) {
			if (result != null) {
				try {
					ExifInterface exif = new ExifInterface(result.getAbsolutePath());
					Log.d("orientation", " " + exif.getAttribute(ExifInterface.TAG_ORIENTATION));
					exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + ExifInterface.ORIENTATION_ROTATE_90);
					exif.saveAttributes();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Toast.makeText(getApplicationContext(), "Picture saved", Toast.LENGTH_LONG).show();
			}
		}
		
		private File saveImage(byte[] data) {
			File file = getOutputMediaFile(MEDIA_TYPE_IMAGE);
			if(file == null){
				Log.d("pictureCallback", "Error creating media file, check storage permission");
				return null;
			}
			Log.d("FileName", " " + file.getName());
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(file);
				fos.write(data);
				fos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return file;
		}
	}
	
	class CameraTimerTask extends TimerTask {

		@Override
		public void run() {
			if(mCamera != null){
				mCamera.autoFocus(mAFCallback);
			}
		}
	}
}
