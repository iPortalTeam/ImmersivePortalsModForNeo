package qouteall.q_misc_util;

public class MiscUtilModEntryClient {

    public void onInitializeClient() {
        ImplRemoteProcedureCall.initClient();
        
        MiscNetworking.initClient();
    }
}
