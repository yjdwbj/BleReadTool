package bt.lcy.btread;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final static String TAG=CrashHandler.class.getName();
    public static final String APP_CACHE_PATH = Environment.getExternalStorageDirectory().getPath() + "/BleRead/crash/";

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try{
            Thread.sleep(3000);
        }catch (InterruptedException e)
        {
            Log.e(TAG,"error : ", e);
            e.printStackTrace();
        }
    }

    private boolean handleException(Throwable ex){
        if(ex==null){
            return false;
        }
        return true;
    }
}
