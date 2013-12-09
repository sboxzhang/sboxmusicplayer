package com.sbox.musicserviceplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public class Utils
{

    public static Activity activity = null;

    public static boolean isForceJumpToHome = false;

    public static ProgressDialog pd;

    public static boolean hasFriendCircle;

    public static boolean useSwitchAnimation = false;

    public static boolean isNullOrEmpty(String str)
    {
        boolean retv = false;

        if (str == null)
        {
            retv = true;
        }
        else if (str.isEmpty())
        {
            retv = true;
        }
        return retv;
    }

    @SuppressLint("SimpleDateFormat")
    public static String getCurrentFormatTime()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return formatter.format(new Date());
    }

    public static void shutDown()
    {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public static float getPXScale()
    {
        final float scale = MpsApplication.mContext.getResources()
                .getDisplayMetrics().density;

        return scale;
    }

    public static String getObjectForName(final Object json, final String name)
    {
        String result = null;
        if (json != null)
        {
            try
            {
                JSONTokener jsonParser = new JSONTokener(json.toString());
                JSONObject status = (JSONObject) jsonParser.nextValue();
                Object obj = status.get(name);// getJSONObject(name);
                if (obj != null)
                {
                    result = obj.toString();
                }
                jsonParser = null;
                status = null;
            }
            catch (Throwable e)
            {
                result = null;
            }
        }
        return result;
    }

    public static String getStringFromAsset(final String fileName,
            final String format)
    {
        String result = "";
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    MpsApplication.mContext.getAssets().open(fileName), format));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }
            br.close();
            result = sb.toString();
            sb = null;
        }
        catch (UnsupportedEncodingException e)
        {
        }
        catch (IOException e)
        {
        }

        return result;
    }

    public static void showToast(final String message)
    {
        MyToast.makeText(MpsApplication.mContext, message, Toast.LENGTH_SHORT)
                .show();
    }

    public static String getString(int resId)
    {
        return MpsApplication.mContext.getString(resId);
    }

    public static void showWaitingDialog(Context mContext, String title,
            String message)
    {

        if (pd != null)
        {
            pd.dismiss();
            pd = null;
        }

        pd = new ProgressDialog(mContext);
        pd.setMessage(message);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    public static void closeWaitingDialog()
    {
        if (pd != null)
        {
            pd.dismiss();
            pd = null;
        }
    }

    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels)
    {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);
        int roundedSize;
        if (initialSize <= 8)
        {
            roundedSize = 1;
            while (roundedSize < initialSize)
            {
                roundedSize <<= 1;
            }
        }
        else
        {
            roundedSize = (initialSize + 7) / 8 * 8;
        }
        return roundedSize;
    }

    public static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels)
    {
        double w = options.outWidth;
        double h = options.outHeight;
        int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
                .sqrt(w * h / maxNumOfPixels));
        int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));
        if (upperBound < lowerBound)
        {
            return lowerBound;
        }
        if ((maxNumOfPixels == -1) && (minSideLength == -1))
        {
            return 1;
        }
        else if (minSideLength == -1)
        {
            return lowerBound;
        }
        else
        {
            return upperBound;
        }
    }
    
    @SuppressLint("SimpleDateFormat")
    public static String ConvertYYYYMMDD(long ms)
    {
        String hms = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        hms = formatter.format(ms * 1000);
        return hms;
    }

    public static String m2(double f)
    {
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(f);
    }

    @SuppressLint("SimpleDateFormat")
    public static String ConvertHHMMSS(long ms)
    {
        String hms = "";
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        hms = formatter.format(ms);
        return hms;
    }
}
