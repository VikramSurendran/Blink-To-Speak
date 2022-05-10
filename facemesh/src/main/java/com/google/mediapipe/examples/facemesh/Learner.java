package com.google.mediapipe.examples.facemesh;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import androidx.appcompat.app.ActionBar;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class Learner extends AppCompatActivity {

    YouTubePlayerView youTubePlayerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learner);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.action_bar_layout);
        ColorDrawable colorDrawable
                = new ColorDrawable(Color.parseColor("#8CBBF1"));
        // Set BackgroundDrawable
        //actionBar.setBackgroundDrawable(colorDrawable);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.gradient_action_bar_background));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_action_back);

        String[] videoIds = {"tXnnM_chaJA", "uLwRij1rvHQ", "VYbiMtBxan4", "3YR-rCOxNa4", "rL03rXlIH4I", "ywJ_ceAL_nA", "m6sGmiyReq0",
                "UNtdn5JfkXM", "Jv6mVTjXN0g", "C4ZP8M80XzY", "h-zBkYCbTrc", "as6Wj3WeRfI", "KePVqKuXtUs", "e-Ik2nXUlpI",
                "M-ZwOqQIxRU", "LnDpXyThO0I", "YT2Wqvylois", "G5DhmY7_p44", "GaWhvdii1eM", "Lay5vkf9YFc", "xNyDEqgaen4",
                "xAd_KEcbh-w", "J0-bcJOqRmI", "c5yoMP5b7No", "hGVTuAmm7nE", "m9BKOP8CmGM", "tvMUpNeln4g", "KhP_VqhLaYU",
                "nEmFyxJBjSI", "-TrOeGIrb50", "qL0dHS3RmcY", "QxS6gwfAXM0", "1ea8msunA_4", "EeKhS4ngbsU", "VbhFAvsqSA8",
                "-IxZrllENmE", "Naqc5RT7zhE", "Mtfk-lmYvZg", "ARBOmFDCU9g", "aoyk0GAeNlA", "6qkfxTnqM5o", "EyXFhMPMA1Y",
                "QfUGn_anHR8", "ypDjDxlv8LY"};
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.llContainer);
        for(String videoIdFromArray:videoIds)
        {
            youTubePlayerView = new YouTubePlayerView(this);
            getLifecycle().addObserver(youTubePlayerView);
            youTubePlayerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            youTubePlayerView.setPadding(20,50,20,0);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                    String videoId = videoIdFromArray;
                    youTubePlayer.cueVideo(videoId, 0);
                }
            });
            linearLayout.addView(youTubePlayerView);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        youTubePlayerView.release();
    }


}