import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    private final Context context;
    private static final String DATABASE_NAME = "SocraticTutor.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SESSIONS = "sessions";
    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_SESSION_JSON_DATA = "json_data";
    public static final String COLUMN_SETTINGS_ID = "settings_id";
    public static final String COLUMN_SETTINGS_JSON_DATA = "json_data";

    private static final String CREATE_TABLE_SESSIONS = "CREATE TABLE " + TABLE_SESSIONS + "(" + COLUMN_SESSION_ID + " TEXT PRIMARY KEY," + COLUMN_UPDATED_AT + " INTEGER," + COLUMN_SESSION_JSON_DATA + " TEXT" + ")";
    private static final String CREATE_TABLE_SETTINGS = "CREATE TABLE " + TABLE_SETTINGS + "(" + COLUMN_SETTINGS_ID + " INTEGER PRIMARY KEY DEFAULT 1," + COLUMN_SETTINGS_JSON_DATA + " TEXT" + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SESSIONS);
        db.execSQL(CREATE_TABLE_SETTINGS);

        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Native database created. Using Bridge for data storage.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
}