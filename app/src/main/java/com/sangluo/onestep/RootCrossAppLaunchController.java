package com.sangluo.onestep;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Intercepts cross-package starts while a OneStep virtual display owns focus. */
final class RootCrossAppLaunchController extends Binder {
    private static final String TAG = "OneStepLaunchRouter";
    private static final String DESCRIPTOR = "android.app.IActivityController";

    private final RootVirtualDisplayBridge displayBridge;
    private final int activityStartingTransaction;
    private final int activityResumingTransaction;
    private final int appCrashedTransaction;
    private final int appEarlyNotRespondingTransaction;
    private final int appNotRespondingTransaction;
    private final int systemNotRespondingTransaction;

    private RootCrossAppLaunchController(RootVirtualDisplayBridge displayBridge,
                                         Class<?> controllerStubClass)
            throws ReflectiveOperationException {
        this.displayBridge = displayBridge;
        activityStartingTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_activityStarting");
        activityResumingTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_activityResuming");
        appCrashedTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_appCrashed");
        appEarlyNotRespondingTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_appEarlyNotResponding");
        appNotRespondingTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_appNotResponding");
        systemNotRespondingTransaction = readTransaction(
                controllerStubClass, "TRANSACTION_systemNotResponding");
        attachInterface(null, DESCRIPTOR);
    }

    static RootCrossAppLaunchController install(RootVirtualDisplayBridge displayBridge)
            throws ReflectiveOperationException {
        Object activityTaskManager = RootActivityManagerCompat.getTaskService();

        Class<?> controllerInterface = Class.forName("android.app.IActivityController");
        Class<?> controllerStubClass = Class.forName("android.app.IActivityController$Stub");
        RootCrossAppLaunchController controller = new RootCrossAppLaunchController(
                displayBridge, controllerStubClass);
        Object controllerProxy = Proxy.newProxyInstance(
                controllerInterface.getClassLoader(),
                new Class<?>[] {controllerInterface},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("asBinder".equals(name)) {
                        return controller;
                    }
                    if ("toString".equals(name)) {
                        return "OneStepRootCrossAppLaunchController";
                    }
                    if ("hashCode".equals(name)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(name)) {
                        return proxy == (args == null ? null : args[0]);
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return true;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    return null;
                });
        Method setActivityController = activityTaskManager.getClass().getMethod(
                "setActivityController", controllerInterface, boolean.class);
        setActivityController.setAccessible(true);
        setActivityController.invoke(activityTaskManager, controllerProxy, false);
        Log.i(TAG, "installed root activity launch controller");
        return controller;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);
        if (code == activityStartingTransaction) {
            Intent intent = data.readInt() == 0 ? null : Intent.CREATOR.createFromParcel(data);
            String targetPackage = data.readString();
            boolean allow = shouldAllowStart(intent, targetPackage);
            reply.writeNoException();
            reply.writeInt(allow ? 1 : 0);
            return true;
        }
        if (code == activityResumingTransaction) {
            data.readString();
            reply.writeNoException();
            reply.writeInt(1);
            return true;
        }
        if (code == appCrashedTransaction) {
            data.readString();
            data.readInt();
            data.readString();
            data.readString();
            data.readLong();
            data.readString();
            reply.writeNoException();
            reply.writeInt(0);
            return true;
        }
        if (code == appEarlyNotRespondingTransaction
                || code == appNotRespondingTransaction) {
            data.readString();
            data.readInt();
            data.readString();
            reply.writeNoException();
            reply.writeInt(0);
            return true;
        }
        if (code == systemNotRespondingTransaction) {
            data.readString();
            reply.writeNoException();
            reply.writeInt(0);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    /**
     * Payment verification chains (Alipay fingerprint / password) die inside a
     * container: the verification activity is finished before its auth dialog can
     * appear. Reroute such launches to the physical display with the original intent
     * intact — the system_server identity can start not-exported components and
     * keeps every extra, unlike a shell `am start` replay.
     */
    private boolean shouldBypassToPhysicalDisplay(Intent intent, String targetPackage) {
        ComponentName component = intent == null ? null : intent.getComponent();
        if (component == null) {
            return false;
        }
        boolean unreachableOrNotExported;
        try {
            android.content.pm.ActivityInfo info = displayBridge.context()
                    .getPackageManager()
                    .getActivityInfo(component, 0);
            unreachableOrNotExported = info == null || !info.exported;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            unreachableOrNotExported = true;
        } catch (RuntimeException e) {
            Log.w(TAG, "component reachability check failed: "
                    + e.getClass().getSimpleName());
            return false;
        }
        boolean bypass = CrossAppLaunchRoutingPolicy.shouldBypassContainerRouting(
                targetPackage, unreachableOrNotExported);
        if (bypass) {
            launchOnDefaultDisplay(intent, targetPackage);
        }
        return bypass;
    }

    private void launchOnDefaultDisplay(Intent intent, String targetPackage) {
        try {
            Intent physical = new Intent(intent);
            physical.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(Display.DEFAULT_DISPLAY);
            displayBridge.context().startActivity(
                    physical, options.toBundle());
            Log.i(TAG, "launched on default display, bypassing container routing: "
                    + "target=" + targetPackage
                    + " component=" + intent.getComponent());
        } catch (RuntimeException e) {
            Log.w(TAG, "default-display launch failed, letting the original start "
                    + "proceed: target=" + targetPackage + ", error="
                    + e.getClass().getSimpleName());
        }
    }

    private boolean shouldAllowStart(Intent intent, String targetPackage) {
        if (intent == null || targetPackage == null || targetPackage.isEmpty()
                || displayBridge.consumeLaunchBypass(targetPackage)) {
            return true;
        }
        try {
            RootVirtualDisplayBridge.RoutingSource source =
                    displayBridge.getArmedRoutingSource(targetPackage);
            if (source == null) {
                return true;
            }
            if (CrossAppLaunchRoutingPolicy.shouldBypassHomeLaunch(
                    intent.getAction(), intent.hasCategory(Intent.CATEGORY_HOME),
                    intent.hasCategory(Intent.CATEGORY_SECONDARY_HOME))) {
                displayBridge.consumeArmedRoutingSource(source);
                Log.i(TAG, "bypassed HOME launch routing: display=" + source.displayId
                        + " target=" + targetPackage);
                return true;
            }
            if (CrossAppLaunchRoutingPolicy.shouldPreserveCallerTask(
                    intent.getAction(), intent.getFlags())) {
                displayBridge.consumeArmedRoutingSource(source);
                Log.i(TAG, "preserved activity-result launch on source display: source="
                        + source.packageName + " display=" + source.displayId
                        + " target=" + targetPackage + " action=" + intent.getAction());
                return true;
            }
            if (shouldBypassToPhysicalDisplay(intent, targetPackage)) {
                return false;
            }
            boolean routed = displayBridge.routeCrossAppLaunch(
                    source.displayId, source.packageName, new Intent(intent), targetPackage);
            if (routed) {
                Log.i(TAG, "intercepted cross-app launch: source=" + source.packageName
                        + " display=" + source.displayId + " target=" + targetPackage
                        + " action=" + intent.getAction()
                        + " component=" + intent.getComponent());
            }
            return !routed;
        } catch (RuntimeException e) {
            Log.w(TAG, "cross-app launch inspection failed: "
                    + e.getClass().getSimpleName());
            return true;
        }
    }

    private static int readTransaction(Class<?> stubClass, String fieldName)
            throws ReflectiveOperationException {
        Field field = stubClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

}
