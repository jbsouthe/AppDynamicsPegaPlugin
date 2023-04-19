package com.singularity.com.cisco.josouthe;

import com.appdynamics.agent.api.*;
import com.appdynamics.agent.api.impl.NoOpTransaction;
import com.appdynamics.apm.appagent.api.DataScope;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.template.AGenericInterceptor;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.ReflectorException;

import java.util.*;

/*
[tomcat-http--36] 17 Apr 2023 09:48:35,959  INFO PegaExecutableInterceptor - onMethodBegin com.pega.pegarules.session.internal.mgmt.Executable.doActivity(
    com.pega.pegarules.pub.util.HashStringMap {pxObjClass=Rule-Obj-Activity, pyClassName=Data-Portal, pyActivityName=ReloadSection},
    com.pega.pegarules.data.internal.clipboard.ClipboardPageImpl page "pyDisplayHarness" of class "Data-Portal",
    44/44 props expanded
    expanded properties:
       -BuildChildParentRank
       -CombinedSearchResultsCount = "20"
       -KMBreadcrumbsDisplaySearchText = ""website""
       -KMRecentlyPublished = ""
       -KMSearchDisplayText = ""website""
       -KMSearchText = "website"
       -KMSuggestedContentThankyouNote = "false"
       -MyCancelledItems = ""
       -MyResolvedItems = ""
       -pxCreateDateTime = "20230417T134834.222 GMT"
       -pxCreateOperator = "TNTRAIN28"
       -pxCreateOpName = "TN TRAINER"
       -pxCreateSystemID = "KM_UAT_842"
       -pxObjClass = "Data-Portal"
       -pyActiveWorkGroup = "Default"
       -pyActiveWorkGroupLabel = "Default WorkGroup"
       -pyCommonParams
       -pyCurrentPage = "Work"
       -pyCustomPortalActivity = ""
       -pyCustomPortalParams
       -pyDocumentTitle = ""
       -pyFormPost = ""
       -pyGadget
       -pyID = ""
       -pyLabel = "KMIRX"
       -pyLoadOCX = ""
       -pyOverridePreferences = "false"
       -pyOwner = ""
       -pyPortalPages
       -pyPortalSummary = "Summary for Last 7 Days for Default WorkGroup"
       -pyRefreshInterval = "180"
       -pyReportingDateTimeInterval = "20230411T040000.000 GMT"
       -pyReportingTimeInterval = "Last 7 Days"
       -pyRuleHarness = "RuleForm"
       -pySpecialtyComponentData = ""
       -pyStartPage = "Work"
       -pyTemplateInputBox = ""
       -pyTopPerformers = "Top Performers for Last 7 Days from Default WorkGroup"
       -pyUrgentWorkHeader = "Urgent Work"
       -pyWelcomeHTML = "WelcomeScreen"
       -pyWindowTitle = ""
       -pzDocumentKey = ""
       -SearchTextLength = "100"
       -SelectedFilterValues = "",
   com.pega.pegarules.pub.runtime.ParameterPage {
        BaseReference=D_KMLatestArticles.pxResults(15);
        ReadOnly=-1;
        strPHarnessClass=Data-Portal;
        pzHarnessID=HID3630D98BE08E5694DE83578FCEAB9D61;
        pzPrimaryPageName=pyDisplayHarness;
        StreamClass=Rule-HTML-Section;
        pzKeepPageMessages=true;
        pyLayoutMethodName=simpleLayout_1;
        FormError=;
        pzTransactionId=46f5760b272bc91f385c51837b76f526;
        FieldError=;
        UITemplatingStatus=Y;
        AJAXTrackID=2;
        strPHarnessPurpose=KMHelpSitePortalContent;
        StreamName=KMPublishedArticleLink;
        pzFromFrame=;
        pyActivity=ReloadSection;
        inStandardsMode=true;
        bClientValidation=true;
        pyCallStreamMethod=simpleLayout_1;
        pyCustomError=
    },  )
 */
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
        getLogger().info(String.format("onMethodBegin %s.%s( %s )[%d]", className, methodName, paramsToString(param), (param != null ? param.length : 0)  ));
        Object aKeysStringMap = param[0];
        Object aNewPrimaryPageClipboardPage = param[1];
        Object aNewParamParameterPage = param[2];
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( transaction instanceof NoOpTransaction ) {
            getLogger().info("No active BT, creating a placeholder BT to help resolve");
            //transaction = AppdynamicsAgent.startTransaction("Placeholder", null, EntryTypes.POJO, false );
            return null;
        }

        String activityClass = getString( aKeysStringMap, "pxObjClass");
        if( activityClass == null || activityClass.length() == 0 ) activityClass="Rule-Obj-Activity";
        transaction.collectData("Activity-ObjectClass", activityClass, snapshotDataScopes);

        String activityName = "UNKNOWN-ACTIVITY";
        if( activityClass.equals("Rule-Obj-Activity") || activityClass.equals("Rule-Generated-Activity") || activityClass.equals("Rule-Obj-Validate") ) {
            activityName = getString( aKeysStringMap,"pyActivityName" );
        }
        transaction.collectData("Activity-Name", activityName, snapshotDataScopes);
        //AppdynamicsAgent.startTransactionAndServiceEndPoint("Placeholder", null, String.format("Pega-Activity-%s",activityName), EntryTypes.HTTP, false);
        ExitCall exitCall = transaction.startExitCall( new HashMap<String,String>(), String.format("Pega-Activity-%s",activityName), ExitTypes.CUSTOM, false);
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

        return new State(transaction, exitCall);
    }

    public class State{
        public Transaction transaction;
        public ExitCall exitCall;
        public State( Transaction t, ExitCall e ){
            this.transaction=t;
            this.exitCall=e;
        }
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
        State tranStates = (State) state;
        Transaction transaction = tranStates.transaction;
        ExitCall exitCall = tranStates.exitCall;

        String activityClassName = getString( params[0], "pyClassName" );
        transaction.collectData("Activity-ClassName-postlogic", activityClassName, snapshotDataScopes);
        if( exception != null ) transaction.markAsError( exception.getMessage() );
        exitCall.end();
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
