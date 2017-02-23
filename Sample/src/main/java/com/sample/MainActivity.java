
package com.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.VideoView;

import java.util.Timer;
import java.util.TimerTask;

import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.util.SystemClock;

public class MainActivity extends Activity implements View.OnClickListener {

    private IDanmakuView mDanmakuView;

    private View mMediaController;

    public PopupWindow mPopupWindow;

    private Button mBtnRotate;

    private Button mBtnHideDanmaku;

    private Button mBtnShowDanmaku;

    private BaseDanmakuParser mParser;

    private Button mBtnPauseDanmaku;

    private Button mBtnResumeDanmaku;

    private Button mBtnSendDanmaku;

    private Button mBtnSendDanmakuTextAndImage;

    private Button mBtnSendDanmakus;
    private DanmaSvManager mDanmaSvManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    private void findViews() {

        mMediaController = findViewById(R.id.media_controller);
        mBtnRotate = (Button) findViewById(R.id.rotate);
        mBtnHideDanmaku = (Button) findViewById(R.id.btn_hide);
        mBtnShowDanmaku = (Button) findViewById(R.id.btn_show);
        mBtnPauseDanmaku = (Button) findViewById(R.id.btn_pause);
        mBtnResumeDanmaku = (Button) findViewById(R.id.btn_resume);
        mBtnSendDanmaku = (Button) findViewById(R.id.btn_send);
        mBtnSendDanmakuTextAndImage = (Button) findViewById(R.id.btn_send_image_text);
        mBtnSendDanmakus = (Button) findViewById(R.id.btn_send_danmakus);
        mBtnRotate.setOnClickListener(this);
        mBtnHideDanmaku.setOnClickListener(this);
        mMediaController.setOnClickListener(this);
        mBtnShowDanmaku.setOnClickListener(this);
        mBtnPauseDanmaku.setOnClickListener(this);
        mBtnResumeDanmaku.setOnClickListener(this);
        mBtnSendDanmaku.setOnClickListener(this);
        mBtnSendDanmakuTextAndImage.setOnClickListener(this);
        mBtnSendDanmakus.setOnClickListener(this);

        // VideoView
        VideoView mVideoView = (VideoView) findViewById(R.id.videoview);
        mDanmakuView = (IDanmakuView) findViewById(R.id.sv_danmaku);
        // TODO DanmakuView 初始化
        mDanmaSvManager = new DanmaSvManager(this, mDanmakuView, new DanmaSvManager.OnClickEvent() {
            @Override
            public void onclick() {
                mMediaController.setVisibility(View.VISIBLE);
            }
        });


        if (mVideoView != null) {
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            mVideoView.setVideoPath(Environment.getExternalStorageDirectory() + "/1.flv");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDanmaSvManager != null) {
            mDanmaSvManager.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDanmaSvManager != null) {
            mDanmaSvManager.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDanmaSvManager != null) {
            mDanmaSvManager.onDestory();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mDanmaSvManager != null) {
            mDanmaSvManager.onDestory();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mMediaController) {
            mMediaController.setVisibility(View.GONE);
        }
        if (mDanmakuView == null || !mDanmakuView.isPrepared())
            return;
        if (v == mBtnRotate) {
            setRequestedOrientation(getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (v == mBtnHideDanmaku) {
            mDanmakuView.hide();
            // mPausedPosition = mDanmakuView.hideAndPauseDrawTask();
        } else if (v == mBtnShowDanmaku) {
            mDanmakuView.show();
            // mDanmakuView.showAndResumeDrawTask(mPausedPosition); // sync to the video time in your practice
        } else if (v == mBtnPauseDanmaku) {
            mDanmakuView.pause();
        } else if (v == mBtnResumeDanmaku) {
            mDanmakuView.resume();
        } else if (v == mBtnSendDanmaku) {
            mDanmaSvManager.addDanmaku(false);
        } else if (v == mBtnSendDanmakuTextAndImage) {
            mDanmaSvManager.addDanmaKuShowTextAndImage(false);
        } else if (v == mBtnSendDanmakus) {
            Boolean b = (Boolean) mBtnSendDanmakus.getTag();
            timer.cancel();
            if (b == null || !b) {
                mBtnSendDanmakus.setText(R.string.cancel_sending_danmakus);
                timer = new Timer();
                timer.schedule(new AsyncAddTask(), 0, 1000);
                mBtnSendDanmakus.setTag(true);
            } else {
                mBtnSendDanmakus.setText(R.string.send_danmakus);
                mBtnSendDanmakus.setTag(false);
            }
        }
    }

    Timer timer = new Timer();

    class AsyncAddTask extends TimerTask {

        @Override
        public void run() {
            for (int i = 0; i < 20; i++) {
                mDanmaSvManager.addDanmaku(true);
                SystemClock.sleep(20);
            }
        }
    };


}
