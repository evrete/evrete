package org.evrete.showcase.abs.town;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.evrete.showcase.abs.town.json.GeoData;
import org.evrete.showcase.abs.town.types.MapPoint;
import org.evrete.showcase.abs.town.types.XYPoint;
import org.evrete.showcase.shared.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageUtil {
    private final static int step = 4;
    private final static int margin = 6;
    private static final int[][] SCAN_DIRECTION = new int[][]{
            new int[]{0, 1},
            new int[]{0, -1},

            new int[]{-1, 1},
            new int[]{-1, -1},
            new int[]{-1, 0},

            new int[]{1, 1},
            new int[]{1, -1},
            new int[]{1, 0},

    };

    public static void main(String[] args) {
        //readHumanMade();
        //probe();

        //saveHomeCenters();
        //places();
        //businessLocations();
        postProcessProbes();

/*
        MapPoint p = new MapPoint(42.217000, -87.826276);
        XYPoint p2 = new XYPoint(p);
        System.out.println(p2.x + " : " + p2.y);
*/
    }


    private static void places() {
        String urlStart = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=AIzaSyB5WUH1JJLPWlHDzYirhuDG_8qZ6fnkgAo&location=42.186147,-87.830392&radius=10000";
        String urlNext = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?key=AIzaSyB5WUH1JJLPWlHDzYirhuDG_8qZ6fnkgAo&pagetoken=";

        int i = 0;

        String nextToken = null;
        Gson gson = new Gson();
        while (i < 20) {
            File f = new File("places_" + i + ".json");
            String data;
            if (i == 0) {
                data = readUrl(urlStart);
                PlaceBase placeBase = gson.fromJson(data, PlaceBase.class);
                nextToken = placeBase.next();
            } else {
                if (nextToken == null) {
                    break;
                } else {
                    data = readUrl(urlNext + nextToken);
                    PlaceBase placeBase = gson.fromJson(data, PlaceBase.class);
                    nextToken = placeBase.next();
                }
            }


            writeToFile(f, data);
            Utils.delay(10000);
            i++;
        }


    }


    private static void businessLocations() {
        try {
            GeoResponse locations = new Gson().fromJson(new FileReader("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/places.json"), GeoResponse.class);
            List<XYPoint> points = new ArrayList<>();
            System.out.println(locations.results.length);
            for (Result r : locations.results) {
                XYPoint point = new XYPoint(r.geometry.location);
                if (test(point)) {
                    points.add(point);
                }
            }

            writeToFile(new File("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/businesses.json"), new Gson().toJson(points));


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private static void saveHomeCenters() {
        List<XYPoint> homeCenters = new ArrayList<>();
        try (Connection connection = connect()) {

            PreparedStatement reader = connection.prepareStatement("select x,y,json from tmp");
            ResultSet rs = reader.executeQuery();
            Map<String, Result> uniqueResults = new HashMap<>();
            while (rs.next()) {
                String json = rs.getString(3);
                GeoResponse geoResponse = Utils.fromJson(json, GeoResponse.class);
                if ("OK".equals(geoResponse.status)) {
                    if (geoResponse.results == null || geoResponse.results.length == 0) {
                        throw new IllegalStateException();
                    } else {
                        Result result = geoResponse.results[0];
                        String placeId = result.place_id;
                        if (uniqueResults.containsKey(placeId)) {
                            //System.out.println("Dup!!!: " + result + " vs " + uniqueResults.get(placeId));
                        } else {
                            if (result.geometry == null || result.geometry.location == null) {
                                throw new IllegalStateException();
                            }
                            XYPoint point = new XYPoint(result.geometry.location);
                            homeCenters.add(point);
                            uniqueResults.put(placeId, result);
                        }
                    }
                }
            }

            String homeCentersJson = new GsonBuilder().setPrettyPrinting().create().toJson(homeCenters);
            writeToFile(new File("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/homes.json"), homeCentersJson);
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void postProcessProbes() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        GeoData mapData = new GeoData();
        Map<XYPoint, List<XYPoint>> roadAccess = new HashMap<>();
        try {
            XYPoint[] homes = new Gson().fromJson(new FileReader("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/homes.json"), XYPoint[].class);
            XYPoint[] businesses = new Gson().fromJson(new FileReader("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/businesses.json"), XYPoint[].class);
            BufferedImage roadsImg = ImageIO.read(new File("./evrete-showcase/evrete-town-emulation/src/main/webapp/roads.png"));


            Set<XYPoint> deleted = new HashSet<>();

            for (XYPoint center : businesses) {
                if (test(center)) {
                    roadAccess.put(center, nearestRoads(roadsImg, center));
                    mapData.businesses.add(center);
                }
            }

            for (XYPoint center : homes) {
                if (test(center)) {
                    // Test how it's clode to a business
                    for (XYPoint b : mapData.businesses) {
                        if (XYPoint.distance2(b, center) < 25) {
                            deleted.add(center);
                        }
                    }

                    for (XYPoint otherHome : homes) {
                        if (otherHome != center && !deleted.contains(otherHome) && !deleted.contains(center)) {
                            if (XYPoint.distance2(otherHome, center) < 25) {
                                deleted.add(center);
                            }
                        }
                    }


                    if (!deleted.contains(center)) {
                        roadAccess.put(center, nearestRoads(roadsImg, center));
                        mapData.homes.add(center);
                    }

                }
            }

            System.out.println("Deleted: " + deleted.size());
/*
            for (XYPoint center : schools) {
                if (test(center)) {
                    roadAccess.put(center, nearestRoads(roadsImg, center));
                    mapData.schools.add(center);
                }
            }
*/


            System.out.println("Homes: " + mapData.homes.size());
            System.out.println("Businesses: " + mapData.businesses.size());
//            System.out.println("Schools: " + mapData.schools.size());

            writeToFile(new File("./evrete-showcase/evrete-town-emulation/src/main/webapp/WEB-INF/data.json"), gson.toJson(mapData));

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static boolean test(XYPoint center) {
        return center.x >= 0 && center.x < 2048 && center.y >= 0 && center.y < 2048;
    }

    private static List<XYPoint> nearestRoads(BufferedImage roadsImg, XYPoint point) {
        List<XYPoint> result = new ArrayList<>();
        int clr = grayColor(roadsImg, point.x, point.y);
        if (clr < 200) {
            result.add(point);
        } else {
            for (int[] delta : ImageUtil.SCAN_DIRECTION) {
                int x = point.x;
                int y = point.y;
                while (x >= 0 && x < 2048 && y >= 0 && y < 2048) {
                    clr = grayColor(roadsImg, x, y);
                    if (clr < 128) {
                        result.add(new XYPoint(x, y));
                        break;
                    }
                    x = x + delta[0];
                    y = y + delta[1];
                }


            }


        }
        return result;
    }

    static int grayColor(BufferedImage roadsImg, int x, int y) {
        int clr = roadsImg.getRGB(x, y);
        int red = (clr & 0x00ff0000) >> 16;
        int green = (clr & 0x0000ff00) >> 8;
        int blue = clr & 0x000000ff;
        return (red + green + blue) / 3;
    }


    private static void writeToFile(File f, String content) {
        try {
            FileWriter ps = new FileWriter(f);
            ps.write(content);
            ps.flush();
            ps.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

    }

    private static void probe() {


        try {


            Connection connection = connect();
            connection.setAutoCommit(false);
            Gson gson = new Gson();
            JsonReader jsonReader = gson.newJsonReader(new FileReader("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/address-probes.json"));

            HumanMade made = gson.fromJson(jsonReader, HumanMade.class);

            int counter = 0;
            for (Tuple point : made.points) {
                if (exists(connection, point.xy)) {
                    //System.out.println("Exists!!!");
                } else {
                    String data = geoRead(point.ll);
                    if (data != null) {
                        save(connection, point.xy, data);
                        Utils.delay(25);
                    }
                }

                counter++;
                if (counter % 100 == 0) {
                    System.out.println(counter + " of " + made.points.size());
                }
            }


            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static boolean exists(Connection connection, XYPoint point) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("select * from tmp where x=? and y=?");
        stmt.setInt(1, point.x);
        stmt.setInt(2, point.y);
        ResultSet rs = stmt.executeQuery();
        boolean b = rs.next();
        rs.close();
        stmt.close();
        return b;

    }

    private static void save(Connection connection, XYPoint point, String json) throws Exception {
        PreparedStatement stmt = connection.prepareStatement("insert into tmp(x,y,json) values (?,?,?)");
        stmt.setInt(1, point.x);
        stmt.setInt(2, point.y);
        stmt.setString(3, json);
        stmt.executeUpdate();
        connection.commit();
        stmt.close();
    }

    private static Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:postgresql://localhost:5432/trade", "trade", "trade");
        } catch (SQLException e) {
            throw new IllegalStateException();
        }
    }

    private static String geoRead(MapPoint point) {

        String s = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%.8f,%.8f&key=%s&location_type=ROOFTOP&result_type=street_address", point.lat, point.lng, "AIzaSyB5WUH1JJLPWlHDzYirhuDG_8qZ6fnkgAo");
        return readUrl(s);
    }

    private static String readUrl(String s) {
        try {
            URL url = new URL(s);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder ret = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                ret.append(inputLine).append("\n");
            }
            in.close();

            return ret.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void readHumanMade() {
        try {
            BufferedImage img = ImageIO.read(new File("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/map-human-made.png"));


            int centerX, centerY;

            AtomicInteger cnt = new AtomicInteger();
            HumanMade made = new HumanMade();
            int y = margin;
            while (y < img.getHeight() - step - margin) {
                centerY = y + step / 2;
                int x = margin;
                while (x < img.getWidth() - step - margin) {
                    centerX = x + step / 2;

                    int color = avgColor(img, x, y, step);
                    //System.out.println("XX: " + x + " C: " + color);
                    if (color < 250) {
                        cnt.incrementAndGet();
                        XYPoint point1 = new XYPoint(centerX, centerY);
                        MapPoint point2 = new MapPoint(point1);
                        made.points.add(new Tuple(point1, point2));
                    }

                    x += step;
                }
                y += step;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonWriter writer = gson.newJsonWriter(new FileWriter("./evrete-showcase/evrete-town-emulation/src/main/webapp/admin/address-probes.json"));
            gson.toJson(made, made.getClass(), writer);

            writer.flush();
            writer.close();


            System.out.println(cnt.get());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int avgColor(BufferedImage img, int x, int y, int step) {
        long sum = 0;
        for (int i = 0; i < step; i++) {
            for (int j = 0; j < step; j++) {
                int clr = img.getRGB(x + i, y + j);
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;
                sum += (red + green + blue) / 3;
            }
        }
        return (int) (sum / (step * step));
    }


    static class Tuple {
        XYPoint xy;
        MapPoint ll;

        public Tuple(XYPoint xy, MapPoint ll) {
            this.xy = xy;
            this.ll = ll;
        }
    }

    static class HumanMade {
        List<Tuple> points = new ArrayList<>();
    }

    static class GeoResponse {
        String status;
        Result[] results;
    }

    static class Result {
        String place_id;
        PlusCode plus_code;
        String formatted_address;
        Geometry geometry;

        @Override
        public String toString() {
            return "Result{" +
                    "place_id='" + place_id + '\'' +
                    ", formatted_address=" + formatted_address +
                    ", plus_code=" + plus_code +
                    '}';
        }
    }

    static class PlusCode {
        String global_code;

        @Override
        public String toString() {
            return "{" +
                    "global_code='" + global_code + '\'' +
                    '}';
        }
    }


    static class PlaceBase {
        String next_page_token;

        String next() {
            return next_page_token == null || next_page_token.isEmpty() ? null : next_page_token;
        }
    }


    static class Geometry {
        MapPoint location;
    }
}
