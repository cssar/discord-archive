import java.util.*;

public class User {

    private final String token;
    private String ID;
    private final HashSet<String> channelIDs;
    private final HashSet<String> emptyChannels;
    private final HashMap<String, ArrayList<String>> channelMessageIDs;
    private final ArrayList<String> files;
    private final HashSet<String> fileNames;

    public User(String token) {
        this.token = token;
        this.channelIDs = new HashSet<>();
        this.emptyChannels = new HashSet<>();
        this.channelMessageIDs = new HashMap<>();
        this.files = new ArrayList<>();
        this.fileNames = new HashSet<>();
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void addChannelID(String s) {
        channelIDs.add(s);
    }

    public void addChannelMessageID(String channel, String messageID) {
        if (!channelMessageIDs.containsKey(channel)) {
            channelMessageIDs.put(channel, new ArrayList<>());
        }
        channelMessageIDs.get(channel).add(messageID);
    }

    public void addFile(String file) {
        files.add(file);
    }

    public void clearChannelMessageIDs(){
        this.channelMessageIDs.clear();
    }

    public void clearFiles(){
        this.files.clear();
    }

    public String getToken(){
        return token;
    }

    public String getID(){
        return ID;
    }

    public HashSet<String> getChannelIDs(){
        return channelIDs;
    }

    public HashSet<String> getEmptyChannels(){
        return emptyChannels;
    }

    public HashMap<String, ArrayList<String>> getChannelMessageIDs(){
        return channelMessageIDs;
    }

    public ArrayList<String> getFiles(){
        return files;
    }

    public HashSet<String> getFileNames(){
        return fileNames;
    }
}
