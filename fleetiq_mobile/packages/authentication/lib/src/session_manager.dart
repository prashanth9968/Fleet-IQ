import 'package:shared_preferences/shared_preferences.dart';

class SessionManager {
  final SharedPreferences prefs;

  static const _lastActiveKey = 'fleetiq_last_active';
  static const _sessionTimeoutMinutes = 30;

  SessionManager({required this.prefs});

  void recordActivity() {
    prefs.setString(_lastActiveKey, DateTime.now().toIso8601String());
  }

  bool isSessionExpired() {
    final lastActive = prefs.getString(_lastActiveKey);
    if (lastActive == null) return true;

    final lastTime = DateTime.parse(lastActive);
    final diff = DateTime.now().difference(lastTime);
    return diff.inMinutes > _sessionTimeoutMinutes;
  }

  Future<void> clearSession() async {
    await prefs.remove(_lastActiveKey);
  }
}
