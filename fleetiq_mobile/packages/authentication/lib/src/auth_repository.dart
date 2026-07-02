import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthRepository {
  final Dio dio;
  final SharedPreferences prefs;

  static const _tokenKey = 'fleetiq_auth_token';
  static const _refreshTokenKey = 'fleetiq_refresh_token';
  static const _tenantKey = 'fleetiq_tenant_id';
  static const _userKey = 'fleetiq_user_email';

  AuthRepository({required this.dio, required this.prefs});

  Future<Map<String, dynamic>> login(String email, String password, String tenantSlug) async {
    try {
      final response = await dio.post('/auth/login', data: {
        'email': email,
        'password': password,
        'tenantSlug': tenantSlug,
      });

      final data = response.data as Map<String, dynamic>;
      final token = data['token'] as String;
      final refreshToken = data['refreshToken'] as String? ?? '';
      final tenantId = data['tenantId'] as String? ?? '';

      await prefs.setString(_tokenKey, token);
      await prefs.setString(_refreshTokenKey, refreshToken);
      await prefs.setString(_tenantKey, tenantId);
      await prefs.setString(_userKey, email);

      return data;
    } catch (e) {
      rethrow;
    }
  }

  Future<String?> refreshToken() async {
    final currentRefresh = prefs.getString(_refreshTokenKey);
    if (currentRefresh == null || currentRefresh.isEmpty) return null;

    try {
      final response = await dio.post('/auth/refresh', data: {
        'refreshToken': currentRefresh,
      });
      final newToken = response.data['token'] as String;
      await prefs.setString(_tokenKey, newToken);
      return newToken;
    } catch (e) {
      await logout();
      return null;
    }
  }

  Future<void> logout() async {
    await prefs.remove(_tokenKey);
    await prefs.remove(_refreshTokenKey);
    await prefs.remove(_tenantKey);
    await prefs.remove(_userKey);
  }

  String? get currentToken => prefs.getString(_tokenKey);
  String? get currentTenantId => prefs.getString(_tenantKey);
  String? get currentUserEmail => prefs.getString(_userKey);
  bool get isAuthenticated => currentToken != null && currentToken!.isNotEmpty;
}
