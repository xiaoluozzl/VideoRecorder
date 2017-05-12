package com.xiaoluo.recorder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;

/**
 * 录制控件
 *
 * author: xiaoluo
 * date: 2017/5/11 16:52
 */
public class VideoRecordView extends RelativeLayout {
    private final static String TAG = VideoRecordView.class.getSimpleName();
    private Context mContext;

    private static final int FRONT_CAMERA = 0;   // 前置摄像头
    private static final int BACE_CAMERA = 1;    // 后置摄像头
    private int mCurrentCamera = BACE_CAMERA;    // 当前摄像头
    private static final int FLASH_ON = 2;       // 闪光灯开
    private static final int FLASH_OFF = 3;      // 闪光灯关
    private int mCurrentFlash = FLASH_OFF;       // 当前闪光灯状态

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private CircleRecordBar mCircleRecordBar;
    private ImageView mSwitchBtn;
    private ImageView mFlashBtn;

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private Camera.Parameters mParameters;

    private int mCircleColor;          // 圆环初始颜色
    private int mProgressColor;        // 进度条颜色
    private float mCircleWidth;        // 圆环宽度
    private int mTimeColor;            // 中间时间文字颜色
    private float mTimeSize;           // 文字大小
    private boolean isShowTime;        // 是否显示时间
    private int mRecordMaxTime;        // 录制最长时间(ms),默认10*1000ms
    private int mRecordMinTime;        // 录制最短时间(ms),默认1*1000ms
    private int mRecordTime;           // 录制时间
    private File mRecordFile = null;   // 录制文件
    private boolean isRecording = false;

    private OnRecordFinishListener mListener;
    private static String VIDEO_PATH = Constant.SD_CARD_PATH + "/"
            + Constant.APP_NAME + "/";

    public VideoRecordView(Context context) {
        this(context, null);
    }

