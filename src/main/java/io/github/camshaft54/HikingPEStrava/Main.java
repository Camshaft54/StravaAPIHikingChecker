package io.github.camshaft54.HikingPEStrava;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;


public class Main {
    public static HashMap<String, String[]> refreshCodes;

    static {
        try {
            refreshCodes = readRefreshCodesFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, ParseException, java.text.ParseException {
        BufferedReader br = new BufferedReader(new FileReader("clientConfig.txt"));
        long clientId = Long.parseLong(br.readLine().split(": ")[1]);
        String clientSecret = br.readLine();

        Scanner scan = new Scanner(System.in);
        System.out.print("Enter \"1\" to check PE, enter \"2\" to submit refresh code: ");
        switch (scan.nextLine()) {
            case "1" -> {
                System.out.print("Enter earliest time for activities (eg. Feb 11 2021 14:45 PST): ");
                checkPE(clientId, clientSecret, scan.nextLine());
            }
            case "2" -> {
                System.out.print("Enter an Authorization Code from Oauth: ");
                String[] athlete = getRefreshCodeAndInfoFromAuthCode(clientId, clientSecret, scan.nextLine());
                refreshCodes.put(athlete[0], Arrays.copyOfRange(athlete, 1, 3));
                writeRefreshCodesToFile();
                System.out.println("Added " + athlete[1] + " to refreshCodes.txt");
            }
            default -> System.out.println("Invalid Request");
        }
    }

    public static void checkPE(long clientId, String clientSecret, String timeStr) throws IOException, ParseException, java.text.ParseException { // time = MMM dd yyyy HH:mm zzz (eg. Feb 12 2021 13:16 PST)
        SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy HH:mm zzz");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = df.parse(timeStr);
        String epochTime = ("" + date.getTime());
        epochTime = epochTime.substring(0, epochTime.length()-3);
        for (String id : refreshCodes.keySet()) {
            String accessToken = getAccessToken(clientId, clientSecret, refreshCodes.get(id)[1]);
            System.out.println(refreshCodes.get(id)[0] + ": " + getActivityTimeSinceTime(accessToken, epochTime));
        }
    }

    public static void writeRefreshCodesToFile() throws IOException {
        HashMap<String, String[]> refreshCodesFromFile = readRefreshCodesFromFile();
        BufferedWriter bf = new BufferedWriter(new FileWriter("refreshCodes.txt", true));
        for (String id : refreshCodes.keySet()) {
            if (!refreshCodesFromFile.containsKey(id))
                bf.write(id + ": " + refreshCodes.get(id)[0] + ", " + refreshCodes.get(id)[1]);
        }
        bf.close();
    }

    public static HashMap<String, String[]> readRefreshCodesFromFile() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("refreshCodes.txt"));
        HashMap<String, String[]> refreshCodes = new HashMap<>();
        String line;
        while ((line = br.readLine()) != null) {
            String lineArrKey = line.split(": ")[0];
            String[] lineArrValues = line.split(": ")[1].split(", ");
            refreshCodes.put(lineArrKey, lineArrValues);
        }
        return refreshCodes;
    }

    public static String[] getRefreshCodeAndInfoFromAuthCode(long clientId, String clientSecret, String authCode) throws IOException, ParseException {
        String[] athleteInfo = new String[3];

        URL url = new URL("https://www.strava.com/oauth/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + authCode + "&grant_type=authorization_code");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Length", "0");

        String jsonString = new BufferedReader(new InputStreamReader(http.getInputStream())).readLine();
        JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
        http.disconnect();

        athleteInfo[2] = json.get("refresh_token").toString();
        JSONObject athlete = (JSONObject) new JSONParser().parse(json.get("athlete").toString());
        athleteInfo[0] = athlete.get("id").toString();
        athleteInfo[1] = athlete.get("firstname").toString() + " " + athlete.get("lastname").toString();
        return athleteInfo;
    }

    public static String getAccessToken(long clientId, String clientSecret, String refreshToken) throws IOException, ParseException {
        URL url = new URL("https://www.strava.com/oauth/token?client_id=" + clientId + "&client_secret=" + clientSecret + "&refresh_token=" + refreshToken + "&grant_type=refresh_token");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setRequestProperty("Content-Length", "0");

        String jsonString = new BufferedReader(new InputStreamReader(http.getInputStream())).readLine();
        http.disconnect();

        JSONObject json = (JSONObject) new JSONParser().parse(jsonString);
        return json.get("access_token").toString();
    }

    public static long getActivityTimeSinceTime(String accessToken, String timestamp) throws IOException, ParseException {
        URL url = new URL("https://www.strava.com/api/v3/athlete/activities?after=" + timestamp + "&per_page=30");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestProperty("Authorization", "Bearer " + accessToken);
        String jsonString = new BufferedReader(new InputStreamReader(http.getInputStream())).readLine();
        http.disconnect();
        JSONArray activities = (JSONArray) new JSONParser().parse(jsonString);
        long totalTime = 0;
        for (Object obj : activities) {
            JSONObject activity = (JSONObject) new JSONParser().parse(obj.toString());
            totalTime += (Long) activity.get("moving_time");
        }
        return totalTime;
    }

    public static void printClubActivityFulfillment() throws IOException, ParseException {
        URL url = new URL("https://www.strava.com/api/v3/clubs/CLUBIDHERE/activities?per_page=18&access_token=ENTERACCESSTOKENHERE");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        System.out.println("Page downloaded.");
        JSONArray activities = (JSONArray) new JSONParser().parse(reader.readLine());
        ArrayList<User> users = new ArrayList<>();
        for (Object obj : activities) {
            JSONObject activity = (JSONObject) new JSONParser().parse(obj.toString());
            JSONObject athlete = (JSONObject) new JSONParser().parse(activity.get("athlete").toString());
            String name = athlete.get("firstname") + " " + athlete.get("lastname");
            Long time = (Long) activity.get("moving_time");
            boolean alreadyInList = false;
            for (User user : users) {
                if (user.name.equals(name)) {
                    alreadyInList = true;
                    user.addTime(time);
                }
            }
            if (!alreadyInList) {
                users.add(new User(name, time));
            }
        }

        for (User user : users) {
            if (user.isGood())
                System.out.println(user.name + " " + user.isGood());
            else
                System.out.println(user.name + " " + user.isGood() + " " + user.getTime());
        }
    }
}
