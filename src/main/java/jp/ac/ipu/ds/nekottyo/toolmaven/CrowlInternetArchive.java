/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.ac.ipu.ds.nekottyo.toolmaven;

import jp.ac.ipu.ds.nekottyo.kttestclient.ProcessBroker;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author nekottyo
 */
public class CrowlInternetArchive {

    private static final String sourceFile = "data/source.txt";
    private static final String resultFile = "data/result1.txt";
    private static final String htmlFile = "data/";

    private static final String queryURL = "https://web.archive.org/web/*/";

    public static void main(String[] args) throws IOException {

        try {
            Date date = new Date();
            Format sdf = new SimpleDateFormat("dd_km");
            String now = sdf.format(date);
            FileHandler fh = new FileHandler("DSNotify_" + now + ".log", true);
            fh.setFormatter(new java.util.logging.SimpleFormatter());
            LOG.addHandler(fh);
        } catch (SecurityException ex) {
            Logger.getLogger(CrowlInternetArchive.class.getName()).log(Level.SEVERE, null, ex);
        }

        List list = new ArrayList();

        try (Stream<String> stream = Files.lines(Paths.get(sourceFile), StandardCharsets.UTF_8)) {
            stream.parallel().forEach((String l) -> {
                try {
                    ListMultimap<Integer, String> resultmap = crowlPastRDF(l);

                    StringBuilder logInfo = new StringBuilder();
                    logInfo.append(l).append("\n\t");

                    resultmap.keySet().stream()
                            .forEach((Integer key) -> {
                                logInfo.append(key).append(":").append(resultmap.get(key).size()).append(" ");
                            });
                    String except404 = String.valueOf(1 - ((double) resultmap.get(200).size() / resultmap.values().size()));
                    list.add(l + "," + except404);
                    LOG.info(logInfo.append("\n\t").append(except404).toString());
                } catch (InterruptedException ex) {
                    Logger.getLogger(CrowlInternetArchive.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }

        try {
            Path dest = Paths.get(resultFile);
            Files.write(dest, list, Charset.defaultCharset());
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static ListMultimap<Integer, String> crowlPastRDF(String baseURL) throws InterruptedException {
        ListMultimap result = ArrayListMultimap.create();
        try {
            String queryString = queryURL + baseURL + "*";
//            System.setProperty("http.proxyHost", "27.96.46.110");
//            System.setProperty("http.proxyPort", "80");
            System.out.println(queryString);

            String command = "./downloadArchive.sh " + queryString + " " + baseURL.replaceAll("/", ".") + ".html";
            ProcessBroker pb = new ProcessBroker(command.split(" "));
            pb.execute();
//            Document document = Jsoup.connect(queryString)
//                    .timeout(2000000000)
//                    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36")
//                    .get();
            Document document = Jsoup.parse(new File(htmlFile + baseURL.replaceAll("/", ".") + ".html"), "UTF-8");

            Elements elements = document.select(".url a");

            elements.parallelStream()
                    //                    .filter(s -> s.text().endsWith("rdf"))
                    .filter(s -> s.text().endsWith("rdf") || s.text().endsWith("nt") || s.text().endsWith("n3"))
                    //                    .filter(s -> s.text().contains("foaf"))
                    .limit(20000)
                    .forEach((Element e) -> {
                        System.out.println("checking -> " + e.text());
                        result.put(getStatusCode(e.text()), e.text());
                    });

            result.keySet().stream().forEach(k -> {
                System.out.println(k + " " + result.get(k));
            });

            return result;

        } catch (IOException ex) {
            LOG.severe(ex.toString());
        }
        return result;
    }

    public static int getStatusCode(String urlString) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36");
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);

            return connection.getResponseCode();
        } catch (Exception e) {
        } finally {
            connection.disconnect();
        }
        return -1;
    }

    private static final Logger LOG = Logger.getLogger(CrowlInternetArchive.class
            .getName());

}