    public VideoRecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        initViews(attrs);
        initProgressBar();
        createFile();
        initListener();
    }

    /**
     * 初始化视图
     */
    private void initViews(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.VideoRecordView);
        mRecordMaxTime = typedArray.getInteger(R.styleable.VideoRecordView_record_max_time, 10 * 1000);
        mRecordMinTime = typedArray.getInteger(R.styleable.VideoRecordView_record_min_time, 1 * 1000);
        mCircleColor = typedArray.getColor(R.styleable.VideoRecordView_circle_color, Color.GRAY);
        mProgressColor = typedArray.getColor(R.styleable.VideoRecordView_progress_color, Color.RED);
        mTimeColor = typedArray.getColor(R.styleable.VideoRecordView_time_color, Color.RED);
        mCircleWidth = typedArray.getDimension(R.styleable.VideoRecordView_circle_width, getResources().getDimension(R.dimen.circle_width));
        mTimeSize = typedArray.getDimension(R.styleable.VideoRecordView_time_size, getResources().getDimension(R.dimen.time_size));
        isShowTime = typedArray.getBoolean(R.styleable.VideoRecordView_show_time, true);
        typedArray.recycle();

        LayoutInflater.from(mContext).inflate(R.layout.widget_video_record, this);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mCircleRecordBar = (CircleRecordBar) findViewById(R.id.progress_bar);
        mSwitchBtn = (ImageView) findViewById(R.id.switch_btn);
        mFlashBtn = (ImageView) findViewById(R.id.flash_btn);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceCallBack());
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 初始化圆形进度条
     */
    private void initProgressBar() {
        mCircleRecordBar.setMaxTime(mRecordMaxTime);
        mCircleRecordBar.setMinTime(mRecordMinTime);
        mCircleRecordBar.setCircleColor(mCircleColor);
        mCircleRecordBar.setProgressColor(mProgressColor);
        mCircleRecordBar.setTimeColor(mTimeColor);
        mCircleRecordBar.setTimeSize(mTimeSize);
        mCircleRecordBar.setCircleWidth(mCircleWidth);
        mCircleRecordBar.setShowTime(isShowTime);

        mCircleRecordBar.setOnClickEvent(new CircleRecordBar.OnClickEvent() {
            @Override
            public void onBegin() {
                startRecord();
            }

            @Override
            public void onStop() {
                stopRecord();
            }
        });
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        mSwitchBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        mFlashBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchFlash();
            }
        });
    }

    /**
     * 创建目录文件
     */
    private void createFile() {
        File file = new File(VIDEO_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        mRecordFile = new File(VIDEO_PATH + "Record.mp4");
    }


    /**
     * 切换闪光灯
     */
    private void switchFlash() {
        if (mCurrentFlash == FLASH_OFF) {
            mFlashBtn.setImageResource(R.mipmap.ic_flash);
            mCurrentFlash = FLASH_ON;
        } else {
            mFlashBtn.setImageResource(R.mipmap.ic_flashoff);
            mCurrentFlash = FLASH_OFF;
        }

        initCamera();
        releaseRecorder();
        initRecorder();
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {
        mCurrentCamera = (mCurrentCamera == BACE_CAMERA) ? FRONT_CAMERA : BACE_CAMERA;
        initCamera();
        releaseRecorder();
        initRecorder();
    }

    /**
     * 初始化摄像头
     */
    private void initCamera() {
        if (mCamera != null) {
            releaseCameraResource();
        }

        if (mCurrentCamera == FRONT_CAMERA) {
            mCamera = openFrontCamera();
        } else {
            mCamera = openBackCamera();
        }

        if (mCamera == null) {
            return;
        }

        mParameters = mCamera.getParameters();  // 必须在unlock前调用

        if (mCurrentCamera == BACE_CAMERA) {
            if (mCurrentFlash == FLASH_ON) {
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            mCamera.setParameters(mParameters);
        }

        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
            mCamera.unlock();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开后置摄像头
     */
    private Camera openBackCamera() {
        if (mCamera != null) {
            releaseCameraResource();
        }

        int cameraCount = 0;
        Camera camera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    camera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    if (mListener != null) {
                        mListener.onFailed("打开后置摄像头失败: " + e.toString());
                    }
                }
            }
        }

        return camera;
    }

    /**
     * 打开前置摄像头
     */
    private Camera openFrontCamera() {
        if (mCamera != null) {
            releaseCameraResource();
        }

        int cameraCount = 0;
        Camera camera = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    camera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    if (mListener != null) {
                        mListener.onFailed("打开前置摄像头失败: " + e.toString());
                    }
                }
            }
        }
        return camera;
    }

    /**
     * 释放摄像头资源
     */
    private void releaseCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * 开始录制
     */
    private void startRecord() {
        mCircleRecordBar.refreshProgress(0);
        mMediaRecorder.start();
        mCircleRecordBar.startTime();
        isRecording = true;
    }

    /**
     * 停止录制
     */
    private void stopRecord() {
        releaseRecorder();
        if (isRecording) {
            isRecording = false;
            if (mListener != null && mRecordFile != null) {
                if (mRecordFile.exists()) {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(mRecordFile.getPath());
                    String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    String width = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String height = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    if (Integer.parseInt(duration) < mRecordMinTime) {
                        mListener.onFailed("录制时间太短");
                        mCircleRecordBar.refreshProgress(0);
                        initCamera();
                        initRecorder();
                    } else {
                        mListener.onSucceed(mRecordFile.getAbsolutePath(), duration, width, height);
                    }
                } else {
                    mListener.onFailed("文件不存在");
                }
            }
        } else {
            if (mListener != null) {
                mListener.onFailed("未录制");
            }
        }
    }

    /**
     * 释放录制器
     */
    private void releaseRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.setOnInfoListener(null);
            if (isRecording) {
                try {
                    mMediaRecorder.stop();
                } catch (IllegalStateException e) {
                    Log.d(TAG, "stopRecord", e);
                } catch (RuntimeException e) {
                    Log.d(TAG, "stopRecord", e);
                } catch (Exception e) {
                    Log.d(TAG, "stopRecord", e);
                }
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    /**
     * 初始化录制器
     */
    private void initRecorder() {
        if (mCamera == null) {
            return;
        }
        if (mMediaRecorder != null) {
            return;
        }

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);      // 视频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);         // 音频源
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);    // 视频输出格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);    // 音频格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP); // 视频录制格式
        mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);// 设置帧频率
//        mMediaRecorder.setVideoFrameRate(25);   // 视频帧频率
        //6.0如果不能打开的话,采用这个方法设置分辨率
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
//        mMediaRecorder.setVideoSize(Constant.HEIGHT_OF_SCREEN, Constant.WIDTH_OF_SCREEN);  // 视频分辨率
        if (mCurrentCamera == BACE_CAMERA) {
            mMediaRecorder.setOrientationHint(90);// 如果是后置摄像头，输出旋转90度，保持竖屏录制
        } else {
            mMediaRecorder.setOrientationHint(270);// 如果是前置摄像头，输出旋转270度，保持竖屏录制
        }

        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                if (mr != null) {
                    mr.reset();
                }
                if (mListener != null) {
                    mListener.onFailed("MediaRecorder Error: what = " + what + ", extra = " + extra);
                }
                mCircleRecordBar.stopTime();
            }
        });
        mMediaRecorder.setOutputFile(mRecordFile.getAbsolutePath());          // 输出文件

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.onFailed("MediaRecorder Error: " + e.toString());
            }
            mCircleRecordBar.stopTime();
        }
    }

    /**
     * suface回调
     */
    private class SurfaceCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            initCamera();
            initRecorder();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCameraResource();
        }
    }

    /**
     * 设置录像回调
     */
    public void setOnRecordFinishListener(OnRecordFinishListener listener) {
        this.mListener = listener;
    }


    /**
     * 录制完成回调
     */
    public interface OnRecordFinishListener {
        void onSucceed(String path, String duration, String width, String height);

        void onFailed(String error);
    }
}
