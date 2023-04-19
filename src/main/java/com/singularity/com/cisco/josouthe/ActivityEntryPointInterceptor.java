package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.SDKStringMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ActivityEntryPointInterceptor extends AGenericInterceptor {

    private IReflector pz_CurrentRuleKey;

    public ActivityEntryPointInterceptor() {
        getLogger().info("Initialized Activity Entry Point");
        pz_CurrentRuleKey = getNewReflectionBuilder().accessFieldValue("pz_CurrentRuleKey", true).build();
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.pegarules.pub.runtime.AbstractActivity") //com.pega.pegarules.pub.runtime.AbstractActivity
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                //.methodMatchString("perform") //the beginning of a BT
                .methodStringMatchType(SDKStringMatchType.NOT_EMPTY)
                .build());
        return rules;
    }

    @Override
    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %s ) [%d] params ", className, methodName, paramsToString(param), (param!=null ? param.length : 0)));
        Transaction transaction = AppdynamicsAgent.getTransaction();

        return transaction;
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        if( state == null ) return;
        Transaction transaction = (Transaction) state;
        String activityName = "UNKNOWN-ACTIVITY";
        try {
            String currentRuleKey = pz_CurrentRuleKey.execute(object.getClass().getClassLoader(), object);

            if( currentRuleKey != null ) {
                activityName = currentRuleKey.split("\\s+")[2];
            }
            getLogger().info(String.format("Activity Name: '%s' Current Rule Key '%s' for class '%s'", activityName, currentRuleKey, className));
            transaction.collectData("pz_CurrentRuleKey", currentRuleKey, new HashSet<DataScope>(){{ add(DataScope.SNAPSHOTS); }});
            transaction.collectData("pega-ActivityName", activityName, new HashSet<DataScope>(){{ add(DataScope.SNAPSHOTS); }});
        } catch (ReflectorException e) {
            getLogger().info(String.format("Exception while trying to get attribute %s.pz_CurrentRuleKey", className));
        }
        //AppdynamicsAgent.setCurrentTransactionName("something");
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
            if( i < param.length-1 ) sb.append(", ");
        }
        return sb.toString();
    }

}
