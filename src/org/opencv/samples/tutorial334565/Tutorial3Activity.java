package org.opencv.samples.tutorial334565;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.Thread;
import java.lang.Runnable;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.samples.tutorial334565.R;
//import org.opencv.samples.tutorial1.R;
import org.opencv.android.JavaCameraView;



import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
//import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

 


public class Tutorial3Activity extends Activity implements CvCameraViewListener2, OnTouchListener {
    private static final String TAG = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private Tutorial3View mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    private ReentrantLock lock; 
    private Thread				 netThread;
    private boolean isthinking=false;
    private String numberansw;
    private MenuItem             mItemSwitchCamera = null;
    
    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;
    
    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat                  DetectedNum;
    private boolean              IsNumDetected;
    private boolean              IsFliped;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
   // private DetectionBasedTracker  mNativeDetector;
    
   private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private int timetoshow=0;
    private Rect Current;
    
    private Socket socket;
    private String serverIpAddress = "192.168.0.54";
    private static final int REDIRECTED_SERVERPORT = 10000;
    private int PortFromFile;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    
                    
                 //   System.loadLibrary("detection_based_tracker");

                    try {
//�������� ������� �� ������
                    	InputStream is = getResources().openRawResource(R.raw.cascade);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "cascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    
                    
                    
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Tutorial3Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    //�������� ���������
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        IsNumDetected=false;
        IsFliped=false;
        super.onCreate(savedInstanceState);
        numberansw =null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);

        mOpenCvCameraView = (Tutorial3View) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        
        mOpenCvCameraView.setCvCameraViewListener(this);
       
