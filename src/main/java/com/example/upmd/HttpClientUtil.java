package com.example.upmd;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class HttpClientUtil {

    public String doPost(String url, Map<String, String> headerMap, String jsonText, String charset) {
        CloseableHttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;
        InputStream in = null;
        try {
            httpClient = HttpClients.createDefault();
            httpPost = new HttpPost(url);
            // 设置参数
            Iterator<Map.Entry<String, String>> iterator = headerMap.entrySet().iterator();
            Map.Entry<String, String> elem = null;
            while (iterator.hasNext()) {
                elem = iterator.next();
                httpPost.addHeader(elem.getKey(), elem.getValue());
            }
            if (StringUtils.isNotBlank(jsonText)) {
                byte[] bytes = jsonText.getBytes(StandardCharsets.UTF_8);
                ByteArrayEntity se = new ByteArrayEntity(bytes);
                httpPost.setEntity(se);
            }
            HttpResponse response = httpClient.execute(httpPost);
            if (response != null) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    in=resEntity.getContent();
                    result = EntityUtils.toString(resEntity, charset);
                }
            }
        } catch (Exception ex) {

        }
        finally
        {
            if (in != null){
                try
                {
                    in.close ();
                }
                catch (IOException e)
                {
                    e.printStackTrace ();
                }
            }
        }
        return result;
    }
}
