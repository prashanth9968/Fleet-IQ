import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  Function(String?)? onNotificationTapped;

  Future<void> initialize() async {
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const settings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _plugin.initialize(
      settings,
      onDidReceiveNotificationResponse: (NotificationResponse response) {
        onNotificationTapped?.call(response.payload);
      },
    );
  }

  Future<void> showAlert({
    required int id,
    required String title,
    required String body,
    String? payload,
  }) async {
    const androidDetails = AndroidNotificationDetails(
      'fleetiq_alerts',
      'FleetIQ Alerts',
      channelDescription: 'Critical fleet alerts and notifications',
      importance: Importance.max,
      priority: Priority.high,
      showWhen: true,
    );

    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );

    const details = NotificationDetails(
      android: androidDetails,
      iOS: iosDetails,
    );

    await _plugin.show(id, title, body, details, payload: payload);
  }

  Future<void> showFuelTheftAlert(String vehicleId) async {
    await showAlert(
      id: vehicleId.hashCode,
      title: '🚨 Fuel Theft Suspected',
      body: 'Stationary fuel level drop detected on vehicle $vehicleId',
      payload: 'fuel_theft:$vehicleId',
    );
  }

  Future<void> showOverheatAlert(String vehicleId) async {
    await showAlert(
      id: vehicleId.hashCode + 1,
      title: '🔥 Engine Overheat',
      body: 'Coolant temperature critical on vehicle $vehicleId',
      payload: 'overheat:$vehicleId',
    );
  }

  Future<void> showMaintenanceDueAlert(String vehicleId) async {
    await showAlert(
      id: vehicleId.hashCode + 2,
      title: '🔧 Maintenance Due',
      body: 'Scheduled service is due for vehicle $vehicleId',
      payload: 'maintenance:$vehicleId',
    );
  }

  Future<void> showDriverAssignment(String driverName, String vehicleId) async {
    await showAlert(
      id: driverName.hashCode,
      title: '🚛 New Assignment',
      body: '$driverName has been assigned to vehicle $vehicleId',
      payload: 'assignment:$vehicleId',
    );
  }

  Future<void> showSosAlert(String driverName) async {
    await showAlert(
      id: driverName.hashCode + 100,
      title: '🆘 Emergency SOS',
      body: 'Driver $driverName has triggered an emergency SOS',
      payload: 'sos:$driverName',
    );
  }
}
