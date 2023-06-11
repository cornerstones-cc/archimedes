package cc.cornerstones.almond.utils;

import com.alibaba.fastjson.JSONObject;

public class AbcJsonUtils {

    public static Object recurseFindField(JSONObject jsonObject, String[] splices) {
        if (jsonObject == null) {
            return null;
        }

        if (splices == null || splices.length == 0) {
            return jsonObject;
        }

        String splice = splices[0];

        String[] remainderSplices = new String[splices.length - 1];
        for (int i = 1; i < splices.length; i++) {
            String item = splices[i];
            remainderSplices[i - 1] = item;
        }

        Object object = jsonObject.get(splice);
        if (object != null) {
            if (object instanceof JSONObject) {
                JSONObject childJsonObject = (JSONObject) object;
                if (childJsonObject == null) {
                    return null;
                }
                return recurseFindField(childJsonObject, remainderSplices);
            } else {
                return object;
            }
        } else {
            return null;
        }
    }
}
