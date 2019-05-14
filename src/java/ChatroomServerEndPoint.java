
import java.io.IOException;

import java.io.StringWriter;
import java.util.ArrayList;

import java.util.Collections;

import java.util.HashMap;

import java.util.HashSet;

import java.util.Iterator;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;

import javax.json.JsonArrayBuilder;

import javax.json.JsonObject;

import javax.json.JsonWriter;

import javax.websocket.OnClose;

import javax.websocket.OnMessage;

import javax.websocket.OnOpen;

import javax.websocket.Session;

import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/son")

public class ChatroomServerEndPoint {

    static Set<Session> chatroomUsers = Collections.synchronizedSet(new HashSet<Session>());
    //static Set<Session> chatroomOfflineUsers = Collections.synchronizedSet(new HashSet<Session>());

    static ArrayList<Session> kullanicilar = new ArrayList<Session>();
    static ArrayList<Session> chatroomOfflineUsers = new ArrayList<Session>();

    static ArrayList<String> izinliKullanicilar = new ArrayList<String>();

    private final static HashMap<String, ChatroomServerEndPoint> sockets = new HashMap<>();

    private String myUniqueId;
    static boolean izin = false;
    

    private String getMyUniqueId() {
        // unique ID from this class' hash code
        return Integer.toHexString(this.hashCode());
    }
    

 
  static ScheduledExecutorService timer = 
       Executors.newSingleThreadScheduledExecutor(); 
 
  private static Set<Session> allSessions; 
    
 
   private void sendTimeToAll(Session session){       
     allSessions = session.getOpenSessions();
     
     Timer timer = new Timer();
 timer.schedule(new TimerTask(){
    public void run() {
            for (Session sess: kullanicilar){          
        try{   
            System.out.println("aktif kullanici username = " + sess.getUserProperties().get("username"));

//sess.getBasicRemote().sendText(buildJsonMessageData("Local time: ", sess.getUserProperties().get("username").toString()));
            sess.getBasicRemote().sendText(buildJsonUsersData());
            sess.getBasicRemote().sendText(buildJsonOfflineUsersData());
        
        } catch (IOException ioe) {        
              System.out.println(ioe.getMessage());         
          }   

     }   
    }
  }, 15000);
           
   
     

  }
   
   


