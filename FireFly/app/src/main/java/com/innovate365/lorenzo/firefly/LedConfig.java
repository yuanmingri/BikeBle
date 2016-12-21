package com.innovate365.lorenzo.firefly;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by WMI on 11/27/2016.
 */

public class LedConfig {
    public int mLedColor;
    public int mBlinking;
    public int mLedOnTime;
    public int mDiscoMode;
    public String mAddress;

    private Context mContext;

    public LedConfig(Context context)
    {
        mContext = context;
    }

    public void loadConfig()
    {
        SharedPreferences pref = mContext.getSharedPreferences("FireFlyLedSetting",Context.MODE_PRIVATE);
        mLedColor = pref.getInt("Color",0);
        mBlinking = pref.getInt("blinking",0);
        mLedOnTime = pref.getInt("LedOnTime",0);
        mDiscoMode = pref.getInt("DiscoMode",0);
        mAddress = pref.getString("Address","");
    }

    public void saveConfig()
    {
        SharedPreferences.Editor e = mContext.getSharedPreferences("FireFlyLedSetting",Context.MODE_PRIVATE).edit();
        e.putInt("Color",mLedColor);
        e.putInt("blinking",mBlinking);
        e.putInt("LedOnTime",mLedOnTime);
        e.putInt("DiscoMode",mDiscoMode);
        e.putString("Address",mAddress);
        e.commit();
    }

    public int getLedOnTimeForDelay()
    {
        if(mLedOnTime == 0)
            return 3000;
        if(mLedOnTime == 1)
            return 6000;
        return 9000;
    }
}
