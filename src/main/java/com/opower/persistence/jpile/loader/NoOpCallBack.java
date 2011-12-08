package com.opower.persistence.jpile.loader;

/**
 * A callback class for HierarchicalInfileObjectLoader that does nothing
 *
 * @author amir.raminfar
 */
class NoOpCallBack implements HierarchicalInfileObjectLoader.CallBack {

    @Override
    public void onBeforeSave(Object o) {
    }

    @Override
    public void onAfterSave(Object o) {
    }
}