    @OnOpen
    public void handleOpen(Session userSession) throws IOException {

        chatroomUsers.add(userSession);
        kullanicilar.add(userSession);
            System.out.println(userSession);

        
        if(kullanicilar.size()==1){
        timer.scheduleAtFixedRate(
             () -> sendTimeToAll(userSession),0,15,TimeUnit.SECONDS);    
        }

        System.out.println("user added");                   //user added
        this.myUniqueId = this.getMyUniqueId();

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + myUniqueId);
        ChatroomServerEndPoint.sockets.put(this.myUniqueId, this);
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + sockets);
    }

    @OnMessage
    public void handleMessage(String message, Session userSession) throws IOException {
        String username = (String) userSession.getUserProperties().get("username");
//        Iterator<Session> itr = chatroomUsers.iterator();
        String[] tokens = message.split("@");
        message = tokens[0];
        if (username == null) {
            userSession.getUserProperties().put("username", message);
            userSession.getBasicRemote().sendText(buildJsonMessageData("System", "You are now connected as  " + message));
//            while (itr.hasNext()) {
//                (itr.next()).getBasicRemote().sendText(buildJsonUsersData());
//            }
            for (int i = 0; i < chatroomOfflineUsers.size(); i++) {
                if (chatroomOfflineUsers.get(i).getUserProperties().get("username").toString().equals(message)) {
                    chatroomOfflineUsers.remove(i);
                    break;
                }
            }
            for (int i = 0; i < kullanicilar.size(); i++) {
                kullanicilar.get(i).getBasicRemote().sendText(buildJsonUsersData());
                kullanicilar.get(i).getBasicRemote().sendText(buildJsonOfflineUsersData());

            }
        } else if (tokens[tokens.length - 1].equals("all")) {
//            while (itr.hasNext()) {
//                itr.next().getBasicRemote().sendText(buildJsonMessageData(username, message));
//
//            }
            for (int i = 0; i < kullanicilar.size(); i++) {
                kullanicilar.get(i).getBasicRemote().sendText(buildJsonMessageData(username, message));
            }
        } else if (tokens[tokens.length - 2].equals("accept")) {
            System.out.println("listeye ekle");
            izinliKullanicilar.add(tokens[1]);
            izinliKullanicilar.add(tokens[2]);
            
            String[] tokens2 = tokens[tokens.length - 1].split(":");

            
            for(int i = 0 ; i < kullanicilar.size();i++){
                if(tokens[2].equals(kullanicilar.get(i).getUserProperties().get("username"))){
                    kullanicilar.get(i).getBasicRemote().sendText(buildJsonMessageData(tokens[2], tokens2[1]));
                    break;
                }
                
            }
            

            System.out.println(izinliKullanicilar.size());

        } else {
            izin = false;
            for (int i = 0; i < kullanicilar.size(); i++) {

                if (kullanicilar.get(i).getUserProperties().get("username").toString().equals(tokens[1])) {
//                    kullanicilar.get(i).getBasicRemote().sendText(buildJsonPrivateMessageData(username, message ,kullanicilar.get(i).getUserProperties().get("username").toString()));

                    if (izinliKullanicilar.size() > 0) {
                        for (int j = 0; j < izinliKullanicilar.size(); j = j + 2) {
                            if (username.equals(izinliKullanicilar.get(j)) && kullanicilar.get(i).getUserProperties().get("username").toString().equals(izinliKullanicilar.get(j + 1))
                                    || username.equals(izinliKullanicilar.get(j + 1)) && kullanicilar.get(i).getUserProperties().get("username").toString().equals(izinliKullanicilar.get(j))) {

                                kullanicilar.get(i).getBasicRemote().sendText(buildJsonMessageData(username, message));
                                userSession.getBasicRemote().sendText(buildJsonMessageData(username, message));
                                izin = true;
                                break;
                            }
                        }
                    } else {
                        kullanicilar.get(i).getBasicRemote().sendText(buildJsonPrivateMessageData(username, message, kullanicilar.get(i).getUserProperties().get("username").toString()));
                        izin = true;
                    }
                    if (izin == false) {
                        kullanicilar.get(i).getBasicRemote().sendText(buildJsonPrivateMessageData(username, message, kullanicilar.get(i).getUserProperties().get("username").toString()));
                    }

                }
                System.out.println(kullanicilar.get(i).getUserProperties().get("username").toString());

            }

//            while (itr.hasNext()) {
//                
//                if(itr.next().getUserProperties().get("username").toString().equals("onur")){
//                                    itr.next().getBasicRemote().sendText(buildJsonMessageData(username, message));
//                }
//                
//                //itr.next().getBasicRemote().sendText(buildJsonMessageData(username, itr.next().getUserProperties().get("username").toString() + " adli kullaniciya mesaj = " + message));
//
//            }
        }
    }

    @OnClose
    public void handleClose(Session userSession) throws IOException {



        // TODO Auto-generated method stub
        System.out.println("user logout");
        chatroomOfflineUsers.add(userSession);
        chatroomUsers.remove(userSession);
        kullanicilar.remove(userSession);

//        for(int i = 0 ; i < kullanicilar.size();i++){
//            kullanicilar.get(i).getBasicRemote().sendText(buildJsonUsersData());
//            kullanicilar.get(i).getBasicRemote().sendText(buildJsonOfflineUsersData());
//        }

//        Iterator<Session> itr = chatroomUsers.iterator();
//        while (itr.hasNext()) {
//            (itr.next()).getBasicRemote().sendText(buildJsonUsersData());
//            (itr.next()).getBasicRemote().sendText(buildJsonOfflineUsersData());
//
//        }
    }

    private String buildJsonUsersData() {

        Iterator<String> itr = getUserNames().iterator();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        while (itr.hasNext()) {
            jsonArrayBuilder.add((String) itr.next());
        }

        return Json.createObjectBuilder().add("users", jsonArrayBuilder).build().toString();

    }

    private String buildJsonOfflineUsersData() {

        Iterator<String> itr = getOfflineUserNames().iterator();
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        while (itr.hasNext()) {
            jsonArrayBuilder.add((String) itr.next());
        }

        return Json.createObjectBuilder().add("offlineUsers", jsonArrayBuilder).build().toString();

    }

    private String buildJsonMessageData(String username, String message) {

        JsonObject jsonObject = Json.createObjectBuilder().add("message", username + " : " + message).build();

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.write(jsonObject);
        }

        return stringWriter.toString();
    }

    private String buildJsonPrivateMessageData(String username, String message, String username2) {

        //username mesajlasmak isteyen kullanici
        //username2 mesajin yollanacagi kullanici
        JsonObject jsonObject = Json.createObjectBuilder().add("private", username + " : " + message + "@" + username + "@" + username2).build();

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stringWriter)) {
            jsonWriter.write(jsonObject);
        }

        return stringWriter.toString();
    }

    private Set<String> getUserNames() {
        HashSet<String> returnSet = new HashSet<String>();

        Iterator<Session> itr = chatroomUsers.iterator();
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@");

        while (itr.hasNext()) {
            returnSet.add(itr.next().getUserProperties().get("username").toString());
        }

        return returnSet;
    }

    private Set<String> getOfflineUserNames() {
        HashSet<String> returnSet = new HashSet<String>();
//
//        Iterator<Session> itr = chatroomOfflineUsers.iterator();
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@");

//        while (itr.hasNext()) {
//            returnSet.add(itr.next().getUserProperties().get("username").toString());
//        }
//        
        for (int i = 0; i < chatroomOfflineUsers.size(); i++) {
            returnSet.add(chatroomOfflineUsers.get(i).getUserProperties().get("username").toString());

        }

        return returnSet;
    }

}
