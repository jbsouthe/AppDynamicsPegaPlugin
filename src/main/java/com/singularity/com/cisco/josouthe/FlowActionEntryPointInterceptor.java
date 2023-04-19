package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.*;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FlowActionEntryPointInterceptor extends AGenericInterceptor {

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.pegarules.priv.AbstractFUASupport")
                .classMatchType(SDKClassMatchType.INHERITS_FROM_CLASS)
                .methodMatchString("perform") //the beginning of a BT
                .build());
        return rules;
    }

    @Override
    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %d params )", className, methodName, (param!=null ? param.length : 0)));
        //Transaction serviceEndpoint = AppdynamicsAgent.startTransactionAndServiceEndPoint(className, null, "FlowAction."+className, EntryTypes.HTTP, false );
        ExitCall exitCall = AppdynamicsAgent.getTransaction().startExitCall( new HashMap<String,String>(), "FlowAction."+className, ExitTypes.CUSTOM, false);
        getLogger().info(String.format("Exitcall Created: %s",exitCall.getCorrelationHeader()));
        return exitCall;
    }

    public class State{
        public Transaction transaction;
        public ExitCall exitCall;
        public State( Transaction t, ExitCall e ){
            this.transaction=t;
            this.exitCall=e;
        }
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        ExitCall exitCall = (ExitCall) state;
        if( exception != null ) AppdynamicsAgent.getTransaction().markAsError( exception.getMessage() );
        exitCall.end();
    }


}
