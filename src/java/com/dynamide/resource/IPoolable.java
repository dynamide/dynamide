package com.dynamide.resource;


public interface IPoolable {

    //hmmmmm, do you store the poolable objects, or the factory, or both?

    public String getFactoryClassName();
    public String getKey();
    public int getMaxSize();
    public int getMinSize();
    public int getGrowSize(int currentSize);
    /** Implementors should point this to Pool.recycle(IPoolable), which will eventually call onReclaim().*/
    public void recycle();
    public void onInit();
    public void onUse();
    public void onReclaim();
    public void onDispose();

}
