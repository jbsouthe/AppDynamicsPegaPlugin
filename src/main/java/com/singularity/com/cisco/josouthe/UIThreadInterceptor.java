package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.EntryTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.agent.api.impl.NoOpTransaction;
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

public class UIThreadInterceptor extends AGenericInterceptor {

    IReflector mActivityName, mParentRequestorID;

    public UIThreadInterceptor() {
        super();

        mActivityName = getNewReflectionBuilder().accessFieldValue("mActivityName", true).build();
        mParentRequestorID = getNewReflectionBuilder().accessFieldValue("mParentRequestorID", true).build();
    }

    @Override
    public List<Rule> initializeRules() {
        /* some callers on the run side:
        com.pega.platform.serviceregistry.internal.PrThreadFactory$PrpcThread:run:71
        com.pega.pegarules.session.internal.async.agent.PRTimer$RunLoop:run:333
        com.pega.dsm.dnode.impl.prpc.PrpcThreadFactory$PrpcThread:run:124
        com.pega.pegarules.priv.factory.AbstractObjectFactory abstract
         */
        List<Rule> rules = new ArrayList<Rule>();
        for( String method : new String[]{ "add", "remove"}) {
            rules.add(new Rule.Builder("com.pega.pegarules.priv.factory.AbstractObjectFactory")
                    .classMatchType(SDKClassMatchType.INHERITS_FROM_CLASS)
                    .methodMatchString(method) //the handoff of a BT
                    .build());
            rules.add(new Rule.Builder("ThreadFactory")
                    .classStringMatchType(SDKStringMatchType.ENDSWITH)
                    .methodMatchString(method) //the handoff of a BT
                    .build());
        }
        return rules;
    }

    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %s )[%d]", className, methodName, paramsToString(param), (param != null ? param.length : 0)));
        if( methodName.equals("add") ) {
            Transaction transaction = AppdynamicsAgent.getTransaction();
            transaction.markHandoff( param[0] );
        }
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
            if( i < param.length-1 ) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {

        if( methodName.equals("remove") ) {
            AppdynamicsAgent.startSegment(returnVal);
        }
    }
}
