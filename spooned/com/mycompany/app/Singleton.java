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
        java.util.Properties props = new java.util.Properties();
        try {
            props.load(getClass().getResourceAsStream("app.properties"));
        } catch (java.io.IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        java.lang.String name = "Unknown";
        try {
            name = props.getProperty("name");
        } finally {
            // Violation found by PMD
            return name;
        }
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

