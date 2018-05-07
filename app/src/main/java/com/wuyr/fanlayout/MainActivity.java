package com.wuyr.fanlayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;

/**
 * Created by wuyr on 18-5-5 下午6:14.
 */
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener, FanLayout.OnItemRotateListener {

    private FanLayout mFanLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.setDebugLevel(LogUtil.ERROR);
        setContentView(R.layout.act_main_view);
        mFanLayout = findViewById(R.id.fan_layout);
        ((Switch) findViewById(R.id.auto_select)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.bearing_can_roll)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.bearing_on_bottom)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.item_direction_is_fixed)).setOnCheckedChangeListener(this);
        ((SeekBar) findViewById(R.id.radius)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.item_offset)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.bearing_offset)).setOnSeekBarChangeListener(this);

        mFanLayout.setOnItemSelectedListener(new FanLayout.OnItemSelectedListener() {

            @Override
            public void onSelected(View item) {

            }
        });
    }

    public void handleOnClick(View view) {
        switch (view.getId()) {
            case R.id.left:
                mFanLayout.setGravity(FanLayout.LEFT);
                break;
            case R.id.right:
                mFanLayout.setGravity(FanLayout.RIGHT);
                break;
            case R.id.top:
                mFanLayout.setGravity(FanLayout.TOP);
                break;
            case R.id.bottom:
                mFanLayout.setGravity(FanLayout.BOTTOM);
                break;
            case R.id.left_top:
                mFanLayout.setGravity(FanLayout.LEFT_TOP);
                break;
            case R.id.right_top:
                mFanLayout.setGravity(FanLayout.RIGHT_TOP);
                break;
            case R.id.left_bottom:
                mFanLayout.setGravity(FanLayout.LEFT_BOTTOM);
                break;
            case R.id.right_bottom:
                mFanLayout.setGravity(FanLayout.RIGHT_BOTTOM);
                break;
            case R.id.add_item:
                mFanLayout.addView(getView());
                break;
            case R.id.remove_item:
                mFanLayout.removeViewAt(mFanLayout.getChildCount() - 1);
                break;
            case R.id.view_mode:
                mFanLayout.setBearingType(FanLayout.TYPE_VIEW);
                break;
            case R.id.color_mode:
                mFanLayout.setBearingType(FanLayout.TYPE_COLOR);
                break;
            default:
                break;
        }
    }

    private int[] mIds;

    {
        mIds = new int[12];
        for (int i = 0; i < mIds.length; i++) {
            mIds[i] = 0x7f060054 + i;
        }
    }

    @SuppressLint("InflateParams")
    private View getView() {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.item, null);
        int index = mFanLayout.getChildCount();
        if (mFanLayout.getBearingType() == FanLayout.TYPE_VIEW) {
            index--;
        }
        if (index >= mIds.length) {
            index %= mIds.length;
        }
        int id = mIds[index];
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof ImageView) {
                ((ImageView) view).setImageResource(id);
            }
        }
        return viewGroup;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.radius:
                mFanLayout.setRadius(progress);
                break;
            case R.id.item_offset:
                mFanLayout.setItemOffset(progress - seekBar.getMax() / 2);
                break;
            case R.id.bearing_offset:
                mFanLayout.setBearingOffset(progress - seekBar.getMax() / 2);
                break;
            default:
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.auto_select:
                mFanLayout.setAutoSelect(isChecked);
                break;
            case R.id.bearing_can_roll:
                mFanLayout.setBearingCanRoll(isChecked);
                break;
            case R.id.bearing_on_bottom:
                mFanLayout.setBearingOnBottom(isChecked);
                break;
            case R.id.item_direction_is_fixed:
                if (isChecked) {
                    mFanLayout.setOnItemRotateListener(this);
                    onRotate(0);
                } else {
                    mFanLayout.setOnItemRotateListener(null);
                    for (int i = 0; i < mFanLayout.getChildCount(); i++) {
                        View v = mFanLayout.getChildAt(i);
                        if (!mFanLayout.isBearingView(v)) {
                            ViewGroup viewGroup = (ViewGroup) v;
                            for (int j = 0; j < viewGroup.getChildCount(); j++) {
                                View child = viewGroup.getChildAt(j);
                                child.setRotation(0);
                            }
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRotate(float rotation) {
        for (int i = 0; i < mFanLayout.getChildCount(); i++) {
            View v = mFanLayout.getChildAt(i);
            if (!mFanLayout.isBearingView(v)) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int j = 0; j < viewGroup.getChildCount(); j++) {
                    View child = viewGroup.getChildAt(j);
                    child.setRotation(-viewGroup.getRotation());
                }
            }
        }
    }
}

