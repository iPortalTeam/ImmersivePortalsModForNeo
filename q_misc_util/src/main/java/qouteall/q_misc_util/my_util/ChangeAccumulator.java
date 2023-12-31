package qouteall.q_misc_util.my_util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ChangeAccumulator<KEY> {
    private final Set<KEY> changedKeys = new HashSet<>();
    private final Consumer<KEY> updatingFunction;
    
    public ChangeAccumulator(Consumer<KEY> updatingFunction) {
        this.updatingFunction = updatingFunction;
    }
    
    public synchronized void notifyChanged(KEY key){
        changedKeys.add(key);
    }
    
    public synchronized void processChanges(){
        changedKeys.forEach(updatingFunction);
        changedKeys.clear();
    }
}
