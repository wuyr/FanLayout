package com.wuyr.fanlayouttest;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
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

import com.wuyr.fanlayout.FanLayout;

import java.util.Locale;


/**
 * Created by wuyr on 18-5-5 下午6:14.
 */
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener, FanLayout.OnItemRotateListener {

    private FanLayout mFanLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_view);

        findViews();
        initListener();

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    private void findViews() {
        mFanLayout = findViewById(R.id.fan_layout);
        ((Switch) findViewById(R.id.auto_select)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.bearing_can_roll)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.bearing_on_bottom)).setOnCheckedChangeListener(this);
        ((Switch) findViewById(R.id.item_direction_is_fixed)).setOnCheckedChangeListener(this);
        ((SeekBar) findViewById(R.id.item_angle_offset)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.radius)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.item_offset)).setOnSeekBarChangeListener(this);
        ((SeekBar) findViewById(R.id.bearing_offset)).setOnSeekBarChangeListener(this);
    }

    private void initListener() {
        mFanLayout.setOnItemRotateListener(this);
        mFanLayout.setOnItemSelectedListener(new FanLayout.OnItemSelectedListener() {

            @Override
            public void onSelected(View item) {
                if (item instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) item;
                    for (int i = 0; i < viewGroup.getChildCount(); i++) {
                        View child = viewGroup.getChildAt(i);
                        if (child instanceof ImageView) {
                            ImageView imageView = (ImageView) child;
                            imageView.getDrawable().setColorFilter(getResources()
                                    .getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);
                            imageView.invalidate();
                        }
                    }
                    isRestored = false;
                }

            }
        });

        mFanLayout.setOnBearingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("BearingView clicked");
            }
        });

        mFanLayout.setOnItemClickListener(new FanLayout.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int index) {
                showToast(String.format(Locale.getDefault(), "item %d clicked", index));
            }
        });

        mFanLayout.setOnItemLongClickListener(new FanLayout.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view, int index) {
                showToast(String.format(Locale.getDefault(), "item %d long clicked", index));
                return true;
            }
        });
    }

    private void showToast(String content) {
        mToast.setText(content);
        mToast.show();
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
                mFanLayout.setSelection(0, true);
                break;
            case R.id.s1:
                mFanLayout.setSelection(1, true);
                break;
            case R.id.s2:
                mFanLayout.setSelection(2, true);
                break;
            case R.id.s3:
                mFanLayout.setSelection(3, true);
                break;
            case R.id.s4:
                mFanLayout.setSelection(4, true);
                break;
            case R.id.s5:
                mFanLayout.setSelection(5, true);
                break;
            case R.id.s6:
                mFanLayout.setSelection(6, true);
                break;
            case R.id.s7:
                mFanLayout.setSelection(7, true);
                break;
            case R.id.s8:
                mFanLayout.setSelection(8, true);
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
                showToast(String.valueOf((float) progress - (float) seekBar.getMax() * .5F));
                break;
            case R.id.radius:
                mFanLayout.setRadius(progress);
                showToast(String.valueOf(progress));
                break;
            case R.id.item_offset:
                mFanLayout.setItemOffset(progress - seekBar.getMax() / 2);
                showToast(String.valueOf(progress - seekBar.getMax() / 2));
                break;
            case R.id.bearing_offset:
                mFanLayout.setBearingOffset(progress - seekBar.getMax() / 2);
                showToast(String.valueOf(progress - seekBar.getMax() / 2));
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
                mFanLayout.setItemDirectionFixed(isChecked);
                break;
            default:
                break;
        }
    }

    private boolean isRestored = true;

    @Override
    public void onRotate(float rotation) {
        if (!isRestored) {
            for (int i = 0; i < mFanLayout.getChildCount(); i++) {
                View v = mFanLayout.getChildAt(i);
                if (!mFanLayout.isBearingView(v)) {
                    ViewGroup viewGroup = (ViewGroup) v;
                    for (int j = 0; j < viewGroup.getChildCount(); j++) {
                        View child = viewGroup.getChildAt(j);
                        if (child instanceof ImageView) {
                            ((ImageView) child).getDrawable()
                                    .setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.DST);
                            child.invalidate();
                        }
                    }
                }
            }
            isRestored = true;
        }
    }
}

