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
    public java.lang.String getUsefulInfo() {
        return "Yes";
    }
}

