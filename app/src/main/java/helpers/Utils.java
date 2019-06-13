package helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.text.SimpleDateFormat;

public class Utils {

    public static final String API_KEY = "7a36e06a9633548f992c160d1050e0cc";
    public static final String LANGUAGE = "es-MX";
    public static final int PAGE = 1;
    public static final String BASE_URL = "https://api.themoviedb.org/3/";
    public static final String SORT_BY_ASC = "created_at.asc";
    public static final String SORT_BY_DESC = "created_at.desc";

    public static void ShowAlertDialog(Context context, String title, String message, String ok, DialogInterface.OnClickListener onclick, String cancel){

        if (message == null) message = "Error de Conexión..";
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        if (title != null)
            alert.setTitle(title);
        alert.setMessage(message);
        alert.setPositiveButton(ok, onclick);
        if (cancel != null)
            alert.setNegativeButton(cancel, null);
        AlertDialog dialog = alert.create();
        alert.show();
    }

    public static SimpleDateFormat GetDateFormat(String format){
        return new SimpleDateFormat(format);
    }
}
