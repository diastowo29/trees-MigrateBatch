package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class LdapUtil {

	public static boolean validateLdap(String ldapHost, String domain, String username, String password) {
		boolean valid = false;
		String url = "ldap://" + ldapHost + "/";

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, username + "@" + domain);
		env.put(Context.SECURITY_CREDENTIALS, password);

		try {
			DirContext ctx = new InitialDirContext(env);
			if (ctx != null) {
				valid = true;
				ctx.close();
			}
		} catch (AuthenticationNotSupportedException ex) {
			System.out.println("The authentication is not supported by the server");
		} catch (AuthenticationException ex) {
			System.out.println("incorrect password or username");
		} catch (NamingException ex) {
			System.out.println("error when trying to create the context");
		}
		return valid;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayList getEmailAndFullName(String ldapHost, String username, String password) {
		ArrayList arraylist = new ArrayList();

		String url = "ldap://" + ldapHost + "/";

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, username);
		env.put(Context.SECURITY_CREDENTIALS, password);

		try {
			DirContext ctx = new InitialDirContext(env);
			if (ctx != null) {
				String searchBase = "dc=trees,dc=co,dc=id";
				String filter = "(&(objectCategory=Person))";
				SearchControls searchCtls = new SearchControls();
				String[] attributes = { "mail", "email", "cn", "displayName", "member", "memberof", "zendeskflag", "logonName" };
				searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
				searchCtls.setReturningAttributes(attributes);
				NamingEnumeration<SearchResult> userData = ctx.search(searchBase, filter, searchCtls);
				while (userData.hasMoreElements()) {
					HashMap<String, String> data = new HashMap<String, String>();
					SearchResult result = (SearchResult) userData.next();
					Attributes attrs = result.getAttributes();
					// System.out.println(attrs.size());
					if (attrs.get("memberOf") != null) {
						if (attrs.get("memberOf").get().toString().contains("ZD_FA")
								|| attrs.get("memberOf").get().toString().contains("ZD_LA")) {
							System.out.println(attrs);
							data.put("name", attrs.get("displayName").get().toString());
							data.put("cn", attrs.get("cn").get().toString());
							if (attrs.get("mail") != null) {
								data.put("mail", attrs.get("mail").get().toString());
							} else {
								data.put("mail", "null");
							}
							/*FIXME add logonname*/
//							if(attrs.get("logonName")!=null){
//								data.put("msadusername", attrs.get("logonName").toString());
//							} else {
//								data.put("msadusername", "null");
//							}
							if (attrs.get("zendeskflag") != null) {
								data.put("zendeskflag", attrs.get("zendeskflag").get().toString());
							} else {
								data.put("zendeskflag", "null");
							}
							arraylist.add(data);
						}
					}
				}
				ctx.close();
			}
		} catch (AuthenticationNotSupportedException ex) {
			System.out.println("The authentication is not supported by the server");
		} catch (AuthenticationException ex) {
			System.out.println("incorrect password or username");
		} catch (NamingException ex) {
			System.out.println("error when trying to create the context");
		}
		return arraylist;
	}

	public static boolean modifiedAtr(String ldapHost, String username, String password, String name) {
		System.out.println("attribute user: " + name);
		String url = "ldap://" + ldapHost + "/";

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, url);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, username);
		env.put(Context.SECURITY_CREDENTIALS, password);

		try {
			DirContext ctx = new InitialDirContext(env);
			if (ctx != null) {
				String searchBase = "dc=trees,dc=co,dc=id";
				String filter = "(&(objectCategory=Person)(cn=" + name + "))";
				SearchControls searchCtls = new SearchControls();
				searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
				/*NamingEnumeration<SearchResult> userData = */ctx.search(searchBase, filter, searchCtls);
				ModificationItem[] mods = new ModificationItem[1];
				javax.naming.directory.Attribute mod0 = new BasicAttribute("zendeskflag", "y");
				mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);
				ctx.modifyAttributes("cn=" + name + ", cn=users, dc=trees, dc=co, dc=id", mods);
				System.out.println("working..");
				ctx.close();
			}
		} catch (AuthenticationNotSupportedException ex) {
			System.out.println("The authentication is not supported by the server");
		} catch (AuthenticationException ex) {
			System.out.println("incorrect password or username");
		} catch (NamingException ex) {
			System.out.println("error when trying to create the context/modificatioin failed");
			return false;
		}
		return true;
	}
}
