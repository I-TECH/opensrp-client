package util;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.apache.commons.lang3.StringUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.DristhiConfiguration;
import org.ei.opensrp.domain.Response;
import org.ei.opensrp.event.Listener;
import org.ei.opensrp.path.fragment.AdvancedSearchFragment;
import org.ei.opensrp.view.BackgroundAction;
import org.ei.opensrp.view.LockingBackgroundTask;
import org.ei.opensrp.view.ProgressIndicator;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import static android.view.View.VISIBLE;

/**
 * Created by keyman on 26/01/2017.
 */
public class GlobalSearchUtils {

    public static void backgroundSearch(final Map<String, String> map, final Listener<JSONArray> listener, final ProgressBar progressBar) {

        Utils.startAsyncTask(new AsyncTask<Void, Void, JSONArray>() {
            @Override
            protected JSONArray doInBackground(Void... params) {
                publishProgress();
                Response<String> response = globalSearch(map);
                if (response.isFailure()) {
                    return null;
                } else {
                    try {
                        JSONArray jsonArray = new JSONArray(response.payload());
                        return jsonArray;
                    } catch (Exception e) {
                        Log.e(getClass().getName(), "", e);
                        return null;
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                progressBar.setVisibility(VISIBLE);
            }

            @Override
            protected void onPostExecute(JSONArray result) {
                listener.onEvent(result);
                progressBar.setVisibility(View.GONE);
            }
        }, null);
    }

    public static Response<String> globalSearch(Map<String, String> map) {
        Context context = Context.getInstance();
        DristhiConfiguration configuration = context.configuration();
        String baseUrl = configuration.dristhiBaseURL();
        String paramString = "";
        if (!map.isEmpty()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.contains(AdvancedSearchFragment.ACTIVE) && !key.contains(AdvancedSearchFragment.INACTIVE)) {
                    key = AdvancedSearchFragment.INACTIVE;
                    boolean v = !Boolean.valueOf(value);
                    value = Boolean.toString(v);
                }

                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    value = urlEncode(value);
                    String param = key.trim() + "=" + value.trim();
                    if (StringUtils.isBlank(paramString)) {
                        paramString = "?" + param;
                    } else {
                        paramString += "&" + param;
                    }
                }

            }

        }
        String uri = baseUrl + "/rest/search/path" + paramString;

        Response<String> response = context.getHttpAgent().fetch(uri);
        return response;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
