package com.mycompany.app;


public interface ISingleton {
    public void doSomething() {
        com.mycompany.app.App.LOG.info("doSomething");
    }

    public java.lang.String getUsefulInfo() {
        return "Yes";
    }
}

