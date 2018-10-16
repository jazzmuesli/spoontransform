package com.mycompany.app;


public class Singleton implements com.mycompany.app.ISingleton {
    private static final com.mycompany.app.App.Singleton instance = new com.mycompany.app.App.Singleton();

    public Singleton() {
        com.mycompany.app.App.LOG.info("How bad are private singleton constructors?");
        LOG.info("Empty private Singleton constructors are bad. Now it's public");
    }

    public static com.mycompany.app.App.Singleton getInstance() {
        return com.mycompany.app.App.Singleton.instance;
    }

    @java.lang.Override
    public void doSomething() throws java.lang.Exception {
        com.mycompany.app.App.LOG.info("doSomething");
    }

    private java.lang.String getName() {
        return java.lang.Thread.currentThread().getName();
    }

    @java.lang.Override
    public long currentTime() {
        return java.lang.System.currentTimeMillis();
    }

    @java.lang.Override
    public java.lang.String getUsefulInfo() {
        return "Yes, " + (getName());
    }
}

