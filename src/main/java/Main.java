import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean saveFiles;
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Discord Auth Token: ");
        String token = sc.nextLine();

        System.out.print("Would you like to save files/pictures locally before deleting them? [y/n]");
        String response = sc.nextLine();

        saveFiles = response.equals("y") || response.equals("yes");
        sc.close();

        User user = new User(token);
        Request req = new Request(user);

        System.out.println("Fetching user id");
        req.fetchUserID();

        System.out.println("Fetching channel ids");
        req.fetchChannelIDs();

        while(true) {
            System.out.println("Fetching channel messages");

            user.clearChannelMessageIDs();
            user.clearFiles();
            req.fetchChannelMessageIDs();

            if(saveFiles){
                req.downloadFiles();
            }

            int numMessages = 0;
            for(ArrayList<String> a: user.getChannelMessageIDs().values()){
                numMessages += a.size();
            }
            System.out.println("Found " + numMessages + " messages");

            HashMap<String, ArrayList<String>> cmsg = user.getChannelMessageIDs();
            int deleted = 0;
            for (String channel : cmsg.keySet()) {
                ArrayList<String> messages = cmsg.get(channel);
                for (String msg_id : messages) {
                    if (req.deleteChannelMessage(channel, msg_id)) ++deleted;
                }
            }
            if(deleted < 2){
                System.out.println("Finished Deleting");
                break;
            }
        }
    }
}
