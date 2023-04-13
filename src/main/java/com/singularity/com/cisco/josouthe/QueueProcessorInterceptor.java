package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;

import java.util.ArrayList;
import java.util.List;

//com.pega.pegarules.session.internal.async.agent.QueueProcessor:execute:620
public class QueueProcessorInterceptor extends AGenericInterceptor {

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.pegarules.session.internal.async.agent.QueueProcessor")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("execute") //the beginning of a BT
                .build());
        return rules;
    }

    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %s )", className, methodName, paramsToString(param)));

        return null;
    }

    private Object paramsToString(Object[] param) {
        if( param == null || param.length == 0 ) return "";
        StringBuilder sb = new StringBuilder();
        for( int i =0 ; i< param.length; i++ ) {
            if( param[i] == null ) {
                sb.append("notSure null");
            } else {
                sb.append(param[i].getClass().getCanonicalName());
                sb.append(" ").append(String.valueOf(param[i]));
            }
            if( i < param.length ) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        if( state == null ) return;
        Transaction transaction = (Transaction) state;
        if( exception != null ) transaction.markAsError( exception.getMessage() );
        transaction.end();
    }
}
