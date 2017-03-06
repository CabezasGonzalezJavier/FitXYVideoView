package com.thedeveloperworldisyours.view;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    FitXYVideoView mFitXYVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFitXYVideoView = (FitXYVideoView) findViewById(R.id.activity_main_video_view);

    }

    private void initView() {

        mFitXYVideoView.stop();
        mFitXYVideoView.setScaleType(FitXYVideoView.ScaleType.CENTER_CROP);
        Uri video1 = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.unit);
        try {
            mFitXYVideoView.setDataSource(MainActivity.this, video1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mFitXYVideoView.pause();
        mFitXYVideoView.play();
        mFitXYVideoView.setLooping(true);

    }

    @Override
    public void onResume() {
        super.onResume();

        initView();
    }

}