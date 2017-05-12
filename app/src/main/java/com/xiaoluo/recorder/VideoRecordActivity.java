package com.xiaoluo.recorder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 录制界面
 *
 * author: xiaoluo
 * date: 2017/5/11 14:55
 */
public class VideoRecordActivity extends AppCompatActivity {

    private Context mContext;

    @BindView(R.id.record_view)
    VideoRecordView recordView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ButterKnife.bind(this);
        mContext = this;

        recordView.setOnRecordFinishListener(new VideoRecordView.OnRecordFinishListener() {

            @Override
            public void onSucceed(String path, String duration, String width, String height) {
                Toast.makeText(mContext, "录制成功:" + duration + path, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(mContext, VideoPlayActivity.class);
                intent.putExtra("path", path);
                startActivity(intent);
            }

            @Override
            public void onFailed(String error) {
                Toast.makeText(mContext, "录制失败:" + error, Toast.LENGTH_SHORT).show();
            }

        });
    }
}
