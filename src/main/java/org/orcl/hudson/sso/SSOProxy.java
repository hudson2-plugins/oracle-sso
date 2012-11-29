/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orcl.hudson.sso;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author sdash
 */
public class SSOProxy {
    private static final SSOProxy instance = new SSOProxy();
    private final HashMap<String, sUserSession> utable =
        new HashMap<String, sUserSession>();

    private SSOProxy() {
        super();
    }

    public static SSOProxy getInstance() {
        return instance;
    }
    public HashMap<String,sUserSession> displayUsers() {
        HashMap<String,sUserSession> tmd = null;
        synchronized(utable)  {
            if(utable != null && !utable.isEmpty()) {
                tmd = new HashMap<String,sUserSession>(utable);
            }
        }
        return tmd;
    }
    public void removeOldSession() {
        HashSet<String> tmp = new HashSet<String>();
        Date d = new Date();
        synchronized(utable) {
            if(utable == null || utable.isEmpty()) return;
            for(String key : utable.keySet()) {
                if(key == null || key.isEmpty()) {
                    System.out.println("FATAL ERROR: There are null value in USer session" );
                    continue;
                }
                sUserSession us = utable.get(key);
                if(us == null) {
                    System.out.println("FATAL ERROR: This is how come possible of null session value for sid " + key);
                    continue;
                }
                if(d.getTime() - us.getLoggedinTime().getTime() > 300000) {
                    System.out.println("Found Session older than 5 min " + key + " => " + us.getName());
                    tmp.add(key);
                }
            }
            if(!tmp.isEmpty()) {
                for(String key : tmp) {
                    if(key == null || key.isEmpty()) continue;
                    utable.remove(key);
                    System.out.println("Removing older session " + key );
                }
            }
        }
    }
    public boolean addUid(String sid, String uid) {
        if (sid == null || sid.isEmpty() || uid == null || uid.isEmpty())
            return false;
        this.removeOldSession();
        sUserSession us = new sUserSession(uid,new Date());
        synchronized (utable) {
            if (utable.containsKey(sid))
                return false;
            utable.put(sid, us);
        }
        return true;
    }

    public String getUname(String sid) {
        if (sid == null || sid.isEmpty())
            return null;
        sUserSession us = null;
        synchronized (utable) {
            us = utable.get(sid);
        }
        if(us == null) return null;
        return us.getName();
    }

    public String getNameAndRemove(String sid) {
        if (sid == null || sid.isEmpty())
            return null;
        sUserSession us = null;
        synchronized (utable) {
            if (utable.containsKey(sid)) {
                us = utable.get(sid);
                utable.remove(sid);
            }
        }
        if(us == null) return null;
        return us.getName();
    }
    public static class sUserSession {
        private String name = null;
        private Date loggedinTime = null;
        public sUserSession(String name,Date loggedinTime) {
            this.name = name;
            this.loggedinTime = loggedinTime;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setLoggedinTime(Date loggedinTime) {
            this.loggedinTime = loggedinTime;
        }

        public Date getLoggedinTime() {
            return loggedinTime;
        }
    }
}
