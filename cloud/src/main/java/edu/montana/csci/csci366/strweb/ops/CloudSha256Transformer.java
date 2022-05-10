package edu.montana.csci.csci366.strweb.ops;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class is should pass the SHA 256 calculation through to the two listed NODES in
 * the SHA 256 cloud
 *
 * It should dispatch them via a POST with a `Content-Type` header set to `text/plain`
 *
 * It should dispatch the two requests concurrently and then merge them.
 *
 */
public class CloudSha256Transformer {

    List<String> NODES = Arrays.asList("http://localhost:8001", "http://localhost:8002");
    private final String _strings;

    public CloudSha256Transformer(String strings) {
        _strings = strings;
    }

    public String toSha256Hashes() {
        try {
            //split the index into two different parts @ newline
            int index = _strings.indexOf("\n", _strings.length() / 2);
            //take in first half of the piece as chunk.
            String firstChunk = _strings.substring(0, index);
            //http client to take first chunk and create post
            var client = HttpClient.newHttpClient();
            //New post to send to server
            var request = HttpRequest.newBuilder()
                    //first entry in array from node
                    .uri(URI.create(NODES.get(0)))
                    //assign headers
                    .headers("Content-Type", "text/plain")
                    //Post to server with body to do new cloud computation. We send to client and we're going to do the next one to next server
                    .POST(HttpRequest.BodyPublishers.ofString("op=Line+Sha256&strings=" + URLEncoder.encode(firstChunk, StandardCharsets.UTF_8.name())))
                    .build();
            //Send POST for first chunk to the server on 8001
            HttpResponse<String> firstResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            //This is effectively the same thing as before, but for this one we are sending work to a different server.
            //The takeaway from this is that we are distributing operations between two different systems
            String secondChunk = _strings.substring(index);
            var request2 = HttpRequest.newBuilder()
                    //get second entry in server
                    .uri(URI.create(NODES.get(1)))
                    .headers("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString("op=Line+Sha256&strings=" + URLEncoder.encode(secondChunk, StandardCharsets.UTF_8.name())))
                    .build();
            //Send POST for first chunk to the server on 8002
            HttpResponse<String> secondResponse = client.send(request2, HttpResponse.BodyHandlers.ofString());
            //we concatenate the response from our first server with the response from our second server
            return firstResponse.body() + secondResponse.body();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}