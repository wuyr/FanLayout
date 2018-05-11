package com.wuyr.fanlayout;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.Toast;

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
        ((SeekBar) findViewById(R.id.item_angle_offset)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.radius)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.item_offset)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.bearing_offset)).setOnSeekBarChangeListener(this);

        mFanLayout.setOnItemRotateListener(this);
        mFanLayout.setOnItemSelectedListener(new FanLayout.OnItemSelectedListener() {

            @Override
            public void onSelected(View item) {
                if (item instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) item;
                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                        View child = viewGroup.getChildAt(i);
                        if (child instanceof ImageView) {
                            BitmapDrawable drawable = (BitmapDrawable) ((ImageView) child).getDrawable();
                            drawable.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);
                        }
                    }
                    isRestored = false;
                }
            }
        });

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    public void handleOnClick(View view) {
        switch (view.getId()) {
            case R.id.average:
                mFanLayout.setItemLayoutMode(FanLayout.MODE_AVERAGE);
                break;
            case R.id.fixed:
                mFanLayout.setItemLayoutMode(FanLayout.MODE_FIXED);
                break;
            case R.id.clockwise:
                mFanLayout.setItemAddDirection(FanLayout.ADD_DIRECTION_CLOCKWISE);
                break;
            case R.id.counterclockwise:
                mFanLayout.setItemAddDirection(FanLayout.ADD_DIRECTION_COUNTERCLOCKWISE);
                break;
            case R.id.interlaced:
                mFanLayout.setItemAddDirection(FanLayout.ADD_DIRECTION_INTERLACED);
                break;
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
                onRotate(0);
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
            case R.id.s0:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(0, view.isSelected());
                break;
            case R.id.s1:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(1, view.isSelected());
                break;
            case R.id.s2:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(2, view.isSelected());
                break;
            case R.id.s3:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(3, view.isSelected());
                break;
            case R.id.s4:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(4, view.isSelected());
                break;
            case R.id.s5:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(5, view.isSelected());
                break;
            case R.id.s6:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(6, view.isSelected());
                break;
            case R.id.s7:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(7, view.isSelected());
                break;
            case R.id.s8:
                view.setSelected(!view.isSelected());
                mFanLayout.setSelection(8, view.isSelected());
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

    private Toast mToast;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.item_angle_offset:
                mFanLayout.setItemAngleOffset((float) progress - (float) seekBar.getMax() * .5F);
                mToast.setText(String.valueOf((float) progress - (float) seekBar.getMax() * .5F));
                break;
            case R.id.radius:
                mFanLayout.setRadius(progress);
                mToast.setText(String.valueOf(progress));
                break;
            case R.id.item_offset:
                mFanLayout.setItemOffset(progress - seekBar.getMax() / 2);
                mToast.setText(String.valueOf(progress - seekBar.getMax() / 2));
                break;
            case R.id.bearing_offset:
                mFanLayout.setBearingOffset(progress - seekBar.getMax() / 2);
                mToast.setText(String.valueOf(progress - seekBar.getMax() / 2));
                break;
            default:
                break;
        }
        mToast.show();
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
                mFanLayout.setItemDirectionFixed(isChecked);
                break;
            default:
                break;
        }
    }

    private boolean isRestored = true;

    @Override
    public void onRotate(float rotation) {
        for (int i = 0; i < mFanLayout.getChildCount(); i++) {
            View v = mFanLayout.getChildAt(i);
            if (!mFanLayout.isBearingView(v)) {
                ViewGroup viewGroup = (ViewGroup) v;
                for (int j = 0; j < viewGroup.getChildCount(); j++) {
                    View child = viewGroup.getChildAt(j);
                    if (!isRestored && child instanceof ImageView) {
                        BitmapDrawable drawable = (BitmapDrawable) ((ImageView) child).getDrawable();
                        drawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.DST);
                    }
                }
            }
        }
        isRestored = true;
    }
}

