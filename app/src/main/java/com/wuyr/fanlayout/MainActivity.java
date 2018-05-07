package com.wuyr.fanlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Created by wuyr on 18-5-5 下午6:14.
 */
public class MainActivity extends AppCompatActivity {

    FanLayout mFanLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.setDebugLevel(LogUtil.ERROR);
        setContentView(R.layout.act_main_view);
        mFanLayout = findViewById(R.id.fan_layout);
        SeekBar radius = findViewById(R.id.radius);
        SeekBar itemOffset = findViewById(R.id.item_offset);
        SeekBar centerOffset = findViewById(R.id.center_offset);
        final View view = findViewById(R.id.view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.print("===");
            }
        });
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                Log.e("pX", view.getPivotX() + "");
//                Log.e("pY", view.getPivotY() + "");
//                LogUtil.print(progress);
//                view.setRotation(progress);
                mFanLayout.setRadius(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        itemOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                view.setPivotX(progress - 250);
                mFanLayout.setItemOffset(progress - seekBar.getMax() / 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        centerOffset.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                view.setPivotY(progress - 250);
                mFanLayout.setCenterOffset(progress - seekBar.getMax() / 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mFanLayout.setOnItemSelectedListener(new FanLayout.OnItemSelectedListener() {
            Toast toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

            @Override
            public void onSelected(View item) {
//                toast.setText("选中了: " + ((TextView) item).getText().toString());
//                toast.show();
            }
        });
        mFanLayout.setOnItemRotateListener(new FanLayout.OnItemRotateListener() {
            @Override
            public void onRotate(float rotation) {
                for (int i = 0; i < mFanLayout.getChildCount(); i++) {
                    ViewGroup viewGroup = (ViewGroup) mFanLayout.getChildAt(i);
                    for (int j = 0; j < viewGroup.getChildCount(); j++) {
                        View child = viewGroup.getChildAt(j);
                        child.setRotation(-viewGroup.getRotation());
                    }
                }
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
            default:
                break;
        }
    }

    private View getView() {
//        Button button = new Button(this);
//        button.setLayoutParams(new ViewGroup.LayoutParams(300, ViewGroup.LayoutParams.WRAP_CONTENT));
//        button.setText(String.valueOf(mFanLayout.getChildCount()));
//        return button;
        return LayoutInflater.from(this).inflate(R.layout.item, null);
    }
}

