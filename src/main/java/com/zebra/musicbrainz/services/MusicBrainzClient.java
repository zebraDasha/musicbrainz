package com.zebra.musicbrainz.services;

import com.zebra.musicbrainz.dtos.AlbumInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MusicBrainzClient {

    private static final String MAIN_EP = "http://musicbrains.org/ws/2";
    private static final String COVER_EP = "http://coverartarchive.org/release/";

    /**
     * Retrieves the album information from MusicBrainz. This method is synchronous and must be
     * called from a Thread!
     *  @param artist The name of the artist. Must be filled.
     * @param album  The name of the album. May be empty.
     */
    public static List<AlbumInfo> getAlbum(String artist, String album) {
        if (artist == null && album == null) {
            return null;
        }

        try {
            String query = "";
            if (artist != null) {
                query += URLEncoder.encode("artist:\"" + artist + "\"", StandardCharsets.UTF_8);
            }
            if (album != null && !album.isEmpty()) {
                query += URLEncoder.encode(" AND release:\"" + album + "\"", StandardCharsets.UTF_8);
            }
            String s = new String(getBytes(MAIN_EP + "/release/", query, false));
            JSONObject object = new JSONObject(s);


            if (object.has("releases")) {
                JSONArray releases = object.getJSONArray("releases");
                final int releasesCount = releases.length();
                if (releasesCount > 0) {
                    AlbumInfo[] infoArray = new AlbumInfo[releasesCount];
                    List<AlbumInfo> albumInfos = new CopyOnWriteArrayList<>();
                    for (int i = 0; i < releasesCount; i++) {
                        AlbumInfo info = new AlbumInfo();

                        JSONObject release = releases.getJSONObject(i);

                        info.id = release.getString("id");
                        try {
                            info.track_count = release.getInt("track-count");
                        } catch (JSONException e) {
                            info.track_count = 0;
                        }

                        albumInfos.add(i,info);
                    }

                    return albumInfos;
                }
            } else if (object.has("error")) {
                throw new RuntimeException();
            }
            return null;
        } catch (JSONException | IOException e) {
            // May happen due to an API error, e.g. error 502
            throw new RuntimeException();
        }
    }

    /**
     * Returns the URL to an image representing the album art of the provided album ID. This album
     * id must be retrieved with {@link #getAlbum(String, String)}
     *
     * @param albumId The album ID
     * @return An album art URL, or null if none found
     */
    public static String getAlbumArtUrl(String albumId) throws RuntimeException {
        try {
            String s = new String(getBytes(COVER_EP + "albumId", "", false));

            JSONObject object = new JSONObject(s);
            JSONArray images = object.getJSONArray("images");
            JSONObject image = images.getJSONObject(0);
            return image.getJSONObject("thumbnails").getString("large");
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    /**
     * Downloads the data from the provided URL.
     *
     * @param inUrl The URL to get from
     * @param query The query field. '?' + query will be appended automatically, and the query data
     *              MUST be encoded properly.
     * @return A byte array of the data
     */
    private static byte[] getBytes(String inUrl, String query, boolean cached)
            throws IOException {
        final String formattedUrl = inUrl + (query.isEmpty() ? "" : ("?" + query));

        URL url = new URL(formattedUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "OmniMusic/1.0-dev (http://www.omnirom.org)");
        urlConnection.setUseCaches(cached);
        urlConnection.setInstanceFollowRedirects(true);
        int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
        urlConnection.addRequestProperty("Cache-Control", "max-stale=" + maxStale);
        try {
            final int status = urlConnection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                int contentLength = urlConnection.getContentLength();
                if (contentLength <= 0) {
                    contentLength = 100 * 1024;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream(contentLength);
                byte[] buffer = new byte[2048];
                BufferedInputStream bis = new BufferedInputStream(in);
                int read;

                while ((read = bis.read(buffer, 0, buffer.length)) > 0) {
                    baos.write(buffer, 0, read);
                }
                return baos.toByteArray();
            } else if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                return new byte[]{};
            } else if (status == HttpURLConnection.HTTP_FORBIDDEN) {
                return new byte[]{};
            } else if (status == HttpURLConnection.HTTP_UNAVAILABLE) {
                throw new RuntimeException();
            } else if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                final String followUrl = urlConnection.getHeaderField("Location");
                return getBytes(followUrl, "", cached);
            } else {
                return new byte[]{};
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}
