package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;

import java.util.ArrayList;
import java.util.List;

public class DataTransformEntryPointInterceptor extends AGenericInterceptor {

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.platform.coreassemblers.datatransform.AbstractDataTransform")
                .classMatchType(SDKClassMatchType.INHERITS_FROM_CLASS)
                .methodMatchString("evaluate") //the beginning of a BT
                .build());
        return rules;
    }

    @Override
    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %d params )", className, methodName, (param!=null ? param.length : 0)));
        Transaction serviceEndpoint = AppdynamicsAgent.startTransactionAndServiceEndPoint(className, null, "DataTransform."+className, EntryTypes.POJO, false );
        getLogger().info(String.format("Service Endpoint Created: %s",serviceEndpoint.getUniqueIdentifier()));
        return serviceEndpoint;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction serviceEndpoint = (Transaction) state;
        if( exception != null ) serviceEndpoint.markAsError( exception.getMessage() );
        serviceEndpoint.end();
    }


}
