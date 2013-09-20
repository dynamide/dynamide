package com.dynamide.security;

import java.io.FileDescriptor;

import java.net.InetAddress;

import java.security.Permission;

/** This class is used to test against someone trying to install their own SecurityManager.  Dynamide
 * specifically disallows anyone setting any SecurityManager other than com.dynamide.security.SecurityManager
 * and disallows other class loaders.
 */
public class HackerSecurityManager extends SecurityManager {

    public HackerSecurityManager(){
        super();
    }

    public void checkCreateClassLoader() {
            System.out.println("HA HA. HackerSecurityManager ALLOWING checkCreateClassLoader for all.");
    }

    public void checkAccess(Thread g) {
        //Actually, everything is OK.  You can't get out of your thread group.
    }

    public void checkAccess(ThreadGroup g) { }
    public void checkExit(int status) { }
    public void checkExec(String cmd) { }
    public void checkLink(String lib) { }
    public void checkRead(FileDescriptor fd) { }
    public void checkRead(String file) { }
    public void checkRead(String file, Object context) { }
    public void checkWrite(FileDescriptor fd) { }
    public void checkWrite(String file) { }
    public void checkDelete(String file) { }
    public void checkConnect(String host, int port) { }
    public  void checkConnect(String host, int port, Object context) { }
    public void checkListen(int port) { }
    public void checkAccept(String host, int port) { }
    public void checkMulticast(InetAddress maddr) { }
    public void checkMulticast(InetAddress maddr, byte ttl) { }
    public void checkPermission(Permission perm) {}
    public void checkPermission(Permission perm, Object context) {}
    public void checkPropertiesAccess() { }
    public void checkPropertyAccess(String key) { }
    public void checkPropertyAccess(String key, String def) { }
    public boolean checkTopLevelWindow(Object window) { return true; }
    public void checkPrintJobAccess() { }
    public void checkSystemClipboardAccess() { }
    public void checkAwtEventQueueAccess() { }
    public void checkPackageAccess(String pkg) { }
    public void checkPackageDefinition(String pkg) { }
    public void checkSetFactory() { }
    public void checkMemberAccess(Class clazz, int which) { }
    public void checkSecurityAccess(String provider) { }

}
