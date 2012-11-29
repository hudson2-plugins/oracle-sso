/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orcl.hudson.misc;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sdash
 */
public class LdapCache {
    private static final LdapCache instance = new LdapCache();
    private final HashMap<String, Manager> mtable;

    private LdapCache() {
        mtable = new HashMap<String, Manager>();
    }

    public static LdapCache getInstance() {
        return instance;
    }

    public List<String> getEmployeeifExist(String manager) {
        if (manager == null || manager.isEmpty() || manager.indexOf('@') < 1) {
            return null;
        }
        String lcm = manager.toLowerCase();
        synchronized (mtable) {
            if (mtable != null && !mtable.isEmpty() && mtable.containsKey(lcm)) {
                Manager m = mtable.get(lcm);
                if (m != null) {
                    return m.employees;
                }
            }
        }
        return null;
    }

    public List<String> getEmployees(String manager) {
        if (manager == null || manager.isEmpty() || manager.indexOf('@') < 1) {
            return null;
        }
        String lcm = manager.toLowerCase();
        long ct = (new Date()).getTime();
        synchronized (mtable) {
            if (mtable.containsKey(lcm)) {
                Manager m = mtable.get(lcm);
                if (m != null && (ct - m.latupdatetime < 86400000)) {
                    return m.employees;
                }
            }
            OLdap nol = null;
            try {
                nol = new OLdap();
                System.out.println("Getting manager Info for User " + lcm);
                List<String> empls = nol.getEmpTreeForManagerByEmail(lcm);
                mtable.put(lcm, new Manager(empls, (new Date()).getTime()));
                return empls;
            } catch (Exception ex) {
                Logger.getLogger(LdapCache.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    class Manager {

        private List<String> employees = null;
        private long latupdatetime = 0L;

        public Manager(List<String> employees, long latupdatetime) {
            this.latupdatetime = latupdatetime;
            if (employees != null && !employees.isEmpty()) {
                this.employees = employees;
            }
        }

        public List<String> getEmployees() {
            return employees;
        }

        public void setEmployees(List<String> employees) {
            this.employees = employees;
        }

        public long getLatupdatetime() {
            return latupdatetime;
        }

        public void setLatupdatetime(long latupdatetime) {
            this.latupdatetime = latupdatetime;
        }
    }
}
