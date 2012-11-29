/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orcl.hudson.sso;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.SecurityRealm;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;

import org.kohsuke.stapler.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Date;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataAccessException;

/**
 *
 * @author sdash
 */
public class OSSOSecurityRealm extends SecurityRealm {
    public final String oracleSSOURL;
    public final String hudsonHostName;
    @DataBoundConstructor
    public OSSOSecurityRealm(String oracleSSOURL, String hudsonHostName) {
        this.oracleSSOURL = Util.fixEmptyAndTrim(oracleSSOURL);
        this.hudsonHostName = Util.fixEmptyAndTrim(hudsonHostName);
    }

    public static String getLoggedInUser() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        OSSOAuthentication auth = null;
        synchronized (session) {
            auth = (OSSOAuthentication) session.getAttribute("HUDSON_AUTH_KEY");
        }
        if (auth == null) {
            return null;
        }
        return auth.getName();
    }

    @Override
    protected String getPostLogOutUrl(StaplerRequest req, Authentication auth) {
        HttpSession session = req.getSession(false);
        String id = null;
        if (session != null) {
            id = session.getId();
            //SSOProxy.getInstance().getNameAndRemove(id);
            synchronized (session) {
                session.removeAttribute("HUDSON_AUTH_KEY");
                session.invalidate();
            }
        }
        return "https://login.oracle.com/pls/orasso/orasso.wwsso_app_admin.ls_logout";
    }

    public void doOracleSSOLoggedin(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

        String username = req.getHeader("Proxy-Remote-User");
        if (username == null || username.isEmpty()) {
            username = req.getRemoteUser();
        }
        String id = req.getParameter("HUDSON_SESSION_ID");
        if (username != null && !username.isEmpty()
                && id != null && !id.isEmpty()) {
            SSOProxy.getInstance().addUid(id, username);
            rsp.sendRedirect(hudsonHostName + "/securityRealm/oracleSSOLoggedin");
            return;
        }
        HttpSession session = req.getSession();
        username = SSOProxy.getInstance().getNameAndRemove(session.getId());
        //System.out.println("Got user Name (Header) : " + username);
        if (username == null || username.isEmpty()) {
            if (id != null && !id.isEmpty()) {
                HttpSession s = req.getSession();
                if (s != null && s.getId() != null
                        && s.getId().equals(id)) {
                    username = req.getParameter("HUDSON_USER");
                    //System.out.println("Got user Name (" + id + ") : " + username);
                }
            }
        }
        if (username != null && !username.isEmpty()) {
            session.setAttribute("HUDSON_USER", username);
        }
        String from = (String) session.getAttribute("from");
        if (from == null || from.isEmpty()) {
            rsp.sendRedirect(hudsonHostName);
        } else {
            if (from.startsWith("/hudson")) {
                from = from.replaceFirst("/hudson", "");
            }
            rsp.sendRedirect(hudsonHostName + from);
        }
    }

    public void doOracleSSOLogin(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if (req == null) {
            return;
        }
        HttpSession session = req.getSession(true);
        if (session == null) {
            return;
        }
        String sid = null;
        synchronized (session) {
            sid = session.getId();
        }
        if (sid == null) {
            return;
        }
        String login_url = oracleSSOURL;
        login_url += "?URL=" + hudsonHostName + "&HUDSON_SESSION_ID=" + sid + "&appname=hudson";

        String from = null;
        //  else if (request.requestURI=='/loginError' || request.requestURI=='/login') '/'; else request.requestURI;
        if (req.getParameter("from") != null) {
            from = req.getParameter("from");
        } else if (req.getRequestURI() != null && (req.getRequestURI().equals("/loginError")
                || req.getRequestURI().equals("/login"))) {
            from = "/";
        } else {
            from = req.getRequestURI();
        }
        if (from == null || from.isEmpty()) {
            from = "/";
        }
        //System.out.println("Setting from to " + from);
        if (from.contains("securityRealm")) {
            from = "/";
        }
        session.setAttribute("from", from);

        //System.out.println("Redirecting " + login_url);
        rsp.sendRedirect(login_url);
    }

    @Override
    public String getLoginUrl() {

        //String loginurl = Hudson.getInstance().getRootUrl() + "securityRealm/oracleSSOLogin";
        String loginurl = (hudsonHostName==null)?Hudson.getInstance().getRootUrl():hudsonHostName + "/securityRealm/oracleSSOLogin";
        return loginurl;
    }
    @Override
    public Filter createFilter(FilterConfig filterConfig) {
        Filter ossofilter = new OnlyDoFilter() {
            public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
                final HttpServletRequest request = (HttpServletRequest) servletRequest;
                final HttpSession session = request.getSession(false);
                OSSOAuthentication auth = null;
                if (session != null) {
                    synchronized (session) {
                        auth = (OSSOAuthentication) session.getAttribute("HUDSON_AUTH_KEY");
                    }
                }
                if (auth == null && session != null) {
                    String username = (String) session.getAttribute("HUDSON_USER");
                    if (username != null && !username.isEmpty()) {
                        System.out.println("Creating session for user " + username + " at " + new Date());
                        auth = new OSSOAuthentication(username);
                        session.setAttribute("HUDSON_AUTH_KEY", auth);
                    }
                }

                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                try {
                    filterChain.doFilter(servletRequest, servletResponse);
                } finally {
                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        };
        return ossofilter;
    }
    public SecurityComponents createSecurityComponents() {
        return new SecurityComponents(); // do nothing, falling back to createFilter()
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        return new User(username, "", true, true, true, true,
                new GrantedAuthority[]{AUTHENTICATED_AUTHORITY});
    }
    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        @Override
        public String getDisplayName() {
            return "Oracle SSO by Satyabhanu.Dash@oracle.com";
        }
        
    }
    @Extension
    public static DescriptorImpl install() {
        return new DescriptorImpl();
    }
    private static abstract class OnlyDoFilter implements Filter {

        public void init(FilterConfig filterConfig) throws ServletException {
            // do nothing
        }

        public OnlyDoFilter() {
            super();
        }

        public void destroy() {
            // do nothing
        }
    }
}
