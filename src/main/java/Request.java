import java.io.*;
import java.util.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class Request {

    private static final String API = "https://discord.com/api/v9";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36";
    private static long baseSleepTime = 250;
    private static long deleteSleepTime = 800;
    private static final HashSet<String> invalidMessages = new HashSet<>();
    private final User user;

    public Request(User user) {
        this.user = user;
    }

    private void downloadFile(URL url, String fileName){
        try{
            Thread.sleep(baseSleepTime * 2);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", USER_AGENT);
            connection.addRequestProperty("Authorization", user.getToken());
            BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
            FileOutputStream fos = new FileOutputStream(fileName);
            byte[] data = new byte[1024];
            int b;
            while((b = bis.read(data,0,1024)) != -1){
                fos.write(data, 0, b);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private String sendRequest(String method, URL url, long sleepTime) {
        try {
            Thread.sleep(sleepTime);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.addRequestProperty("User-Agent", USER_AGENT);
            connection.addRequestProperty("Authorization", user.getToken());
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = br.readLine();
            connection.disconnect();
            return response;
        } catch (Exception e) {
            String res = e.toString();
            if (res.contains("429")) {
                System.out.println("Rate limited, sleeping...");
                try {
                    Thread.sleep(10000);
                    if(method.equals("GET")) {
                        System.out.print("Increasing base delay to: ");
                        baseSleepTime += 250;
                        System.out.println(baseSleepTime + " ms");
                    } else{
                        System.out.print("Increasing delete delay to: ");
                        deleteSleepTime += 100;
                        System.out.println(deleteSleepTime + " ms");
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } else if (res.contains("403")) {
                if (method.equals("DELETE")) {
                    System.out.println("Invalid message / Already deleted");
                    return "invalid";
                } else {
                    System.out.println("Could not obtain user ID or channel IDs");
                }
            }
        }
        return null;
    }

    public void fetchUserID() {
        try {
            URL url = new URL(API + "/users/@me");
            String response = sendRequest("GET", url, baseSleepTime);
            if(response == null) {
                return;
            }
            JSONParser p = new JSONParser();
            JSONObject o = (JSONObject) p.parse(response);
            user.setID((String) o.get("id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchChannelIDs() {
        try {
            URL url = new URL(API + "/users/@me/channels");
            String response = sendRequest("GET", url, baseSleepTime);
            if(response == null) {
                return;
            }
            JSONParser p = new JSONParser();
            JSONArray a = (JSONArray) p.parse(response);
            for (Object value : a) {
                JSONObject o = (JSONObject) value;
                user.addChannelID((String) o.get("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchChannelMessageIDs() {
        HashSet<String> emptyChannels = user.getEmptyChannels();
        for (String channel : user.getChannelIDs()) {
            System.out.println("Fetching messages in channel " +  channel);
            if(emptyChannels.contains(channel)) {
                continue;
            }
            try {
                URL url = new URL(API + "/channels/" + channel + "/messages/search?author_id=" + user.getID());
                String response = sendRequest("GET", url, baseSleepTime * 2);
                if(response == null) continue;
                JSONParser p = new JSONParser();
                JSONArray json = (JSONArray) ((JSONObject) p.parse(response)).get("messages");
                boolean addedAMessage = false;
                if (json != null)
                    for (Object obj : json) {
                        var attachments = (((JSONObject)((JSONArray) obj).get(0)).get("attachments"));
                        if(attachments != null) {
                            String proxy_url = null;
                            String arrayContents = attachments.toString();
                            if(!arrayContents.equals("[]")) {
                                proxy_url = ((JSONObject) ((JSONArray) attachments).get(0)).get("proxy_url").toString();
                            }
                            if (proxy_url != null) user.addFile(proxy_url);
                        }
                        String id = ((String) ((JSONObject) ((JSONArray) obj).get(0)).get("id"));
                        if(!invalidMessages.contains(id)) {
                            user.addChannelMessageID(channel, id);
                            addedAMessage = true;
                        }
                    }
                if(!addedAMessage) emptyChannels.add(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    boolean deleteChannelMessage(String channelID, String messageID) {
        try {
            if(invalidMessages.contains(messageID))
                return false;
            System.out.println("Deleting message: " + messageID + " in channel: " + channelID);
            URL url = new URL(API + "/channels/" + channelID + "/messages/" + messageID);
            String response = sendRequest("DELETE", url, deleteSleepTime);
            if(response != null && response.equals("invalid")){
                invalidMessages.add(messageID);
                return false;
            }
            return true;
        } catch (Exception e) {
            if (e.toString().contains("429")) {
                System.out.println("Rate limited, sleeping...");
                try {
                    Thread.sleep(30000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
            if (!e.toString().contains("403"))
                e.printStackTrace();
            return false;
        }
    }

    void downloadFiles(){
        try{
            ArrayList<String> files = user.getFiles();
            HashSet<String> fileNames = user.getFileNames();
            for(String file: files){
                String fileName = file.substring(file.lastIndexOf('/') + 1);
                while(fileNames.contains(fileName)){
                    fileName += "*";
                }
                fileNames.add(fileName);
                URL url = new URL(file);
                System.out.println("Downloading file: " + fileName);
                downloadFile(url,fileName);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
