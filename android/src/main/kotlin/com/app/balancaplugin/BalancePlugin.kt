package com.app.balancaplugin

import android.content.BroadcastReceiver
import android.hardware.usb.UsbDevice
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import balanca.services.interfaces.BalancaCallback
import balanca.services.interfaces.BalancaUiCallback
import balanca.services.objects.BalanceChannelHandler
import balanca.services.objects.BalanceEventHandler
import balanca.services.objects.UsbDeviceManager

class BalancePlugin: FlutterPlugin, ActivityAware, BalancaCallback, BalancaUiCallback {
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var usbReceiver: BroadcastReceiver? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel = MethodChannel(binding.binaryMessenger, "com.balance/channel")
        eventChannel = EventChannel(binding.binaryMessenger, "com.balance/events")

        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                eventSink = events
                activityBinding?.activity?.let { activity ->
                    BalanceEventHandler.setEventSink(events, activity)
                }
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
                BalanceEventHandler.setEventSink(null, null)
            }
        })
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        val activity = binding.activity

        methodChannel.setMethodCallHandler { call, result ->
            BalanceChannelHandler.handleMethodCall(
                call,
                result,
                activity,
                BalanceEventHandler
            ) {
                BalanceEventHandler.clearEventSink()
            }
        }

        usbReceiver = UsbDeviceManager.createUsbReceiver(
            activity,
            eventSink,
            BalanceEventHandler
        )
        UsbDeviceManager.registerReceiver(activity, usbReceiver!!)

        BalanceEventHandler.setEventSink(eventSink, activity)
    }

    override fun onDetachedFromActivity() {
        activityBinding?.activity?.let {
            UsbDeviceManager.unregisterReceiver(it)
        }
        BalanceEventHandler.setEventSink(null, null)
        activityBinding = null
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onWeightRead(weight: String?) {}
    override fun onError(error: String) {}
    override fun onShowPortSelection() {
        activityBinding?.activity?.let { activity ->
            activity.runOnUiThread {
                UsbDeviceManager.showPortSelectionDialog(activity, BalanceEventHandler)
            }
        }
    }
    override fun onDeviceNotConfigured() {}
    override fun onPermissionRequired(device: UsbDevice) {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }
}