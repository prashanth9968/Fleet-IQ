import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FleetValidators {
  static String? validateEmail(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Email is required';
    }
    final emailRegex = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
    if (!emailRegex.hasMatch(value)) {
      return 'Please enter a valid email address';
    }
    return null;
  }

  static String? validateLicensePlate(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'License plate is required';
    }
    if (value.trim().length < 3) {
      return 'License plate must be at least 3 characters';
    }
    return null;
  }

  static String? validateNotEmpty(String? value, String fieldName) {
    if (value == null || value.trim().isEmpty) {
      return '$fieldName is required';
    }
    return null;
  }
}

class FleetLogger {
  static void log(String message, {String level = 'INFO', Object? error, StackTrace? stackTrace}) {
    final timestamp = DateTime.now().toIso8601String();
    final logMessage = '[$timestamp] [$level] - $message';
    if (kDebugMode) {
      print(logMessage);
      if (error != null) print('Error: $error');
      if (stackTrace != null) print('StackTrace: $stackTrace');
    }
  }

  static void info(String message) => log(message, level: 'INFO');
  static void warn(String message) => log(message, level: 'WARNING');
  static void error(String message, {Object? error, StackTrace? stackTrace}) => 
      log(message, level: 'ERROR', error: error, stackTrace: stackTrace);
}

class MockSecureStorageHelper {
  final SharedPreferences _prefs;
  static const String _prefix = 'secure_';

  MockSecureStorageHelper(this._prefs);

  Future<void> write({required String key, required String value}) async {
    // Basic base64 or URI encoding mock for security simulation
    final encodedValue = Uri.encodeComponent(value);
    await _prefs.setString('$_prefix$key', encodedValue);
  }

  Future<String?> read({required String key}) async {
    final value = _prefs.getString('$_prefix$key');
    if (value == null) return null;
    try {
      return Uri.decodeComponent(value);
    } catch (_) {
      return null;
    }
  }

  Future<void> delete({required String key}) async {
    await _prefs.remove('$_prefix$key');
  }

  Future<bool> containsKey({required String key}) async {
    return _prefs.containsKey('$_prefix$key');
  }
}
