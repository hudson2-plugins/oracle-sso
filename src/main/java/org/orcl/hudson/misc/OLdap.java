/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orcl.hudson.misc;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 *
 * @author sdash
 */
public class OLdap {
    private static int MAX_RECURSION = 8;
    private static String LDAP_URL = "ldap://stldap.oracle.com:389";
    private static String LDAP_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private static String SIMPLE_AUTH = "simple";
    private DirContext ctx = null;
    private SearchControls cons = null;

    public OLdap() throws Exception {
        this.ctx = getDirContext();
        this.cons = new SearchControls();
        cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
    }

    public void closeContext() {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
            }
            ctx = null;
        }

    }

    private DirContext getDirContext() throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_FACTORY);
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTH);
        return new InitialDirContext(env);
    }

    private NamingEnumeration doSearch(String filter) throws NamingException {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        if (ctx == null) {
            ctx = this.getDirContext();
        }
        return ctx.search("", filter, cons);
    }

    public List<String> getEmpTreeForManagerByEmail(String email) {
        String ldapName = null;
        NamingEnumeration managerInfo = null;
        NamingEnumeration reports = null;
        SearchResult result = null;
        BasicAttributes attrs = null;
        List<String> employees = new ArrayList<String>();
        String filter = "mail=" + email;
        try {
            managerInfo = doSearch(filter);
            while (managerInfo.hasMore()) {
                result = (SearchResult) managerInfo.next();
                ldapName = result.getName();
            }
            if (ldapName == null || ldapName.isEmpty()) {
                return employees;
            }
            filter = "manager=" + ldapName;
            reports = doSearch(filter);
            while (reports.hasMore()) {
                if ((result = (SearchResult) reports.next()) == null) {
                    continue;
                }
                if ((attrs = (BasicAttributes) result.getAttributes())
                        == null) {
                    continue;
                }
                String eml = attrs.get("mail").toString();
                if (eml == null || eml.isEmpty()) {
                    continue;
                }
                eml = eml.replaceFirst("mail:", "").trim();
                employees.add(eml.toLowerCase());
                List<String> subemp = this.getEmpTreeForManagerByEmail(eml);
                if (subemp == null || subemp.isEmpty()) {
                    continue;
                }
                employees.addAll(subemp);
                if(employees.size() > 50) return employees;
            }
        } catch (NamingException e) {
            return employees;
        }
        return employees;
    }

    public BasicAttributes getUserInfo(String filter) throws NamingException {
        if (filter == null || filter.isEmpty()) {
            return null;
        }
        NamingEnumeration names = doSearch(filter);
        BasicAttributes attrs = null;
        while (names.hasMore()) {
            SearchResult result = (SearchResult) names.next();
            if (result == null) {
                continue;
            }
            attrs = (BasicAttributes) result.getAttributes();
            if (attrs == null) {
                continue;
            }
            break;
        }
        return attrs;
    }

    public BasicAttributes getUserInfoByMail(String email) throws NamingException {
        if (email == null || email.isEmpty()) {
            return null;
        }
        String filter = null;
        if (email.indexOf('@') > 1) {
            filter = "mail=" + email;
        } else {
            filter = "mail=" + email + "@oracle.com";
        }
        return getUserInfo(filter);
    }

    public String getAttribute(BasicAttributes attrs,
            String id) throws NamingException {
        if (attrs == null || id == null || id.isEmpty()) {
            return null;
        }
        Attribute attr = attrs.get(id);
        if (attr != null) {
            return (String) attr.get();
        }
        return null;
    }

    public String parseAttribute(String src, String attribute) {
        //cn=LARRY_ELLISON,L=AMER,DC=ORACLE,DC=COM

        if (src == null || attribute == null) {
            return null;
        }

        src = src.toLowerCase();

        String[] parts = src.split(",");
        for (String p : parts) {
            if (p != null && p.indexOf(attribute) >= 0) {
                return p;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        OLdap oracleLdap = new OLdap();
        List<String> employee =
                oracleLdap.getEmpTreeForManagerByEmail("Thomas.Kurian@oracle.com");
        if (employee == null || employee.isEmpty()) {
            System.out.println("Failed to get Employees");
            System.exit(0);
        }
        for (String e : employee) {
            System.out.println(e);
        }
    }
    
}
