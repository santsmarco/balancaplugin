import 'dart:async';
import 'package:flutter/services.dart';

class BalancePlugin {
  static const MethodChannel _channel = MethodChannel('com.balance/channel');
  static const EventChannel _eventChannel = EventChannel('com.balance/events');

  static StreamController<Map<String, dynamic>>? _eventController;
  static Stream<Map<String, dynamic>>? _eventStream;
  static StreamSubscription? _eventSubscription;

  /// Stream de eventos completos da balança
  static Stream<Map<String, dynamic>> get balanceEventStream {
    if (_eventStream == null) {
      _eventController = StreamController<Map<String, dynamic>>.broadcast();

      _eventStream = _eventChannel.receiveBroadcastStream().map((event) {
        return Map<String, dynamic>.from(event as Map);
      });

      _eventSubscription = _eventStream!.listen(
            (event) => _eventController?.add(event),
        onError: (error) => print("Erro no stream: $error"),
      );
    }
    return _eventController!.stream;
  }

  /// Stream apenas do peso
  static Stream<String> get weightStream {
    return balanceEventStream.map((event) {
      if (event['success'] == true) {
        return event['weight']?.toString() ?? '0.000';
      }
      return '0.000';
    });
  }

  /// Abre tela de configuração da balança
  static Future<void> openConfigBalance() async {
    await _channel.invokeMethod('openConfigBalance');
  }

  /// Inicia leitura do peso
  static Future<String> readWeight() async {
    final result = await _channel.invokeMethod('readWeight');
    return result.toString();
  }

  /// Cancela job de leitura
  static Future<void> cancelJobBalance() async {
    await _channel.invokeMethod('cancelJobBalance');
  }

  /// Verifica se tem configurações salvas
  static Future<bool> hasSavedConfigurations() async {
    final result = await _channel.invokeMethod('hasSavedConfigurations');
    return result as bool;
  }

  /// Retorna tipo de conexão (Serial/Bluetooth)
  static Future<String> getConnectionType() async {
    final result = await _channel.invokeMethod('getConnectionType');
    return result.toString();
  }

  /// Verifica se leitura contínua está ativa
  static Future<bool> isContinuousReadingEnabled() async {
    final result = await _channel.invokeMethod('isContinuousReadingEnabled');
    return result as bool;
  }

  /// Libera recursos
  static void dispose() {
    _eventSubscription?.cancel();
    _eventController?.close();
    _eventController = null;
    _eventStream = null;
  }
}