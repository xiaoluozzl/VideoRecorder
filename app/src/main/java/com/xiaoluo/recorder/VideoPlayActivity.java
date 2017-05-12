package com.xiaoluo.recorder;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;
import android.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * author: xiaoluo
 * date: 2017/5/11 17:26
 */
public class VideoPlayActivity extends AppCompatActivity {

    @BindView(R.id.video_view)
    VideoView videoView;

    private Context mContext;
    private String mVideoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        ButterKnife.bind(this);
        mContext = this;

        mVideoPath = getIntent().getStringExtra("path");

        if (!TextUtils.isEmpty(mVideoPath)) {
            videoView.setVideoPath(mVideoPath);
        }

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(mContext, "Error:" + what, Toast.LENGTH_SHORT).show();
                finish();
                return false;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoView.start();
            }
        });

    }
}
