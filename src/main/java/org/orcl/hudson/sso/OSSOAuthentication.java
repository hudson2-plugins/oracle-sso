/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orcl.hudson.sso;

import hudson.model.Hudson;
import hudson.security.SecurityRealm;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.orcl.hudson.misc.LdapCache;
import org.orcl.hudson.misc.OLdap;

/**
 *
 * @author sdash
 */
public class OSSOAuthentication implements Authentication{
        private final String username;
    private final GrantedAuthority[] authorities;

    public OSSOAuthentication(String username) {
        if (username == null || username.isEmpty()) {
            this.username = "anonymous";
        } else {
            this.username = username.toLowerCase();
        }
        OLdap nol = null;
        List<String> employees = LdapCache.getInstance().getEmployeeifExist(username);
        if (employees == null || employees.isEmpty()) {
            try {
                nol = new OLdap();
                if (this.username != null && this.username.indexOf('@') > 1) {
                    employees = nol.getEmpTreeForManagerByEmail(this.username);
                }
            } catch (Exception ex) {
                Logger.getLogger(OSSOAuthentication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        for (String g : Hudson.getInstance().getAuthorizationStrategy().getGroups()) {
            if (g == null || g.isEmpty()) {
                continue;
            }
            String lg = g.toLowerCase();
            if (username.equals(lg) || (employees != null && !employees.isEmpty() && employees.contains(lg))) {
                l.add(new GrantedAuthorityImpl(g));
            }
        }
        l.add(SecurityRealm.AUTHENTICATED_AUTHORITY);
        authorities = l.toArray(new GrantedAuthority[l.size()]);

    }

    public GrantedAuthority[] getAuthorities() {
        return authorities;
    }

    public Object getCredentials() {
        return null;
    }

    public Object getDetails() {
        return null;
    }

    public Object getPrincipal() {
        return username;
    }

    public boolean isAuthenticated() {
        return true;
    }

    public void setAuthenticated(boolean bln) throws IllegalArgumentException {
        System.out.println("Do Nothing");
    }

    public String getName() {
        return username;
    }
}
