## 可定制性超强的圆弧滑动组件
### 博客详情： https://blog.csdn.net/u011387817/article/details/80788704

### 使用方式:
#### 添加依赖：
```
implementation 'com.wuyr:fanlayout:1.0.0'
```

### APIs:
|Method|Description|
|------|-----------|
|setAutoSelect(boolean isAutoSelect)|设置滚动完毕是否自动选中最近的Item|
|setBearingCanRoll(boolean isCanRoll)|设置轴承是否可以跟随Item旋转<br>当**BearingType**为**TYPE_VIEW**时有效|
|setBearingColor(int color)|设置轴承颜色<br>当**BearingType**为**TYPE_COLOR**时有效|
|setBearingLayoutId(int layoutId)|指定轴承的布局id<br>当**BearingType**为**TYPE_VIEW**时需设置|
|setBearingOffset(int offset)|设置轴承的偏移量|
|setBearingOnBottom(boolean isOnBottom)|设置轴承是否在底部|
|setBearingType(int type)|设置轴承类型|
|setFixingAnimationDuration(int duration)|设置惯性滚动后，自动选中的动画时长|
|setGravity(int gravity)|设置对齐方式|
|setItemAddDirection(int direction)|设置Item的添加方向 默认: 顺时针添加|
|setItemAngleOffset(float angle)|指定Item的偏移角度<br>当**LayoutMode**为**MODE_FIXED**时有效|
|setItemDirectionFixed(boolean isFixed)|设置item是否保持垂直|
|setItemLayoutMode(int layoutMode)|item的布局方式: 默认: MODE_AVERAGE(平均) <br>如设置为**MODE_FIXED**需指定偏移的角度:<br>**setItemAngleOffset(float angle)**|
|setItemOffset(int itemOffset)|设置Item的偏移量|
|setRadius(int radius)|指定轴承半径<br>当**BearingType**为**TYPE_COLOR**时有效|
|setScrollAvailabilityRatio(float ratio)|惯性滚动利用率<br>数值越大，惯性滚动的动画时间越长|
|setSelection(int index, boolean isSmooth)|选中指定的Item|
|setOnItemRotateListener(Listener listener)|设置旋转事件监听器|
|setOnItemSelectedListener(Listener listener)|设置自动选中监听器|

### Attributes:
|Name|Format|Description|
|----|-----|-----------|
|auto_select|boolean (默认: false)|滚动完毕是否自动选中最近的Item|
|bearing_can_roll|boolean (默认: false)|轴承是否跟随Item转动 |
|bearing_color|color (默认: #000000)|轴承颜色<br>**bearing_type**为**color**时才有效|
|bearing_gravity|enum (默认: left)<br>**top(顶部)**<br>**bottom(底部)**<br>**left(左边)**<br>**left_top(左上)**<br>**left_bottom(左下)**<br>**right(右边)**<br>**right_top(右上)**<br>**right_bottom(右下)**|对齐方式 |
|bearing_layout|reference|自定义的轴承布局<br>**bearing_type**为**view**时才有效|
|bearing_offset|dimension|轴承偏移量|
|bearing_on_bottom|boolean (默认: false)|轴承是否在底部|
|bearing_radius|dimension|轴承半径|
|bearing_type|enum (默认: color)<br>**view**<br>**color**|轴承类型<br>设置为**view**时需指定**bearing_layout**|
|item_add_direction|enum (默认: clockwise)<br>**clockwise(顺时针)**<br>**counterclockwise(逆时针)**<br>**interlaced(交叉)**|item的添加方向|
|item_angle_offset|float|固定的偏移角度<br>当**item_layout_mode**为**fixed**时有效|
|item_direction_fixed|boolean (默认: false)|Item是否保持垂直|
|item_layout_mode|enum (默认: average)<br>**average(平均分配)**<br>**fixed(指定角度)**|item的布局方式<br>设置为**fixed**时需指定**item_angle_offset**|
|item_offset|dimension|item偏移量|


### Demo下载: [app-debug.apk](https://github.com/wuyr/FanLayout/raw/master/app-debug.apk)
### 库源码地址： https://github.com/Ifxcyr/FanLayout
### 几行代码实现Android弧形滑动 (圆弧滑动辅助) https://github.com/Ifxcyr/ArcSlidingHelper

### 效果图 (表情包来源：百度贴吧)：
![preview](https://github.com/wuyr/FanLayout/raw/master/previews/1.gif) ![preview](https://github.com/wuyr/FanLayout/raw/master/previews/2.gif)
![preview](https://github.com/wuyr/FanLayout/raw/master/previews/3.gif) ![preview](https://github.com/wuyr/FanLayout/raw/master/previews/4.gif)
![preview](https://github.com/wuyr/FanLayout/raw/master/previews/5.gif) ![preview](https://github.com/wuyr/FanLayout/raw/master/previews/6.gif)
![preview](https://github.com/wuyr/FanLayout/raw/master/previews/7.gif) ![preview](https://github.com/wuyr/FanLayout/raw/master/previews/8.gif)
![preview](https://github.com/wuyr/FanLayout/raw/master/previews/9.gif) ![preview](https://github.com/wuyr/FanLayout/raw/master/previews/10.gif)
