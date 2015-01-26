/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.ac.ipu.ds.nekottyo.toolmaven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author nekottyo
 */
public class CountRedirect {

    private static final String filePath = "/Users/nekottyo/Downloads/redirects_en.nt";

    public static void main(String[] args) throws IOException {
        System.out.println(getRedirectedList(filePath).size());
    }

    public static List getRedirectedList(String filePath) throws IOException {
        try (Stream<String> stream = Files.lines(Paths.get(filePath), Charset.defaultCharset())) {
            return stream
                    .map(s -> {
                        s = s.substring(s.indexOf(">") + 1, s.length());
                        return s.substring(s.indexOf(">") + 1, s.length() - 1).trim();
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

}
