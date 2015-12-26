package ee.graph.userpreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gabrielmorin on 13/09/15.
 */
public class UserPreferences {
    public static final String
            PREFIX_SPARSE_KEY = "key_",
            PREFIX_SPARSE_VALUE = "value_";
    private static final String TAG = UserPreferences.class.getName();
    private final Context context;
    private SharedPreferences preferences;

    private Array array = new Array();
    private Sparse sparse = new Sparse();

    public UserPreferences(Context context) {
        this.preferences = context.getSharedPreferences(getSharedPreferencesName(), 0);
        this.context = context;
    }

    // Override this function to set your shared preferences name
    protected String getSharedPreferencesName() {
        return "";
    }

    protected Context getContext() {
        return context;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }


    protected void putString(String keyPref, String cache) {
        preferences.edit()
                .putString(keyPref, cache)
                .apply();
    }

    public String getJson(String keyPref) {
        return cleanJsonString(getString(keyPref));
    }

    public String getString(String keyPref) {
        return preferences.getString(keyPref, "");
    }

    public Integer getInt(String keyPref) {
        return preferences.getInt(keyPref, -1);
    }

    public Long getLong(String keyPref) {
        return preferences.getLong(keyPref, -1);
    }

    public Sparse Sparse() {
        return sparse;
    }

    public Array Array() {
        return array;
    }


    private static String cleanJsonString(String json) {
        return json.replace("\\", "").replace("\"{","{").replace("}\"", "}");
    }

    public class Array {

        public Array() {
        }

        public <T> void set(String keyPref, List<T> values) {
            SharedPreferences.Editor pref = preferences.edit();

            JSONArray array = new JSONArray();
            for (int i = 0; i < values.size(); i++) {
                //Log.d(TAG, "String put in json array : " + values.get(i));
                array.put(values.get(i));
            }

            if (values.isEmpty())
                pref.remove(keyPref);
            else
                pref.putString(keyPref, array.toString());
            //Log.w("PUT JSON", array.toString());
            pref.apply();
        }

        public <T> void add(String keyPref, T value, boolean replace) {
            List<T> values = (List<T>) array(keyPref, value.getClass());
            if(replace)
                values.remove(value);
            values.add(0, value);
            set(keyPref, values);
        }

        public <T> void add(String keyPref, T value) {
            add(keyPref, value, true);
        }

        public <T extends Object> boolean remove(String keyPref, T value) {
            if(value == null)
                return false;
            List<T> values = (List<T>) array(keyPref, value.getClass());
            boolean removed = values.remove(value);
            set(keyPref, values);
            return removed;
        }

        public void clear(String keyPref) {
            preferences.edit().remove(keyPref).apply();
            //putString(keyPref, "");
        }

        private void clear(String[] keyPrefs, String prefix) {
            SharedPreferences.Editor e = preferences.edit();
            for(String k : keyPrefs)
                e.remove(prefix+k);
            e.apply();
        }

        public void clear(String[] keyPrefs) {
            clear(keyPrefs, "");
        }


        public List<String> arrayString(String keyPref) {
            return array(keyPref, String.class);
        }
        public List<Integer> arrayInt(String keyPref) {
            return array(keyPref, Integer.class);
        }
        public List<Boolean> arrayBoolean(String keyPref) {
            return array(keyPref, Boolean.class);
        }


        public <T> List<T> array(String keyPref, Class<T> type) {
            String json = preferences.getString(keyPref, null);
            List<T> objects = new ArrayList<>();
            if (json != null) {
                try {
                    JSONArray a = new JSONArray(json);
                    for (int i = 0; i < a.length(); i++) {
                        if (type.equals(String.class)) {
                            //Log.d(TAG, "String get in json array : " + a.optString(i));
                            objects.add((T) a.optString(i));
                        } else if (type.equals(Integer.class)) {
                            objects.add((T) ((Integer) a.optInt(i)));
                        } else if (type.equals(Boolean.class)) {
                            objects.add((T) ((Boolean) a.optBoolean(i)));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return objects;
        }

    }

    public class Sparse {

        public Sparse() {
        }

        public <T> void put(String keyPref, Integer key, T value) {
            remove(keyPref, key, value);
            Array().add(PREFIX_SPARSE_KEY + keyPref, key, true);
            Array().add(PREFIX_SPARSE_VALUE + keyPref, value, true);
        }

        public <T> boolean remove(String keyPref, Integer key, Class type) {
            T value = valueAt(keyPref, key, type);
            return remove(keyPref, key, value);
        }

        public <T> boolean remove(String keyPref, T value) {
            Integer key = keyAt(keyPref, value);
            return key > -1 && remove(keyPref, key, value);
        }

        private <T> boolean remove(String keyPref, Integer key, T value) {
            return Array().remove(PREFIX_SPARSE_KEY + keyPref, key) && Array().remove(PREFIX_SPARSE_VALUE + keyPref, value);
        }


        public List<Integer> keys(String keyPref) {
            return Array().arrayInt(PREFIX_SPARSE_KEY + keyPref);
        }

        public <T> List<T> values(String keyPref, Class<T> type) {
            return Array().array(PREFIX_SPARSE_VALUE + keyPref, type);
        }

        public <T> T valueAt(String keyPref, Integer key, Class type) {
            List<Integer> keys = keys(keyPref);
            List<T> values = values(keyPref, type);
            int i = keys.indexOf(key);
            if(i > -1 && i < keys.size() )
                return values.get(i);
            return null;
        }

        public <T> Integer keyAt(String keyPref, T value) {
            List<Integer> keys = keys(keyPref);
            List<T> values = (List<T>) values(keyPref, value.getClass());
            int i = values.indexOf(value);
            if(i > -1 && i < keys.size())
                return keys.get(i);
            return -1;
        }

        public void clear(String[] keyPrefs) {
            Array().clear(keyPrefs, PREFIX_SPARSE_KEY);
            Array().clear(keyPrefs, PREFIX_SPARSE_VALUE);
        }

        public <T> SparseArray<T> sparse(String keyPref, Class type) {
            List<Integer> keys = keys(keyPref);
            List<T> values = values(keyPref, type);
            SparseArray<T> array = new SparseArray<>();
            for (int i = 0; i < keys.size(); i++) {
                Integer key = keys.get(i);
                T value = values.get(i);
                array.put(key, value);
            }

            return array;
        }

        public SparseArray<Integer> sparseInt(String keyPref) {
            return sparse(keyPref, Integer.class);
        }

        public SparseArray<String> sparseString(String keyPref) {
            return sparse(keyPref, String.class);
        }

        public SparseArray<Boolean> sparseBoolean(String keyPref) {
            return sparse(keyPref, Boolean.class);
        }
    }
}
