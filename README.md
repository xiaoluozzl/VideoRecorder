#VideoRecorder

自定义相机录制视频

集成进度条,摄像头转换,闪光灯,封装成控件VideoRecorderView;

1.在xml文件中使用VideoRecordView,自定义属性

2.在Activity/Fragment中添加录制回调setOnRecordFinishListener

targetSdkVersion <= 22, 需要23及以上需要在Activity中先进行权限获取.