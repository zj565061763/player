package com.fanwe.www.mediaplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.fanwe.library.media.player.SDMediaPlayer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private static final String TAG = "MainActivity";

    private SurfaceView sfv_media;

    private SDMediaPlayer mMediaPlayer = new SDMediaPlayer();

    private Button btn_start, btn_stop;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sfv_media = (SurfaceView) findViewById(R.id.sfv_media);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

        sfv_media.getHolder().addCallback(new SurfaceHolder.Callback()
        {
            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
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

        mMediaPlayer.setOnExceptionCallback(new SDMediaPlayer.OnExceptionCallback()
        {
            @Override
            public void onException(Exception e)
            {
                Log.i(TAG, "onException:" + String.valueOf(e));
            }
        });
        mMediaPlayer.setOnStateChangeCallback(new SDMediaPlayer.OnStateChangeCallback()
        {
            @Override
            public void onStateChanged(SDMediaPlayer.State oldState, SDMediaPlayer.State newState, SDMediaPlayer player)
            {
                Log.i(TAG, "onStateChanged:" + String.valueOf(newState));
            }
        });

        mMediaPlayer.setDataRawResId(R.raw.cbg, this);
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mMediaPlayer.release();
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btn_start:
                mMediaPlayer.start();
                break;
            case R.id.btn_stop:

                break;
        }

    }
}
