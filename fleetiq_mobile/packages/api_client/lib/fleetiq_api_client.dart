import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:fleetiq_models/fleetiq_models.dart';
import 'package:fleetiq_shared/fleetiq_shared.dart';

class FleetApiClient {
  final Dio dio;
  final SharedPreferences sharedPreferences;
  late final MockSecureStorageHelper _secureStorage;

  FleetApiClient({
    required this.dio,
    required this.sharedPreferences,
    required String baseUrl,
    required String tenantId,
  }) {
    _secureStorage = MockSecureStorageHelper(sharedPreferences);
    dio.options.baseUrl = baseUrl;
    dio.options.connectTimeout = const Duration(seconds: 10);
    dio.options.receiveTimeout = const Duration(seconds: 10);

    // Add interceptors for headers and tracing
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // 1. Inject Tenant ID
          options.headers['X-Tenant-ID'] = tenantId;

          // 2. Inject Auth Token if it exists
          final token = await _secureStorage.read(key: 'auth_token');
          if (token != null && token.isNotEmpty) {
            options.headers['Authorization'] = 'Bearer $token';
          }

          FleetLogger.info('API Request: [${options.method}] ${options.path}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          FleetLogger.info('API Response: [${response.statusCode}] ${response.requestOptions.path}');
          return handler.next(response);
        },
        onError: (DioException e, handler) {
          FleetLogger.error(
            'API Error: [${e.response?.statusCode}] ${e.requestOptions.path}',
            error: e,
            stackTrace: e.stackTrace,
          );
          return handler.next(e);
        },
      ),
    );
  }

  // API Methods
  Future<List<Vehicle>> getVehicles() async {
    try {
      final response = await dio.get('/vehicles');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => Vehicle.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Invalid response format for getVehicles',
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<List<Trip>> getTrips() async {
    try {
      final response = await dio.get('/trips');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => Trip.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Invalid response format for getTrips',
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<DriverScore> getDriverScore(String driverId) async {
    try {
      final response = await dio.get('/drivers/$driverId/score');
      if (response.data is Map) {
        return DriverScore.fromJson(response.data as Map<String, dynamic>);
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Invalid response format for getDriverScore',
      );
    } catch (e) {
      rethrow;
    }
  }

  Future<List<AlertHistory>> getAlerts() async {
    try {
      final response = await dio.get('/alerts');
      if (response.data is List) {
        return (response.data as List)
            .map((json) => AlertHistory.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw DioException(
        requestOptions: response.requestOptions,
        response: response,
        message: 'Invalid response format for getAlerts',
      );
    } catch (e) {
      rethrow;
    }
  }

  // Submit Emergency SOS
  Future<void> submitSosAlert({required String vehicleId, required String message}) async {
    try {
      await dio.post('/alerts/sos', data: {
        'vehicleId': vehicleId,
        'message': message,
        'timestamp': DateTime.now().toIso8601String(),
        'severity': 'CRITICAL',
      });
    } catch (e) {
      rethrow;
    }
  }

  // Submit Inspection
  Future<void> submitInspection({
    required String vehicleId,
    required bool brakeOk,
    required bool tiresOk,
    required bool oilOk,
    required bool lightsOk,
    required DateTime timestamp,
  }) async {
    try {
      await dio.post('/inspections', data: {
        'vehicleId': vehicleId,
        'brakeOk': brakeOk,
        'tiresOk': tiresOk,
        'oilOk': oilOk,
        'lightsOk': lightsOk,
        'timestamp': timestamp.toIso8601String(),
      });
    } catch (e) {
      rethrow;
    }
  }

  // Auth Helpers
  Future<void> login(String username, String password) async {
    try {
      // Simulation of auth response
      final response = await dio.post('/auth/login', data: {
        'username': username,
        'password': password,
      });
      final token = response.data['token'] as String;
      await _secureStorage.write(key: 'auth_token', value: token);
    } catch (e) {
      rethrow;
    }
  }

  Future<void> logout() async {
    await _secureStorage.delete(key: 'auth_token');
  }
}