        lock = new ReentrantLock();
        File sdcard  = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"/documents/ip.txt");
        //�������� ���������� �� ������
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
        //    String line;
            serverIpAddress = br.readLine();
            PortFromFile = Integer.parseInt(br.readLine());

        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
       
        
    }

    @Override
    public void onPause()
    {
    	//���������� �� �����
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
    	//������������� �� ������
        super.onResume();
      //  OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	mRgba = new Mat();
    	mGray = new Mat();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("FlipCamera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	IsFliped=!IsFliped;
        return true;
    }
    public void onCameraViewStopped() {
    }

    long counter=0; 
    String tempansw=null;
    
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    //�� ������ ����� ����� �������� ���� � ��� ���������� �������
    	lock.lock(); //������� ������ ����� ��� �� ���������� ���� �� ��� �������
    	try { 

    		if (IsFliped)
    		{

    			mRgba =  inputFrame.rgba();//.copyTo(mRgba);
    			try
    			{
    				mGray.release();
    			}
    			catch(Exception ex)
    			{}
    			mGray = inputFrame.gray();
    	    		    Core.flip(mRgba, mRgba, 1);
    	    		    Core.flip(mGray, mGray, 1);
    	    		
    		}
    		else
    		{

    			mRgba =  inputFrame.rgba();//.copyTo(mRgba);
    			mGray = inputFrame.gray();
    		}
    	}
    	finally {
    	  lock.unlock(); 
    	}
    	if (timetoshow>0)
    	{
    	  Mat t = mRgba.submat(Current);
    	  DetectedNum.copyTo(t);
    	  if(!busy)
    		  timetoshow--;
    	  
    	}
    	
    	counter++;
    	//show connecting if busy
    	if(busy)
    	{
    		if((counter/10)%4==0)
    			tempansw="Connect";
    		if((counter/10)%4==1)
    			tempansw="Connect .";
    		if((counter/10)%4==2)
    			tempansw="Connect ..";
    		if((counter/10)%4==3)
    			tempansw="Connect ...";
    	}else
    		tempansw=null;
    	
    	

    	Scalar s = new Scalar(3);
		 s.val[0]=0;
		 s.val[1]=0;
		 s.val[2]=0;
		 if ((numberansw!=null)||(tempansw!=null))
			 Core.rectangle(mRgba, new Point(45,15), new Point(285,60), s, -1);
		 s.val[0]=255;
		 s.val[1]=255;
		 s.val[2]=255;
		 if ((numberansw!=null)||(tempansw!=null))
					 Core.rectangle(mRgba, new Point(50,20), new Point(280,55), s, -1);
		 s.val[0]=0;
		 s.val[1]=0;
		 s.val[2]=0;
		 
		 if(!busy)
			 Core.putText(mRgba, numberansw, new Point(50,50), 2, 1, s);
		 else
			 Core.putText(mRgba, tempansw, new Point(50,50), 2, 1, s);

		 
        return mRgba;
     //   return inputFrame.rgba();
    }
    
    //����� ������ ������
	private void haar() {
		
		lock.lock(); //������� ������ ����� ��� �� ���������� ���� �� ��� �������
    	try { 
    		MatOfRect faces = new MatOfRect();

    		Mat temp = new Mat();
    		mGray.copyTo(temp);
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(temp, faces, 1.1, 10, 5, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                   //     new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
                		 new Size(70, 21), new Size(500,150));


	        Rect[] facesArray = faces.toArray();
	        for (int i = 0; i < facesArray.length; i++)
	        {
	        	DetectedNum = new Mat();
	        	IsNumDetected=true;
	        	DetectedNum = temp.submat(facesArray[i]).clone();
	        	timetoshow=20;
	        	Current = facesArray[i];
	           // Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
	            
	        }

    	}
    	finally {
    	  lock.unlock(); 
    	}
    	  
		        
	}


	//������� ����������� ��� �������� �� ����
	public static byte[] compressBitmap(Bitmap bitmap, int quality)
	  {
	    try
	    {
	      ByteArrayOutputStream baos = new ByteArrayOutputStream();
	      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
	      
	      return baos.toByteArray();
	    }
	    catch(Exception e)
	    {
	    //  Logger.e(e.toString());
	    }
	    
	    return null;
	  }
	
	View curView=null;
	String FileName="";
	Boolean busy=false;
	
    @SuppressLint("SimpleDateFormat")
    @Override
    //������� ���� �� ����� � �������� ���
    public boolean onTouch(View v, MotionEvent event) {
    	 Run(v);
        return false;
       
    }
    
    public void buttonPhoto(View view) {
    	Run(view);
    }
    
    String ID="";
    int timesWas=0; 
    
    void Run(View v)
    {
    	if(!busy)
    	{
    	 
    		curView=v;
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	        String currentDateandTime = sdf.format(new Date());
	        ContextWrapper ctx = new ContextWrapper(getApplicationContext());
	        String fileName = ctx.getFilesDir().toString()+"/sample_picture_.jpg";
	
    		FileName=fileName;
	        busy=true;
	        counter=0;
	       
	        haar();

		    isthinking=false;
		    
	        class LongAndComplicatedTask extends AsyncTask<Void, Void, String> {
	            
	            @Override
	            protected String doInBackground(Void... params) {
             	
	            	
	            	
	            	if (IsNumDetected) //���� ����� �����
	            	{
	            		IsNumDetected=false;
	            		Bitmap resultBitmap = Bitmap.createBitmap(DetectedNum.cols(),  DetectedNum.rows(),Bitmap.Config.ARGB_8888); //������� ���
	        	    	Utils.matToBitmap(DetectedNum, resultBitmap);
	        	    	byte[] im = compressBitmap(resultBitmap,90);
	        	    	
	        	    	//��������� �����������
	        	    	
	        	        
	        	  //      String fileName = Environment.getExternalStorageDirectory().getPath() +"/documents/numbers"+
	              //      "/sample_picture_" + currentDateandTime + ".jpg";
	        	        try {
	        			        FileOutputStream fos = new FileOutputStream(FileName);
	        					fos.write(im);
	        					fos.close();
	        	        } catch (IOException e) {
	        				e.printStackTrace();
	        			}
	        	    	
	        	        
	        	        resultBitmap.recycle();
	        	        
	            	String ans="";
	            	
	            	HttpClient httpclient = new DefaultHttpClient();

	    	        final HttpParams httpParameters = httpclient.getParams();

	    	        HttpConnectionParams.setConnectionTimeout(httpParameters, 10 * 1000);
	    	        HttpConnectionParams.setSoTimeout        (httpParameters, 10 * 1000);
	    	        
	    	        HttpPost httppost = new HttpPost("http://193.138.232.71:10000/result");
	    	         InputStreamEntity reqEntity;
	    	        httppost.setEntity(new FileEntity(new File(FileName), "application/octet-stream"));
	    		    try {
	    					HttpResponse response = httpclient.execute(httppost);
	    					HttpEntity responseEntity = response.getEntity();
	    					//String responseString = EntityUtils.toString(responseEntity);
	    				//	System.out.println(responseString);
	    					ans = EntityUtils.toString(responseEntity);
	    					
	    					String[] strs=ans.split("\r\n");
	    					if(strs.length>2)
	    					{
	    						ans=strs[0];
	    						timesWas=Integer.parseInt(strs[1]);
	    						ID=strs[2];
	    					}
	    					
	    					
	    			        
	    				} catch (ClientProtocolException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    					ans = "NOT CONNECT";
	    				} catch (IOException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    					ans = "NOT CONNECT";
	    				}
	                return ans;
	            	}
	            	
	            	return null;
	            }

	            @Override
	            protected void onPostExecute(String result) {
	            	busy=false;
	            	numberansw=result;
	            	if(result!=null)
	            	{
	            		if(!result.equals("NOT CONNECT"))
	            			{
			            		Intent myIntent = new Intent(curView.getContext(), SecondActivity.class);
		    					myIntent.putExtra("BMP",FileName);
		    					myIntent.putExtra("TEXT", numberansw);
		    					myIntent.putExtra("ID", ID);
		    					myIntent.putExtra("times", timesWas);
		    			        startActivityForResult(myIntent, 0);
	            			}
	            	}
	                
	            }
	        }
	            
	            LongAndComplicatedTask longTask = new LongAndComplicatedTask(); // ������� ���������
		        longTask.execute(); // ���������

	        
		    
    	
    	}
    }
    
}
