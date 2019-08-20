package com.sd.www.mediaplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sd.lib.looper.FLooper;
import com.sd.lib.looper.impl.FSimpleLooper;
import com.sd.lib.player.FMediaPlayer;


public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "MainActivity";

    private SurfaceView sfv_media;

    private final FMediaPlayer mPlayer = new FMediaPlayer();

    private Button btn_start, btn_pause, btn_stop, btn_reset, btn_play_pause, btn_play_stop;
    private TextView tv_duration;
    private SeekBar sb_progress;

    private final FLooper mLooper = new FSimpleLooper();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sfv_media = findViewById(R.id.sfv_media);
        sb_progress = findViewById(R.id.sb_progress);
        tv_duration = findViewById(R.id.tv_duration);
        btn_start = findViewById(R.id.btn_start);
        btn_pause = findViewById(R.id.btn_pause);
        btn_stop = findViewById(R.id.btn_stop);
        btn_reset = findViewById(R.id.btn_reset);
        btn_play_pause = findViewById(R.id.btn_play_pause);
        btn_play_stop = findViewById(R.id.btn_play_stop);
        btn_start.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_stop.setOnClickListener(this);
        btn_reset.setOnClickListener(this);
        btn_play_pause.setOnClickListener(this);
        btn_play_stop.setOnClickListener(this);

        sb_progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if (fromUser)
                {
                    mPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        sfv_media.getHolder().addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
                mPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
            }
        });

        mPlayer.setOnExceptionCallback(new FMediaPlayer.OnExceptionCallback()
        {
            @Override
            public void onException(Exception e)
            {
                Log.i(TAG, "onException:" + e);
            }
        });
        mPlayer.addOnStateChangeCallback(new FMediaPlayer.OnStateChangeCallback()
        {
            @Override
            public void onStateChanged(FMediaPlayer player, FMediaPlayer.State oldState, FMediaPlayer.State newState)
            {
                if (newState == FMediaPlayer.State.Playing)
                    mLooper.start(mPlayTimeRunnable);
                else if (newState == FMediaPlayer.State.Stopped || newState == FMediaPlayer.State.Paused)
                    mLooper.stop();

                Log.i(TAG, "onStateChanged:" + newState);
            }
        });
    }

    private final Runnable mPlayTimeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            final int currentPosition = mPlayer.getCurrentPosition();
            final int totalDuration = mPlayer.getDuration();

            sb_progress.setMax(totalDuration);
            sb_progress.setProgress(currentPosition);

            final String total = SDDateUtil.formatDuring2hhmmss(totalDuration);
            final String current = SDDateUtil.formatDuring2hhmmss(currentPosition);
            tv_duration.setText(current + "/" + total);
        }
    };

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_start:
                mPlayer.setDataRawResId(R.raw.cbg, this); //设置要播放的数据
                mPlayer.start(); //播放
                break;
            case R.id.btn_pause:
                mPlayer.pause(); //暂停
                break;
            case R.id.btn_stop:
                mPlayer.stop(); //停止
                break;
            case R.id.btn_reset:
                mPlayer.reset(); //重置
                break;
            case R.id.btn_play_pause:
                mPlayer.performPlayPause(); // 播放/暂停
                break;
            case R.id.btn_play_stop:
                mPlayer.performPlayStop(); // 播放/停止
                break;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mPlayer.release();
        mLooper.stop();
    }
}
