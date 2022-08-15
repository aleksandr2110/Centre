package orlov.home.centurapp.service.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HTTPConnector {

    private static String urlGet = "https://astim.in.ua/";
    private static String urlPost = "https://astim.in.ua/datawork/changeperpage/";

    public static void main(String[] args) {

        String link = "https://astim.in.ua/profi_line";
        Connection.Response response = get(link, new HashMap<>());

        Map<String, String> cookies = response.cookies();
        System.out.println("cookies: " + cookies);
        Map<String, String> data = new HashMap<>();
        data.put("perpage", "100000");
        data.put("rand", "0.6140656206877313");
        System.out.println("data: " + data);
        Connection.Response post = post(urlPost, cookies, data);

        int statusCode = post.statusCode();
        System.out.println("statusCode: " + statusCode);
        String body = post.body();
        System.out.println("body: " + body);

    }

    public static Connection.Response post(String url, Map<String, String> cookies, Map<String, String> date) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .timeout(60 * 1000)
                    .userAgent("Mozilla")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .method(Connection.Method.POST)
                    .cookies(cookies)
                    .data(date)
                    .followRedirects(true)
                    .execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Connection.Response get(String url, Map<String, String> cookies) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .timeout(60 * 1000)
                    .userAgent("Mozilla")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .cookies(cookies)
                    .execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}


