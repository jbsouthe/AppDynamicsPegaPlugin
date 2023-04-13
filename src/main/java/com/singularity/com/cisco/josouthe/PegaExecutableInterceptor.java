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
import java.util.Set;

public class PegaExecutableInterceptor extends AGenericInterceptor {

    IReflector getStringFromStringMap; //StringMap
    IReflector getClassNameClipboardPage; //ClipboardPage
    IReflector getStepPageExecutable; //Executable

    Set<DataScope> snapshotDataScopes;

    public PegaExecutableInterceptor() {
        super();

        this.snapshotDataScopes = new HashSet<>();
        this.snapshotDataScopes.add(DataScope.SNAPSHOTS);

        getStringFromStringMap = getNewReflectionBuilder().invokeInstanceMethod("getString", true, new String[]{ String.class.getCanonicalName() }).build(); //String
        getClassNameClipboardPage = getNewReflectionBuilder().invokeInstanceMethod("getClassName", true).build();
        getStepPageExecutable = getNewReflectionBuilder().invokeInstanceMethod("getStepPage", true).build();
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();
        rules.add(new Rule.Builder("com.pega.pegarules.session.internal.mgmt.Executable")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("doActivity") //the beginning of a BT
                .build());
        return rules;
    }

    @Override
    public Object onMethodBegin(Object object, String className, String methodName, Object[] param) {
        getLogger().info(String.format("onMethodBegin %s.%s( %s )", className, methodName, paramsToString(param)));
        Object aKeysStringMap = param[0];
        Object aNewPrimaryPageClipboardPage = param[1];
        Object aNewParamParameterPage = param[2];
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) {
            getLogger().info("No active BT, creating a placeholder BT to help resolve");
            transaction = AppdynamicsAgent.startTransaction("Placeholder", null, EntryTypes.POJO, false );
        }

        String activityClass = getString( aKeysStringMap, "pxObjClass");
        if( activityClass == null || activityClass.length() == 0 ) activityClass="Rule-Obj-Activity";
        transaction.collectData("Activity-ObjectClass", activityClass, snapshotDataScopes);

        String activityName = "UNKNOWN-ACTIVITY";
        if( activityClass.equals("Rule-Obj-Activity") || activityClass.equals("Rule-Generated-Activity") || activityClass.equals("Rule-Obj-Validate") ) {
            activityName = getString( aKeysStringMap,"pyActivityName" );
        }
        transaction.collectData("Activity-Name", activityName, snapshotDataScopes);

        String activityClassName = getString( aKeysStringMap, "pyClassName");
        if( activityClassName != null ) {
            activityClassName = activityClassName.trim();
        } else {
            if( aNewParamParameterPage != null ) {
                activityClassName = getClassName(aNewPrimaryPageClipboardPage);
            } else {
                activityClassName = getStepPageClassName(object);
            }
        }
        transaction.collectData("Activity-ClassName-prelogic", activityClassName, snapshotDataScopes);

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
            if( i < param.length ) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        Transaction transaction = (Transaction) state;
        String activityClassName = getString( params[0], "pyClassName" );
        transaction.collectData("Activity-ClassName-postlogic", activityClassName, snapshotDataScopes);
        if( exception != null ) transaction.markAsError( exception.getMessage() );
        transaction.end();
    }

    protected Object getReflectiveObject(Object object, IReflector method, Object... args) {
        Object value = null;
        if( object == null || method == null ) return value;
        try{
            if( args.length > 0 ) {
                value = method.execute(object.getClass().getClassLoader(), object, args);
            } else {
                value = method.execute(object.getClass().getClassLoader(), object);
            }
        } catch (ReflectorException e) {
            this.getLogger().debug("Error in reflection call, method: "+ method.getClass().getCanonicalName() +" object: "+ object.getClass().getCanonicalName() +" exception: "+ e.getMessage(),e);
        }
        return value;
    }

    private String getString( Object object, String key) {
        return (String) getReflectiveObject( object, getStringFromStringMap, key);
    }

    private String getClassName( Object object ) {
        return (String) getReflectiveObject(object, getClassNameClipboardPage);
    }

    private String getStepPageClassName( Object object ) {
        return (String) getClassName( getReflectiveObject(object, getStepPageExecutable));
    }
}
