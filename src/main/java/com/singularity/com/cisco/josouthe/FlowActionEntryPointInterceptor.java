package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;

import java.util.ArrayList;
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
        Transaction serviceEndpoint = AppdynamicsAgent.startTransactionAndServiceEndPoint(className, null, "FlowAction."+className, EntryTypes.POJO, false );
        return serviceEndpoint;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction serviceEndpoint = (Transaction) state;
        if( exception != null ) serviceEndpoint.markAsError( exception.getMessage() );
        serviceEndpoint.end();
    }


}
