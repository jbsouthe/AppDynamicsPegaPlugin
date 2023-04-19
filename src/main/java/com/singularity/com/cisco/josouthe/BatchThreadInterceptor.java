package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.agent.api.impl.NoOpTransaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BatchThreadInterceptor extends AGenericInterceptor {

    IReflector mActivityName, mParentRequestorID;

    public BatchThreadInterceptor() {
        super();

        mActivityName = getNewReflectionBuilder().accessFieldValue("mActivityName", true).build();
        mParentRequestorID = getNewReflectionBuilder().accessFieldValue("mParentRequestorID", true).build();
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.pegarules.session.internal.async.BatchRequestorTask")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("run") //the beginning of a BT
                .build());
        return rules;
    }

    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %s )[%d]", className, methodName, paramsToString(param), (param != null ? param.length : 0)));
        Transaction transaction = AppdynamicsAgent.getTransaction();
        String activityName = "UNKNOWN-ACTIVITY";
        try {
            activityName = mActivityName.execute(object.getClass().getClassLoader(), object);
        } catch (ReflectorException e) {
            getLogger().info("Error trying to access the batch job mActivityName, Exception: "+ e.getMessage());
        }
        if( transaction instanceof NoOpTransaction ) {
            transaction = AppdynamicsAgent.startTransaction(String.format("PegaBatch-%s", activityName), null, EntryTypes.POJO, false);
        }
        transaction.collectData("Activity-Name", activityName, new HashSet<DataScope>(){{ add(DataScope.SNAPSHOTS); }});
        return transaction;
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

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        if( state == null ) return;
        Transaction transaction = (Transaction) state;
        if( exception != null ) transaction.markAsError( exception.getMessage() );
        transaction.end();
    }
}
